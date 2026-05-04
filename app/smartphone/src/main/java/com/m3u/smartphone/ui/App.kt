package com.m3u.smartphone.ui

import android.app.ActivityOptions
import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopSearchBar
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import androidx.paging.PagingData
import com.m3u.core.foundation.architecture.preferences.PreferencesKeys
import com.m3u.core.foundation.architecture.preferences.preferenceOf
import com.m3u.core.wrapper.eventOf
import com.m3u.data.database.model.Channel
import com.m3u.data.service.MediaCommand
import com.m3u.data.tv.model.RemoteDirection
import com.m3u.business.setting.PresetImportViewModel
import com.m3u.smartphone.R
import com.m3u.i18n.R as I18nR
import com.m3u.smartphone.ui.business.channel.PlayerActivity
import com.m3u.smartphone.ui.common.AppNavHost
import com.m3u.smartphone.ui.common.connect.RemoteControlSheet
import com.m3u.smartphone.ui.common.connect.RemoteControlSheetValue
import com.m3u.smartphone.ui.common.helper.LocalHelper
import com.m3u.smartphone.ui.common.internal.Events
import com.m3u.smartphone.ui.material.components.Destination
import com.m3u.smartphone.ui.material.components.SettingDestination
import com.m3u.smartphone.ui.material.components.SnackHost
import com.m3u.smartphone.ui.material.components.TvKeyboard
import com.m3u.smartphone.ui.material.model.LocalSpacing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun App(
    modifier: Modifier = Modifier,
) {
    val viewModel: AppViewModel = hiltViewModel()
    val navController = rememberNavController()

    AppImpl(
        navController = navController,
        channels = viewModel.channels,
        isRemoteControlSheetVisible = viewModel.isConnectSheetVisible,
        remoteControlSheetValue = viewModel.remoteControlSheetValue,
        openRemoteControlSheet = { viewModel.isConnectSheetVisible = true },
        onCode = { viewModel.code = it },
        checkTvCodeOnSmartphone = viewModel::checkTvCodeOnSmartphone,
        forgetTvCodeOnSmartphone = viewModel::forgetTvCodeOnSmartphone,
        onRemoteDirection = viewModel::onRemoteDirection,
        onDismissRequest = {
            viewModel.code = ""
            viewModel.isConnectSheetVisible = false
        },
        modifier = modifier
    )
}

@Composable
private fun AppImpl(
    navController: NavHostController,
    channels: Flow<PagingData<Channel>>,
    isRemoteControlSheetVisible: Boolean,
    remoteControlSheetValue: RemoteControlSheetValue,
    openRemoteControlSheet: () -> Unit,
    onCode: (String) -> Unit,
    checkTvCodeOnSmartphone: () -> Unit,
    forgetTvCodeOnSmartphone: () -> Unit,
    onRemoteDirection: (RemoteDirection) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val spacing = LocalSpacing.current

    // Preset import dialog
    val presetVm: PresetImportViewModel = hiltViewModel()
    val showPresetDialog by presetVm.showDialog.collectAsState()
    val isImporting by presetVm.importing.collectAsState()
    val progressPct by presetVm.progress.collectAsState()
    val statusText by presetVm.statusText.collectAsState()
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(showPresetDialog) {
        if (showPresetDialog) presetVm.importAll()
    }

    if (showPresetDialog && !dismissed) {
        AlertDialog(
            onDismissRequest = { dismissed = true },
            title = { Text("Loading Playlists") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isImporting && progressPct >= 0) {
                        LinearProgressIndicator(
                            progress = { progressPct / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (isImporting) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Text(statusText)
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { dismissed = true }) {
                    Text("Close")
                }
            }
        )
    }

    val zappingMode by preferenceOf(PreferencesKeys.ZAPPING_MODE)
    val remoteControl by preferenceOf(PreferencesKeys.REMOTE_CONTROL)

    // Favorite tab is only surfaced after the user has favourited at least one
    // channel. We keep this at the App level (rather than inside
    // NavigationSuiteScaffold) so the edge-case LaunchedEffect below can
    // redirect away from the Favorite route if the last favourite is removed.
    val appViewModel: AppViewModel = hiltViewModel()
    val hasFavorites by appViewModel.hasFavorites.collectAsState()

    val entry by navController.currentBackStackEntryAsState()

    val isTvDevice = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION

    val currentDestination by remember {
        derivedStateOf {
            Destination.of(entry?.destination?.route)
        }
    }
    val isOnSearchTab = currentDestination == Destination.Search
    val isOnRootTab = currentDestination != null
    val isOnPlaylistPage = !isOnRootTab

    val navigateToDestination = { destination: Destination ->
        navController.navigate(destination.name, navOptions {
            popUpTo(destination.name) {
                inclusive = true
            }
        })
    }

    // If the user is sitting on the Favorite tab and they unfavourite the last
    // channel, the tab will disappear from the nav bar on the next recomposition.
    // Redirect them to Foryou so they are never stranded on a route whose tab
    // is no longer visible.
    LaunchedEffect(hasFavorites, currentDestination) {
        if (!hasFavorites && currentDestination == Destination.Favorite) {
            navigateToDestination(Destination.Foryou)
        }
    }

    val navigateToChannel: () -> Unit = {
        if (!zappingMode || !PlayerActivity.isInPipMode) {
            val options = ActivityOptions.makeCustomAnimation(
                context,
                0,
                0
            )
            context.startActivity(
                Intent(context, PlayerActivity::class.java),
                options.toBundle()
            )
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            Destination.entries
                .filter { it != Destination.Extension }
                .filter { it != Destination.Favorite || hasFavorites }
                .forEach { destination ->
                val isSelected = destination == currentDestination
                item(
                    icon = {
                        Icon(
                            imageVector = when {
                                isSelected -> destination.selectedIcon
                                else -> destination.unselectedIcon
                            },
                            contentDescription = stringResource(destination.iconTextId)
                        )
                    },
                    label = {
                        Text(stringResource(destination.iconTextId))
                    },
                    selected = isSelected,
                    onClick = { navigateToDestination(destination) },
                    alwaysShowLabel = false
                )
            }
        },
        modifier = modifier
    ) {
        Column {
            val coroutineScope = rememberCoroutineScope()
            val searchBarState = rememberSearchBarState()
            val textFieldState = rememberTextFieldState()
            val searchQuery by remember {
                derivedStateOf { textFieldState.text.toString() }
            }

            // Save search text when leaving Search tab, restore when returning
            var savedSearchQuery by remember { mutableStateOf("") }
            LaunchedEffect(isOnSearchTab) {
                if (isOnSearchTab) {
                    if (savedSearchQuery.isNotEmpty()) {
                        textFieldState.edit {
                            replace(0, length, savedSearchQuery)
                        }
                    }
                } else {
                    savedSearchQuery = textFieldState.text.toString()
                    textFieldState.edit { replace(0, length, "") }
                    searchBarState.animateToCollapsed()
                }
            }

            val inputField = @Composable {
                SearchBarDefaults.InputField(
                    searchBarState = searchBarState,
                    textFieldState = textFieldState,
                    onSearch = {
                        // On playlist pages, collapse after search submit
                        if (isOnPlaylistPage) {
                            coroutineScope.launch { searchBarState.animateToCollapsed() }
                        }
                    },
                    placeholder = {
                        Text(
                            if (isOnPlaylistPage) "Filter in playlist..."
                            else "Search..."
                        )
                    },
                    leadingIcon = {
                        if (isOnPlaylistPage && searchBarState.currentValue == SearchBarValue.Expanded) {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        textFieldState.edit { replace(0, length, "") }
                                        searchBarState.animateToCollapsed()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Default.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                    },
                    trailingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
                )
            }

            val isOnForyouTab = currentDestination == Destination.Foryou
            val isOnSettingTab = currentDestination == Destination.Setting
            if (isOnForyouTab) {
                // App header banner on Foryou tab
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = {
                        navigateToDestination(Destination.Setting)
                        Events.settingDestination = eventOf(SettingDestination.Playlists)
                    }) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "Add playlist"
                        )
                    }
                }
            } else if (isOnSettingTab) {
                Text(
                    text = stringResource(I18nR.string.ui_title_setting),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            } else if (isTvDevice) {
                // TV: clickable search bar — tap to show keyboard
                var showTvKeyboard by remember { mutableStateOf(false) }

                // Dismiss keyboard on back
                BackHandler(showTvKeyboard) { showTvKeyboard = false }

                // Hide keyboard when navigating away
                LaunchedEffect(currentDestination) { showTvKeyboard = false }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .border(
                            width = 1.dp,
                            color = if (showTvKeyboard) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(28.dp)
                        )
                        .clickable { showTvKeyboard = true }
                        .focusable()
                        .onFocusChanged { if (it.isFocused && !showTvKeyboard) { /* don't auto-show */ } }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Text(
                        text = searchQuery.ifEmpty {
                            if (isOnPlaylistPage) "Filter in playlist..." else "Search..."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (searchQuery.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                if (showTvKeyboard) {
                    TvKeyboard(
                        onChar = { ch ->
                            textFieldState.edit { append(ch.toString()) }
                        },
                        onBackspace = {
                            textFieldState.edit {
                                if (length > 0) replace(length - 1, length, "")
                            }
                        },
                        onSpace = {
                            textFieldState.edit { append(" ") }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                TopSearchBar(
                    state = searchBarState,
                    inputField = inputField
                )
            }

            AppNavHost(
                navController = navController,
                navigateToDestination = { navController.navigate(it.name) },
                navigateToChannel = navigateToChannel,
                searchQuery = searchQuery,
                onCollapseSearch = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            // snack-host area
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.small, Alignment.End),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.medium)
            ) {
                SnackHost(Modifier.weight(1f))
                AnimatedVisibility(
                    visible = remoteControl,
                    enter = scaleIn(initialScale = 0.65f) + fadeIn(),
                    exit = scaleOut(targetScale = 0.65f) + fadeOut()
                ) {
                    FloatingActionButton(
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = spacing.none,
                            pressedElevation = spacing.none,
                            focusedElevation = spacing.extraSmall,
                            hoveredElevation = spacing.extraSmall
                        ),
                        onClick = openRemoteControlSheet
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SettingsRemote,
                            contentDescription = stringResource(com.m3u.i18n.R.string.feat_setting_remote_control)
                        )
                    }
                }
            }

            RemoteControlSheet(
                value = remoteControlSheetValue,
                visible = isRemoteControlSheetVisible,
                onCode = onCode,
                checkTvCodeOnSmartphone = checkTvCodeOnSmartphone,
                forgetTvCodeOnSmartphone = forgetTvCodeOnSmartphone,
                onRemoteDirection = onRemoteDirection,
                onDismissRequest = onDismissRequest
            )
        }
    }
}
