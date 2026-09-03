package com.sainpas.nkc.content

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.io.File

/** The UI consumes this abstraction rather than directly reading update files. */
interface ContentRepository {
    fun snapshot(): ContentSnapshot
    fun activeVersion(): Long?
}
data class ContentSnapshot(val version: Long?, val source: String, val config: JsonObject?)

class FileContentRepository(context: Context) : ContentRepository {
    private val store = AtomicContentStore(File(context.filesDir, "content"))
    private val json = Json { ignoreUnknownKeys = true }
    override fun activeVersion() = store.activeVersion()
    override fun snapshot(): ContentSnapshot {
        val config = store.activeDirectory()?.resolve("config.json")?.takeIf(File::isFile)?.let {
            runCatching { json.parseToJsonElement(it.readText()).jsonObject }.getOrNull()
        }
        return ContentSnapshot(activeVersion(), if (config == null) "Built-in fallback" else "Downloaded content", config)
    }
}
