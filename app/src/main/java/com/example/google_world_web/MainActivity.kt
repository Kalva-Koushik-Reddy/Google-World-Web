package com.example.google_world_web

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import kotlinx.coroutines.launch
import com.example.google_world_web.ui.theme.GoogleWorldWebTheme

// ---------------------- DATA CLASSES ----------------------
data class FileItem(val name: String, val dateAdded: Long)
data class NavigationItem(val title: String, val icon: ImageVector, val route: String)

// ---------------------- MAIN ACTIVITY ----------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GoogleWorldWebTheme {
                NavigationApp()
            }
        }
    }
}

// ---------------------- APP COMPOSABLE ----------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationApp() {
    var showAddFileDialog by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf("Name") }
    var viewType by remember { mutableStateOf("List") }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    var bottomNavIndex by remember { mutableIntStateOf(0) }

    val drawerItems = listOf(
        NavigationItem("Recent", Icons.Default.AccessTime, "recent"),
        NavigationItem("Starred", Icons.Default.Star, "starred"),
        NavigationItem("Offline", Icons.Default.CloudOff, "offline"),
        NavigationItem("Bin", Icons.Default.Delete, "bin"),
        NavigationItem("Notifications", Icons.Default.Notifications, "notifications"),
        NavigationItem("Settings", Icons.Default.Settings, "settings"),
        NavigationItem("Help & Feedback", Icons.Default.Info, "help")
    )

    // Track current route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                            scope.launch { drawerState.close() }
                        }
                        .padding(16.dp)
                ) {
                    Text("Google World Web", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Tap to go home",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                drawerItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.title) },
                        selected = navController.currentDestination?.route == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // Title Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Search Here", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // Sort & View Controls (only for main content routes)
            if (currentRoute in listOf("home", "recent", "favorites", "shared", "files")) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 100.dp, start = 32.dp, end = 32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        DropdownMenuSort(sortBy) { sortBy = it }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        DropdownMenuViewType(viewType) { viewType = it }
                    }
                }
            }

            // Main Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (currentRoute in listOf("home", "recent", "favorites", "shared", "files")) 140.dp else 100.dp)
            ) {
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") { HomePage(sortBy, viewType) }
                    composable("recent") { RecentPage(sortBy, viewType) }
                    composable("starred") { StarredPage() }
                    composable("offline") { OfflinePage() }
                    composable("bin") { BinPage() }
                    composable("notifications") { NotificationsPage() }
                    composable("settings") { SettingsPage() }
                    composable("help") { HelpPage() }
                    composable("favorites") { FavoritesScreen() }
                    composable("shared") { SharedScreen() }
                    composable("files") { FilesScreen() }
                }

                // FAB
                if (currentRoute in listOf("home", "recent", "favorites", "shared", "files")) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 120.dp, end = 24.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        FloatingActionButton(onClick = { showAddFileDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add File")
                        }
                    }
                }

                // Dialog
                if (showAddFileDialog) {
                    AlertDialog(
                        onDismissRequest = { showAddFileDialog = false },
                        title = { Text("Add File") },
                        text = { Text("File adding functionality coming soon.") },
                        confirmButton = {
                            TextButton(onClick = { showAddFileDialog = false }) { Text("OK") }
                        }
                    )
                }
            }

            // Bottom Navigation (only for main content routes)
            if (currentRoute in listOf("home", "favorites", "shared", "files")) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    BottomNavigationBar(
                        currentIndex = bottomNavIndex,
                        onItemSelected = { index ->
                            bottomNavIndex = index
                            when (index) {
                                0 -> navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                    launchSingleTop = true
                                }
                                1 -> navController.navigate("favorites") {
                                    popUpTo("favorites") { inclusive = true }
                                    launchSingleTop = true
                                }
                                2 -> navController.navigate("shared") {
                                    popUpTo("shared") { inclusive = true }
                                    launchSingleTop = true
                                }
                                3 -> navController.navigate("files") {
                                    popUpTo("files") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

// ---------------------- NAVIGATION BAR ----------------------
@Composable
fun BottomNavigationBar(
    currentIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Home") },
            selected = currentIndex == 0,
            onClick = { onItemSelected(0) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Favorite, null) },
            label = { Text("Favorites") },
            selected = currentIndex == 1,
            onClick = { onItemSelected(1) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Share, null) },
            label = { Text("Shared") },
            selected = currentIndex == 2,
            onClick = { onItemSelected(2) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Folder, null) },
            label = { Text("Files") },
            selected = currentIndex == 3,
            onClick = { onItemSelected(3) }
        )
    }
}

// ---------------------- DROPDOWN MENUS ----------------------
@Composable
fun DropdownMenuSort(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) { Text(selected) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Name") }, onClick = { onSelect("Name"); expanded = false })
            DropdownMenuItem(text = { Text("Date") }, onClick = { onSelect("Date"); expanded = false })
        }
    }
}

@Composable
fun DropdownMenuViewType(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) { Text(selected) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("List") }, onClick = { onSelect("List"); expanded = false })
            DropdownMenuItem(text = { Text("Grid") }, onClick = { onSelect("Grid"); expanded = false })
        }
    }
}

// ---------------------- PAGES ----------------------
@Composable fun HomePage(sortBy: String, viewType: String) {
    val files = remember {
        mutableStateListOf(
            FileItem("Document.txt", 1718000000000),
            FileItem("Image.png", 1718100000000),
            FileItem("Notes.pdf", 1718200000000)
        )
    }
    FileListView(files.sortFiles(sortBy), viewType)
}

@Composable fun RecentPage(sortBy: String, viewType: String) {
    val files = remember {
        mutableStateListOf(
            FileItem("Recent1.txt", 1718300000000),
            FileItem("Recent2.png", 1718400000000)
        )
    }
    FileListView(files.sortFiles(sortBy), viewType)
}

fun List<FileItem>.sortFiles(sortBy: String): List<FileItem> = when (sortBy) {
    "Name" -> sortedBy { it.name }
    "Date" -> sortedByDescending { it.dateAdded }
    else -> this
}

@Composable
fun FileListView(files: List<FileItem>, viewType: String) {
    when (viewType) {
        "List" -> Column(Modifier.fillMaxSize().padding(16.dp)) {
            files.forEach { Text("${it.name} - ${it.dateAdded}") }
        }
        "Grid" -> LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(files) { Text("${it.name} - ${it.dateAdded}") }
        }
    }
}

// Other Pages (Starred, Offline, Bin, etc.)
@Composable fun StarredPage() { CenteredPage(Icons.Default.Star, "Starred", "Your starred items will appear here") }
@Composable fun OfflinePage() { CenteredPage(Icons.Default.CloudOff, "Offline", "Offline content will appear here") }
@Composable fun BinPage() { CenteredPage(Icons.Default.Delete, "Bin", "Deleted items will appear here") }
@Composable fun NotificationsPage() { CenteredPage(Icons.Default.Notifications, "Notifications", "Your notifications will appear here") }
@Composable fun SettingsPage() { CenteredPage(Icons.Default.Settings, "Settings", "App settings will appear here") }
@Composable fun HelpPage() { CenteredPage(Icons.Default.Info, "Help & Feedback", "Help and feedback options will appear here") }

// Generic page layout
@Composable
fun CenteredPage(icon: ImageVector, title: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// Dummy pages for bottom nav
@Composable fun FavoritesScreen() { CenteredPage(Icons.Default.Favorite, "Favorites", "Your favorite files will appear here") }
@Composable fun SharedScreen() { CenteredPage(Icons.Default.Share, "Shared", "Shared files will appear here") }
@Composable fun FilesScreen() { CenteredPage(Icons.Default.Folder, "Files", "All files will appear here") }

// Preview
@Preview(showBackground = true)
@Composable fun NavigationAppPreview() { GoogleWorldWebTheme { NavigationApp() } }
