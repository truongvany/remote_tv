package com.example.remote_tv.data

import android.content.Context
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * Quản lý RSA-2048 keypair dùng cho ADB authentication.
 * Key được tạo 1 lần và lưu vào SharedPreferences.
 * Format public key tuân theo chuẩn AOSP android_pubkey.c
 */
object AdbKeyManager {

    private var keyPair: KeyPair? = null
    private const val PREFS = "adb_keypair_v1"
    private const val K_PRIV = "private_key"
    private const val K_PUB  = "public_key"

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val privB64 = prefs.getString(K_PRIV, null)
        val pubB64  = prefs.getString(K_PUB,  null)

        if (privB64 != null && pubB64 != null) {
            try {
                val factory = KeyFactory.getInstance("RSA")
                val priv = factory.generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(privB64)))
                val pub  = factory.generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(pubB64)))
                keyPair = KeyPair(pub, priv)
                return
            } catch (_: Exception) {}
        }
        val gen = KeyPairGenerator.getInstance("RSA")
        gen.initialize(2048)
        val kp = gen.generateKeyPair()
        keyPair = kp
        prefs.edit()
            .putString(K_PRIV, Base64.getEncoder().encodeToString(kp.private.encoded))
            .putString(K_PUB,  Base64.getEncoder().encodeToString(kp.public.encoded))
            .apply()
    }

    fun getKeyPair(): KeyPair? = keyPair

    /**
     * Build ADB RSA public key payload theo chuẩn AOSP (android_pubkey.c).
     * Format: base64(RSAPublicKey struct) + " " + username + "\0"
     */
    fun buildAdbPublicKeyPayload(username: String = "remote_tv@android"): ByteArray {
        val rsaPub = keyPair?.public as? RSAPublicKey ?: return ByteArray(0)
        val n       = rsaPub.modulus
        val NUM_WORDS = 64          // 2048-bit / 32-bit per word
        val MOD32   = BigInteger.ONE.shiftLeft(32)
        val MASK32  = MOD32.subtract(BigInteger.ONE)

        // n0inv = -(n^-1 mod 2^32) mod 2^32  (AOSP formula)
        val nLow   = n.mod(MOD32)
        val n0inv  = MOD32.subtract(nLow.modInverse(MOD32))

        // rr = 2^(NUM_WORDS*32)^2 mod n = 2^4096 mod n
        val r       = BigInteger.ONE.shiftLeft(NUM_WORDS * 32)
        val rSquared = r.multiply(r).mod(n)

        // Pack: len(4) + n0inv(4) + n[](256) + rr[](256) + exponent(4) = 524 bytes
        val buf = ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(NUM_WORDS)
        buf.putInt(n0inv.toInt())
        for (i in 0 until NUM_WORDS) buf.putInt(n.shiftRight(i * 32).and(MASK32).toInt())
        for (i in 0 until NUM_WORDS) buf.putInt(rSquared.shiftRight(i * 32).and(MASK32).toInt())
        buf.putInt(65537)

        val encoded = Base64.getEncoder().encodeToString(buf.array())
        return "$encoded $username\u0000".toByteArray(Charsets.UTF_8)
    }
}
