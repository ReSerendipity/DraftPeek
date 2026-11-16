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
 * Read a file in chunks and search for patterns.
 *
 * FIXED (P3-3): Previous implementation had a cross-chunk match defect —
 * if a pattern straddled the boundary between two read() calls, it would
 * be missed. Now we overlap consecutive reads by max_pattern_len-1 bytes
 * to ensure patterns spanning chunk boundaries are detected.
 *
 * Returns: 1 if any pattern is found, 0 otherwise, -1 on error.
 */
static int scan_file_for_patterns(const char *filepath, const char *patterns[], int pattern_count) {
    int fd = open(filepath, O_RDONLY);
    if (fd < 0) {
        return -1;
    }

    /* Find the maximum pattern length for overlap calculation */
    size_t max_pattern_len = 0;
    for (int i = 0; i < pattern_count; i++) {
        size_t len = strlen(patterns[i]);
        if (len > max_pattern_len) {
            max_pattern_len = len;
        }
    }
    /* Ensure we don't allocate an absurd overlap */
    if (max_pattern_len >= BUFFER_SIZE) {
        max_pattern_len = BUFFER_SIZE - 1;
    }

    char buffer[BUFFER_SIZE];
    ssize_t bytes_read;
    int found = 0;
    /* Number of valid bytes carried over from the previous chunk for overlap */
    size_t carryover = 0;

    while (1) {
        /* Read into buffer after any carryover from previous iteration */
        size_t read_pos = carryover;
        bytes_read = read(fd, buffer + read_pos, sizeof(buffer) - 1 - read_pos);

        if (bytes_read <= 0 && carryover == 0) {
            break;
        }

        size_t total_valid = carryover + (bytes_read > 0 ? (size_t)bytes_read : 0);
        buffer[total_valid] = '\0';

        /* Search for each pattern in the valid portion of the buffer */
        for (int i = 0; i < pattern_count && !found; i++) {
            if (strstr(buffer, patterns[i]) != NULL) {
                found = 1;
                break;
            }
        }

        if (found) break;
        if (bytes_read <= 0) break;

        /* Calculate carryover: keep the last (max_pattern_len - 1) bytes
         * so patterns spanning the chunk boundary are caught in the next read. */
        if (max_pattern_len > 1 && total_valid >= max_pattern_len) {
            carryover = max_pattern_len - 1;
            memmove(buffer, buffer + total_valid - carryover, carryover);
        } else {
            carryover = 0;
        }

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
 * P2-1 FIXED: Changed from exported JNI naming (Java_com_draftpeek_...)
 * to static function registered dynamically via RegisterNatives in JNI_OnLoad.
 * This prevents the function names from appearing in the .so symbol table,
 * making it harder for Frida to find and hook them by name.
 *
 * Returns: JNI_TRUE if Frida is detected, JNI_FALSE otherwise.
 */
static jboolean native_check_frida_in_maps(
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
 * P2-1: Dynamically registered via RegisterNatives.
 *
 * Returns: JNI_TRUE if the process is being traced (TracerPid > 0).
 */
static jboolean native_check_tracer_pid(
    JNIEnv *env, jobject thiz) {

    int tracer_pid = read_tracer_pid();
    return (tracer_pid > 0) ? JNI_TRUE : JNI_FALSE;
}

/*
 * JNI: Check for Zygisk/Magisk traces in /proc/self/maps
 *
 * P2-1: Dynamically registered via RegisterNatives.
 *
 * Returns: JNI_TRUE if Zygisk/Magisk injection is detected.
 */
static jboolean native_check_zygisk_in_maps(
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
 * P2-1: Dynamically registered via RegisterNatives.
 *
 * Bit 0 (1): Frida detected
 * Bit 1 (2): TracerPid > 0 (being traced)
 * Bit 2 (4): Zygisk/Magisk detected
 *
 * Returns: bitmask of detected threats (0 = all clear).
 */
static jint native_perform_security_check(
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
 * B 层精选：字符串加密密钥片 3/3
 * P2-1: Dynamically registered via RegisterNatives.
 *
 * R8 (2026-09-04): 计划未实现 —— BUILD_PERIOD_REPLACE 构建期替换步骤尚未接线，
 * 本函数恒返回全零占位。B 层字符串加密整体为脚手架：IR 插件
 * （StringEncIrGenerationExtension）从未被注册、Transformer 为 no-op、
 * SecureStringResolver 无调用方，故此占位不影响任何实际功能。
 * 启用 B 层前须同时完成：构建期替换本分片 + IR 插件使用相同分片，
 * 否则密钥不一致将导致解密失败。
 */
static jbyteArray native_get_key_fragment3(
    JNIEnv *env, jobject thiz) {
    /* BUILD_PERIOD_REPLACE: KEY_FRAGMENT_3_BYTES（计划未实现，R8） */
    jbyte placeholder[] = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                           0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    jbyteArray result = (*env)->NewByteArray(env, 16);
    (*env)->SetByteArrayRegion(env, result, 0, 16, placeholder);
    return result;
}

// ===== P2-1: JNI_OnLoad + RegisterNatives 动态注册 =====
/*
 * 使用 RegisterNatives 动态注册 JNI 方法，替代传统的 Java_ 前缀命名导出。
 *
 * 优势：
 * 1. 函数符号不出现在 .so 导出符号表中（配合 -fvisibility=hidden）
 * 2. Frida 无法通过 dlsym 找到函数地址
 * 3. 方法名与 C 函数名解耦，增加逆向分析难度
 *
 * 注册两个类的方法：
 * - NativeSecurityChecker: 4 个安全检测方法
 * - SecureStringResolver: 1 个密钥分片方法
 */

/* NativeSecurityChecker 的方法注册表 */
static const char *NSC_CLASS = "com/draftpeek/security/NativeSecurityChecker";
static JNINativeMethod nsc_methods[] = {
    {"nativeCheckFridaInMaps",  "()Z",                                       (void *)native_check_frida_in_maps},
    {"nativeCheckTracerPid",   "()Z",                                       (void *)native_check_tracer_pid},
    {"nativeCheckZygiskInMaps", "()Z",                                      (void *)native_check_zygisk_in_maps},
    {"nativePerformSecurityCheck", "()I",                                    (void *)native_perform_security_check},
};

/* SecureStringResolver 的方法注册表 */
static const char *SSR_CLASS = "com/draftpeek/core/common/security/SecureStringResolver";
static JNINativeMethod ssr_methods[] = {
    {"nativeGetKeyFragment3", "()[B",                                        (void *)native_get_key_fragment3},
};

#define ARRAY_LEN(arr) (sizeof(arr) / sizeof(arr[0]))

/*
 * FindClass 失败时 JNI 会留下一个挂起异常。若不清理，System.loadLibrary 返回时
 * 调用方（Kotlin 的 NativeSecurityChecker.init）会看到异常，整个类初始化失败
 * （实测 1.0.32 发行包：ClassNotFoundException 抛穿导致 ExceptionInInitializerError，
 *  native 检测层整体失效）。所以每次失败都必须 ExceptionClear 后再继续。
 */
static void clear_pending(JNIEnv *env) {
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
    }
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    /* Register NativeSecurityChecker methods */
    jclass nsc_class = (*env)->FindClass(env, NSC_CLASS);
    if (nsc_class == NULL) {
        LOGW("Failed to find class %s", NSC_CLASS);
        clear_pending(env);
        return JNI_ERR;
    }
    if ((*env)->RegisterNatives(env, nsc_class, nsc_methods, ARRAY_LEN(nsc_methods)) < 0) {
        LOGW("Failed to register natives for %s", NSC_CLASS);
        clear_pending(env);
        return JNI_ERR;
    }
    (*env)->DeleteLocalRef(env, nsc_class);

    /* Register SecureStringResolver methods */
    jclass ssr_class = (*env)->FindClass(env, SSR_CLASS);
    if (ssr_class == NULL) {
        LOGW("Failed to find class %s", SSR_CLASS);
        /* SSR is optional (string encryption may be disabled) — continue */
        clear_pending(env);
    } else {
        if ((*env)->RegisterNatives(env, ssr_class, ssr_methods, ARRAY_LEN(ssr_methods)) < 0) {
            LOGW("Failed to register natives for %s", SSR_CLASS);
            clear_pending(env);
        }
        (*env)->DeleteLocalRef(env, ssr_class);
    }

    LOGD("JNI_OnLoad: native methods registered successfully");
    return JNI_VERSION_1_6;
}
