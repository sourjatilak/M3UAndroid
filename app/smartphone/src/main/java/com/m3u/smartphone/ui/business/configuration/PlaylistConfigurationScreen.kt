package com.m3u.smartphone.ui.business.configuration

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.rememberPermissionState
import com.m3u.business.playlist.configuration.EpgManifest
import com.m3u.business.playlist.configuration.PlaylistConfigurationViewModel
import com.m3u.core.foundation.util.basic.title
import com.m3u.core.foundation.wrapper.Resource
import com.m3u.data.database.model.DataSource
import com.m3u.data.database.model.Playlist
import com.m3u.data.database.model.epgUrlsOrXtreamXmlUrl
import com.m3u.data.parser.xtream.XtreamUserInfo
import com.m3u.data.repository.playlist.PlaylistRepository
import com.m3u.i18n.R.string
import com.m3u.smartphone.ui.business.configuration.components.AutoSyncProgrammesButton
import com.m3u.smartphone.ui.business.configuration.components.EpgManifestGallery
import com.m3u.smartphone.ui.business.configuration.components.SyncProgrammesButton
import com.m3u.smartphone.ui.business.configuration.components.XtreamPanel
import com.m3u.smartphone.ui.common.helper.LocalHelper
import com.m3u.smartphone.ui.common.helper.Metadata
import com.m3u.smartphone.ui.material.components.Background
import com.m3u.smartphone.ui.material.components.TvEditableField
import com.m3u.smartphone.ui.material.ktx.checkPermissionOrRationale
import com.m3u.smartphone.ui.material.model.LocalHazeState
import com.m3u.smartphone.ui.material.model.LocalSpacing
import dev.chrisbanes.haze.hazeSource
import kotlinx.datetime.LocalDateTime

private const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

@Composable
internal fun PlaylistConfigurationRoute(
    modifier: Modifier = Modifier,
    viewModel: PlaylistConfigurationViewModel = hiltViewModel(),
    contentPadding: PaddingValues = PaddingValues()
) {
    val helper = LocalHelper.current

    val permissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val manifest by viewModel.manifest.collectAsStateWithLifecycle()
    val subscribingOrRefreshingWorkInfo by viewModel.subscribingOrRefreshingWorkInfo.collectAsStateWithLifecycle()
    val expired by viewModel.expired.collectAsStateWithLifecycle()
    val xtreamUserInfo by viewModel.xtreamUserInfo.collectAsStateWithLifecycle()

    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    LifecycleResumeEffect(playlist?.title) {
        Metadata.title = AnnotatedString(playlist?.title?.title().orEmpty())
        Metadata.color = Color.Unspecified
        Metadata.contentColor = Color.Unspecified
        onPauseOrDispose {
        }
    }

    playlist?.let {
        PlaylistConfigurationScreen(
            playlist = it,
            manifest = manifest,
            subscribingOrRefreshing = subscribingOrRefreshingWorkInfo != null,
            expired = expired,
            xtreamUserInfo = xtreamUserInfo,
            onUpdatePlaylistTitle = viewModel::onUpdatePlaylistTitle,
            onUpdateDisplayTitle = viewModel::onUpdateDisplayTitle,
            onUpdateVisibility = viewModel::onUpdateVisibility,
            onUpdatePlaylistUserAgent = viewModel::onUpdatePlaylistUserAgent,
            onUpdateEpgPlaylist = viewModel::onUpdateEpgPlaylist,
            onUpdatePlaylistAutoRefreshProgrammes = viewModel::onUpdatePlaylistAutoRefreshProgrammes,
            onSyncProgrammes = {
                if (permissionState == null) {
                    viewModel.onSyncProgrammes()
                    return@PlaylistConfigurationScreen
                }
                permissionState.checkPermissionOrRationale(
                    showRationale = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .apply {
                                putExtra(
                                    Settings.EXTRA_APP_PACKAGE,
                                    helper.activityContext.packageName
                                )
                            }
                        helper.activityContext.startActivity(intent)
                    },
                    block = {
                        viewModel.onSyncProgrammes()
                    }
                )
            },
            onCancelSyncProgrammes = viewModel::onCancelSyncProgrammes,
            onUnsubscribe = {
                viewModel.unsubscribe {
                    onBackPressedDispatcher?.onBackPressed()
                }
            },
            modifier = modifier,
            contentPadding = contentPadding
        )
    }
}

@Composable
private fun PlaylistConfigurationScreen(
    playlist: Playlist,
    manifest: EpgManifest,
    subscribingOrRefreshing: Boolean,
    expired: LocalDateTime?,
    xtreamUserInfo: Resource<XtreamUserInfo>,
    onUpdatePlaylistTitle: (String) -> Unit,
    onUpdateDisplayTitle: (String) -> Unit,
    onUpdateVisibility: (Boolean, Boolean, Boolean) -> Unit,
    onUpdatePlaylistUserAgent: (String?) -> Unit,
    onUpdateEpgPlaylist: (PlaylistRepository.EpgPlaylistUseCase) -> Unit,
    onUpdatePlaylistAutoRefreshProgrammes: () -> Unit,
    onSyncProgrammes: () -> Unit,
    onCancelSyncProgrammes: () -> Unit,
    onUnsubscribe: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    val spacing = LocalSpacing.current

    var title: String by remember(playlist.title) { mutableStateOf(playlist.title) }
    var displayTitle: String by remember(playlist.displayTitle) { mutableStateOf(playlist.displayTitle ?: playlist.title) }
    var userAgent: String by remember(playlist.userAgent) {
        mutableStateOf(playlist.userAgent ?: DEFAULT_USER_AGENT)
    }
    var showLive by remember(playlist.showLive) { mutableStateOf(playlist.showLive) }
    var showVod by remember(playlist.showVod) { mutableStateOf(playlist.showVod) }
    var showSeries by remember(playlist.showSeries) { mutableStateOf(playlist.showSeries) }

    val hasChanged by remember(playlist.title, playlist.displayTitle, playlist.userAgent, playlist.showLive, playlist.showVod, playlist.showSeries) {
        derivedStateOf {
            title != playlist.title ||
                    displayTitle != (playlist.displayTitle ?: playlist.title) ||
                    userAgent != (playlist.userAgent ?: DEFAULT_USER_AGENT) ||
                    showLive != playlist.showLive ||
                    showVod != playlist.showVod ||
                    showSeries != playlist.showSeries
        }
    }
    var showUnsubscribeConfirm by remember { mutableStateOf(false) }

    if (showUnsubscribeConfirm) {
        AlertDialog(
            onDismissRequest = { showUnsubscribeConfirm = false },
            title = {
                Text(
                    text = "Remove playlist?",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                val message = when (playlist.source) {
                    DataSource.Xtream ->
                        "This will unsubscribe the Xtream server \"${playlist.title}\" " +
                                "and remove its Live, VOD, and Series playlists along with " +
                                "all downloaded channels. This can't be undone."
                    else ->
                        "This will unsubscribe \"${playlist.title}\" and remove all its " +
                                "channels. This can't be undone."
                }
                Text(text = message)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnsubscribeConfirm = false
                        onUnsubscribe()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsubscribeConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Background(modifier) {
        Box {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(spacing.small),
                contentPadding = contentPadding,
                modifier = Modifier
                    .hazeSource(LocalHazeState.current)
                    .fillMaxSize()
                    .padding(spacing.medium)
            ) {
                item {
                    TvEditableField(
                        text = title,
                        placeholder = stringResource(string.feat_playlist_configuration_title).title(),
                        onValueChange = { title = it },
                    )
                }

                item {
                    TvEditableField(
                        text = displayTitle,
                        placeholder = "Display Title",
                        onValueChange = { displayTitle = it },
                    )
                }

                item {
                    TvEditableField(
                        text = userAgent,
                        placeholder = stringResource(string.feat_playlist_configuration_user_agent).title(),
                        onValueChange = { userAgent = it }
                    )
                }

                if (playlist.source == DataSource.Xtream) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Visibility",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                            )
                            VisibilityToggleRow(
                                label = "Live",
                                checked = showLive,
                                onCheckedChange = { if (it || showVod || showSeries) showLive = it }
                            )
                            VisibilityToggleRow(
                                label = "VOD",
                                checked = showVod,
                                onCheckedChange = { if (it || showLive || showSeries) showVod = it }
                            )
                            VisibilityToggleRow(
                                label = "Series",
                                checked = showSeries,
                                onCheckedChange = { if (it || showLive || showVod) showSeries = it }
                            )
                        }
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = playlist.epgUrlsOrXtreamXmlUrl().isNotEmpty(),
                        enter = fadeIn() + expandIn(
                            expandFrom = Alignment.BottomCenter,
                            initialSize = { IntSize(it.width, 0) }
                        ),
                        exit = fadeOut() + shrinkOut(
                            shrinkTowards = Alignment.BottomCenter,
                            targetSize = { IntSize(it.width, 0) }
                        )
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(spacing.small)
                        ) {
                            SyncProgrammesButton(
                                subscribingOrRefreshing = subscribingOrRefreshing,
                                expired = expired,
                                onSyncProgrammes = onSyncProgrammes,
                                onCancelSyncProgrammes = onCancelSyncProgrammes
                            )
                            AutoSyncProgrammesButton(
                                checked = playlist.autoRefreshProgrammes,
                                onCheckedChange = onUpdatePlaylistAutoRefreshProgrammes
                            )
                        }
                    }
                }

                if (playlist.source == DataSource.M3U) {
                    EpgManifestGallery(
                        playlistUrl = playlist.url,
                        manifest = manifest,
                        onUpdateEpgPlaylist = onUpdateEpgPlaylist
                    )
                }

                if (playlist.source == DataSource.Xtream) {
                    item {
                        XtreamPanel(
                            info = xtreamUserInfo,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(spacing.medium))
                }
                item {
                    OutlinedButton(
                        onClick = { showUnsubscribeConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteForever,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(spacing.small))
                        Text(
                            text = when (playlist.source) {
                                DataSource.Xtream -> "Unsubscribe server"
                                else -> "Remove playlist"
                            }
                        )
                    }
                }
            }

            val fabBottomPadding by animateDpAsState(
                targetValue = maxOf(
                    WindowInsets.ime.asPaddingValues().calculateBottomPadding(),
                    contentPadding.calculateBottomPadding()
                ),
                label = "apply changes bottom padding"
            )
            AnimatedVisibility(
                visible = hasChanged,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = fabBottomPadding)
            ) {
                FloatingActionButton(
                    onClick = {
                        if (title != playlist.title) onUpdatePlaylistTitle(title)
                        val effectiveDisplayTitle = displayTitle.ifBlank { title }
                        if (effectiveDisplayTitle != (playlist.displayTitle ?: playlist.title)) onUpdateDisplayTitle(effectiveDisplayTitle)
                        if (userAgent != (playlist.userAgent ?: DEFAULT_USER_AGENT)) onUpdatePlaylistUserAgent(userAgent)
                        if (showLive != playlist.showLive || showVod != playlist.showVod || showSeries != playlist.showSeries) {
                            onUpdateVisibility(showLive, showVod, showSeries)
                        }
                    },
                    modifier = Modifier.padding(spacing.medium)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Save,
                        contentDescription = "apply changes"
                    )
                }
            }
        }
    }
}

@Composable
private fun VisibilityToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
