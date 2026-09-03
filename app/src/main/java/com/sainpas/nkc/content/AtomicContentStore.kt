package com.sainpas.nkc.content

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

/** Filesystem transaction: active content is never overwritten until staging validates. */
class AtomicContentStore(private val root: File) {
    private val active = File(root, "active")
    private val backup = File(root, "last-known-good")
    private val staging = File(root, "staging")
    private val versionFile = File(root, "active-version")

    fun activeDirectory(): File? = active.takeIf { it.isDirectory }
    fun activeVersion(): Long? = versionFile.takeIf { it.isFile }?.readText()?.trim()?.toLongOrNull()

    fun install(zip: File, version: Long) {
        ContentValidator.validateArchive(zip)
        staging.deleteRecursively(); staging.mkdirs()
        ZipFile(zip).use { archive -> archive.entries().asSequence().filterNot { it.isDirectory }.forEach { entry ->
            val output = File(staging, entry.name.removePrefix("content/"))
            output.parentFile?.mkdirs()
            archive.getInputStream(entry).use { input -> output.outputStream().use(input::copyTo) }
        } }
        require(File(staging, "config.json").isFile) { "Staged package lacks config.json" }
        root.mkdirs()
        backup.deleteRecursively()
        if (active.exists()) move(active, backup)
        try {
            move(staging, active)
            val pending = File(root, "active-version.pending")
            pending.writeText(version.toString())
            move(pending, versionFile)
        } catch (error: Exception) {
            active.deleteRecursively()
            if (backup.exists()) move(backup, active)
            throw error
        }
    }

    private fun move(from: File, to: File) {
        Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }
}
