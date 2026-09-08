/**
 * RemotePolicyManager R1 验签逻辑的 JVM 单测。
 *
 * 不依赖 Android / BuildConfig：直接驱动 `verifySignatureWithKey(...)`，
 * 在本地 JVM（Java 17 原生支持 Ed25519）上完整覆盖"验签通过 / 失败 / fail-closed"分支。
 */
package com.draftpeek.core.common.security

import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RemotePolicyManagerTest {

    private fun makeKeyPair() = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()

    private fun pubDerB64(pub: java.security.PublicKey): String = Base64.getEncoder().encodeToString(pub.encoded)

    private fun sign(priv: java.security.PrivateKey, payload: String): String {
        val sig = Signature.getInstance("Ed25519")
        sig.initSign(priv)
        sig.update(payload.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(sig.sign())
    }

    @Test
    fun `valid signature verifies true`() {
        val kp = makeKeyPair()
        val payload = """{"kill_switch":false,"version":3}"""
        val sig = sign(kp.private, payload)
        assertTrue(
            RemotePolicyManager.verifySignatureWithKey(payload, sig, pubDerB64(kp.public)),
            "正确签名应验签通过"
        )
    }

    @Test
    fun `tampered payload fails verification`() {
        val kp = makeKeyPair()
        val payload = """{"kill_switch":false,"version":3}"""
        val sig = sign(kp.private, payload)
        val tampered = """{"kill_switch":true,"version":3}"""
        assertFalse(
            RemotePolicyManager.verifySignatureWithKey(tampered, sig, pubDerB64(kp.public)),
            "篡改后的负载不应验签通过"
        )
    }

    @Test
    fun `wrong signature fails verification`() {
        val kp = makeKeyPair()
        val payload = """{"kill_switch":false,"version":3}"""
        val otherSig = sign(kp.private, "completely different content")
        assertFalse(
            RemotePolicyManager.verifySignatureWithKey(payload, otherSig, pubDerB64(kp.public)),
            "错误签名不应验签通过"
        )
    }

    @Test
    fun `empty public key fails closed`() {
        val kp = makeKeyPair()
        val payload = """{"kill_switch":false}"""
        val sig = sign(kp.private, payload)
        assertFalse(
            RemotePolicyManager.verifySignatureWithKey(payload, sig, ""),
            "公钥缺失必须 fail-closed（返回 false）"
        )
    }

    @Test
    fun `malformed public key fails closed`() {
        val kp = makeKeyPair()
        val payload = """{"kill_switch":false}"""
        val sig = sign(kp.private, payload)
        assertFalse(
            RemotePolicyManager.verifySignatureWithKey(payload, sig, "not-a-valid-key!!!"),
            "畸形公钥必须 fail-closed（异常 → 返回 false）"
        )
    }

    @Test
    fun `signature from different key pair fails`() {
        val kpA = makeKeyPair()
        val kpB = makeKeyPair()
        val payload = """{"kill_switch":false,"version":7}"""
        val sig = sign(kpA.private, payload)
        assertFalse(
            RemotePolicyManager.verifySignatureWithKey(payload, sig, pubDerB64(kpB.public)),
            "非对应私钥签名的策略必须被拒绝（抵御伪造服务端）"
        )
    }
}
