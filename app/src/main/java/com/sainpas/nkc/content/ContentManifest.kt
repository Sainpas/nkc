package com.sainpas.nkc.content

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Metadata fetched independently from the APK. Only HTTPS package URLs are accepted. */
@Serializable
data class ContentManifest(
    val contentVersion: Long,
    val minimumAppVersion: Int,
    val contentUrl: String,
    val sha256: String,
) {
    fun isCompatible(appVersion: Int) = minimumAppVersion <= appVersion
    fun hasValidRemoteUrl() = contentUrl.startsWith("https://")
    fun hasValidHash() = sha256.matches(Regex("^[a-fA-F0-9]{64}$"))
}

object ManifestParser {
    private val json = Json { ignoreUnknownKeys = true }
    fun parse(text: String): ContentManifest = json.decodeFromString(ContentManifest.serializer(), text).also {
        require(it.contentVersion >= 0) { "contentVersion must not be negative" }
        require(it.minimumAppVersion >= 1) { "minimumAppVersion must be positive" }
        require(it.hasValidRemoteUrl()) { "Content URL must use HTTPS" }
        require(it.hasValidHash()) { "sha256 must be a 64 character hexadecimal digest" }
    }
}
