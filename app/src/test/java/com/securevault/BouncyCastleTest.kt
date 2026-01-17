package com.securevault

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Security

/**
 * Test to verify Bouncy Castle and ML-KEM-768 availability
 * Part of Phase 1, Day 1: Bouncy Castle Setup
 */
class BouncyCastleTest {

    @Test
    fun `bouncy castle provider is available`() {
        // Add BouncyCastle provider
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProvider())

        val provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
        assertNotNull("BouncyCastle provider should be available", provider)
        println("✓ BouncyCastle provider loaded: ${provider.name} v${provider.version}")
    }

    @Test
    fun `bouncy castle pqc provider is available`() {
        // Add BouncyCastle PQC provider
        Security.removeProvider(BouncyCastlePQCProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastlePQCProvider())

        val provider = Security.getProvider(BouncyCastlePQCProvider.PROVIDER_NAME)
        assertNotNull("BouncyCastle PQC provider should be available", provider)
        println("✓ BouncyCastle PQC provider loaded: ${provider.name} v${provider.version}")
    }

    @Test
    fun `ml-kem-768 key generation works`() {
        // Add providers
        Security.removeProvider(BouncyCastlePQCProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastlePQCProvider())

        try {
            // Generate ML-KEM-768 keypair using the NIST standardized name
            val keyPairGenerator = KeyPairGenerator.getInstance("ML-KEM-768", BouncyCastlePQCProvider.PROVIDER_NAME)

            val keyPair = keyPairGenerator.generateKeyPair()

            assertNotNull("ML-KEM-768 keypair should be generated", keyPair)
            assertNotNull("Public key should exist", keyPair.public)
            assertNotNull("Private key should exist", keyPair.private)

            println("✓ ML-KEM-768 keypair generated successfully")
            println("  Public key size: ${keyPair.public.encoded.size} bytes")
            println("  Private key size: ${keyPair.private.encoded.size} bytes")

            // Verify ML-KEM-768 key sizes (actual sizes: public ~1206 bytes, private ~86 bytes in BC encoding)
            assertTrue("Public key should be reasonable size", keyPair.public.encoded.size > 1000)
            assertTrue("Private key should exist and be non-empty", keyPair.private.encoded.isNotEmpty())

        } catch (e: Exception) {
            throw AssertionError("ML-KEM-768 not available: ${e.message}", e)
        }
    }

    @Test
    fun `list available pqc algorithms`() {
        Security.removeProvider(BouncyCastlePQCProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastlePQCProvider())

        val provider = Security.getProvider(BouncyCastlePQCProvider.PROVIDER_NAME)

        println("Available PQC algorithms:")
        provider.services
            .filter { it.type == "KeyPairGenerator" }
            .forEach { service ->
                println("  - ${service.algorithm}")
            }
    }
}
