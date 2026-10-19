package com.m3u.smartphone.ui

import android.app.ActivityOptions
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.text.input.TextFieldState
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopSearchBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import androidx.paging.PagingData
import com.m3u.business.playlist.configuration.PlaylistConfigurationNavigation
import com.m3u.core.foundation.architecture.preferences.PreferencesKeys
import com.m3u.core.foundation.architecture.preferences.ThemeStyle
import com.m3u.core.foundation.architecture.preferences.preferenceOf
import com.m3u.core.foundation.wrapper.eventOf
import com.m3u.business.playlist.ChannelWithProgramme
import com.m3u.data.service.MediaCommand
import com.m3u.data.tv.model.RemoteDirection
import com.m3u.business.setting.PresetImportViewModel
import com.m3u.smartphone.R
import com.m3u.i18n.R as I18nR
import com.m3u.i18n.R.string
import com.m3u.smartphone.ui.business.channel.PlayerActivity
import com.m3u.smartphone.ui.business.playlist.components.ChannelGallery
import com.m3u.smartphone.ui.common.AppNavHost
import com.m3u.smartphone.ui.common.connect.RemoteControlSheet
import com.m3u.smartphone.ui.common.connect.RemoteControlSheetValue
import com.m3u.smartphone.ui.common.helper.LocalHelper
import com.m3u.smartphone.ui.common.internal.Events
import com.m3u.smartphone.ui.common.helper.Metadata
import com.m3u.smartphone.ui.material.components.Destination
import com.m3u.smartphone.ui.material.components.SettingDestination
import com.m3u.smartphone.ui.material.components.SnackHost
import com.m3u.smartphone.ui.material.components.TvKeyboard
import com.m3u.smartphone.ui.material.components.withEditorialVoice
import com.m3u.smartphone.ui.material.model.LocalThemeStyle
import com.m3u.smartphone.ui.material.model.LocalSpacing
import com.m3u.smartphone.ui.navigation.AppContentInsets
import com.m3u.smartphone.ui.navigation.AppNavigationMode
import com.m3u.smartphone.ui.navigation.FloatingAppNavigationDock
import com.m3u.smartphone.ui.navigation.calculateContentBottomPadding
import com.m3u.smartphone.ui.navigation.calculateLayoutPadding
import com.m3u.smartphone.ui.navigation.resolveAppNavigationMode
import com.m3u.smartphone.ui.navigation.shouldCaptureNavigationBackdrop
import com.m3u.smartphone.ui.navigation.shouldReserveBottomNavigationSpace
import com.m3u.smartphone.ui.navigation.shouldShowBottomEdgeBlur
import com.m3u.smartphone.ui.navigation.shouldShowBottomNavigation
import com.m3u.smartphone.ui.navigation.shouldShowContextualTopBar
import com.m3u.smartphone.ui.navigation.shouldShowRemoteControlAction
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
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
        onSearchQuery = { query -> viewModel.searchQuery.value = query },
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
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AppImpl(
    navController: NavHostController,
    channels: Flow<PagingData<ChannelWithProgramme>>,
    onSearchQuery: (String) -> Unit,
    isRemoteControlSheetVisible: Boolean,
    remoteControlSheetValue: RemoteControlSheetValue,
    openRemoteControlSheet: () -> Unit,
    onCode: (String) -> Unit,
    checkTvCodeOnSmartphone: () -> Unit,
    forgetTvCodeOnSmartphone: () -> Unit,
    onRemoteDirection: (RemoteDirection) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val spacing = LocalSpacing.current
    val helper = LocalHelper.current
    val coroutineScope = rememberCoroutineScope()
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    val navigationBackdrop = rememberLayerBackdrop()
    var nestedDetailVisible by remember { mutableStateOf(false) }
    val onNestedDetailVisibilityChanged = remember {
        { visible: Boolean -> nestedDetailVisible = visible }
    }

    // Preset import dialog (fork feature: first-run playlist import)
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
    // channel. We keep this at the App level (rather than inside the navigation
    // dock) so the edge-case LaunchedEffect below can redirect away from the
    // Favorite route if the last favourite is removed.
    val appViewModel: AppViewModel = hiltViewModel()
    val hasFavorites by appViewModel.hasFavorites.collectAsState()

    val entry by navController.currentBackStackEntryAsState()
    val currentDestination by remember(entry) {
        derivedStateOf {
            Destination.of(entry?.destination?.route)
        }
    }
    val isRootPlaylistConfiguration =
        entry?.destination?.route ==
            PlaylistConfigurationNavigation.PLAYLIST_CONFIGURATION_ROUTE
    val navigationMode = resolveAppNavigationMode(
        with(density) {
            LocalWindowInfo.current.containerSize.width.toDp()
        },
    )
    val searchActive = searchBarState.currentValue != SearchBarValue.Collapsed ||
        searchBarState.targetValue != SearchBarValue.Collapsed
    val imeVisible = WindowInsets.isImeVisible
    val isTopLevelRoute = currentDestination != null && !nestedDetailVisible
    val showBottomNavigation = shouldShowBottomNavigation(
        mode = navigationMode,
        isTopLevelRoute = isTopLevelRoute,
        isSearchActive = searchActive,
        isImeVisible = imeVisible,
    )
    val bottomNavigationVisibility = remember(navigationMode) {
        MutableTransitionState(showBottomNavigation)
    }
    LaunchedEffect(showBottomNavigation, bottomNavigationVisibility) {
        bottomNavigationVisibility.targetState = showBottomNavigation
    }
    val supportsBackdropEffects = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val captureNavigationBackdrop = shouldCaptureNavigationBackdrop(
        mode = navigationMode,
        supportsBackdropEffects = supportsBackdropEffects,
        isNavigationCurrentlyVisible = bottomNavigationVisibility.currentState,
        isNavigationTargetVisible = bottomNavigationVisibility.targetState,
    )
    val bottomNavigationOccupiesSpace = shouldReserveBottomNavigationSpace(
        mode = navigationMode,
        isNavigationCurrentlyVisible = bottomNavigationVisibility.currentState,
        isNavigationTargetVisible = bottomNavigationVisibility.targetState,
    )
    val remoteControlVisible = shouldShowRemoteControlAction(
        remoteControlEnabled = remoteControl,
        isSearchActive = searchActive,
        isImeVisible = imeVisible,
        isRootPlaylistConfiguration = isRootPlaylistConfiguration,
        isNestedDetailVisible = nestedDetailVisible,
    )

    var measuredNavigationHeight by remember { mutableStateOf(64.dp) }
    val safeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues()
    val safeBottomInset = safeDrawingPadding.calculateBottomPadding()
    val layoutPadding = calculateLayoutPadding(
        mode = navigationMode,
        safeStartInset = safeDrawingPadding.calculateStartPadding(layoutDirection),
        safeEndInset = safeDrawingPadding.calculateEndPadding(layoutDirection),
    )
    val contentBottomPadding = calculateContentBottomPadding(
        mode = navigationMode,
        isTopLevelRoute = bottomNavigationOccupiesSpace,
        safeBottomInset = safeBottomInset,
        measuredNavigationHeight = measuredNavigationHeight,
        floatingUtilityHeight = if (
            navigationMode == AppNavigationMode.SideRail &&
            remoteControlVisible
        ) {
            REMOTE_CONTROL_FAB_SIZE
        } else {
            0.dp
        },
        regularContentSpacing = spacing.medium,
    )
    val contentInsets = AppContentInsets(
        layoutPadding = layoutPadding,
        contentPadding = PaddingValues(bottom = contentBottomPadding),
        navigationClearance = if (bottomNavigationOccupiesSpace) {
            safeBottomInset + FLOATING_NAVIGATION_BOTTOM_GAP + measuredNavigationHeight
        } else {
            safeBottomInset
        },
    )

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }.collect(onSearchQuery)
    }

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
            val options = ActivityOptions.makeCustomAnimation(context, 0, 0)
            context.startActivity(
                Intent(context, PlayerActivity::class.java),
                options.toBundle(),
            )
        }
    }
    val movableAppContent = remember {
        movableContentOf<AppContentArguments> { arguments ->
            AppContent(
                navController = arguments.navController,
                channels = arguments.channels,
                searchBarState = arguments.searchBarState,
                textFieldState = arguments.textFieldState,
                navigateToDestination = arguments.navigateToDestination,
                navigateToChannel = arguments.navigateToChannel,
                contentPadding = arguments.contentPadding,
                showBottomEdgeBlur = arguments.showBottomEdgeBlur,
                showContextualTopBar = arguments.showContextualTopBar,
                onNestedDetailVisibilityChanged =
                    arguments.onNestedDetailVisibilityChanged,
            )
        }
    }
    val appContentArguments = AppContentArguments(
        navController = navController,
        channels = channels,
        searchBarState = searchBarState,
        textFieldState = textFieldState,
        navigateToDestination = { navController.navigate(it.name) },
        navigateToChannel = navigateToChannel,
        contentPadding = contentInsets.contentPadding,
        showBottomEdgeBlur = shouldShowBottomEdgeBlur(navigationMode),
        showContextualTopBar = shouldShowContextualTopBar(
            isRootPlaylistConfiguration = isRootPlaylistConfiguration,
            isNestedDetailVisible = nestedDetailVisible,
        ),
        onNestedDetailVisibilityChanged = onNestedDetailVisibilityChanged,
    )

    // Fork conditional tabs: the Extension destination is deep-link only and the
    // Favorite tab is hidden until the user has at least one favourite channel.
    val visibleDestinations = remember(hasFavorites) {
        Destination.entries
            .filter { it != Destination.Extension }
            .filter { it != Destination.Favorite || hasFavorites }
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        when (navigationMode) {
            AppNavigationMode.BottomOverlay -> {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (captureNavigationBackdrop) {
                                Modifier.layerBackdrop(navigationBackdrop)
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentInsets.layoutPadding),
                    ) {
                        movableAppContent(appContentArguments)
                    }
                }
            }

            AppNavigationMode.SideRail -> {
                NavigationSuiteScaffold(
                    navigationSuiteItems = {
                        visibleDestinations.forEach { destination ->
                            val selected = destination == currentDestination
                            item(
                                icon = {
                                    Icon(
                                        imageVector = if (selected) {
                                            destination.selectedIcon
                                        } else {
                                            destination.unselectedIcon
                                        },
                                        contentDescription = stringResource(destination.iconTextId),
                                    )
                                },
                                label = {
                                    Text(stringResource(destination.iconTextId))
                                },
                                selected = selected,
                                onClick = { navigateToDestination(destination) },
                                alwaysShowLabel = false,
                                modifier = Modifier.testTag(
                                    "side-navigation-item:${destination.name}"
                                ),
                            )
                        }
                    },
                    layoutType = NavigationSuiteType.NavigationRail,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentInsets.layoutPadding),
                    ) {
                        movableAppContent(appContentArguments)
                        AppUtilityLayer(
                            remoteControlVisible = remoteControlVisible,
                            bottomNavigationVisible = false,
                            navigationClearance = contentInsets.navigationClearance,
                            safeBottomInset = safeBottomInset,
                            imeBottomInset = WindowInsets.ime
                                .asPaddingValues()
                                .calculateBottomPadding(),
                            onOpenRemoteControlSheet = openRemoteControlSheet,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }
        }

        if (navigationMode == AppNavigationMode.BottomOverlay) {
            AppUtilityLayer(
                remoteControlVisible = false,
                bottomNavigationVisible = bottomNavigationOccupiesSpace,
                navigationClearance = contentInsets.navigationClearance,
                safeBottomInset = safeBottomInset,
                imeBottomInset = WindowInsets.ime.asPaddingValues().calculateBottomPadding(),
                onOpenRemoteControlSheet = openRemoteControlSheet,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(contentInsets.layoutPadding),
            )
        }

        if (navigationMode == AppNavigationMode.BottomOverlay) {
            AnimatedVisibility(
                visibleState = bottomNavigationVisibility,
                enter = slideInVertically(initialOffsetY = { it / 3 }) +
                    fadeIn() +
                    scaleIn(initialScale = 0.94f),
                exit = slideOutVertically(targetOffsetY = { it / 3 }) +
                    fadeOut() +
                    scaleOut(targetScale = 0.94f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = safeDrawingPadding.calculateStartPadding(layoutDirection),
                        end = safeDrawingPadding.calculateEndPadding(layoutDirection),
                        bottom = safeBottomInset + FLOATING_NAVIGATION_BOTTOM_GAP,
                    ),
            ) {
                FloatingAppNavigationDock(
                    selectedDestination = currentDestination,
                    backdrop = navigationBackdrop,
                    useBackdropEffects = supportsBackdropEffects,
                    enabled = showBottomNavigation,
                    remoteControlVisible = remoteControlVisible,
                    onDestinationSelected = navigateToDestination,
                    onOpenRemoteControl = openRemoteControlSheet,
                    onHeightChanged = { measuredNavigationHeight = it },
                    destinations = visibleDestinations,
                )
            }
        }

        RemoteControlSheet(
            value = remoteControlSheetValue,
            visible = isRemoteControlSheetVisible,
            onCode = onCode,
            checkTvCodeOnSmartphone = checkTvCodeOnSmartphone,
            forgetTvCodeOnSmartphone = forgetTvCodeOnSmartphone,
            onRemoteDirection = onRemoteDirection,
            onDismissRequest = onDismissRequest,
        )
    }
}

private class AppContentArguments(
    val navController: NavHostController,
    val channels: Flow<PagingData<ChannelWithProgramme>>,
    val searchBarState: SearchBarState,
    val textFieldState: TextFieldState,
    val navigateToDestination: (Destination) -> Unit,
    val navigateToChannel: () -> Unit,
    val contentPadding: PaddingValues,
    val showBottomEdgeBlur: Boolean,
    val showContextualTopBar: Boolean,
    val onNestedDetailVisibilityChanged: (Boolean) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppContent(
    navController: NavHostController,
    channels: Flow<PagingData<ChannelWithProgramme>>,
    searchBarState: SearchBarState,
    textFieldState: TextFieldState,
    navigateToDestination: (Destination) -> Unit,
    navigateToChannel: () -> Unit,
    contentPadding: PaddingValues,
    showBottomEdgeBlur: Boolean,
    showContextualTopBar: Boolean,
    onNestedDetailVisibilityChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val helper = LocalHelper.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val entry by navController.currentBackStackEntryAsState()
    val currentDestination by remember(entry) {
        derivedStateOf {
            Destination.of(entry?.destination?.route)
        }
    }
    val isTvDevice = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION
    val searchQuery by remember {
        derivedStateOf { textFieldState.text.toString() }
    }
    val contextualTitleStyle = if (LocalThemeStyle.current == ThemeStyle.WARM_EDITORIAL) {
        MaterialTheme.typography.titleLarge.withEditorialVoice()
    } else {
        MaterialTheme.typography.titleLarge
    }
    val inputField = @Composable {
        SearchBarDefaults.InputField(
            searchBarState = searchBarState,
            textFieldState = textFieldState,
            onSearch = { coroutineScope.launch { searchBarState.animateToCollapsed() } },
            placeholder = { Text(stringResource(string.ui_search_placeholder)) },
            leadingIcon = {
                if (searchBarState.currentValue == SearchBarValue.Expanded) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                searchBarState.animateToCollapsed()
                            }
                        },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(
                                string.ui_cd_top_bar_on_back_pressed,
                            ),
                        )
                    }
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            },
            trailingIcon = {
                Icon(Icons.Default.MoreVert, contentDescription = null)
            },
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (showContextualTopBar) {
            TopAppBar(
                title = {
                    Text(
                        text = Metadata.title,
                        style = contextualTitleStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    Metadata.fob?.let { backAction ->
                        IconButton(onClick = backAction.onClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(
                                    string.ui_cd_top_bar_on_back_pressed
                                ),
                            )
                        }
                    }
                },
                actions = {
                    Metadata.actions.forEach { action ->
                        IconButton(
                            onClick = action.onClick,
                            enabled = action.enabled,
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.contentDescription,
                            )
                        }
                    }
                },
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
            )
        } else if (currentDestination == Destination.Foryou) {
            // Fork app header banner on Foryou (no search bar on this tab)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                // Debug info icon — shows worker status when debug_config.json enables it
                val debugContext = LocalContext.current
                val showDebugOverlay = remember {
                    try {
                        val file = java.io.File(debugContext.filesDir, "debug_config.json")
                        if (file.exists()) {
                            val json = file.readText()
                            json.contains("\"show_debug_overlay\"") && json.contains("true")
                        } else false
                    } catch (_: Exception) { false }
                }
                if (showDebugOverlay) {
                    var showDebugDialog by remember { mutableStateOf(false) }
                    val debugVm: AppViewModel = hiltViewModel()
                    val workerInfos by debugVm.debugWorkerInfos.collectAsState()

                    IconButton(onClick = { showDebugDialog = true }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Debug info",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    if (showDebugDialog) {
                        AlertDialog(
                            onDismissRequest = { showDebugDialog = false },
                            title = { Text("Worker Debug") },
                            text = {
                                Column {
                                    if (workerInfos.isEmpty()) {
                                        Text("No active workers")
                                    } else {
                                        workerInfos.forEach { info ->
                                            Text(
                                                text = info,
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showDebugDialog = false }) {
                                    Text("Close")
                                }
                            },
                        )
                    }
                }
                IconButton(onClick = {
                    navigateToDestination(Destination.Setting)
                    Events.settingDestination = eventOf(SettingDestination.Playlists)
                }) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Add playlist",
                    )
                }
            }
        } else if (currentDestination == Destination.Setting) {
            // Fork: plain title instead of the search bar on the Setting tab
            Text(
                text = stringResource(I18nR.string.ui_title_setting),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        } else if (isTvDevice) {
            // Fork: on TV form factors the IME-based search field is unusable with
            // a DPad — render a clickable search row plus the focusable grid keyboard
            var showTvKeyboard by remember { mutableStateOf(false) }

            BackHandler(showTvKeyboard) { showTvKeyboard = false }
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
                        shape = RoundedCornerShape(28.dp),
                    )
                    .clickable { showTvKeyboard = true }
                    .focusable()
                    .onFocusChanged { }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Text(
                    text = searchQuery.ifEmpty { stringResource(string.ui_search_placeholder) },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (searchQuery.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(start = 12.dp),
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
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            TopSearchBar(
                state = searchBarState,
                inputField = inputField,
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
            )
            ExpandedFullScreenSearchBar(
                inputField = inputField,
                state = searchBarState,
            ) {
                BackHandler {
                    coroutineScope.launch {
                        searchBarState.animateToCollapsed()
                    }
                }
                val state = rememberLazyStaggeredGridState()
                ChannelGallery(
                    state = state,
                    rowCount = 1,
                    channels = channels,
                    zapping = null,
                    recently = false,
                    isVodOrSeriesPlaylist = false,
                    onClick = { channel ->
                        coroutineScope.launch {
                            helper.play(MediaCommand.Common(channel.id))
                            navigateToChannel()
                        }
                    },
                    onLongClick = {},
                    reloadThumbnail = { null },
                    syncThumbnail = { null },
                    contentPadding = WindowInsets.ime.asPaddingValues(),
                )
            }
        }
        AppNavHost(
            navController = navController,
            navigateToDestination = navigateToDestination,
            navigateToChannel = navigateToChannel,
            searchQuery = searchQuery,
            onCollapseSearch = {
                coroutineScope.launch { searchBarState.animateToCollapsed() }
            },
            contentPadding = contentPadding,
            showBottomEdgeBlur = showBottomEdgeBlur,
            onNestedDetailVisibilityChanged = onNestedDetailVisibilityChanged,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@Composable
private fun AppUtilityLayer(
    remoteControlVisible: Boolean,
    bottomNavigationVisible: Boolean,
    navigationClearance: Dp,
    safeBottomInset: Dp,
    imeBottomInset: Dp,
    onOpenRemoteControlSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val targetBottomPadding = if (bottomNavigationVisible) {
        navigationClearance + FLOATING_NAVIGATION_CONTENT_GAP
    } else {
        maxOf(safeBottomInset, imeBottomInset) + spacing.medium
    }
    val bottomPadding by animateDpAsState(
        targetValue = targetBottomPadding,
        label = "app-utility-bottom-padding",
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.small, Alignment.End),
        verticalAlignment = Alignment.Bottom,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = spacing.medium,
                end = spacing.medium,
                bottom = bottomPadding,
            ),
    ) {
        SnackHost(Modifier.weight(1f))
        AnimatedVisibility(
            visible = remoteControlVisible,
            enter = scaleIn(initialScale = 0.65f) + fadeIn(),
            exit = scaleOut(targetScale = 0.65f) + fadeOut(),
        ) {
            FloatingActionButton(
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = spacing.none,
                    pressedElevation = spacing.none,
                    focusedElevation = spacing.extraSmall,
                    hoveredElevation = spacing.extraSmall,
                ),
                onClick = onOpenRemoteControlSheet,
            ) {
                Icon(
                    imageVector = Icons.Rounded.SettingsRemote,
                    contentDescription = stringResource(string.feat_setting_remote_control),
                )
            }
        }
    }
}

private val FLOATING_NAVIGATION_BOTTOM_GAP = 12.dp
private val FLOATING_NAVIGATION_CONTENT_GAP = 12.dp
private val REMOTE_CONTROL_FAB_SIZE = 56.dp
