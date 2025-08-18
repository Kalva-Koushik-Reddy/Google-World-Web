package com.example.google_world_web

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.example.google_world_web.ui.theme.GoogleWorldWebTheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import kotlin.toString

data class FileItem(
    val name: String,
    val dateAdded: Long // Use System.currentTimeMillis() for example
)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationApp() {
    var showAddFileDialog by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf("Name") } // <-- Add this line
    var viewType by remember { mutableStateOf("List") } // <-- Add this line
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    
    val items = listOf(
        NavigationItem("Recent", Icons.Default.AccessTime, "recent"),
        NavigationItem("Starred", Icons.Default.Star, "starred"),
        NavigationItem("Offline", Icons.Default.CloudOff, "offline"),
        NavigationItem("Bin", Icons.Default.Delete, "bin"),
        NavigationItem("Notifications", Icons.Default.Notifications, "notifications"),
        NavigationItem("Settings", Icons.Default.Settings, "settings"),
        NavigationItem("Help & Feedback", Icons.Default.Info, "help")
    )
    
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
                                popUpTo("home") {
                                    inclusive = true
                                }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        }
                        .padding(16.dp)
                ) {
                    Text(
                        "Google World Web",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Tap to go home",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                items.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.title) },
                        selected = navController.currentDestination?.route == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo("home") {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Custom Title Bar
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
                                if (drawerState.isClosed) {
                                    drawerState.open()
                                } else {
                                    drawerState.close()
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Search Here",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            // Place the sort and view type buttons below the title bar
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
            // Main Content with padding for title bar and sort/view row
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 140.dp) // 80dp title bar + 16dp padding + 44dp row
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        HomePage(sortBy, viewType)
                    }
                    composable("recent") {
                        RecentPage(sortBy, viewType)
                    }
                    composable("starred") {
                        StarredPage()
                    }
                    composable("offline") {
                        OfflinePage()
                    }
                    composable("bin") {
                        BinPage()
                    }
                    composable("notifications") {
                        NotificationsPage()
                    }
                    composable("settings") {
                        SettingsPage()
                    }
                    composable("help") {
                        HelpPage()
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                    }
                }

                // FAB at bottom right
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    FloatingActionButton(
                        onClick = { showAddFileDialog = true }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add File")
                    }
                }

                // Add File Dialog
                if (showAddFileDialog) {
                    AlertDialog(
                        onDismissRequest = { showAddFileDialog = false },
                        title = { Text("Add File") },
                        text = { Text("File adding functionality coming soon.") },
                        confirmButton = {
                            TextButton(onClick = { showAddFileDialog = false }) {
                                Text("OK")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ViewTypeSelector(selected: String, onSelect: (String) -> Unit) {
    Row {
        listOf("Details", "List", "Icons").forEach { type ->
            TextButton(
                onClick = { onSelect(type) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (selected == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(type)
            }
        }
    }
}

// Update HomePage and RecentPage signatures:
@Composable
fun HomePage(sortBy: String, viewType: String) {
    val files = remember {
        mutableStateListOf(
            FileItem("Document.txt", 1718000000000),
            FileItem("Image.png", 1718100000000),
            FileItem("Notes.pdf", 1718200000000)
        )
    }
    val sortedFiles = when (sortBy) {
        "Name" -> files.sortedBy { it.name }
        "Date" -> files.sortedByDescending { it.dateAdded }
        else -> files
    }
    if (viewType == "List") {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (file in sortedFiles) {
                Text("${file.name} - ${file.dateAdded}")
            }
        }
    } else {
        // Grid view
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            items(sortedFiles) { file ->
                Text("${file.name} - ${file.dateAdded}")
            }
        }
    }
}

@Composable
fun RecentPage(sortBy: String, viewType: String) {
    val files = remember {
        mutableStateListOf(
            FileItem("Recent1.txt", 1718300000000),
            FileItem("Recent2.png", 1718400000000)
        )
    }
    val sortedFiles = when (sortBy) {
        "Name" -> files.sortedBy { it.name }
        "Date" -> files.sortedByDescending { it.dateAdded }
        else -> files
    }
    if (viewType == "List") {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (file in sortedFiles) {
                Text("${file.name} - ${file.dateAdded}")
            }
        }
    } else {
        // Grid view
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            items(sortedFiles) { file ->
                Text("${file.name} - ${file.dateAdded}")
            }
        }
    }
}

@Composable
fun FileListView(files: List<FileItem>, viewType: String) {
    when (viewType) {
        "Details" -> Column {
            files.forEach { file ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Description, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(file.name, modifier = Modifier.weight(1f))
                    Text(file.dateAdded.toString())
                }
            }
        }
        "List" -> Column {
            files.forEach { file ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Description, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(file.name)
                }
            }
        }
        "Icons" -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            files.forEach { file ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(48.dp))
                    Text(file.name, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun StarredPage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Starred",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your starred items will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OfflinePage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Offline",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Offline content will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BinPage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Bin",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Deleted items will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NotificationsPage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your notifications will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsPage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "App settings will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HelpPage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Help & Feedback",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Help and feedback options will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DropdownMenuSort(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val textStyle = MaterialTheme.typography.titleMedium.copy(
        color = androidx.compose.ui.graphics.Color.Black
    )
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(
                selected,
                style = textStyle
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Name", style = textStyle) },
                onClick = {
                    onSelect("Name")
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Date", style = textStyle) },
                onClick = {
                    onSelect("Date")
                    expanded = false
                }
            )
        }
    }
}

@Composable
fun DropdownMenuViewType(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val textStyle = MaterialTheme.typography.titleMedium.copy(
        color = androidx.compose.ui.graphics.Color.Black
    )
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(
                selected,
                style = textStyle
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("List", style = textStyle) },
                onClick = {
                    onSelect("List")
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Grid", style = textStyle) },
                onClick = {
                    onSelect("Grid")
                    expanded = false
                }
            )
        }
    }
}

data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

@Preview(showBackground = true)
@Composable
fun NavigationAppPreview() {
    GoogleWorldWebTheme {
        NavigationApp()
    }
}