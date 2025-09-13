package com.example.google_world_web

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.google_world_web.problem.ProblemDetailPage
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.example.google_world_web.problem.LoggedProblemsPage
import com.example.google_world_web.problem.ProblemEntry
import com.example.google_world_web.problem.ProblemPage
import com.example.google_world_web.ui.theme.GoogleWorldWebTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

// It's good practice to define all your routes as constants
object AppRoutes {
    const val HOME = "home"
    const val RECENT = "recent"
    const val STARRED = "starred"
    const val OFFLINE = "offline"
    const val BIN = "bin"
    const val NOTIFICATIONS = "notifications_route" // Unique route
    const val SETTINGS = "settings_route"         // Unique route
    const val HELP = "help"
    const val LOGGED_PROBLEMS = "logged_problems"
    const val PROBLEM_DETAIL_BASE = "problem_detail"
    const val FAVORITES = "favorites"
    const val SHARED = "shared"
    const val FILES = "files"

    fun problemDetailRoute(): String {
        return "$PROBLEM_DETAIL_BASE/" +
                "{${NavArgKeys.PROBLEM_TIMESTAMP}}/" +
                "{${NavArgKeys.PROBLEM_QUERY}}/" +
                "{${NavArgKeys.PROBLEM_APP_VERSION}}/" +
                "{${NavArgKeys.PROBLEM_ANDROID_VERSION}}/" +
                "{${NavArgKeys.PROBLEM_DEVICE_MODEL}}"
    }
}

object NavArgKeys {
    const val PROBLEM_TIMESTAMP = "problemTimestamp"
    const val PROBLEM_QUERY = "problemQuery"
    const val PROBLEM_APP_VERSION = "problemAppVersion"
    const val PROBLEM_ANDROID_VERSION = "problemAndroidVersion"
    const val PROBLEM_DEVICE_MODEL = "problemDeviceModel"
}

// Data Classes
data class FileItem(val name: String, val dateAdded: Long)
data class NavigationItem(val title: String, val icon: ImageVector, val route: String)


// CSV Logger (remains the same)
object CsvLogger {
    private const val FILENAME = "problem_log.csv"
    private val HEADER_COLUMNS = listOf("Timestamp", "Problem Query", "App Version", "Android Version", "Device Model")

    private fun String.escapeCsv(): String {
        if (this.contains("\"") || this.contains(",")) {
            return "\"${this.replace("\"", "\"\"")}\""
        }
        return this
    }

    fun getAppVersion(context: Context): String {
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
            FileWriter(file, true).buffered().use { writer ->
                if (!fileExists || file.length() == 0L) {
                    writer.write(HEADER_COLUMNS.joinToString(separator = ",") { it.escapeCsv() })
                    writer.newLine()
                }
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val appVersion = getAppVersion(context)
                val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
                val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
                val rowData = listOf(
                    timestamp.escapeCsv(), query.escapeCsv(), appVersion.escapeCsv(),
                    androidVersion.escapeCsv(), deviceModel.escapeCsv()
                )
                writer.write(rowData.joinToString(separator = ","))
                writer.newLine()
            }
        } catch (e: IOException) {
            Log.e("CsvLogger", "Error writing to CSV file: ${e.message}", e)
        } catch (t: Throwable) {
            Log.e("CsvLogger", "Critical error logging to CSV (Throwable): ${t.message}", t)
        }
    }
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
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
    var sortBy by remember { mutableStateOf("Name") }
    var viewType by remember { mutableStateOf("List") }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    var bottomNavIndex by remember { mutableIntStateOf(0) }
    val snackBarHostState = remember { SnackbarHostState() }

    val drawerItems = listOf(
        NavigationItem("Recent", Icons.Default.AccessTime, AppRoutes.RECENT),
        NavigationItem("Starred", Icons.Default.Star, AppRoutes.STARRED),
        NavigationItem("Offline", Icons.Default.CloudOff, AppRoutes.OFFLINE),
        NavigationItem("Bin", Icons.Default.Delete, AppRoutes.BIN),
        NavigationItem("Notifications", Icons.Default.Notifications, AppRoutes.NOTIFICATIONS),
        NavigationItem("Settings", Icons.Default.Settings, AppRoutes.SETTINGS),
        NavigationItem("Report Problem", Icons.AutoMirrored.Filled.HelpOutline, AppRoutes.HELP),
        NavigationItem("Logged Problems", Icons.AutoMirrored.Filled.List, AppRoutes.LOGGED_PROBLEMS)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("NavigationApp", "Coroutine Exception", throwable)
        scope.launch {
            snackBarHostState.showSnackbar(
                message = "An error occurred: ${throwable.localizedMessage}",
                duration = SnackbarDuration.Long
            )
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp)) // Added more space at top
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate(AppRoutes.HOME) {
                                popUpTo(AppRoutes.HOME) { inclusive = true }
                            }
                            scope.launch { drawerState.close() }
                        }
                        .padding(horizontal = 28.dp, vertical = 16.dp), // Adjusted padding
                    horizontalAlignment = Alignment.CenterHorizontally // Center home icon and text
                ) {
                    Icon( // Added Home Icon
                        imageVector = Icons.Filled.Home,
                        contentDescription = "Home",
                        modifier = Modifier.size(28.dp), // Adjusted size
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp)) // Space between icon and text
                    Text("Home",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Tap to go home",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) // Divider
                drawerItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(AppRoutes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding) // Standard padding
                    )
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackBarHostState) },
            topBar = {
                val pageTitle = when (currentRoute) {
                    AppRoutes.HOME -> "Home"
                    AppRoutes.RECENT -> "Recent"
                    AppRoutes.FAVORITES -> "Favorites"
                    AppRoutes.SHARED -> "Shared"
                    AppRoutes.FILES -> "Files"
                    else -> null // Will use specific title for other pages
                }

                val topBarText = if (pageTitle != null) {
                    "Search $pageTitle"
                } else {
                    when (currentRoute) {
                        AppRoutes.STARRED -> "Starred"
                        AppRoutes.OFFLINE -> "Offline"
                        AppRoutes.BIN -> "Bin"
                        AppRoutes.NOTIFICATIONS -> "Notifications"
                        AppRoutes.SETTINGS -> "Settings"
                        AppRoutes.HELP -> "Report a Problem"
                        AppRoutes.LOGGED_PROBLEMS -> "Logged Problems"
                        // Check if currentRoute starts with problem_detail_base for detail page
                        // For this, you need to parse the route or use a more specific check
                        // For simplicity, let's assume it's handled or falls to default
                        else -> if (currentRoute?.startsWith(AppRoutes.PROBLEM_DETAIL_BASE) == true) {
                            "Problem Details"
                        } else {
                            "Google World Web" // Default
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp) // Consider CenterAlignedTopAppBar for dynamic height
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    shape = RoundedCornerShape(28.dp), // Slightly more rounded
                    color = MaterialTheme.colorScheme.surfaceVariant, // Changed color slightly
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp), // Padding inside the Surface
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } }
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                        // To make the text clickable for search (conceptual)
                        Text(
                            text = topBarText,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .clickable(
                                    enabled = pageTitle != null, // Only clickable if it's a "Search..." title
                                    onClick = {
                                        // TODO: Implement actual search UI trigger if needed
                                        Log.d("TopBar", "Search area clicked for $pageTitle")
                                    }
                                ),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        // Optional: Add a search icon if you want a dedicated search trigger
                        if (pageTitle != null) {
                            IconButton(onClick = { /* TODO: Trigger search */ }) {
                                Icon(Icons.Default.Search, contentDescription = "Search $pageTitle")
                            }
                        } else {
                            Spacer(Modifier.width(48.dp)) // Placeholder to balance menu icon
                        }
                    }
                }
            },
            bottomBar = {
                if (currentRoute in listOf(AppRoutes.HOME, AppRoutes.FAVORITES, AppRoutes.SHARED, AppRoutes.FILES)) {
                    BottomNavigationBar(
                        currentIndex = bottomNavIndex,
                        onItemSelected = { index ->
                            bottomNavIndex = index
                            val route = when (index) {
                                0 -> AppRoutes.HOME
                                1 -> AppRoutes.FAVORITES
                                2 -> AppRoutes.SHARED
                                3 -> AppRoutes.FILES
                                else -> AppRoutes.HOME
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
        ) { innerPadding ->
            Column(modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()) {
                if (currentRoute in listOf(AppRoutes.HOME, AppRoutes.RECENT, AppRoutes.FAVORITES, AppRoutes.SHARED, AppRoutes.FILES)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp), // Adjusted padding
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DropdownMenuSort(sortBy) { sortBy = it }
                        DropdownMenuViewType(viewType) { viewType = it }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = AppRoutes.HOME,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(
                            top = if (currentRoute in listOf(
                                    AppRoutes.HOME,
                                    AppRoutes.RECENT,
                                    AppRoutes.FAVORITES,
                                    AppRoutes.SHARED,
                                    AppRoutes.FILES
                                )
                            ) 8.dp else 0.dp
                        ) // Adjusted top padding
                ) {
                    composable(AppRoutes.HOME) { HomePage(sortBy, viewType) }
                    composable(AppRoutes.RECENT) { RecentPage(sortBy, viewType) }
                    composable(AppRoutes.STARRED) { StarredPage() }
                    composable(AppRoutes.OFFLINE) { OfflinePage() }
                    composable(AppRoutes.BIN) { BinPage() }
                    composable(AppRoutes.NOTIFICATIONS) { NotificationsPage() }
                    composable(AppRoutes.SETTINGS) { SettingsPage() }
                    composable(AppRoutes.HELP) {
                        val context = LocalContext.current
                        ProblemPage(
                            onSearchSubmitted = { query ->
                                scope.launch(coroutineExceptionHandler + Dispatchers.IO) {
                                    CsvLogger.logProblem(context, query)
                                    val db = FirebaseDatabase.getInstance().reference
                                    val problemRef = db.child("problems").push()
                                    val problemData = mapOf(
                                        "problemQuery" to query,
                                        "timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                                        "appVersion" to CsvLogger.getAppVersion(context),
                                        "androidVersion" to "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                                        "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}" // Added Manufacturer
                                    )
                                    problemRef.setValue(problemData).addOnCompleteListener { task ->
                                        scope.launch(Dispatchers.Main) { // Switch back to Main for UI
                                            if (task.isSuccessful) {
                                                snackBarHostState.showSnackbar(
                                                    message = "Problem submitted successfully!",
                                                    duration = SnackbarDuration.Short
                                                )
                                            } else {
                                                Log.e("ProblemPage", "Firebase submission failed", task.exception)
                                                snackBarHostState.showSnackbar(
                                                    message = "Error submitting problem: ${task.exception?.message}",
                                                    duration = SnackbarDuration.Long
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                    composable(AppRoutes.LOGGED_PROBLEMS) {
                        LoggedProblemsPage(
                            onProblemClick = { problemEntry ->
                                // Basic URL encoding for safety, especially for query
                                val encodedQuery = URLEncoder.encode(problemEntry.problemQuery, StandardCharsets.UTF_8.toString())
                                val encodedTimestamp = URLEncoder.encode(problemEntry.timestamp, StandardCharsets.UTF_8.toString())
                                val encodedAppVersion = URLEncoder.encode(problemEntry.appVersion, StandardCharsets.UTF_8.toString())
                                val encodedAndroidVersion = URLEncoder.encode(problemEntry.androidVersion, StandardCharsets.UTF_8.toString())
                                val encodedDeviceModel = URLEncoder.encode(problemEntry.deviceModel, StandardCharsets.UTF_8.toString())

                                navController.navigate(
                                    "${AppRoutes.PROBLEM_DETAIL_BASE}/" +
                                            "$encodedTimestamp/" +
                                            "$encodedQuery/" +
                                            "$encodedAppVersion/" +
                                            "$encodedAndroidVersion/" +
                                            encodedDeviceModel
                                )
                            }
                        )
                    }
                    composable(
                        route = AppRoutes.problemDetailRoute(),
                        arguments = listOf(
                            navArgument(NavArgKeys.PROBLEM_TIMESTAMP) { type = NavType.StringType },
                            navArgument(NavArgKeys.PROBLEM_QUERY) { type = NavType.StringType },
                            navArgument(NavArgKeys.PROBLEM_APP_VERSION) { type = NavType.StringType },
                            navArgument(NavArgKeys.PROBLEM_ANDROID_VERSION) { type = NavType.StringType },navArgument(NavArgKeys.PROBLEM_DEVICE_MODEL) { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val problemEntry = ProblemEntry(
                            timestamp = URLDecoder.decode(backStackEntry.arguments?.getString(NavArgKeys.PROBLEM_TIMESTAMP) ?: "", StandardCharsets.UTF_8.toString()),
                            problemQuery = URLDecoder.decode(backStackEntry.arguments?.getString(NavArgKeys.PROBLEM_QUERY) ?: "", StandardCharsets.UTF_8.toString()),
                            appVersion = URLDecoder.decode(backStackEntry.arguments?.getString(NavArgKeys.PROBLEM_APP_VERSION) ?: "N/A", StandardCharsets.UTF_8.toString()),
                            androidVersion = URLDecoder.decode(backStackEntry.arguments?.getString(NavArgKeys.PROBLEM_ANDROID_VERSION) ?: "N/A", StandardCharsets.UTF_8.toString()),
                            deviceModel = URLDecoder.decode(backStackEntry.arguments?.getString(NavArgKeys.PROBLEM_DEVICE_MODEL) ?: "N/A", StandardCharsets.UTF_8.toString())
                        )
                        ProblemDetailPage(
                            problemEntry = problemEntry,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(AppRoutes.FAVORITES) { FavoritesScreen() }
                    composable(AppRoutes.SHARED) { SharedScreen() }
                    composable(AppRoutes.FILES) { FilesScreen() }
                }
            }

            if (currentRoute in listOf(AppRoutes.HOME, AppRoutes.RECENT, AppRoutes.FAVORITES, AppRoutes.SHARED, AppRoutes.FILES)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding) // Ensure FAB is within Scaffold's content area
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
                    title = { Text("Add New Item") }, // Generic title
                    text = { Text("This feature is coming soon!") },
                    confirmButton = {
                        TextButton(onClick = { showAddFileDialog = false }) { Text("OK") }
                    }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    NavigationBar { // Using Material3 NavigationBar
        val items = listOf(
            Triple("Home", Icons.Default.Home, AppRoutes.HOME),
            Triple("Favorites", Icons.Default.Favorite, AppRoutes.FAVORITES),
            Triple("Shared", Icons.Default.Share, AppRoutes.SHARED),
            Triple("Files", Icons.Default.Folder, AppRoutes.FILES)
        )

        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(item.second, contentDescription = item.first) },
                label = { Text(item.first) },
                selected = currentIndex == index,
                onClick = { onItemSelected(index) }
            )
        }
    }
}

@Composable
fun DropdownMenuSort(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(selected)
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Sort by $selected")
        }
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
        TextButton(onClick = { expanded = true }) {
            Text(selected)
            Icon(Icons.Default.ArrowDropDown, contentDescription = "View as $selected")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("List") }, onClick = { onSelect("List"); expanded = false })
            DropdownMenuItem(text = { Text("Grid") }, onClick = { onSelect("Grid"); expanded = false })
        }
    }
}

// Pages (Placeholder implementations)
@Composable
fun HomePage(sortBy: String, viewType: String) {
    val files = remember {
        mutableStateListOf(
            FileItem("Alpha Document.txt", System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000), // 2 days ago
            FileItem("Beta Image.png", System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000),   // 1 day ago
            FileItem("Gamma Notes.pdf", System.currentTimeMillis())                             // Today
        )
    }
    FileListView(files.sortFiles(sortBy), viewType, "Home")
}

@Composable
fun RecentPage(sortBy: String, viewType: String) {
    val files = remember {
        mutableStateListOf(
            FileItem("RecentDoc1.txt", System.currentTimeMillis() - 5 * 60 * 60 * 1000), // 5 hours ago
            FileItem("RecentImg2.png", System.currentTimeMillis() - 2 * 60 * 60 * 1000)  // 2 hours ago
        )
    }
    FileListView(files.sortFiles(sortBy), viewType, "Recent")
}

fun List<FileItem>.sortFiles(sortBy: String): List<FileItem> = when (sortBy) {
    "Name" -> sortedBy { it.name.lowercase(Locale.getDefault()) }
    "Date" -> sortedByDescending { it.dateAdded }
    else -> this
}

@Composable
fun FileListView(files: List<FileItem>, viewType: String, pageName: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp), // Consistent padding
    ) {
        if (files.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No files in $pageName.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            when (viewType) {
                "List" -> {
                    LazyColumn( // Changed to LazyColumn for better performance with many items
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(files) { file ->
                            FileListItem(file)
                            HorizontalDivider()
                        }
                    }
                }
                "Grid" -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 128.dp), // More responsive grid
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 8.dp)
                    ) {
                        items(files) { file ->
                            FileGridItem(file)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileListItem(file: FileItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (file.name.endsWith(".png") || file.name.endsWith(".jpg")) Icons.Default.Image else Icons.Default.Description,
            contentDescription = "File type",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(file.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(file.dateAdded)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Optional: Add an options icon (three dots)
        // IconButton(onClick = { /* TODO: Show file options */ }) {
        //     Icon(Icons.Default.MoreVert, contentDescription = "More options for ${file.name}")
        // }
    }
}

@Composable
fun FileGridItem(file: FileItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (file.name.endsWith(".png") || file.name.endsWith(".jpg")) Icons.Default.Image else Icons.Default.Description,
                contentDescription = "File type",
                modifier = Modifier.size(48.dp), // Larger icon for grid
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                file.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date(file.dateAdded)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
fun StarredPage() { CenteredMessagePage(Icons.Default.Star, "Starred", "Your starred items will appear here.") }

@Composable
fun OfflinePage() { CenteredMessagePage(Icons.Default.CloudOff, "Offline", "Offline content will appear here.") }

@Composable
fun BinPage() { CenteredMessagePage(Icons.Default.Delete, "Bin", "Items in the bin will appear here.") }

@Composable
fun NotificationsPage() { CenteredMessagePage(Icons.Default.Notifications, "Notifications", "Your notifications will appear here.") }

@Composable
fun SettingsPage() { CenteredMessagePage(Icons.Default.Settings, "Settings", "App settings will appear here.") }

// ProblemPage is in its own file: problem/ProblemPage.kt

@Composable
fun CenteredMessagePage(icon: ImageVector, title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                icon,
                contentDescription = title,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// CenteredPageWithSearch is used by ProblemPage, which should be in its own file.
// If you want to keep it in MainActivity for now, ensure ProblemPage uses it correctly.

@Composable
fun FavoritesScreen() { CenteredMessagePage(Icons.Default.Favorite, "Favorites", "Your favorite files will appear here.") }

@Composable
fun SharedScreen() { CenteredMessagePage(Icons.Default.Share, "Shared", "Files shared with you will appear here.") }

@Composable
fun FilesScreen() { CenteredMessagePage(Icons.Default.Folder, "Files", "All your files will appear here.") }


@Preview(showBackground = true, device = "id:pixel_5", name = "Full App Preview")
@Composable
fun NavigationAppPreview() {
    GoogleWorldWebTheme {
        NavigationApp()
    }
}

@Preview(showBackground = true, name = "Home Page List Preview")
@Composable
fun HomePageListPreview() {
    GoogleWorldWebTheme {
        HomePage(sortBy = "Name", viewType = "List")
    }
}

@Preview(showBackground = true, name = "Home Page Grid Preview")
@Composable
fun HomePageGridPreview() {
    GoogleWorldWebTheme {
        HomePage(sortBy = "Date", viewType = "Grid")
    }
}
