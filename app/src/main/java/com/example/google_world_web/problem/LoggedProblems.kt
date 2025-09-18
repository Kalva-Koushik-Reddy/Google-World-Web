// file: com/example/google_world_web/problem/LoggedProblemsPage.kt
package com.example.google_world_web.problem

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Added for TopAppBar
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.MutableData
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue // For by remember
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Transaction
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue // For by remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.database.* // For ValueEventListener
import java.text.SimpleDateFormat
import java.util.Locale


// Data class to hold both entry and its ID
data class ProblemWithId(val id: String, val entry: ProblemEntry)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LoggedProblemsPage(
    currentUserEmail: String?, // This is the sanitized email
    onProblemClick: (ProblemWithId) -> Unit, // Changed to ProblemWithId for navigation
    onBack: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val tabTitles = listOf("My Problems", "Universal Problems")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logged Problems") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(title) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                val (problemsPath, isUserSpecificPath) = when (pageIndex) {
                    0 -> Pair(if (currentUserEmail != null) "user_problems/$currentUserEmail" else null, true)
                    1 -> Pair("universal_problems", false)
                    else -> Pair(null, false)
                }
                ProblemListScreen(
                    problemsPath = problemsPath,
                    currentLoggedInUserSanitizedEmail = currentUserEmail, // Pass it here
                    onProblemClick = onProblemClick,
                    isUserSpecific = isUserSpecificPath
                )
            }
        }
    }
}

@Composable
private fun ProblemListScreen(
    problemsPath: String?,
    currentLoggedInUserSanitizedEmail: String?, // Pass this down
    onProblemClick: (ProblemWithId) -> Unit, // Changed to pass ProblemWithId
    isUserSpecific: Boolean
) {
    val problemsWithIds = remember { mutableStateListOf<ProblemWithId>() } // Using ProblemWithId
    var loading by remember { mutableStateOf(false) } // Default to false, set to true before fetch
    var error by remember { mutableStateOf<String?>(null) }
    // val scope = rememberCoroutineScope() // Already defined if needed for other coroutines


    LaunchedEffect(problemsPath) { // Re-fetch when path changes
        if (problemsPath == null && isUserSpecific) {
            error = "Login required to see your problems."
            loading = false
            problemsWithIds.clear() // CORRECTED: Use problemsWithIds
            return@LaunchedEffect
        }
        if (problemsPath == null){
            error = "Cannot load problems."
            loading = false
            problemsWithIds.clear() // CORRECTED: Use problemsWithIds
            return@LaunchedEffect
        }

        loading = true
        error = null
        val dbRef = FirebaseDatabase.getInstance().getReference(problemsPath)

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                problemsWithIds.clear()
                snapshot.children.mapNotNullTo(problemsWithIds) { child ->
                    try {
                        val entry = child.getValue(ProblemEntry::class.java)
                        val id = child.key
                        if (entry != null && id != null && entry.timestamp.isNotBlank() && entry.problemQuery.isNotBlank()) {
                            ProblemWithId(id, entry) // Store with ID
                        } else {
                            Log.w("ProblemListScreen", "Invalid problem entry from $problemsPath: ${child.key}")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("ProblemListScreen", "Error parsing problem from $problemsPath: ${child.key}", e)
                        null
                    }
                }
                loading = false
            }


            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("ProblemListScreen", "Firebase load from $problemsPath cancelled: ${databaseError.message}", databaseError.toException())
                error = "Failed to load problems: ${databaseError.message}"
                loading = false
            }
        }
        dbRef.addValueEventListener(valueEventListener)

    }
    DisposableEffect(problemsPath) { // Effect will re-run if problemsPath changes
        if (problemsPath == null && isUserSpecific) {
            error = "Login required to see your problems."
            loading = false
            problemsWithIds.clear()
            onDispose { /* No listener to remove if path was null */ } // Required onDispose block
        } else if (problemsPath == null) {
            error = "Cannot load problems."
            loading = false
            problemsWithIds.clear()
            onDispose { /* No listener to remove if path was null */ }
        } else {
            loading = true
            error = null
            val dbRef = FirebaseDatabase.getInstance().getReference(problemsPath)
            Log.d("ProblemListScreen", "Adding listener for path: $problemsPath")

            val valueEventListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    problemsWithIds.clear()
                    snapshot.children.mapNotNullTo(problemsWithIds) { child ->
                        try {
                            val entry = child.getValue(ProblemEntry::class.java)
                            val id = child.key
                            if (entry != null && id != null && entry.timestamp.isNotBlank() && entry.problemQuery.isNotBlank()) {
                                ProblemWithId(id, entry)
                            } else {
                                Log.w("ProblemListScreen", "Invalid problem entry from $problemsPath: ${child.key}")
                                null
                            }
                        } catch (e: Exception) {
                            Log.e("ProblemListScreen", "Error parsing problem from $problemsPath: ${child.key}", e)
                            null
                        }
                    }
                    if (loading) loading = false // Set loading to false after first data load
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("ProblemListScreen", "Firebase load from $problemsPath cancelled: ${databaseError.message}", databaseError.toException())
                    error = "Failed to load problems: ${databaseError.message}"
                    loading = false
                }
            }
            dbRef.addValueEventListener(valueEventListener)

            // onDispose is the cleanup block for DisposableEffect
            onDispose {
                Log.d("ProblemListScreen", "Disposing effect for path: $problemsPath. Removing listener.")
                dbRef.removeEventListener(valueEventListener)
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp) // Padding from the TabRow or content area
    ) {
        when {
            loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                    Text("Loading problems...", modifier = Modifier.padding(top = 60.dp))
                }
            }
            error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
            // CORRECTED: Check problemsWithIds.isEmpty()
            problemsWithIds.isEmpty() && !loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (isUserSpecific && problemsPath == null) "Login to see your reported problems."
                        else "No problems logged yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = problemsWithIds.sortedByDescending { it.entry.timestamp }, // Sort by timestamp
                        key = { problemWithId -> problemWithId.id } // Use ID as key
                    ) { problemItem -> // problemItem is ProblemWithId
                        ProblemQueryRow(
                            problem = problemItem.entry,
                            problemId = problemItem.id,
                            currentLoggedInUserSanitizedEmail = currentLoggedInUserSanitizedEmail,
                            onProblemClick = { onProblemClick(problemItem) }, // Pass ProblemWithId
                            onLikeToggle = { problemIdToToggle, isCurrentlyLiked ->
                                Log.d("LikeCallSite", "Calling toggleProblemLike. Current User Email: $currentLoggedInUserSanitizedEmail, Problem ID: $problemIdToToggle") // ADD THIS
                                if (problemsPath != "universal_problems" && !isUserSpecific) {
                                    Log.d("LikeToggle", "Liking disabled for non-universal problems in this context.")
                                    return@ProblemQueryRow
                                }
                                toggleProblemLike(problemIdToToggle, currentLoggedInUserSanitizedEmail, isCurrentlyLiked) { success, exception ->
                                    if (success) {
                                        Log.d("LikeToggle", "Like toggled successfully for $problemIdToToggle")
                                    } else {
                                        Log.e("LikeToggle", "Failed to toggle like for $problemIdToToggle", exception)
                                    }
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}


// In LoggedProblemsPage.kt or your ProblemUtils.kt

fun toggleProblemLike(
    problemId: String,
    currentLoggedInUserSanitizedEmail: String?, // This is the sanitized email of the user clicking "like"
    isCurrentlyLiked: Boolean, // Reflects if currentLoggedInUserSanitizedEmail is in problem.likes
    onComplete: (Boolean, Exception?) -> Unit
) {
    if (currentLoggedInUserSanitizedEmail == null) {
        onComplete(false, IllegalArgumentException("User not logged in"))
        return
    }

    val problemRef = FirebaseDatabase.getInstance().getReference("universal_problems").child(problemId)

    problemRef.runTransaction(object : Transaction.Handler {
        override fun doTransaction(currentData: MutableData): Transaction.Result {
            val problem = currentData.getValue(ProblemEntry::class.java)
                ?: return Transaction.success(currentData) // Problem not found or malformed

            Log.d("ToggleLike", "Problem ID: $problemId, Current User: $currentLoggedInUserSanitizedEmail, Problem Reporter: ${problem.userEmail}")
            val originalReporterEmail = problem.userEmail
            val isActionByReporter = originalReporterEmail == currentLoggedInUserSanitizedEmail

            if (isActionByReporter) {
                // Reporter cannot like or unlike their own problem at all
                Log.d("ToggleLike", "Reporter $currentLoggedInUserSanitizedEmail cannot like their own problem.")
                return Transaction.success(currentData)
            }



            var newLikeCount = problem.likeCount
            val newLikes = problem.likes.toMutableMap()


            if (isCurrentlyLiked) { // User wants to UNLIKE
                if (newLikes.containsKey(currentLoggedInUserSanitizedEmail)) {
                    newLikes.remove(currentLoggedInUserSanitizedEmail)
                    if (!isActionByReporter) { // Only non-reporters affect the likeCount
                        newLikeCount = (newLikeCount - 1).coerceAtLeast(0)
                    }
                    Log.d("ToggleLike", "User $currentLoggedInUserSanitizedEmail unliked. Reporter: $isActionByReporter. New count: $newLikeCount")
                }
            } else {
                if (!newLikes.containsKey(currentLoggedInUserSanitizedEmail)) {
                    newLikes[currentLoggedInUserSanitizedEmail] = true
                    if (!isActionByReporter) { // Only non-reporters affect the likeCount
                        newLikeCount += 1
                    }
                    Log.d("ToggleLike", "User $currentLoggedInUserSanitizedEmail liked. Reporter: $isActionByReporter. New count: $newLikeCount")
                }
            }

            currentData.child("likeCount").value = newLikeCount
            currentData.child("likes").value = newLikes
            return Transaction.success(currentData)
        }

        override fun onComplete(
            databaseError: DatabaseError?,
            committed: Boolean,
            currentData: DataSnapshot?
        ) {
            if (databaseError != null) {
                Log.e("ToggleLike", "Transaction failed for $problemId: ${databaseError.message}")
                onComplete(false, databaseError.toException())
            } else {
                Log.d("ToggleLike", "Transaction completed for $problemId: $committed. New Like Count: ${currentData?.child("likeCount")?.value}")
                onComplete(committed, null)
            }
        }
    })
}







@Composable
fun ProblemQueryRow(
    problem: ProblemEntry,
    problemId: String,
    currentLoggedInUserSanitizedEmail: String?,
    onProblemClick: () -> Unit, // This is for navigating to detail page
    onLikeToggle: (problemId: String, isCurrentlyLiked: Boolean) -> Unit
) {
    val isLikedByCurrentUser = currentLoggedInUserSanitizedEmail?.let { problem.likes.containsKey(it) } ?: false
    val isReporter = problem.userEmail == currentLoggedInUserSanitizedEmail

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onProblemClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.BugReport,
            contentDescription = "Problem Report",
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = problem.problemQuery,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "v${problem.appVersion.ifEmpty { "-" }} on ${problem.deviceModel.ifEmpty { "Unknown Device" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    if (!isReporter && currentLoggedInUserSanitizedEmail != null) {
                        onLikeToggle(problemId, isLikedByCurrentUser)
                    } else {
                        Log.d("ProblemQueryRow", "Like disabled for reporter or not logged in.")
                    }
                },
                enabled = !isReporter && currentLoggedInUserSanitizedEmail != null,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isLikedByCurrentUser) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isLikedByCurrentUser) "Unlike" else "Like",
                    tint = if (isLikedByCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = problem.likeCount.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp, end = 8.dp)
            )
        }
        Text(
            text = getDisplayTime(problem.timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// getDisplayTime function (assuming it's correctly defined in this file or imported)
fun getDisplayTime(timestamp: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(timestamp)
        if (date != null) {
            val now = System.currentTimeMillis()
            val diff = now - date.time
            val minutes = diff / (1000 * 60)
            val hours = minutes / 60
            val days = hours / 24
            when {
                minutes < 1 -> "Just now"
                minutes < 60 -> "$minutes min ago"
                hours < 24 -> "$hours hr ago"
                days < 2 -> "Yesterday"
                days < 7 -> SimpleDateFormat("EEE", Locale.getDefault()).format(date)
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
            }
        } else { timestamp }
    } catch (e: Exception) { timestamp }
}

