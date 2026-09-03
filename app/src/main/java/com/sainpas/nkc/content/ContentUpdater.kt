package com.sainpas.nkc.content

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateResult {
    data object UpToDate : UpdateResult
    data class Updated(val version: Long) : UpdateResult
    data class Deferred(val reason: String) : UpdateResult
    data class Failed(val reason: String) : UpdateResult
}

interface ContentTransport { suspend fun get(url: String): ByteArray }

class HttpsContentTransport : ContentTransport {
    override suspend fun get(url: String): ByteArray = withContext(Dispatchers.IO) {
        require(url.startsWith("https://")) { "Only HTTPS requests are permitted" }
        (URL(url).openConnection() as HttpURLConnection).run {
            connectTimeout = 10_000; readTimeout = 20_000; instanceFollowRedirects = false
            try { require(responseCode in 200..299) { "HTTP $responseCode" }; inputStream.use { it.readBytes() } } finally { disconnect() }
        }
    }
}

class ContentUpdater(
    private val manifestUrl: String,
    private val appVersion: Int,
    private val store: AtomicContentStore,
    private val transport: ContentTransport = HttpsContentTransport(),
    private val cacheDirectory: File,
) {
    suspend fun checkForUpdate(): UpdateResult = try {
        require(manifestUrl.startsWith("https://")) { "Manifest URL must use HTTPS" }
        val manifest = ManifestParser.parse(transport.get(manifestUrl).decodeToString())
        when {
            !manifest.isCompatible(appVersion) -> UpdateResult.Deferred("Content requires app version ${manifest.minimumAppVersion}")
            store.activeVersion()?.let { it >= manifest.contentVersion } == true -> UpdateResult.UpToDate
            else -> downloadAndActivate(manifest)
        }
    } catch (e: Exception) { UpdateResult.Failed(e.message ?: "Update failed; retained last known-good content") }

    private suspend fun downloadAndActivate(manifest: ContentManifest): UpdateResult {
        cacheDirectory.mkdirs()
        val packageFile = File.createTempFile("content-", ".zip", cacheDirectory)
        return try {
            packageFile.writeBytes(transport.get(manifest.contentUrl))
            require(ContentValidator.verifyHash(packageFile, manifest.sha256)) { "Content hash verification failed" }
            store.install(packageFile, manifest.contentVersion)
            UpdateResult.Updated(manifest.contentVersion)
        } finally { packageFile.delete() }
    }
}
