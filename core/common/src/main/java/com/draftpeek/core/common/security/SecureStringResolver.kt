/**
 * 字符串加密解密器（B 层精选）。
 *
 * 密钥分片存储策略：
 * - 分片 1/2：在本文件常量池中（Java 层）
 * - 分片 3：通过 JNI 从 native_security.so 获取（nativeGetKeyFragment3）
 *
 * 运行时拼接 3 个分片 → AES-256 密钥 → 解密 @EncryptedString 标注的字符串。
 *
 * 注意：当前阶段 IR 插件默认关闭（gradle.properties: draftpeek.stringEnc.enabled=false），
 * 所有 @EncryptedString 标注的字符串在编译期不会被加密，运行时解密器不被调用。
 * 此文件作为 B 层基础设施预留，方案 C 启用 OLLVM 时激活。
 *
 * @author DraftPeek Team
 * @since 1.0.30
 */
package com.draftpeek.core.common.security

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 加密字符串注解（B 层精选）。
 *
 * 标记需要编译期加密的字符串常量。IR 插件在编译期将其替换为
 * AES-256-CBC 密文 + IV，运行时通过 [SecureStringResolver.decrypt] 解密。
 *
 * 当前 IR 插件默认关闭，此注解仅作为标记，不影响编译。
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class EncryptedString

/**
 * 字符串运行时解密器。
 *
 * 密钥由 3 个分片拼接而成：
 * - 片 1/2 在本文件常量池
 * - 片 3 通过 JNI 从 native_security.so 获取
 */
object SecureStringResolver {

    /** 密钥分片 1（16 字节，Java 常量池） */
    private val keyFragment1 = byteArrayOf(
        0x53, 0x65, 0x63, 0x75, 0x72, 0x65, 0x4b, 0x65,
        0x79, 0x46, 0x72, 0x61, 0x67, 0x31, 0x30, 0x30,
    )

    /** 密钥分片 2（16 字节，Java 常量池） */
    private val keyFragment2 = byteArrayOf(
        0x46, 0x72, 0x61, 0x67, 0x32, 0x30, 0x30, 0x44,
        0x72, 0x61, 0x66, 0x74, 0x50, 0x65, 0x65, 0x6b,
    )

    /**
     * 密钥分片 3（16 字节，通过 JNI 从 native_security.so 获取）。
     * JNI 方法在 native_security.c 中实现。
     * 库未加载时返回全零占位。
     */
    private val keyFragment3: ByteArray by lazy {
        try {
            nativeGetKeyFragment3()
        } catch (_: UnsatisfiedLinkError) {
            ByteArray(16) // 降级：全零占位
        }
    }

    /**
     * 解密 AES-256-CBC 加密的字符串。
     *
     * @param ciphertext 密文字节数组
     * @param iv 初始化向量（16 字节）
     * @return 解密后的明文字符串
     */
    fun decrypt(ciphertext: ByteArray, iv: ByteArray): String {
        val key = ByteArray(48)
        keyFragment1.copyInto(key, 0)
        keyFragment2.copyInto(key, 16)
        keyFragment3.copyInto(key, 32)
        // 取前 32 字节作为 AES-256 密钥
        val aesKey = key.copyOf(32)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"), IvParameterSpec(iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    /**
     * JNI 声明：从 native_security.so 获取密钥分片 3。
     * 实现在 native_security.c 的 nativeGetKeyFragment3 函数中。
     */
    private external fun nativeGetKeyFragment3(): ByteArray
}
