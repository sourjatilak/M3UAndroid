package com.m3u.data.worker

import android.content.Context
import com.m3u.data.parser.xtream.XtreamInput
import com.m3u.data.parser.xtream.XtreamParser
import com.m3u.data.database.model.DataSource
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Caches raw Xtream API JSON responses to local files with a 30-min TTL.
 * On resume, reads from cache instead of re-fetching.
 */
object XtreamResponseCache {
    private const val TTL_MS = 30 * 60 * 1000L // 30 minutes

    private fun cacheDir(context: Context): File =
        File(context.cacheDir, "xtream_cache").also { it.mkdirs() }

    private fun cacheFile(context: Context, input: XtreamInput, type: String): File {
        val key = "${input.basicUrl}_${input.username}_$type".hashCode().toUInt().toString(16)
        return File(cacheDir(context), "$key.json")
    }

    fun progressFile(context: Context, input: XtreamInput, type: String): File {
        val key = "${input.basicUrl}_${input.username}_$type".hashCode().toUInt().toString(16)
        return File(cacheDir(context), "$key.progress")
    }

    private fun isValid(file: File): Boolean =
        file.exists() && (System.currentTimeMillis() - file.lastModified()) < TTL_MS

    /**
     * Ensures the response for [type] is cached locally. Downloads if missing/expired.
     * Returns the cache file.
     */
    fun ensureCached(
        context: Context,
        input: XtreamInput,
        type: String,
        okHttpClient: OkHttpClient
    ): File {
        val file = cacheFile(context, input, type)
        if (isValid(file)) return file

        val action = when (type) {
            DataSource.Xtream.TYPE_LIVE -> XtreamParser.Action.GET_LIVE_STREAMS
            DataSource.Xtream.TYPE_VOD -> XtreamParser.Action.GET_VOD_STREAMS
            DataSource.Xtream.TYPE_SERIES -> XtreamParser.Action.GET_SERIES_STREAMS
            else -> return file
        }
        val url = XtreamParser.createActionUrl(
            input.basicUrl, input.username, input.password, action
        )
        val response = okHttpClient.newCall(Request.Builder().url(url).build()).execute()
        if (response.isSuccessful) {
            response.body?.byteStream()?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        }
        response.close()
        // Reset progress when we re-download
        progressFile(context, input, type).delete()
        return file
    }

    fun readProgress(context: Context, input: XtreamInput, type: String): Int {
        val file = progressFile(context, input, type)
        return if (file.exists()) file.readText().trim().toIntOrNull() ?: 0 else 0
    }

    fun writeProgress(context: Context, input: XtreamInput, type: String, count: Int) {
        progressFile(context, input, type).writeText(count.toString())
    }

    fun cleanup(context: Context) {
        cacheDir(context).listFiles()?.forEach { file ->
            if (System.currentTimeMillis() - file.lastModified() > TTL_MS) file.delete()
        }
    }
}
