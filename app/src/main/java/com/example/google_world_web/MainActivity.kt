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
import androidx.compose.foundation.lazy.grid.items // Correct import
import androidx.compose.foundation.lazy.items // Correct import
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.example.google_world_web.AppRoutes.LOGIN_ROUTE
import com.example.google_world_web.problem.LoggedProblemsPage
import com.example.google_world_web.problem.ProblemEntry
import com.example.google_world_web.problem.ProblemPage
import com.example.google_world_web.ui.theme.GoogleWorldWebTheme
import com.example.google_world_web.userdata.LoginPage
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
// import java.io.File // No longer needed
// import java.io.FileWriter // No longer needed
// import java.io.IOException // No longer needed
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

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
    const val LOGIN_ROUTE = "login"

    fun problemDetailRoute(): String {
        return "$PROBLEM_DETAIL_BASE/" +
                "{${NavArgKeys.PROBLEM_ID}}/" +
                "{${NavArgKeys.PROBLEM_TIMESTAMP}}/" +
                "{${NavArgKeys.PROBLEM_QUERY}}/" +
                "{${NavArgKeys.PROBLEM_APP_VERSION}}/" +
                "{${NavArgKeys.PROBLEM_ANDROID_VERSION}}/" +
                "{${NavArgKeys.PROBLEM_DEVICE_MODEL}}/" +
                "{${NavArgKeys.PROBLEM_USER_EMAIL}}/" +
                "{${NavArgKeys.PROBLEM_OWNER_EMAIL}}"
    }

}
object NavArgKeys {
    const val PROBLEM_TIMESTAMP = "problemTimestamp"
    const val PROBLEM_QUERY = "problemQuery"
    const val PROBLEM_APP_VERSION = "problemAppVersion"
    const val PROBLEM_ANDROID_VERSION = "problemAndroidVersion"
    const val PROBLEM_DEVICE_MODEL = "problemDeviceModel"
    const val PROBLEM_USER_EMAIL = "problemUserEmail" // DEFINED HERE
    const val PROBLEM_ID = "problemId" // New
    const val PROBLEM_OWNER_EMAIL = "problemOwnerEmail"   // 👈 add this


}

// Data Classes
data class FileItem(val name: String, val dateAdded: Long)
data class NavigationItem(val title: String, val icon: ImageVector, val route: String)


// Helper function to get app version (moved from CsvLogger)
fun getAppVersionFromContext(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "N/A"
    } catch (e: Exception) {
        Log.w("AppUtils", "Could not get app version for ${context.packageName}", e)
        "N/A"
    }
}
fun sanitizeEmailForKey(email: String): String {
    return email.replace(".", "_dot_")
        .replace("#", "_hash_")
        .replace("$", "_dollar_")
        .replace("[", "_lb rack_")
        .replace("]", "_rb rack_")
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

    // State to hold the sanitized email of the logged-in user
    var currentSanitizedUserEmail by rememberSaveable { mutableStateOf<String?>(null) }
    // To store the raw email for display or other purposes if needed
    var currentUserRawEmail by rememberSaveable { mutableStateOf<String?>(null) }


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
    val shouldShowChrome = currentRoute != LOGIN_ROUTE

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            if (shouldShowChrome) { // Only show drawer if not on login
                ModalDrawerSheet {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate(AppRoutes.HOME) {
                                    popUpTo(AppRoutes.HOME) { inclusive = true }
                                }
                                scope.launch { drawerState.close() }
                            }
                            .padding(horizontal = 28.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = "Home",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Home",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Display user email if available
                        currentUserRawEmail?.let { email ->
                            Text(
                                email, // Display the raw email
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        } ?: Text(
                            "Tap to go home",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    drawerItems.forEach { item ->
                        NavigationDrawerItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = currentRoute == item.route,
                            onClick = {
                                // Prevent navigation to Logged Problems if user is not "logged in"
                                if (item.route == AppRoutes.LOGGED_PROBLEMS && currentSanitizedUserEmail == null) {
                                    scope.launch {
                                        snackBarHostState.showSnackbar(
                                            message = "Please log in to view logged problems.",
                                            duration = SnackbarDuration.Short
                                        )
                                        drawerState.close() // Close drawer without navigating
                                    }
                                } else {
                                    navController.navigate(item.route) {
                                        popUpTo(AppRoutes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    scope.launch { drawerState.close() }
                                }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        },
        gesturesEnabled = shouldShowChrome // Disable swipe-to-open on login
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackBarHostState) },
            topBar = {
                if (shouldShowChrome) {
                    val pageTitle = when (currentRoute) {
                        AppRoutes.HOME -> "Home"
                        AppRoutes.RECENT -> "Recent"
                        AppRoutes.FAVORITES -> "Favorites"
                        AppRoutes.SHARED -> "Shared"
                        AppRoutes.FILES -> "Files"
                        else -> null
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
                            AppRoutes.LOGGED_PROBLEMS -> "Logged Problems" // Title for the new page
                            else -> if (currentRoute?.startsWith(AppRoutes.PROBLEM_DETAIL_BASE) == true) {
                                "Problem Details"
                            } else {
                                "Google World Web"
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } }
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                            Text(
                                text = topBarText,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                                    .clickable(
                                        enabled = pageTitle != null,
                                        onClick = {
                                            Log.d("TopBar", "Search area clicked for $pageTitle")
                                        }
                                    ),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            if (pageTitle != null) {
                                IconButton(onClick = { /* TODO: Trigger search */ }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search $pageTitle")
                                }
                            } else {
                                Spacer(Modifier.width(48.dp)) // Placeholder
                            }
                        }
                    }
                }
            },
            bottomBar = {
                if (shouldShowChrome && currentRoute in listOf(
                        AppRoutes.HOME, AppRoutes.FAVORITES, AppRoutes.SHARED, AppRoutes.FILES
                    )
                ) {
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
            },
            floatingActionButton = {
                if (shouldShowChrome && currentRoute in listOf(
                        AppRoutes.HOME, AppRoutes.RECENT, AppRoutes.FAVORITES, AppRoutes.SHARED, AppRoutes.FILES
                    )
                ) {
                    FloatingActionButton(onClick = { showAddFileDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add File")
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                if (shouldShowChrome && currentRoute in listOf(
                        AppRoutes.HOME, AppRoutes.RECENT, AppRoutes.FAVORITES, AppRoutes.SHARED, AppRoutes.FILES
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DropdownMenuSort(sortBy) { sortBy = it }
                        DropdownMenuViewType(viewType) { viewType = it }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = LOGIN_ROUTE,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(
                            top = if (shouldShowChrome && currentRoute in listOf(
                                    AppRoutes.HOME, AppRoutes.RECENT, AppRoutes.FAVORITES,
                                    AppRoutes.SHARED, AppRoutes.FILES
                                )
                            ) 8.dp else 0.dp
                        )
                ) {
                    composable(LOGIN_ROUTE) {
                        LoginPage(
                            onLoginClicked = { email, password ->
                                Log.d("LoginPage", "Login attempt: $email")
                                val db = FirebaseDatabase.getInstance().reference
                                val sanitizedEmailKey = sanitizeEmailForKey(email)
                                val userRef = db.child("user_data").child(sanitizedEmailKey)

                                userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.exists()) {
                                            val storedPassword = snapshot.child("password").getValue(String::class.java)
                                            if (storedPassword == password) {
                                                Log.d("LoginPage", "Login successful for $email")
                                                currentSanitizedUserEmail = sanitizedEmailKey // Set user email state
                                                currentUserRawEmail = email // Set raw email
                                                scope.launch {
                                                    snackBarHostState.showSnackbar(
                                                        message = "Login Successful!",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                                navController.navigate(AppRoutes.HOME) {
                                                    popUpTo(LOGIN_ROUTE) { inclusive = true }
                                                }
                                            } else {
                                                Log.w("LoginPage", "Incorrect password for $email")
                                                scope.launch { snackBarHostState.showSnackbar("Incorrect email or password.") }
                                            }
                                        } else {
                                            Log.w("LoginPage", "User not found: $email")
                                            scope.launch { snackBarHostState.showSnackbar("Incorrect email or password.") }
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Log.e("LoginPage", "Firebase login check failed", error.toException())
                                        scope.launch { snackBarHostState.showSnackbar("Login failed: ${error.message}") }
                                    }
                                })
                            },
                            onSignUpClicked = { email, password, confirmPassword ->
                                Log.d("LoginPage", "SignUp attempt: $email")
                                val db = FirebaseDatabase.getInstance().reference
                                val sanitizedEmailKey = sanitizeEmailForKey(email)
                                val userRef = db.child("user_data").child(sanitizedEmailKey)

                                userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.exists()) {
                                            Log.w("LoginPage", "User already exists: $email")
                                            scope.launch { snackBarHostState.showSnackbar("User with this email already exists.") }
                                            // navController.navigate(LOGIN_ROUTE) // Already on login
                                        } else {
                                            val userData = mapOf(
                                                "email" to email,
                                                "password" to password // INSECURE
                                            )
                                            userRef.setValue(userData)
                                                .addOnSuccessListener {
                                                    Log.d("LoginPage", "User signed up successfully: $email")
                                                    scope.launch { snackBarHostState.showSnackbar("Signup Successful! Please login.") }
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("LoginPage", "Firebase signup failed for $email", e)
                                                    scope.launch { snackBarHostState.showSnackbar("Signup failed: ${e.message}") }
                                                }
                                        }
                                    }
                                    override fun onCancelled(error: DatabaseError) {
                                        Log.e("LoginPage", "Firebase user check failed", error.toException())
                                        scope.launch { snackBarHostState.showSnackbar("Signup process failed: ${error.message}") }
                                    }
                                })
                            }
                        )
                    }
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
                                val userEmailToLog = currentSanitizedUserEmail
                                val rawUserEmailForData = currentUserRawEmail

                                if (userEmailToLog == null || rawUserEmailForData == null) {
                                    scope.launch {
                                        snackBarHostState.showSnackbar(
                                            message = "You must be logged in to report a problem.",
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                    return@ProblemPage
                                }

                                scope.launch(coroutineExceptionHandler + Dispatchers.IO) {
                                    val db = FirebaseDatabase.getInstance().reference
                                    val problemId = db.child("universal_problems").push().key

                                    if (problemId == null) {
                                        Log.e("ProblemPage", "Failed to generate problem ID.")
                                        scope.launch(Dispatchers.Main) {
                                            snackBarHostState.showSnackbar("Error submitting problem: No ID.")
                                        }
                                        return@launch
                                    }

                                    val reporterSanitizedEmail = sanitizeEmailForKey(rawUserEmailForData)

                                    val problemData = ProblemEntry(
                                        timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                                        problemQuery = query,
                                        appVersion = getAppVersionFromContext(context),
                                        androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                                        deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                                        userEmail = reporterSanitizedEmail, // Store the sanitized email of the reporter
                                        likeCount = 0, // Initial likeCount is 0, reporter's like doesn't count
                                        likes = emptyMap() // Reporter "likes" their own problem by default
                                    )

                                    var userSpecificSuccess = false

                                    try {

                                        db.child("user_problems").child(userEmailToLog).child(problemId).setValue(problemData).await()
                                        userSpecificSuccess = true
                                        Log.d("ProblemPage", "Problem saved to user_problems/$userEmailToLog")

                                        scope.launch(Dispatchers.Main) {
                                            snackBarHostState.showSnackbar("Problem submitted successfully!")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ProblemPage", "Firebase submission failed", e)
                                        scope.launch(Dispatchers.Main) {
                                            var errorMsg = "Error submitting problem."
                                            if (!userSpecificSuccess) errorMsg = "Error submitting user-specific problem."
                                            snackBarHostState.showSnackbar(errorMsg + " ${e.message}")
                                        }
                                    }
                                }
                            }
                        )
                    }

                    composable(AppRoutes.LOGGED_PROBLEMS) {
                        LoggedProblemsPage(
                            currentUserEmail = currentSanitizedUserEmail,
                            onProblemClick = { problemWithId -> // Parameter is problemWithId
                                val problemEntry = problemWithId.entry // problemEntry is defined here
                                val problemId = problemWithId.id

                                // problemEntry IS USED here to get its fields for encoding
                                val encodedId = URLEncoder.encode(problemId, StandardCharsets.UTF_8.toString())
                                val encodedTimestamp = URLEncoder.encode(problemEntry.timestamp, StandardCharsets.UTF_8.toString())
                                val encodedQuery = URLEncoder.encode(problemEntry.problemQuery, StandardCharsets.UTF_8.toString())
                                val encodedAppVersion = URLEncoder.encode(problemEntry.appVersion, StandardCharsets.UTF_8.toString())
                                val encodedAndroidVersion = URLEncoder.encode(problemEntry.androidVersion, StandardCharsets.UTF_8.toString())
                                val encodedDeviceModel = URLEncoder.encode(problemEntry.deviceModel, StandardCharsets.UTF_8.toString())
                                val encodedUserEmail = URLEncoder.encode(problemEntry.userEmail ?: "N/A_User", StandardCharsets.UTF_8.toString())

                                navController.navigate(
                                    "${AppRoutes.PROBLEM_DETAIL_BASE}/" +
                                            "$encodedId/" +
                                            "$encodedTimestamp/" +
                                            "$encodedQuery/" +
                                            "$encodedAppVersion/" +
                                            "$encodedAndroidVersion/" +
                                            "$encodedDeviceModel/" +
                                            encodedUserEmail
                                )
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = AppRoutes.problemDetailRoute(),
                        arguments = listOf(
                            navArgument(NavArgKeys.PROBLEM_ID) { type = NavType.StringType },
                            navArgument(NavArgKeys.PROBLEM_TIMESTAMP) { type = NavType.StringType },
                            navArgument(NavArgKeys.PROBLEM_QUERY) { type = NavType.StringType },
                            navArgument(NavArgKeys.PROBLEM_APP_VERSION) { type = NavType.StringType; nullable = true },
                            navArgument(NavArgKeys.PROBLEM_ANDROID_VERSION) { type = NavType.StringType; nullable = true },
                            navArgument(NavArgKeys.PROBLEM_DEVICE_MODEL) { type = NavType.StringType; nullable = true },
                            navArgument(NavArgKeys.PROBLEM_USER_EMAIL) { type = NavType.StringType; nullable = true } // <<< ADD THIS ARGUMENT DEFINITION
                        )
                    ) { backStackEntry ->
                        val problemIdArg = backStackEntry.arguments?.getString(NavArgKeys.PROBLEM_ID) ?: ""
                        val decodedUserEmail = URLDecoder.decode(backStackEntry.arguments?.getString(NavArgKeys.PROBLEM_USER_EMAIL) ?: "N/A_User", StandardCharsets.UTF_8.toString())
                        val finalUserEmail = if (decodedUserEmail == "N/A_User") null else decodedUserEmail
                        val ownerEmailFromNav = URLDecoder.decode(
                            backStackEntry.arguments?.getString(NavArgKeys.PROBLEM_OWNER_EMAIL) ?: "",
                            StandardCharsets.UTF_8.toString()
                        )
                        val problemEntry = ProblemEntry(
                            timestamp = URLDecoder.decode(backStackEntry.arguments?.getString(NavArgKeys.PROBLEM_TIMESTAMP) ?: "", StandardCharsets.UTF_8.toString()),
                            problemQuery = URLDecoder.decode(backStackEntry.arguments?.getString(NavArgKeys.PROBLEM_QUERY) ?: "", StandardCharsets.UTF_8.toString()),
                            appVersion = URLDecoder.decode(backStackEntry.arguments?.getString(NavArgKeys.PROBLEM_APP_VERSION) ?: "N/A", StandardCharsets.UTF_8.toString()),
                            androidVersion = URLDecoder.decode(backStackEntry.arguments?.getString(NavArgKeys.PROBLEM_ANDROID_VERSION) ?: "N/A", StandardCharsets.UTF_8.toString()),
                            deviceModel = URLDecoder.decode(backStackEntry.arguments?.getString(NavArgKeys.PROBLEM_DEVICE_MODEL) ?: "N/A", StandardCharsets.UTF_8.toString()),
                            userEmail = finalUserEmail // Use the decoded email
                        )

                        if (problemIdArg.isBlank()) {
                            Log.e("NavToDetail", "Problem ID is null or blank from nav args.")
                            // Optionally show a Text error or popBackStack
                            Text("Error: Problem ID is missing. Cannot display details.")
                            // Consider navController.popBackStack()
                            return@composable // Exit this composable if ID is missing
                        }else {
                            // Handle error: problemId is missing, maybe popBackStack or show error
                            Text("Error: Problem ID missing.")
                            Log.e("NavToDetail", "Problem ID is blank from nav args.")
                        }
                        val timestamp = URLDecoder.decode(backStackEntry.arguments?.getString(NavArgKeys.PROBLEM_TIMESTAMP) ?: "", StandardCharsets.UTF_8.toString())
                        val query = URLDecoder.decode(backStackEntry.arguments?.getString(NavArgKeys.PROBLEM_QUERY) ?: "", StandardCharsets.UTF_8.toString())
                        val appVersion = URLDecoder.decode(backStackEntry.arguments?.getString(NavArgKeys.PROBLEM_APP_VERSION) ?: "N/A", StandardCharsets.UTF_8.toString())
                        val androidVersion = URLDecoder.decode(backStackEntry.arguments?.getString(NavArgKeys.PROBLEM_ANDROID_VERSION) ?: "N/A", StandardCharsets.UTF_8.toString())
                        val deviceModel = URLDecoder.decode(backStackEntry.arguments?.getString(NavArgKeys.PROBLEM_DEVICE_MODEL) ?: "N/A", StandardCharsets.UTF_8.toString())

                        val reporterEmailFromNav = URLDecoder.decode(backStackEntry.arguments?.getString(NavArgKeys.PROBLEM_USER_EMAIL) ?: "N/A_User", StandardCharsets.UTF_8.toString())
                        val finalReporterEmail = if (reporterEmailFromNav == "N/A_User") null else reporterEmailFromNav

                        // Now, correctly define problemEntryInitial
                        val problemEntryInitial = ProblemEntry(
                            timestamp = timestamp,
                            problemQuery = query,
                            appVersion = appVersion,
                            androidVersion = androidVersion,
                            deviceModel = deviceModel,
                            userEmail = finalReporterEmail, // This is the reporter's email
                            likeCount = 0, // Initial default, will be updated from Firebase
                            likes = emptyMap() // Initial default
                        )

                        ProblemDetailPage(
                            problemEntryInitial = problemEntryInitial,
                            problemId = problemIdArg,
                            ownerEmail = ownerEmailFromNav,
                            currentLoggedInUserSanitizedEmail = currentSanitizedUserEmail,
                            onBack = { navController.popBackStack() }
                        )

                    }

                    composable(AppRoutes.FAVORITES) { FavoritesScreen() }
                    composable(AppRoutes.SHARED) { SharedScreen() }
                    composable(AppRoutes.FILES) { FilesScreen() }
                }
            }


            if (showAddFileDialog) {
                AlertDialog(
                    onDismissRequest = { showAddFileDialog = false },
                    title = { Text("Add New Item") },
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
    NavigationBar {
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
            FileItem("Alpha Document.txt", System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000),
            FileItem("Beta Image.png", System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000),
            FileItem("Gamma Notes.pdf", System.currentTimeMillis())
        )
    }
    FileListView(files.sortFiles(sortBy), viewType, "Home")
}

@Composable
fun RecentPage(sortBy: String, viewType: String) {
    val files = remember {
        mutableStateListOf(
            FileItem("RecentDoc1.txt", System.currentTimeMillis() - 5 * 60 * 60 * 1000),
            FileItem("RecentImg2.png", System.currentTimeMillis() - 2 * 60 * 60 * 1000)
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (files.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No files in $pageName.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            when (viewType) {
                "List" -> {
                    LazyColumn(
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
                        columns = GridCells.Adaptive(minSize = 128.dp),
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
                modifier = Modifier.size(48.dp),
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
