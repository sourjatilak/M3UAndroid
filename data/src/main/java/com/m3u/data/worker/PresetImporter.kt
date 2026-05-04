package com.m3u.data.worker

import android.content.Context
import com.m3u.core.architecture.preferences.PreferencesKeys
import com.m3u.core.architecture.preferences.Settings
import com.m3u.core.architecture.preferences.PlaylistStrategy
import com.m3u.core.architecture.preferences.get
import com.m3u.data.api.OkhttpClient
import com.m3u.data.database.dao.ChannelDao
import com.m3u.data.database.dao.PlaylistDao
import com.m3u.data.database.model.DataSource
import com.m3u.data.database.model.Playlist
import com.m3u.data.parser.xtream.XtreamData
import com.m3u.data.parser.xtream.XtreamInput
import com.m3u.data.parser.xtream.XtreamLive
import com.m3u.data.parser.xtream.XtreamParser
import com.m3u.data.parser.xtream.XtreamSerial
import com.m3u.data.parser.xtream.XtreamVod
import com.m3u.data.parser.xtream.toChannel
import com.m3u.data.parser.xtream.asChannel
import com.m3u.data.repository.playlist.PlaylistRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeToSequence
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class Preset(
    val type: String,
    val title: String,
    val url: String? = null,
    @SerialName("basicUrl") val basicUrl: String? = null,
    val username: String? = null,
    val password: String? = null,
) {
    val resolvedUrl: String?
        get() = when (type) {
            "m3u" -> url
            "xtream" -> {
                val b = basicUrl ?: return null
                val u = username ?: return null
                val p = password ?: return null
                XtreamInput.encodeToPlaylistUrl(XtreamInput(b, u, p))
            }
            else -> null
        }
}

@Singleton
class PresetImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistRepository: PlaylistRepository,
    private val xtreamParser: XtreamParser,
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao,
    private val settings: Settings,
    @OkhttpClient(true) private val okHttpClient: OkHttpClient,
) {
    companion object {
        private const val ASSET_FILE = "presets.json"
        private const val BATCH_SIZE = 100
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = true
    }

    fun readPresets(): List<Preset> {
        val raw = try {
            context.assets.open(ASSET_FILE).bufferedReader().use { it.readText() }
        } catch (_: Exception) { return emptyList() }
        if (raw.isBlank()) return emptyList()
        return try { Json.decodeFromString<List<Preset>>(raw) } catch (_: Exception) { emptyList() }
    }

    suspend fun pendingPresets(): List<Preset> {
        return readPresets().filter { preset ->
            val url = preset.resolvedUrl ?: return@filter false
            preset.title.isNotEmpty() && playlistRepository.get(url) == null
        }
    }

    /**
     * Import a single preset. For xtream, uses local file cache and reports progress.
     * @param onProgress called with (completed, total) counts
     */
    suspend fun importPreset(
        preset: Preset,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ) {
        when (preset.type) {
            "m3u" -> {
                val url = preset.url ?: return
                playlistRepository.m3uOrThrow(preset.title, url) { count ->
                    onProgress(count, -1) // total unknown for m3u
                }
            }
            "xtream" -> importXtreamCached(preset, onProgress)
        }
    }

    private suspend fun importXtreamCached(
        preset: Preset,
        onProgress: (completed: Int, total: Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val basicUrl = preset.basicUrl ?: return@withContext
        val username = preset.username ?: return@withContext
        val password = preset.password ?: return@withContext
        val input = XtreamInput(basicUrl, username, password)

        // 1. Fetch categories + server info (small calls)
        val output = xtreamParser.getXtreamOutput(input)
        val liveContainerExtension =
            if ("ts" in output.allowedOutputFormats) "ts"
            else output.allowedOutputFormats.firstOrNull() ?: "ts"
        val serverProtocol = output.serverProtocol
        val port = output.port

        // 2. Download large responses to cache files
        val types = listOf(
            DataSource.Xtream.TYPE_LIVE,
            DataSource.Xtream.TYPE_VOD,
            DataSource.Xtream.TYPE_SERIES
        )
        val cacheFiles = types.associateWith { type ->
            XtreamResponseCache.ensureCached(context, input, type, okHttpClient)
        }

        // 3. Count total from file sizes (rough estimate: ~200 bytes per entry)
        val total = cacheFiles.values.sumOf { it.length() / 200 }.toInt().coerceAtLeast(1)

        // 4. Create/update playlists
        val playlistStrategy = settings[PreferencesKeys.PLAYLIST_STRATEGY]
        val playlists = mutableMapOf<String, Playlist>()
        for (type in types) {
            val url = XtreamInput.encodeToPlaylistUrl(
                input = input.copy(type = type),
                serverProtocol = serverProtocol,
                port = port
            )
            val playlist = playlistDao.get(url)
                ?.takeIf { it.source == DataSource.Xtream }
                ?.copy(title = preset.title)
                ?: Playlist(title = preset.title, url = url, source = DataSource.Xtream)
            when (playlistStrategy) {
                PlaylistStrategy.ALL -> channelDao.deleteByPlaylistUrl(playlist.url)
                PlaylistStrategy.KEEP -> channelDao.deleteByPlaylistUrlIgnoreFavOrHidden(playlist.url)
            }
            playlistDao.insertOrReplace(playlist)
            playlists[type] = playlist
        }

        val favOrHiddenIds = channelDao.getFavOrHiddenRelationIdsByPlaylistUrl(
            playlists[DataSource.Xtream.TYPE_LIVE]!!.url,
            playlists[DataSource.Xtream.TYPE_VOD]!!.url,
            playlists[DataSource.Xtream.TYPE_SERIES]!!.url
        )

        // 5. Read previously saved progress (for resume)
        var globalCompleted = types.sumOf {
            XtreamResponseCache.readProgress(context, input, it)
        }
        onProgress(globalCompleted, total)

        // 6. Stream-parse each cached file and insert in batches (parallel by type)
        val progressMutex = Mutex()
        coroutineScope {
            for (type in types) {
                val file = cacheFiles[type] ?: continue
                val playlist = playlists[type] ?: continue
                launch(Dispatchers.IO) {
                    val alreadyDone = XtreamResponseCache.readProgress(context, input, type)
                    var typeCount = 0
                    val batch = mutableListOf<com.m3u.data.database.model.Channel>()

                    parseFromFile(file, type).forEach { data ->
                        typeCount++
                        if (typeCount <= alreadyDone) return@forEach

                        val channel = mapToChannel(
                            data, input, playlist.url, type,
                            output.liveCategories, output.vodCategories, output.serialCategories,
                            liveContainerExtension, favOrHiddenIds
                        ) ?: return@forEach

                        batch += channel
                        if (batch.size >= BATCH_SIZE) {
                            channelDao.insertOrReplaceAll(*batch.toTypedArray())
                            XtreamResponseCache.writeProgress(context, input, type, typeCount)
                            progressMutex.withLock {
                                globalCompleted += batch.size
                                onProgress(globalCompleted, total)
                            }
                            batch.clear()
                        }
                    }
                    if (batch.isNotEmpty()) {
                        channelDao.insertOrReplaceAll(*batch.toTypedArray())
                        progressMutex.withLock {
                            globalCompleted += batch.size
                            onProgress(globalCompleted, total)
                        }
                    }
                    XtreamResponseCache.writeProgress(context, input, type, typeCount)
                }
            }
        }

        // 7. Cleanup cache after successful import
        types.forEach {
            XtreamResponseCache.progressFile(context, input, it).delete()
        }
        XtreamResponseCache.cleanup(context)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun parseFromFile(file: File, type: String): Sequence<XtreamData> {
        if (!file.exists() || file.length() == 0L) return emptySequence()
        return when (type) {
            DataSource.Xtream.TYPE_LIVE ->
                json.decodeToSequence<XtreamLive>(file.inputStream()).map { it as XtreamData }
            DataSource.Xtream.TYPE_VOD ->
                json.decodeToSequence<XtreamVod>(file.inputStream()).map { it as XtreamData }
            DataSource.Xtream.TYPE_SERIES ->
                json.decodeToSequence<XtreamSerial>(file.inputStream()).map { it as XtreamData }
            else -> emptySequence()
        }
    }

    private fun mapToChannel(
        data: XtreamData,
        input: XtreamInput,
        playlistUrl: String,
        type: String,
        liveCategories: List<com.m3u.data.parser.xtream.XtreamCategory>,
        vodCategories: List<com.m3u.data.parser.xtream.XtreamCategory>,
        serialCategories: List<com.m3u.data.parser.xtream.XtreamCategory>,
        liveContainerExtension: String,
        favOrHiddenIds: List<String>
    ): com.m3u.data.database.model.Channel? {
        return when (data) {
            is XtreamLive -> {
                if (data.streamId?.toString() in favOrHiddenIds) return null
                data.toChannel(
                    basicUrl = input.basicUrl, username = input.username,
                    password = input.password, playlistUrl = playlistUrl,
                    category = liveCategories.find { it.categoryId == data.categoryId }?.categoryName.orEmpty(),
                    containerExtension = liveContainerExtension
                )
            }
            is XtreamVod -> {
                if (data.streamId?.toString() in favOrHiddenIds) return null
                data.toChannel(
                    basicUrl = input.basicUrl, username = input.username,
                    password = input.password, playlistUrl = playlistUrl,
                    category = vodCategories.find { it.categoryId == data.categoryId }?.categoryName.orEmpty()
                )
            }
            is XtreamSerial -> {
                if (data.seriesId?.toString() in favOrHiddenIds) return null
                data.asChannel(
                    basicUrl = input.basicUrl, username = input.username,
                    password = input.password, playlistUrl = playlistUrl,
                    category = serialCategories.find { it.categoryId == data.categoryId }?.categoryName.orEmpty()
                )
            }
        }
    }

}
