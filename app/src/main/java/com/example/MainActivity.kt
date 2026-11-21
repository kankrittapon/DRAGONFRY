package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AnnouncementStudioScreen
import com.example.ui.screens.CodeExportScreen
import com.example.ui.screens.RemoteControlScreen
import com.example.ui.screens.WorldBossStudioScreen
import com.example.ui.theme.CinzelFontFamily
import com.example.ui.theme.DragonGold
import com.example.ui.theme.DragonfyreTheme
import com.example.ui.theme.EmberCrimson
import com.example.ui.theme.MetallicBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.ParchmentIvory
import com.example.ui.theme.SlateIron
import com.example.ui.viewmodel.StudioViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            DragonfyreTheme {
                DragonfyreStudioApp()
            }
        }
    }
}

private data class NavDestination(
    val route: String,
    val labelRes: Int,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
)

private val STUDIO_DESTINATIONS = listOf(
    NavDestination("hud_studio", R.string.nav_hud, Icons.Filled.Campaign, Icons.Outlined.Campaign),
    NavDestination("world_boss", R.string.nav_boss, Icons.Filled.Whatshot, Icons.Outlined.Whatshot),
    NavDestination("remote_gas", R.string.nav_remote, Icons.Filled.CloudSync, Icons.Outlined.CloudSync),
    NavDestination("code_export", R.string.nav_code, Icons.Filled.Code, Icons.Outlined.Code)
)

@Composable
fun DragonfyreStudioApp(
    viewModel: StudioViewModel = viewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val bosses by viewModel.bosses.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val generatedFiles by viewModel.generatedFiles.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val zipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            viewModel.exportProjectZip(uri)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            containerColor = ObsidianVoid,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                StudioTopHeader(
                    bossCount = bosses.count { it.enabled },
                    ruleCount = rules.count { it.enabled }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        STUDIO_DESTINATIONS.forEachIndexed { idx, dest ->
                            val selected = selectedTab == idx
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTab = idx },
                                modifier = Modifier.testTag("nav_${dest.route}"),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.filledIcon else dest.outlinedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(dest.labelRes),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DragonGold,
                                    selectedTextColor = DragonGold,
                                    indicatorColor = DragonGold.copy(alpha = 0.16f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        STUDIO_DESTINATIONS.forEachIndexed { idx, dest ->
                            val selected = selectedTab == idx
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedTab = idx },
                                modifier = Modifier.testTag("rail_${dest.route}"),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.filledIcon else dest.outlinedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = DragonGold,
                                    selectedTextColor = DragonGold,
                                    indicatorColor = DragonGold.copy(alpha = 0.16f)
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 760.dp)
                            .fillMaxWidth()
                    ) {
                        when (selectedTab) {
                            0 -> AnnouncementStudioScreen(
                                rules = rules,
                                onSaveRule = viewModel::saveRule,
                                onToggleRule = viewModel::toggleRule,
                                onDeleteRule = viewModel::deleteRule
                            )
                            1 -> WorldBossStudioScreen(
                                config = config,
                                bosses = bosses,
                                onUpdateConfig = viewModel::updateConfig,
                                onSaveBoss = viewModel::saveBoss,
                                onToggleBoss = viewModel::toggleBoss,
                                onDeleteBoss = viewModel::deleteBoss
                            )
                            2 -> RemoteControlScreen(
                                config = config,
                                bosses = bosses,
                                logs = logs,
                                connectionStatus = connectionStatus,
                                isBusy = isBusy,
                                onUpdateConfig = viewModel::updateConfig,
                                onPingGas = viewModel::pingGas,
                                onSyncHotReload = viewModel::syncHotReloadConfigToGas,
                                onSendBroadcast = viewModel::sendLiveBroadcast,
                                onSpawnBossNow = viewModel::triggerSpawnBossNow,
                                onDespawnBosses = viewModel::triggerDespawnBosses,
                                onSendCustomCommand = viewModel::sendCustomServerCommand,
                                onClearLogs = viewModel::clearLogs
                            )
                            else -> CodeExportScreen(
                                config = config,
                                files = generatedFiles,
                                onUpdateConfig = viewModel::updateConfig,
                                onRequestExportZip = {
                                    zipLauncher.launch("${config.modId}-forge-1.20.1.zip")
                                },
                                onCopiedNotice = viewModel::showMessage
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioTopHeader(
    bossCount: Int,
    ruleCount: Int
) {
    Surface(
        color = ObsidianSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(ObsidianSurface, SlateIron, ObsidianSurface)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_dragonfyre_emblem),
                        contentDescription = stringResource(R.string.cd_app_emblem),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, DragonGold.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "DRAGONFYRE FORGE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = CinzelFontFamily,
                                color = DragonGold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.subtitle_forge_version),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    color = EmberCrimson.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MetallicBorder)
                ) {
                    Text(
                        text = "$bossCount บอส • $ruleCount กฎ",
                        style = MaterialTheme.typography.labelSmall,
                        color = ParchmentIvory,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}
