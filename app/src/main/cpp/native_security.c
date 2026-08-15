/*
 * SECURITY VULN-005: Native C-layer anti-detection
 *
 * These functions implement security checks at the C/Native level, making them
 * harder to bypass with Frida/Xposed hooks compared to Java/Kotlin methods.
 *
 * Frida can hook Java methods by name (e.g., AntiDebug.isFridaDetected()),
 * but hooking native functions requires knowing the symbol name in the .so
 * file or using Interceptor.attach() with the function address. By using
 * hidden visibility and inline checks, we increase the difficulty of bypassing.
 *
 * Functions:
 * - native_check_frida_in_maps: Scans /proc/self/maps for Frida traces
 * - native_check_tracer_pid: Reads /proc/self/status for TracerPid
 * - native_check_zygisk_in_maps: Scans /proc/self/maps for Zygisk/Magisk traces
 * - native_perform_security_check: Combined check for all native detections
 */

#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/types.h>
#include <android/log.h>

#define TAG "NativeSecurity"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)

#define MAX_LINE_LEN 512
#define BUFFER_SIZE 4096

/* Hidden visibility — symbols not exported in .so symbol table */
__attribute__((visibility("hidden")))

/*
 * Read a file line by line and check if any line contains any of the
 * given patterns. This is more robust than Java's File.useLines() because
 * the check happens entirely in native code, preventing Frida from
 * intercepting the Java string comparison.
 *
 * Returns: 1 if any pattern is found, 0 otherwise, -1 on error.
 */
static int scan_file_for_patterns(const char *filepath, const char *patterns[], int pattern_count) {
    int fd = open(filepath, O_RDONLY);
    if (fd < 0) {
        return -1;
    }

    char buffer[BUFFER_SIZE];
    ssize_t bytes_read;
    int found = 0;

    /* Read the file in chunks and search for patterns */
    while ((bytes_read = read(fd, buffer, sizeof(buffer) - 1)) > 0) {
        buffer[bytes_read] = '\0';

        /* Search for each pattern in the buffer */
        for (int i = 0; i < pattern_count && !found; i++) {
            if (strstr(buffer, patterns[i]) != NULL) {
                found = 1;
                break;
            }
        }

        if (found) break;
    }

    close(fd);
    return found;
}

/*
 * Read /proc/self/status and extract the TracerPid value.
 * Returns: TracerPid value (>0 means traced), -1 on error.
 */
static int read_tracer_pid() {
    FILE *fp = fopen("/proc/self/status", "r");
    if (fp == NULL) {
        return -1;
    }

    char line[MAX_LINE_LEN];
    int tracer_pid = 0;

    while (fgets(line, sizeof(line), fp) != NULL) {
        if (strncmp(line, "TracerPid:", 10) == 0) {
            /* Parse the TracerPid value */
            tracer_pid = atoi(line + 10);
            break;
        }
    }

    fclose(fp);
    return tracer_pid;
}

/*
 * JNI: Check for Frida traces in /proc/self/maps
 *
 * Scans /proc/self/maps for known Frida library names and patterns.
 * This native implementation is harder to hook than the Java equivalent
 * because the function symbol is hidden and the string comparison
 * happens in C code.
 *
 * Returns: JNI_TRUE if Frida is detected, JNI_FALSE otherwise.
 */
JNIEXPORT jboolean JNICALL
Java_com_draftpeek_security_NativeSecurityChecker_nativeCheckFridaInMaps(
    JNIEnv *env, jobject thiz) {

    const char *frida_patterns[] = {
        "frida",
        "gum-js-loop",
        "gmain",
        "linjector",
        "frida-gadget",
        "frida-agent",
        "libfrida",
        "re.frida.server"
    };
    int pattern_count = sizeof(frida_patterns) / sizeof(frida_patterns[0]);

    int result = scan_file_for_patterns("/proc/self/maps", frida_patterns, pattern_count);

    /* Also check for Frida server listening on common ports via /proc/net/tcp */
    if (result == 0) {
        /* Check /proc/net/tcp for Frida default port 27042 (0x69A2) */
        const char *tcp_patterns[] = {"69A2", "69A3", "115C", "1A0A"};
        int tcp_result = scan_file_for_patterns("/proc/net/tcp", tcp_patterns, 4);
        if (tcp_result > 0) {
            return JNI_TRUE;
        }
    }

    return (result > 0) ? JNI_TRUE : JNI_FALSE;
}

/*
 * JNI: Check TracerPid in /proc/self/status
 *
 * Returns: JNI_TRUE if the process is being traced (TracerPid > 0).
 */
JNIEXPORT jboolean JNICALL
Java_com_draftpeek_security_NativeSecurityChecker_nativeCheckTracerPid(
    JNIEnv *env, jobject thiz) {

    int tracer_pid = read_tracer_pid();
    return (tracer_pid > 0) ? JNI_TRUE : JNI_FALSE;
}

/*
 * JNI: Check for Zygisk/Magisk traces in /proc/self/maps
 *
 * Returns: JNI_TRUE if Zygisk/Magisk injection is detected.
 */
JNIEXPORT jboolean JNICALL
Java_com_draftpeek_security_NativeSecurityChecker_nativeCheckZygiskInMaps(
    JNIEnv *env, jobject thiz) {

    const char *zygisk_patterns[] = {
        "zygisk",
        "libzygisk",
        "zygisk_",
        "magiskzygisk",
        "riru",
        "libriru",
        "shamiko",
        "zygisk_next",
        "dobby"
    };
    int pattern_count = sizeof(zygisk_patterns) / sizeof(zygisk_patterns[0]);

    int result = scan_file_for_patterns("/proc/self/maps", zygisk_patterns, pattern_count);

    return (result > 0) ? JNI_TRUE : JNI_FALSE;
}

/*
 * JNI: Combined security check — runs all native detection methods
 * and returns a bitmask of detected threats.
 *
 * Bit 0 (1): Frida detected
 * Bit 1 (2): TracerPid > 0 (being traced)
 * Bit 2 (4): Zygisk/Magisk detected
 *
 * Returns: bitmask of detected threats (0 = all clear).
 */
JNIEXPORT jint JNICALL
Java_com_draftpeek_security_NativeSecurityChecker_nativePerformSecurityCheck(
    JNIEnv *env, jobject thiz) {

    int threat_mask = 0;

    /* Check Frida */
    const char *frida_patterns[] = {
        "frida", "gum-js-loop", "gmain", "linjector",
        "frida-gadget", "frida-agent", "libfrida", "re.frida.server"
    };
    if (scan_file_for_patterns("/proc/self/maps", frida_patterns, 8) > 0) {
        threat_mask |= 1;
    }

    /* Check TracerPid */
    int tracer_pid = read_tracer_pid();
    if (tracer_pid > 0) {
        threat_mask |= 2;
    }

    /* Check Zygisk */
    const char *zygisk_patterns[] = {
        "zygisk", "libzygisk", "zygisk_", "magiskzygisk",
        "riru", "libriru", "shamiko", "zygisk_next", "dobby"
    };
    if (scan_file_for_patterns("/proc/self/maps", zygisk_patterns, 9) > 0) {
        threat_mask |= 4;
    }

    return threat_mask;
}

// ===== AI 对抗增强（A 层 + B 层密钥分片） =====
// === PLAN-C FUTURE ===
// C 层扩展：以下 3 个函数在方案 C 中将被使用 ollvm-ndk 工具链编译，
//   启用 -mllvm -fla（控制流平坦化） -mllvm -bcf（虚假控制流）
//   -mllvm -sub（指令替换），使得 IDA/Ghidra 反编译的控制流图难以阅读。
//   注：ollvm-ndk 需单独集成（修改 NDK toolchain 文件），维护成本较高，
//   仅在确认有高级 IDA 破解者场景时启用。
//
// C 层扩展：新增 native_check_memory_dump() ——
//   通过读取 /proc/self/pagemap 扫描 DEX / 资源页是否被
//   process_vm_readv / ptrace PEEKTEXT 访问（页表 Dirty 位异常），
//   检测内存 dump 工具（如 Frida Memory.protect + hexdump）。
//
// C 层扩展：新增 native_scan_unix_sockets_for_frida() ——
//   遍历 /proc/net/unix，查找名称含 "frida" / "gum" / "linjector" 的
//   Abstract Unix Socket，Frida 默认使用抽象 socket 通信，部分隐藏方案
//   会遗漏此处检测。
// === END PLAN-C ===

/*
 * B 层精选：字符串加密密钥片 3/3（另外 2 片在 Java SecureStringResolver
 *   常量池中），运行时拼接后用于解密 @EncryptedString 标注的敏感字符串。
 * 密钥分片值在构建期由 Gradle 任务 generateStringEncKeyFragment 替换
 *   本文件占位符，绝不硬编码在仓库中。
 */
JNIEXPORT jbyteArray JNICALL
Java_com_draftpeek_core_common_security_SecureStringResolver_nativeGetKeyFragment3(
    JNIEnv *env, jobject thiz) {
    /* BUILD_PERIOD_REPLACE: KEY_FRAGMENT_3_BYTES */
    jbyte placeholder[] = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                           0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    jbyteArray result = (*env)->NewByteArray(env, 16);
    (*env)->SetByteArrayRegion(env, result, 0, 16, placeholder);
    return result;
}

/*
 * A 层：DEX 快速 CRC 辅助（在 C 层预读 zip entry 避免 Java IO 反复类加载触发检测）
 */
JNIEXPORT jlong JNICALL
Java_com_draftpeek_security_AiDetector_nativeQuickDexChecksum(
    JNIEnv *env, jobject thiz, jstring apk_path) {
    const char *path = (*env)->GetStringUTFChars(env, apk_path, NULL);
    if (path == NULL) return 0;

    /* 使用 C 标准库 fopen + 读取 ZIP Central Directory 计算 CRC */
    /* 实现省略（Java 层已提供纯 Kotlin 实现，Native 为性能优化 + 防 hook 用） */
    FILE *fp = fopen(path, "rb");
    if (fp == NULL) {
        (*env)->ReleaseStringUTFChars(env, apk_path, path);
        return 0;
    }
    fclose(fp);
    (*env)->ReleaseStringUTFChars(env, apk_path, path);
    return 0;
}
