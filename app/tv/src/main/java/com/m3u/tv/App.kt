package com.m3u.tv

import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.m3u.business.setting.PresetImportViewModel
import com.m3u.business.playlist.PlaylistNavigation
import com.m3u.core.foundation.components.CircularProgressIndicator
import com.m3u.data.service.MediaCommand
import com.m3u.tv.screens.Screens
import com.m3u.tv.screens.dashboard.DashboardScreen
import com.m3u.tv.screens.player.ChannelScreen
import com.m3u.tv.screens.playlist.ChannelDetailScreen
import com.m3u.tv.screens.playlist.PlaylistScreen
import com.m3u.tv.screens.profile.AccountsSectionDialogButton
import com.m3u.tv.theme.JetStreamCardShape
import com.m3u.tv.utils.LocalHelper
import kotlinx.coroutines.launch

@Composable
fun App(
    onBackPressed: () -> Unit
) {
    val helper = LocalHelper.current
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    var isComingBackFromDifferentScreen by remember { mutableStateOf(false) }

    // Preset import dialog
    val presetVm: PresetImportViewModel = hiltViewModel()
    val showPresetDialog by presetVm.showDialog.collectAsState()
    val isImporting by presetVm.importing.collectAsState()
    val statusText by presetVm.statusText.collectAsState()
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(showPresetDialog) {
        if (showPresetDialog) presetVm.importAll()
    }

    StandardDialog(
        showDialog = showPresetDialog && !dismissed,
        onDismissRequest = { dismissed = true },
        confirmButton = {},
        dismissButton = {
            AccountsSectionDialogButton(
                text = "Close",
                shouldRequestFocus = true,
                onClick = { dismissed = true }
            )
        },
        title = {
            Text(
                text = "Loading Playlists",
                modifier = Modifier.padding(start = 8.dp),
                color = MaterialTheme.colorScheme.surface,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
                Text(
                    text = statusText,
                    color = MaterialTheme.colorScheme.surface,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.onSurface,
        shape = JetStreamCardShape
    )
    val navigateToChannel: (Int) -> Unit = { channelId: Int ->
        coroutineScope.launch {
            helper.play(MediaCommand.Common(channelId))
            navController.navigate(Screens.Channel())
        }
    }
    val navigateToChannelDetail: (Int) -> Unit = { channelId: Int ->
        navController.navigate(
            Screens.ChannelDetail.withArgs(channelId)
        )
    }
    NavHost(
        navController = navController,
        startDestination = Screens.Dashboard(),
        builder = {
            composable(
                route = Screens.Dashboard(),
                enterTransition = { null },
                exitTransition = { null }
            ) {
                DashboardScreen(
                    navigateToPlaylist = { playlistUrl ->
                        coroutineScope.launch {
                            navController.navigate(
                                Screens.Playlist.withArgs(playlistUrl)
                            )
                        }
                    },
                    navigateToChannel = navigateToChannel,
                    navigateToChannelDetail = navigateToChannelDetail,
                    onBackPressed = onBackPressed,
                    isComingBackFromDifferentScreen = isComingBackFromDifferentScreen,
                    resetIsComingBackFromDifferentScreen = {
                        isComingBackFromDifferentScreen = false
                    }
                )
            }

            composable(
                route = Screens.Playlist(),
                arguments = listOf(
                    navArgument(PlaylistNavigation.TYPE_URL) {
                        type = NavType.StringType
                    }
                ),
                enterTransition = { fadeIn() }
            ) {
                PlaylistScreen(
                    onChannelClick = { channel -> navigateToChannel(channel.id) }
                )
            }
            composable(
                route = Screens.Channel()
            ) {
                ChannelScreen(
                    onBackPressed = {
                        if (navController.navigateUp()) {
                            isComingBackFromDifferentScreen = true
                        }
                    }
                )
            }
            composable(
                route = Screens.ChannelDetail(),
                arguments = listOf(
                    navArgument(ChannelDetailScreen.ChannelIdBundleKey) {
                        type = NavType.IntType
                    }
                )
            ) {
                ChannelDetailScreen(
                    navigateToChannel = {
                        navController.navigate(Screens.Channel())
                    },
                    onBackPressed = {
                        if (navController.navigateUp()) {
                            isComingBackFromDifferentScreen = true
                        }
                    }
                )
            }
        }
    )
}
