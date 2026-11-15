/*
 * DraftPeek 真 PTY 桥。
 *
 * 通过 openpty + fork + setsid + TIOCSCTTY 为终端会话分配独立伪终端，
 * 使 shell / proot 看到真实的 tty（isatty、stty、SIGWINCH、Ctrl-C 均生效）。
 *
 * 设计约束：
 * - 子进程在 fork 后只调用 async-signal-safe 的 setsid/ioctl/dup2/close/chdir/execve；
 * - master fd 由 Kotlin 侧持有并保证只 close 一次，避免 fd 复用后误关；
 * - 读/写走 JNI 原始字节，不经过 Java 流，避免 FileInputStream 清理器二次 close。
 *
 * 作者：DraftPeek Team
 * 起始版本：1.0.0
 */

#include <jni.h>
#include <android/log.h>
#include <errno.h>
#include <fcntl.h>
#include <pty.h>
#include <signal.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <termios.h>
#include <unistd.h>

#define LOG_TAG "TerminalPtyNative"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

/** 单次 read 允许的最大字节数，避免 JNI 侧长期 pin 住 Java 数组。 */
#define MAX_READ_CHUNK 8192

/** 单次 write 允许的最大字节数，避免 JNI 侧一次拷贝过大。 */
#define MAX_WRITE_CHUNK 8192

/**
 * 将 Java 字符串转成 malloc 分配的 C 字符串；失败返回 NULL。
 * 调用方负责 free。
 */
static char *copy_jstring(JNIEnv *env, jstring value) {
    if (value == NULL) {
        return NULL;
    }
    const char *chars = (*env)->GetStringUTFChars(env, value, NULL);
    if (chars == NULL) {
        return NULL;
    }
    size_t length = strlen(chars);
    char *copy = (char *) malloc(length + 1);
    if (copy != NULL) {
        memcpy(copy, chars, length + 1);
    }
    (*env)->ReleaseStringUTFChars(env, value, chars);
    return copy;
}

/**
 * 将 Java String[] 转成以 NULL 结尾的 char**；任何一项分配失败都返回 NULL。
 * 调用方负责 free_string_array。
 */
static char **copy_jstring_array(JNIEnv *env, jobjectArray array) {
    if (array == NULL) {
        return NULL;
    }
    jsize length = (*env)->GetArrayLength(env, array);
    char **result = (char **) calloc((size_t) length + 1, sizeof(char *));
    if (result == NULL) {
        return NULL;
    }
    for (jsize i = 0; i < length; i++) {
        jstring item = (jstring) (*env)->GetObjectArrayElement(env, array, i);
        result[i] = copy_jstring(env, item);
        (*env)->DeleteLocalRef(env, item);
        if (result[i] == NULL) {
            for (jsize j = 0; j < i; j++) {
                free(result[j]);
            }
            free(result);
            return NULL;
        }
    }
    result[length] = NULL;
    return result;
}

/** 释放 copy_jstring_array 的返回值。 */
static void free_string_array(char **array) {
    if (array == NULL) {
        return;
    }
    for (int i = 0; array[i] != NULL; i++) {
        free(array[i]);
    }
    free(array);
}

/** 抛出 java/io/IOException。 */
static void throw_io_error(JNIEnv *env, const char *message) {
    jclass clazz = (*env)->FindClass(env, "java/io/IOException");
    if (clazz != NULL) {
        (*env)->ThrowNew(env, clazz, message);
    }
}

/** 设置 FD_CLOEXEC，避免 master fd 泄漏到后续 exec 的子进程。 */
static int set_close_on_exec(int fd) {
    int flags = fcntl(fd, F_GETFD);
    if (flags < 0) {
        return -1;
    }
    return fcntl(fd, F_SETFD, flags | FD_CLOEXEC);
}

/**
 * 子进程登录 tty：setsid 建立新会话，TIOCSCTTY 把 slave 设为控制终端。
 * 某些内核要求 arg=1，失败时再尝试一次。
 *
 * 注意：fork 之后子进程只允许 async-signal-safe 调用，因此这里不做日志、
 * 不做堆分配，失败也静默继续（exec 失败会由 127 退出码暴露）。
 */
static void child_login_tty(int slave_fd) {
    if (setsid() < 0) {
        return;
    }
    if (ioctl(slave_fd, TIOCSCTTY, 0) < 0) {
        ioctl(slave_fd, TIOCSCTTY, 1);
    }
}

/**
 * 子进程 exec：优先绝对路径；相对路径时按 PATH 手工查找，
 * 避免依赖 execvpe 在 bionic 上的环境语义差异。
 */
static void child_exec(char **argv, char **envp) {
    if (argv[0] == NULL || argv[0][0] == '\0') {
        _exit(127);
    }

    if (strchr(argv[0], '/') != NULL) {
        execve(argv[0], argv, envp);
        _exit(127);
    }

    const char *path = "/system/bin:/system/xbin";
    for (char **item = envp; item != NULL && *item != NULL; item++) {
        if (strncmp(*item, "PATH=", 5) == 0) {
            path = *item + 5;
            break;
        }
    }

    char candidate[4096];
    const char *segment = path;
    while (segment != NULL && *segment != '\0') {
        const char *colon = strchr(segment, ':');
        size_t segment_length = (colon == NULL) ? strlen(segment) : (size_t) (colon - segment);
        size_t total = segment_length + 1 + strlen(argv[0]) + 1;
        if (total <= sizeof(candidate)) {
            memcpy(candidate, segment, segment_length);
            candidate[segment_length] = '/';
            memcpy(candidate + segment_length + 1, argv[0], strlen(argv[0]) + 1);
            execve(candidate, argv, envp);
            if (errno != ENOENT && errno != ENOTDIR) {
                break;
            }
        }
        segment = (colon == NULL) ? NULL : colon + 1;
    }
    _exit(127);
}

/**
 * JNI：创建 PTY 并 fork 子进程。
 *
 * 返回 int[2] = {masterFd, pid}；失败时抛出 IOException 并返回 NULL。
 */
JNIEXPORT jintArray JNICALL
Java_com_draftpeek_feature_terminal_emulator_TerminalPtyNative_createPty(
        JNIEnv *env, jobject thiz, jobjectArray argv_array, jstring cwd_string,
        jobjectArray env_array, jint rows, jint columns) {
    (void) thiz;

    char **argv = copy_jstring_array(env, argv_array);
    char **envp = copy_jstring_array(env, env_array);
    char *cwd = copy_jstring(env, cwd_string);

    if (argv == NULL || argv[0] == NULL || envp == NULL) {
        free_string_array(argv);
        free_string_array(envp);
        free(cwd);
        throw_io_error(env, "PTY 参数分配失败");
        return NULL;
    }

    struct winsize window;
    memset(&window, 0, sizeof(window));
    window.ws_row = (unsigned short) (rows > 0 ? rows : 24);
    window.ws_col = (unsigned short) (columns > 0 ? columns : 80);

    int master_fd = -1;
    int slave_fd = -1;
    if (openpty(&master_fd, &slave_fd, NULL, NULL, &window) < 0) {
        int saved_errno = errno;
        free_string_array(argv);
        free_string_array(envp);
        free(cwd);
        char message[128];
        snprintf(message, sizeof(message), "openpty 失败: errno=%d", saved_errno);
        throw_io_error(env, message);
        return NULL;
    }

    pid_t pid = fork();
    if (pid < 0) {
        int saved_errno = errno;
        close(master_fd);
        close(slave_fd);
        free_string_array(argv);
        free_string_array(envp);
        free(cwd);
        char message[128];
        snprintf(message, sizeof(message), "fork 失败: errno=%d", saved_errno);
        throw_io_error(env, message);
        return NULL;
    }

    if (pid == 0) {
        /* 子进程：只做 async-signal-safe 调用，禁止触碰 JNIEnv / 堆分配。 */
        close(master_fd);
        child_login_tty(slave_fd);

        /* 清空继承自 JVM 的信号屏蔽字，避免 Ctrl-C/SIGWINCH 被吞。 */
        sigset_t empty;
        sigemptyset(&empty);
        sigprocmask(SIG_SETMASK, &empty, NULL);
        signal(SIGCHLD, SIG_DFL);
        signal(SIGPIPE, SIG_DFL);
        signal(SIGHUP, SIG_DFL);
        signal(SIGINT, SIG_DFL);
        signal(SIGQUIT, SIG_DFL);
        signal(SIGTERM, SIG_DFL);

        if (dup2(slave_fd, STDIN_FILENO) < 0 ||
            dup2(slave_fd, STDOUT_FILENO) < 0 ||
            dup2(slave_fd, STDERR_FILENO) < 0) {
            _exit(126);
        }
        if (slave_fd > STDERR_FILENO) {
            close(slave_fd);
        }

        if (cwd != NULL && cwd[0] != '\0') {
            chdir(cwd);
        }

        child_exec(argv, envp);
        _exit(127);
    }

    /* 父进程：关闭 slave，master 设 CLOEXEC。 */
    close(slave_fd);
    if (set_close_on_exec(master_fd) < 0) {
        LOGW("设置 master fd CLOEXEC 失败: errno=%d", errno);
    }

    free_string_array(argv);
    free_string_array(envp);
    free(cwd);

    jint values[2] = {(jint) master_fd, (jint) pid};
    jintArray result = (*env)->NewIntArray(env, 2);
    if (result == NULL) {
        /* OOM：清理子进程，避免泄漏。 */
        close(master_fd);
        kill(pid, SIGKILL);
        waitpid(pid, NULL, 0);
        throw_io_error(env, "无法分配 PTY 结果数组");
        return NULL;
    }
    (*env)->SetIntArrayRegion(env, result, 0, 2, values);
    LOGD("PTY 已创建 masterFd=%d pid=%d size=%dx%d", master_fd, pid, columns, rows);
    return result;
}

/**
 * JNI：通过 TIOCSWINSZ 调整窗口大小；内核会向前台进程组发送 SIGWINCH。
 */
JNIEXPORT jboolean JNICALL
Java_com_draftpeek_feature_terminal_emulator_TerminalPtyNative_resize(
        JNIEnv *env, jobject thiz, jint master_fd, jint rows, jint columns) {
    (void) env;
    (void) thiz;

    if (master_fd < 0 || rows <= 0 || columns <= 0) {
        return JNI_FALSE;
    }
    struct winsize window;
    memset(&window, 0, sizeof(window));
    window.ws_row = (unsigned short) rows;
    window.ws_col = (unsigned short) columns;
    return ioctl(master_fd, TIOCSWINSZ, &window) == 0 ? JNI_TRUE : JNI_FALSE;
}

/**
 * JNI：阻塞 waitpid，返回退出码；被信号杀死返回 128 + signal。
 */
JNIEXPORT jint JNICALL
Java_com_draftpeek_feature_terminal_emulator_TerminalPtyNative_waitFor(
        JNIEnv *env, jobject thiz, jint pid) {
    (void) env;
    (void) thiz;

    if (pid <= 0) {
        return -1;
    }
    int status = 0;
    while (waitpid((pid_t) pid, &status, 0) < 0) {
        if (errno == EINTR) {
            continue;
        }
        LOGW("waitpid(%d) 失败: errno=%d", pid, errno);
        return -1;
    }
    if (WIFEXITED(status)) {
        return WEXITSTATUS(status);
    }
    if (WIFSIGNALED(status)) {
        return 128 + WTERMSIG(status);
    }
    return -1;
}

/**
 * JNI：关闭 master fd。Kotlin 侧用 AtomicBoolean 保证只调用一次。
 */
JNIEXPORT void JNICALL
Java_com_draftpeek_feature_terminal_emulator_TerminalPtyNative_closeFd(
        JNIEnv *env, jobject thiz, jint fd) {
    (void) env;
    (void) thiz;

    if (fd >= 0) {
        close((int) fd);
    }
}

/**
 * JNI：阻塞读 master fd，返回字节数；失败返回 -errno（EIO=5 表示子进程已退出）。
 */
JNIEXPORT jint JNICALL
Java_com_draftpeek_feature_terminal_emulator_TerminalPtyNative_readFd(
        JNIEnv *env, jobject thiz, jint fd, jbyteArray buffer, jint offset, jint length) {
    (void) thiz;

    if (fd < 0 || buffer == NULL || offset < 0 || length <= 0) {
        return -EINVAL;
    }
    jsize capacity = (*env)->GetArrayLength(env, buffer);
    if (offset + length > capacity) {
        return -EINVAL;
    }
    int chunk = length > MAX_READ_CHUNK ? MAX_READ_CHUNK : length;

    /*
     * 先读进 native 栈缓冲，再 SetByteArrayRegion：
     * read 可能阻塞数秒，不能把 Java 数组 pin 住（会阻塞 GC / 移动）。
     */
    jbyte local[MAX_READ_CHUNK];
    ssize_t count;
    do {
        count = read((int) fd, local, (size_t) chunk);
    } while (count < 0 && errno == EINTR);

    if (count > 0) {
        (*env)->SetByteArrayRegion(env, buffer, offset, (jsize) count, local);
    }
    return (count < 0) ? -errno : (int) count;
}

/**
 * JNI：写 master fd，返回写入字节数；失败返回 -errno。
 */
JNIEXPORT jint JNICALL
Java_com_draftpeek_feature_terminal_emulator_TerminalPtyNative_writeFd(
        JNIEnv *env, jobject thiz, jint fd, jbyteArray buffer, jint offset, jint length) {
    (void) thiz;

    if (fd < 0 || buffer == NULL || offset < 0 || length <= 0) {
        return -EINVAL;
    }
    jsize capacity = (*env)->GetArrayLength(env, buffer);
    if (offset + length > capacity) {
        return -EINVAL;
    }
    int chunk = length > MAX_WRITE_CHUNK ? MAX_WRITE_CHUNK : length;

    jbyte local[MAX_WRITE_CHUNK];
    (*env)->GetByteArrayRegion(env, buffer, offset, chunk, local);
    ssize_t count;
    do {
        count = write((int) fd, local, (size_t) chunk);
    } while (count < 0 && errno == EINTR);
    return (count < 0) ? -errno : (int) count;
}

/**
 * JNI：向子进程所在进程组发信号（pid 为 setsid 后的会话首进程，pgid == pid）。
 */
JNIEXPORT jboolean JNICALL
Java_com_draftpeek_feature_terminal_emulator_TerminalPtyNative_signalProcessGroup(
        JNIEnv *env, jobject thiz, jint pid, jint signal_number) {
    (void) env;
    (void) thiz;

    if (pid <= 0 || signal_number <= 0) {
        return JNI_FALSE;
    }
    if (kill(-(pid_t) pid, signal_number) == 0) {
        return JNI_TRUE;
    }
    /*
     * setsid 理论上不会失败；若失败则 pgid != pid，进程组 kill 会 ESRCH。
     * 此时退化为单进程 kill，保证 destroy 不会留下无法回收的子进程。
     */
    LOGW("kill(-%d, %d) 失败，退化到单进程 kill: errno=%d", pid, signal_number, errno);
    return kill((pid_t) pid, signal_number) == 0 ? JNI_TRUE : JNI_FALSE;
}
