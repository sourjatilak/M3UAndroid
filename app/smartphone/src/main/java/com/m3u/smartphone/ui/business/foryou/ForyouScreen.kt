package com.m3u.smartphone.ui.business.foryou

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.m3u.business.foryou.ForyouViewModel
import com.m3u.business.foryou.Recommend
import com.m3u.core.foundation.architecture.preferences.PreferencesKeys
import com.m3u.core.foundation.architecture.preferences.mutablePreferenceOf
import com.m3u.core.foundation.architecture.preferences.preferenceOf
import com.m3u.core.foundation.ui.composableOf
import com.m3u.core.foundation.ui.thenIf
import com.m3u.core.foundation.util.basic.title
import com.m3u.core.foundation.wrapper.Resource
import com.m3u.data.database.model.Channel
import com.m3u.data.database.model.Playlist
import com.m3u.data.database.model.isSeries
import com.m3u.data.service.MediaCommand
import com.m3u.i18n.R.string
import com.m3u.smartphone.ui.business.foryou.components.HeadlineBackground
import com.m3u.smartphone.ui.business.foryou.components.PlaylistGallery
import com.m3u.smartphone.ui.business.foryou.components.recommend.RecommendGallery
import com.m3u.smartphone.ui.common.helper.Action
import com.m3u.smartphone.ui.common.helper.LocalHelper
import com.m3u.smartphone.ui.common.helper.Metadata
import com.m3u.smartphone.ui.material.components.EpisodesBottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ForyouRoute(
    navigateToPlaylist: (Playlist) -> Unit,
    navigateToChannel: () -> Unit,
    navigateToPlaylistConfiguration: (Playlist) -> Unit,
    navigateToSettingPlaylistManagement: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: ForyouViewModel = hiltViewModel()
) {
    val helper = LocalHelper.current
    val coroutineScope = rememberCoroutineScope()

    val title = stringResource(string.ui_title_foryou)

    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val specs by viewModel.specs.collectAsStateWithLifecycle()
    val episodes by viewModel.episodes.collectAsStateWithLifecycle()

    val series: Channel? by viewModel.series.collectAsStateWithLifecycle()
    val subscribingPlaylistUrls by viewModel.subscribingPlaylistUrls.collectAsStateWithLifecycle()
    val refreshingEpgUrls by viewModel.refreshingEpgUrls.collectAsStateWithLifecycle(emptyList())

    LifecycleResumeEffect(title) {
        Metadata.title = AnnotatedString(title.title())
        Metadata.color = Color.Unspecified
        Metadata.contentColor = Color.Unspecified
        Metadata.actions = listOf(
            Action(
                icon = Icons.Rounded.Add,
                contentDescription = "add",
                onClick = navigateToSettingPlaylistManagement
            )
        )
        onPauseOrDispose {
            Metadata.actions = emptyList()
            Metadata.headlineUrl = ""
        }
    }

    Box(modifier) {
        ForyouScreen(
            playlists = playlists,
            subscribingPlaylistUrls = subscribingPlaylistUrls,
            refreshingEpgUrls = refreshingEpgUrls,
            specs = specs,
            contentPadding = contentPadding,
            navigateToPlaylist = navigateToPlaylist,
            onPlayChannel = { channel ->
                coroutineScope.launch {
                    val playlist = viewModel.getPlaylist(channel.playlistUrl)
                    when {
                        playlist?.isSeries == true -> {
                            viewModel.series.value = channel
                        }

                        else -> {
                            helper.play(MediaCommand.Common(channel.id))
                            navigateToChannel()
                        }
                    }
                }
            },
            navigateToPlaylistConfiguration = navigateToPlaylistConfiguration,
            modifier = Modifier.fillMaxSize()
        )

        EpisodesBottomSheet(
            series = series,
            episodes = episodes,
            onEpisodeClick = { episode ->
                coroutineScope.launch {
                    series?.let { channel ->
                        val input = MediaCommand.XtreamEpisode(
                            channelId = channel.id,
                            episode = episode
                        )
                        helper.play(input)
                        navigateToChannel()
                    }
                }
            },
            onRefresh = { series?.let { viewModel.seriesReplay.value += 1 } },
            onDismissRequest = { viewModel.series.value = null }
        )
    }
}

@Composable
private fun ForyouScreen(
    playlists: Map<Playlist, Int>,
    subscribingPlaylistUrls: List<String>,
    refreshingEpgUrls: List<String>,
    specs: List<Recommend.Spec>,
    contentPadding: PaddingValues,
    navigateToPlaylist: (Playlist) -> Unit,
    onPlayChannel: (Channel) -> Unit,
    navigateToPlaylistConfiguration: (Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    var headlineSpec: Recommend.Spec? by remember { mutableStateOf(null) }

    LaunchedEffect(headlineSpec) {
        val spec = headlineSpec
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(400.milliseconds)
            Metadata.headlineUrl = when (spec) {
                is Recommend.UnseenSpec -> spec.channel.cover.orEmpty()
                is Recommend.DiscoverSpec -> ""
                is Recommend.NewRelease -> ""
                else -> ""
            }
        }
    }

    Box(modifier) {
        HeadlineBackground()
        if (playlists.isEmpty()) {
            Text(
                text = stringResource(string.feat_foryou_no_playlist_added),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
            return
        }
        val header = @Composable {
            RecommendGallery(
                specs = specs,
                navigateToPlaylist = navigateToPlaylist,
                onPlayChannel = onPlayChannel,
                onSpecChanged = { spec -> headlineSpec = spec },
                modifier = Modifier.fillMaxWidth()
            )
        }
        PlaylistGallery(
            // Home screen is list-only: always render as a single column,
            // independent of the shared ROW_COUNT preference used by the
            // Favourite / Playlist screens.
            rowCount = 1,
            playlists = playlists,
            subscribingPlaylistUrls = subscribingPlaylistUrls,
            refreshingEpgUrls = refreshingEpgUrls,
            onClick = navigateToPlaylist,
            onConfigure = navigateToPlaylistConfiguration,
            header = composableOf(specs.isNotEmpty(), header),
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize()
        )
    }
}
