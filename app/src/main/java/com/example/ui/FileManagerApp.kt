package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerApp(viewModel: FileManagerViewModel = viewModel()) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val isOperating by viewModel.isOperating.collectAsState()
    val operationProgress by viewModel.operationProgress.collectAsState()
    val operationLabel by viewModel.operationLabel.collectAsState()

    val sandboxRoot = viewModel.repository.sandboxRoot.absolutePath

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                // Sidebar Header
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Folder, "App Icon", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                    Column {
                        Text("Pro Files", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Text("Professional Explorer", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }

                Divider(modifier = Modifier.padding(horizontal = 16.dp))

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation Items
                NavigationDrawerItem(
                    label = { Text("Dashboard") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    },
                    icon = { Icon(Icons.Default.Dashboard, "Dashboard") },
                    modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_dashboard")
                )

                NavigationDrawerItem(
                    label = { Text("Files Browser") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("browser?path=$sandboxRoot")
                    },
                    icon = { Icon(Icons.Default.FolderOpen, "Files Browser") },
                    modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_browser")
                )

                NavigationDrawerItem(
                    label = { Text("Storage Analyzer") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("analyzer")
                    },
                    icon = { Icon(Icons.Default.QueryStats, "Storage Analyzer") },
                    modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_analyzer")
                )

                NavigationDrawerItem(
                    label = { Text("Secure Vault") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("vault")
                    },
                    icon = { Icon(Icons.Default.EnhancedEncryption, "Secure Vault") },
                    modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_vault")
                )

                Spacer(modifier = Modifier.weight(1f))

                // Drawer Footer info
                Text(
                    text = "Pro Files v1.0.0 • Offline First",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Pro Files", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }, modifier = Modifier.testTag("drawer_trigger")) {
                            Icon(Icons.Default.Menu, "Menu")
                        }
                    }
                )
            },
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showBottomBar = currentRoute in listOf("dashboard", "analyzer", "vault") || (currentRoute?.startsWith("browser") == true)

                if (showBottomBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) },
                            selected = currentRoute == "dashboard",
                            onClick = {
                                if (currentRoute != "dashboard") {
                                    navController.navigate("dashboard") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                }
                            },
                            modifier = Modifier.testTag("bottom_nav_home")
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.FolderOpen, contentDescription = "Files") },
                            label = { Text("Files", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) },
                            selected = currentRoute?.startsWith("browser") == true,
                            onClick = {
                                if (currentRoute?.startsWith("browser") != true) {
                                    navController.navigate("browser?path=$sandboxRoot") {
                                        popUpTo("dashboard") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            modifier = Modifier.testTag("bottom_nav_files")
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.QueryStats, contentDescription = "Analyze") },
                            label = { Text("Analyze", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) },
                            selected = currentRoute == "analyzer",
                            onClick = {
                                if (currentRoute != "analyzer") {
                                    navController.navigate("analyzer") {
                                        popUpTo("dashboard") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            modifier = Modifier.testTag("bottom_nav_analyze")
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.EnhancedEncryption, contentDescription = "Vault") },
                            label = { Text("Vault", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) },
                            selected = currentRoute == "vault",
                            onClick = {
                                if (currentRoute != "vault") {
                                    navController.navigate("vault") {
                                        popUpTo("dashboard") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            modifier = Modifier.testTag("bottom_nav_vault")
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToBrowser = { path ->
                                navController.navigate("browser?path=$path")
                            },
                            onNavigateToAnalyzer = {
                                navController.navigate("analyzer")
                            },
                            onNavigateToVault = {
                                navController.navigate("vault")
                            },
                            onOpenTextFile = { path ->
                                viewModel.openInEditor(path)
                                navController.navigate("editor")
                            },
                            onPreviewImage = { path ->
                                navController.navigate("preview_image?path=$path")
                            },
                            onPreviewAudio = { path ->
                                navController.navigate("preview_audio?path=$path")
                            },
                            onPreviewPdf = { path ->
                                navController.navigate("preview_pdf?path=$path")
                            },
                            onPreviewApk = { path ->
                                navController.navigate("preview_apk?path=$path")
                            }
                        )
                    }

                    composable(
                        route = "browser?path={path}",
                        arguments = listOf(navArgument("path") { type = NavType.StringType; defaultValue = sandboxRoot })
                    ) { backStackEntry ->
                        val path = backStackEntry.arguments?.getString("path") ?: sandboxRoot
                        LaunchedEffect(path) {
                            viewModel.navigateTo(path)
                        }
                        BrowserScreen(
                            viewModel = viewModel,
                            onOpenTextFile = { targetPath ->
                                viewModel.openInEditor(targetPath)
                                navController.navigate("editor")
                            },
                            onPreviewImage = { targetPath ->
                                navController.navigate("preview_image?path=$targetPath")
                            },
                            onPreviewAudio = { targetPath ->
                                navController.navigate("preview_audio?path=$targetPath")
                            },
                            onPreviewPdf = { targetPath ->
                                navController.navigate("preview_pdf?path=$targetPath")
                            },
                            onPreviewApk = { targetPath ->
                                navController.navigate("preview_apk?path=$targetPath")
                            }
                        )
                    }

                    composable("analyzer") {
                        StorageAnalyzerScreen(viewModel = viewModel)
                    }

                    composable("vault") {
                        VaultScreen(viewModel = viewModel, onExit = { navController.popBackStack() })
                    }

                    composable("editor") {
                        TextEditorScreen(viewModel = viewModel, onExit = { navController.popBackStack() })
                    }

                    composable(
                        route = "preview_image?path={path}",
                        arguments = listOf(navArgument("path") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val path = backStackEntry.arguments?.getString("path") ?: ""
                        ImagePreviewScreen(path = path, onExit = { navController.popBackStack() })
                    }

                    composable(
                        route = "preview_audio?path={path}",
                        arguments = listOf(navArgument("path") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val path = backStackEntry.arguments?.getString("path") ?: ""
                        AudioPreviewScreen(path = path, onExit = { navController.popBackStack() })
                    }

                    composable(
                        route = "preview_pdf?path={path}",
                        arguments = listOf(navArgument("path") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val path = backStackEntry.arguments?.getString("path") ?: ""
                        PDFPreviewScreen(path = path, onExit = { navController.popBackStack() })
                    }

                    composable(
                        route = "preview_apk?path={path}",
                        arguments = listOf(navArgument("path") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val path = backStackEntry.arguments?.getString("path") ?: ""
                        ApkPreviewScreen(path = path, onExit = { navController.popBackStack() })
                    }
                }

                // Global Operation Progress Modal Overlays
                if (isOperating) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable(enabled = false) {},
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .testTag("operation_dialog"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = operationLabel,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                LinearProgressIndicator(
                                    progress = operationProgress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
