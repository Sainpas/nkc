package com.sainpas.nkc.content

import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

object ContentValidator {
    private val forbiddenExtensions = setOf("dex", "jar", "so", "class", "apk", "aar", "exe")

    fun sha256(file: File): String = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

    fun verifyHash(file: File, expected: String) = sha256(file).equals(expected, ignoreCase = true)

    /** Rejects archive traversal and executable payloads before anything is extracted. */
    fun validateArchive(zip: File) {
        ZipFile(zip).use { archive ->
            val entries = archive.entries().asSequence().filterNot { it.isDirectory }.toList()
            require(entries.isNotEmpty()) { "Content package is empty" }
            require(entries.all { it.name.startsWith("content/") && !it.name.contains("..") && !it.name.startsWith("/") }) { "Invalid archive path" }
            require(entries.none { it.name.substringAfterLast('.', "").lowercase() in forbiddenExtensions }) { "Executable content is forbidden" }
            val config = entries.firstOrNull { it.name == "content/config.json" } ?: error("config.json is required")
            archive.getInputStream(config).bufferedReader().use { Json.parseToJsonElement(it.readText()).jsonObject }
        }
    }
}
