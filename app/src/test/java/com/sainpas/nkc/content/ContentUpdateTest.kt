package com.sainpas.nkc.content

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ContentUpdateTest {
    private val manifest = """{"contentVersion":2,"minimumAppVersion":1,"contentUrl":"https://host/content.zip","sha256":"%s"}"""
    @Test fun `parses manifest and separates content version`() {
        val value = ManifestParser.parse(manifest.format("a".repeat(64)))
        assertEquals(2, value.contentVersion); assertTrue(value.isCompatible(1))
    }
    @Test fun `rejects invalid manifest URLs`() { assertFails { ManifestParser.parse(manifest.format("a".repeat(64)).replace("https://", "http://")) } }
    @Test fun `verifies SHA 256`() { val f = temp("hello".encodeToByteArray()); assertTrue(ContentValidator.verifyHash(f, ContentValidator.sha256(f))); assertFalse(ContentValidator.verifyHash(f, "0".repeat(64))) }
    @Test fun `atomically replaces known good content`() {
        val root = Files.createTempDirectory("nkc").toFile(); val store = AtomicContentStore(root)
        store.install(zip("{\"value\":\"old\"}"), 1); store.install(zip("{\"value\":\"new\"}"), 2)
        assertEquals(2, store.activeVersion()); assertEquals("{\"value\":\"new\"}", store.activeDirectory()!!.resolve("config.json").readText())
    }
    @Test fun `failed download retains offline fallback`() = runTest {
        val root = Files.createTempDirectory("nkc").toFile(); val store = AtomicContentStore(root); store.install(zip("{\"value\":\"old\"}"), 1)
        val transport = object : ContentTransport { override suspend fun get(url: String): ByteArray = throw IllegalStateException("offline") }
        val result = ContentUpdater("https://host/manifest", 1, store, transport, root).checkForUpdate()
        assertTrue(result is UpdateResult.Failed); assertEquals(1, store.activeVersion())
    }
    @Test fun `minimum application version defers update`() = runTest {
        val bytes = "zip".encodeToByteArray(); val hash = ContentValidator.sha256(temp(bytes))
        // A compatible manifest parser response avoids executing download when APK is too old.
        val m = """{"contentVersion":2,"minimumAppVersion":2,"contentUrl":"https://host/content.zip","sha256":"$hash"}"""
        val response = object : ContentTransport { override suspend fun get(url: String) = m.encodeToByteArray() }
        assertTrue(ContentUpdater("https://host/manifest", 1, AtomicContentStore(Files.createTempDirectory("nkc").toFile()), response, Files.createTempDirectory("cache").toFile()).checkForUpdate() is UpdateResult.Deferred)
    }
    private fun zip(config: String): File { val out = File.createTempFile("content", ".zip"); ZipOutputStream(out.outputStream()).use { z -> z.putNextEntry(ZipEntry("content/config.json")); z.write(config.encodeToByteArray()); z.closeEntry() }; return out }
    private fun temp(bytes: ByteArray): File = File.createTempFile("hash", ".bin").also { it.writeBytes(bytes) }
    private fun assertFails(block: () -> Unit) { try { block(); fail("Expected failure") } catch (_: IllegalArgumentException) {} }
}
