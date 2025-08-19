package com.example.google_world_web

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.example.google_world_web.problem.ProblemPage
import com.example.google_world_web.ui.theme.GoogleWorldWebTheme
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

// ---------------------- CSV LOGGER ----------------------
object CsvLogger {

    private const val FILENAME = "problem_log.csv"
    private val HEADER_COLUMNS = listOf("Timestamp", "Problem Query", "App Version", "Android Version", "Device Model")

    // Function to escape strings for CSV: quotes quotes, and wraps in quotes if comma or quote exists
    private fun String.escapeCsv(): String {
        if (this.contains("\"") || this.contains(",")) {
            return "\"${this.replace("\"", "\"\"")}\""
        }
        return this
    }

    private fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "N/A"
        } catch (e: Exception) {
            Log.w("CsvLogger", "Could not get app version for ${context.packageName}", e)
            "N/A"
        }
    }

    @Synchronized
    fun logProblem(context: Context, query: String) {
        if (query.isBlank()) return

        val file = File(context.filesDir, FILENAME)
        val fileExists = file.exists()

        try {
            // Using FileWriter with append=true and BufferedWriter for efficiency
            FileWriter(file, true).buffered().use { writer ->
                if (!fileExists || file.length() == 0L) {
                    // Write header only if file is new or empty
                    writer.write(HEADER_COLUMNS.joinToString(separator = ",") { it.escapeCsv() })
                    writer.newLine()
                }

                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val appVersion = getAppVersion(context)
                val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
                val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"

                val rowData = listOf(
                    timestamp.escapeCsv(),
                    query.escapeCsv(), // Escape the query as it might contain commas or quotes
                    appVersion.escapeCsv(),
                    androidVersion.escapeCsv(),
                    deviceModel.escapeCsv()
                )
                writer.write(rowData.joinToString(separator = ","))
                writer.newLine()
                Log.d("CsvLogger", "Logged query to CSV: $query")
            }
        } catch (e: IOException) {
            Log.e("CsvLogger", "Error writing to CSV file: ${e.message}", e)
            e.printStackTrace()
        } catch (t: Throwable) {
            Log.e("CsvLogger", "Critical error logging to CSV (Throwable): ${t.message}", t)
            t.printStackTrace()
        }
    }
}


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
    val context = LocalContext.current
    var bottomNavIndex by remember { mutableIntStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }

    val drawerItems = listOf(
        NavigationItem("Recent", Icons.Default.AccessTime, "recent"),
        NavigationItem("Starred", Icons.Default.Star, "starred"),
        NavigationItem("Offline", Icons.Default.CloudOff, "offline"),
        NavigationItem("Bin", Icons.Default.Delete, "bin"),
        NavigationItem("Notifications", Icons.Default.Notifications, "settings"),
        NavigationItem("Settings", Icons.Default.Settings, "settings"),
        NavigationItem("Report Problem", Icons.Default.Info, "help")
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // CoroutineExceptionHandler for logging
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("NavigationApp", "Coroutine Exception in CSV Logging", throwable)
        scope.launch {
            snackbarHostState.showSnackbar(
                message = "Error submitting problem. Please check logs.",
                duration = SnackbarDuration.Long
            )
        }
    }

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
                        selected = currentRoute == item.route,
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
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 0.dp),
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
                        Text(
                            text = when (currentRoute) {
                                "home" -> "Home"
                                "recent" -> "Recent"
                                "starred" -> "Starred"
                                "offline" -> "Offline"
                                "bin" -> "Bin"
                                "notifications" -> "Notifications"
                                "settings" -> "Settings"
                                "help" -> "Report a Problem"
                                "favorites" -> "Favorites"
                                "shared" -> "Shared"
                                "files" -> "Files"
                                else -> "Google World Web"
                            },
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            },
            bottomBar = {
                if (currentRoute in listOf("home", "favorites", "shared", "files")) {
                    BottomNavigationBar(
                        currentIndex = bottomNavIndex,
                        onItemSelected = { index ->
                            bottomNavIndex = index
                            val route = when (index) {
                                0 -> "home"
                                1 -> "favorites"
                                2 -> "shared"
                                3 -> "files"
                                else -> "home"
                            }
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding -> // Content padding from Scaffold

            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()){
                if (currentRoute in listOf("home", "recent", "favorites", "shared", "files")) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, start = 32.dp, end = 32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DropdownMenuSort(sortBy) { sortBy = it }
                        DropdownMenuViewType(viewType) { viewType = it }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = if (currentRoute in listOf("home", "recent", "favorites", "shared", "files")) 16.dp else 8.dp)
                ) {
                    composable("home") { HomePage(sortBy, viewType) }
                    composable("recent") { RecentPage(sortBy, viewType) }
                    composable("starred") { StarredPage() }
                    composable("offline") { OfflinePage() }
                    composable("bin") { BinPage() }
                    composable("notifications") { NotificationsPage() }
                    composable("settings") { SettingsPage() }
                    composable("help") {
                        ProblemPage(
                            onSearchSubmitted = { query ->
                                scope.launch(coroutineExceptionHandler) {
                                    withContext(Dispatchers.IO) {
                                        CsvLogger.logProblem(context, query)
                                    }
                                    snackbarHostState.showSnackbar(
                                        message = "Problem submitted successfully!",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        )
                    }
                    composable("favorites") { FavoritesScreen() }
                    composable("shared") { SharedScreen() }
                    composable("files") { FilesScreen() }
                }
            }

            if (currentRoute in listOf("home", "recent", "favorites", "shared", "files")) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(bottom = 16.dp, end = 16.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    FloatingActionButton(onClick = { showAddFileDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add File")
                    }
                }
            }

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
@Composable
fun HomePage(sortBy: String, viewType: String) {
    val files = remember {
        mutableStateListOf(
            FileItem("Document.txt", 1718000000000L),
            FileItem("Image.png", 1718100000000L),
            FileItem("Notes.pdf", 1718200000000L)
        )
    }
    FileListView(files.sortFiles(sortBy), viewType, "Home Page Content")
}

@Composable
fun RecentPage(sortBy: String, viewType: String) {
    val files = remember {
        mutableStateListOf(
            FileItem("RecentDoc1.txt", 1718300000000L),
            FileItem("RecentImg2.png", 1718400000000L)
        )
    }
    FileListView(files.sortFiles(sortBy), viewType, "Recent Files Content")
}

fun List<FileItem>.sortFiles(sortBy: String): List<FileItem> = when (sortBy) {
    "Name" -> sortedBy { it.name.lowercase() }
    "Date" -> sortedByDescending { it.dateAdded }
    else -> this
}

@Composable
fun FileListView(files: List<FileItem>, viewType: String, pageTitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (files.isEmpty()) {
            Text("No files to display.", style = MaterialTheme.typography.bodyLarge)
        } else {
            when (viewType) {
                "List" -> {
                    Column(Modifier.fillMaxWidth()) {
                        files.forEach { file ->
                            Text(
                                "${file.name} - ${
                                    SimpleDateFormat(
                                        "MMM dd, yyyy",
                                        Locale.getDefault()
                                    ).format(Date(file.dateAdded))
                                }",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                            Divider()
                        }
                    }
                }
                "Grid" -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(files) { file ->
                            Card(modifier = Modifier.padding(4.dp)) {
                                Column(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(file.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        SimpleDateFormat(
                                            "MM/dd/yy",
                                            Locale.getDefault()
                                        ).format(Date(file.dateAdded)),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StarredPage() { CenteredMessagePage(Icons.Default.Star, "Starred", "Your starred items will appear here") }

@Composable
fun OfflinePage() { CenteredMessagePage(Icons.Default.CloudOff, "Offline", "Offline content will appear here") }

@Composable
fun BinPage() { CenteredMessagePage(Icons.Default.Delete, "Bin", "Deleted items will appear here") }

@Composable
fun NotificationsPage() { CenteredMessagePage(Icons.Default.Notifications, "Notifications", "Your notifications will appear here") }

@Composable
fun SettingsPage() { CenteredMessagePage(Icons.Default.Settings, "Settings", "App settings will appear here") }

@Composable
fun HelpPage(onSearchSubmitted: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    CenteredPageWithSearch(
        icon = Icons.AutoMirrored.Filled.HelpOutline,
        title = "Report a Problem",
        subtitle = "Please describe the issue you are facing in detail.",
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        onSearchDone = {
            focusManager.clearFocus()
        },
        onSubmitClicked = {
            if (searchQuery.isNotBlank()) {
                onSearchSubmitted(searchQuery)
                searchQuery = ""
                focusManager.clearFocus()
            }
        }
    )
}

@Composable
fun CenteredMessagePage(icon: ImageVector, title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun CenteredPageWithSearch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchDone: () -> Unit,
    onSubmitClicked: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(32.dp))

        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Describe your problem...") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            singleLine = false,
            maxLines = 8,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    onSearchDone()
                }
            )
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                onSubmitClicked()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = searchQuery.isNotBlank()
        ) {
            Text("SUBMIT PROBLEM")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun FavoritesScreen() { CenteredMessagePage(Icons.Default.Favorite, "Favorites", "Your favorite files will appear here") }

@Composable
fun SharedScreen() { CenteredMessagePage(Icons.Default.Share, "Shared", "Shared files will appear here") }

@Composable
fun FilesScreen() { CenteredMessagePage(Icons.Default.Folder, "Files", "All files will appear here") }

@Preview(showBackground = true, device = "id:pixel_5")
@Composable
fun NavigationAppPreview() {
    GoogleWorldWebTheme {
        NavigationApp()
    }
}

@Preview(showBackground = true)
@Composable
fun HelpPagePreview() {
    GoogleWorldWebTheme {
        HelpPage(onSearchSubmitted = {})
    }
}
