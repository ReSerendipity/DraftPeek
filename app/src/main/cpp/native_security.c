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
 */
static jbyteArray native_get_key_fragment3(
    JNIEnv *env, jobject thiz) {
    /* BUILD_PERIOD_REPLACE: KEY_FRAGMENT_3_BYTES */
    jbyte placeholder[] = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                           0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    jbyteArray result = (*env)->NewByteArray(env, 16);
    (*env)->SetByteArrayRegion(env, result, 0, 16, placeholder);
    return result;
}

/*
 * A 层：DEX 快速 CRC 辅助
 * P2-1: Dynamically registered via RegisterNatives.
 *
 * IMPLEMENTED (P3-4): Reads the APK as a ZIP file, locates the Central
 * Directory, finds the classes.dex entry, and returns its CRC32 value
 * from the ZIP Central Directory header. This avoids Java-level file IO
 * that could be hooked by Frida/Xposed.
 *
 * Returns: CRC32 of classes.dex as jlong, or 0 on error / not found.
 */
static jlong native_quick_dex_checksum(
    JNIEnv *env, jobject thiz, jstring apk_path) {
    const char *path = (*env)->GetStringUTFChars(env, apk_path, NULL);
    if (path == NULL) return 0;

    FILE *fp = fopen(path, "rb");
    if (fp == NULL) {
        (*env)->ReleaseStringUTFChars(env, apk_path, path);
        return 0;
    }

    jlong dex_crc = 0;

    /* Step 1: Find the End of Central Directory (EOCD) record by scanning
     * backwards from the end of the file. EOCD signature: 0x06054b50 */
    fseek(fp, 0, SEEK_END);
    long file_size = ftell(fp);
    if (file_size < 22) { /* Minimum ZIP file size */
        fclose(fp);
        (*env)->ReleaseStringUTFChars(env, apk_path, path);
        return 0;
    }

    /* Scan backwards for EOCD signature (max 65557 bytes = 64KB + 22) */
    long scan_start = file_size - 65557;
    if (scan_start < 0) scan_start = 0;
    long eocd_offset = -1;
    unsigned char eocd_buf[65557];
    size_t eocd_read_size = file_size - scan_start;
    if (eocd_read_size > sizeof(eocd_buf)) eocd_read_size = sizeof(eocd_buf);
    fseek(fp, scan_start, SEEK_SET);
    if (fread(eocd_buf, 1, eocd_read_size, fp) != eocd_read_size) {
        fclose(fp);
        (*env)->ReleaseStringUTFChars(env, apk_path, path);
        return 0;
    }

    for (long i = (long)eocd_read_size - 22; i >= 0; i--) {
        if (eocd_buf[i]   == 0x50 && eocd_buf[i+1] == 0x4b &&
            eocd_buf[i+2] == 0x05 && eocd_buf[i+3] == 0x06) {
            eocd_offset = scan_start + i;
            break;
        }
    }

    if (eocd_offset < 0) {
        fclose(fp);
        (*env)->ReleaseStringUTFChars(env, apk_path, path);
        return 0;
    }

    /* Step 2: Read Central Directory offset and size from EOCD */
    unsigned char eocd[22];
    fseek(fp, eocd_offset, SEEK_SET);
    if (fread(eocd, 1, 22, fp) != 22) {
        fclose(fp);
        (*env)->ReleaseStringUTFChars(env, apk_path, path);
        return 0;
    }

    /* CD offset at byte 16, CD size at byte 12 (little-endian uint32) */
    long cd_offset = (long)((eocd[16] & 0xFF) |
                            ((eocd[17] & 0xFF) << 8) |
                            ((eocd[18] & 0xFF) << 16) |
                            ((unsigned long)(eocd[19]) << 24));
    long cd_size = (long)((eocd[12] & 0xFF) |
                          ((eocd[13] & 0xFF) << 8) |
                          ((eocd[14] & 0xFF) << 16) |
                          ((unsigned long)(eocd[15]) << 24));

    if (cd_offset < 0 || cd_size <= 0 || cd_offset + cd_size > file_size) {
        fclose(fp);
        (*env)->ReleaseStringUTFChars(env, apk_path, path);
        return 0;
    }

    /* Step 3: Scan Central Directory entries for "classes.dex" */
    fseek(fp, cd_offset, SEEK_SET);
    long pos = cd_offset;
    long cd_end = cd_offset + cd_size;

    while (pos < cd_end) {
        unsigned char cd_header[46];
        if (fread(cd_header, 1, 46, fp) != 46) break;

        /* Check CD entry signature: 0x02014b50 */
        if (cd_header[0] != 0x50 || cd_header[1] != 0x4b ||
            cd_header[2] != 0x01 || cd_header[3] != 0x02) {
            break;
        }

        /* CRC32 at bytes 16-19 (little-endian uint32) */
        unsigned int crc32 = ((unsigned int)(cd_header[16] & 0xFF)) |
                            ((unsigned int)(cd_header[17] & 0xFF) << 8) |
                            ((unsigned int)(cd_header[18] & 0xFF) << 16) |
                            ((unsigned int)(cd_header[19] & 0xFF) << 24);

        /* Filename length at bytes 28-29 */
        unsigned int name_len = (cd_header[28] & 0xFF) |
                               ((cd_header[29] & 0xFF) << 8);
        unsigned int extra_len = (cd_header[30] & 0xFF) |
                                ((cd_header[31] & 0xFF) << 8);
        unsigned int comment_len = (cd_header[32] & 0xFF) |
                                 ((cd_header[33] & 0xFF) << 8);

        /* Read filename */
        char filename[256];
        if (name_len >= sizeof(filename)) {
            /* Skip oversized entries */
            fseek(fp, name_len + extra_len + comment_len, SEEK_CUR);
            pos += 46 + name_len + extra_len + comment_len;
            continue;
        }
        if (fread(filename, 1, name_len, fp) != name_len) break;
        filename[name_len] = '\0';

        /* Check if this is classes.dex */
        if (strcmp(filename, "classes.dex") == 0) {
            dex_crc = (jlong)crc32;
            break;
        }

        /* Skip extra field and comment */
        fseek(fp, extra_len + comment_len, SEEK_CUR);
        pos += 46 + name_len + extra_len + comment_len;
    }

    fclose(fp);
    (*env)->ReleaseStringUTFChars(env, apk_path, path);
    return dex_crc;
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
 * - AiDetector: 1 个 DEX CRC 方法
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

/* AiDetector 的方法注册表 */
static const char *AD_CLASS = "com/draftpeek/security/AiDetector";
static JNINativeMethod ad_methods[] = {
    {"nativeQuickDexChecksum", "(Ljava/lang/String;)J",                      (void *)native_quick_dex_checksum},
};

#define ARRAY_LEN(arr) (sizeof(arr) / sizeof(arr[0]))

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    /* Register NativeSecurityChecker methods */
    jclass nsc_class = (*env)->FindClass(env, NSC_CLASS);
    if (nsc_class == NULL) {
        LOGW("Failed to find class %s", NSC_CLASS);
        return JNI_ERR;
    }
    if ((*env)->RegisterNatives(env, nsc_class, nsc_methods, ARRAY_LEN(nsc_methods)) < 0) {
        LOGW("Failed to register natives for %s", NSC_CLASS);
        return JNI_ERR;
    }
    (*env)->DeleteLocalRef(env, nsc_class);

    /* Register SecureStringResolver methods */
    jclass ssr_class = (*env)->FindClass(env, SSR_CLASS);
    if (ssr_class == NULL) {
        LOGW("Failed to find class %s", SSR_CLASS);
        /* SSR is optional (string encryption may be disabled) — continue */
    } else {
        if ((*env)->RegisterNatives(env, ssr_class, ssr_methods, ARRAY_LEN(ssr_methods)) < 0) {
            LOGW("Failed to register natives for %s", SSR_CLASS);
        }
        (*env)->DeleteLocalRef(env, ssr_class);
    }

    /* Register AiDetector methods */
    jclass ad_class = (*env)->FindClass(env, AD_CLASS);
    if (ad_class == NULL) {
        LOGW("Failed to find class %s", AD_CLASS);
    } else {
        if ((*env)->RegisterNatives(env, ad_class, ad_methods, ARRAY_LEN(ad_methods)) < 0) {
            LOGW("Failed to register natives for %s", AD_CLASS);
        }
        (*env)->DeleteLocalRef(env, ad_class);
    }

    LOGD("JNI_OnLoad: all native methods registered successfully");
    return JNI_VERSION_1_6;
}
