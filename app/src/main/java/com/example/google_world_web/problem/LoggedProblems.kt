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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.database.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

// Carry ownerEmail with each problem
data class ProblemWithId(val id: String, val entry: ProblemEntry, val ownerEmail: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LoggedProblemsPage(
    currentUserEmail: String?, // sanitized email of logged-in user
    onProblemClick: (ProblemWithId) -> Unit,
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
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = { Text(title) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                val (problemsPath, isUserSpecific) = when (pageIndex) {
                    0 -> Pair(if (currentUserEmail != null) "user_problems/$currentUserEmail" else null, true)
                    1 -> Pair("user_problems", false) // flatten across all users
                    else -> Pair(null, false)
                }
                ProblemListScreen(
                    problemsPath = problemsPath,
                    currentLoggedInUserSanitizedEmail = currentUserEmail,
                    onProblemClick = onProblemClick,
                    isUserSpecific = isUserSpecific
                )
            }
        }
    }
}

@Composable
private fun ProblemListScreen(
    problemsPath: String?,
    currentLoggedInUserSanitizedEmail: String?,
    onProblemClick: (ProblemWithId) -> Unit,
    isUserSpecific: Boolean
) {
    val problemsWithIds = remember { mutableStateListOf<ProblemWithId>() }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(problemsPath) {
        if (problemsPath == null) {
            error = if (isUserSpecific) "Login required to see your problems." else "Cannot load problems."
            loading = false
            problemsWithIds.clear()
            onDispose { }
        } else {
            loading = true
            error = null
            val dbRef = FirebaseDatabase.getInstance().getReference(problemsPath)
            Log.d("ProblemListScreen", "Listening on $problemsPath")

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    problemsWithIds.clear()

                    if (isUserSpecific) {
                        // snapshot = user_problems/{currentUser}
                        snapshot.children.mapNotNullTo(problemsWithIds) { problemNode ->
                            val entry = problemNode.getValue(ProblemEntry::class.java)
                            val id = problemNode.key
                            if (entry != null && id != null && entry.timestamp.isNotBlank() && entry.problemQuery.isNotBlank()) {
                                ProblemWithId(id, entry, currentLoggedInUserSanitizedEmail ?: "")
                            } else null
                        }
                    } else {
                        // snapshot = user_problems/{userEmail}/{problemId}
                        snapshot.children.forEach { userNode ->
                            val ownerEmail = userNode.key ?: return@forEach
                            userNode.children.mapNotNullTo(problemsWithIds) { problemNode ->
                                val entry = problemNode.getValue(ProblemEntry::class.java)
                                val id = problemNode.key
                                if (entry != null && id != null && entry.timestamp.isNotBlank() && entry.problemQuery.isNotBlank()) {
                                    ProblemWithId(id, entry, ownerEmail)
                                } else null
                            }
                        }
                    }
                    loading = false
                }

                override fun onCancelled(errorDb: DatabaseError) {
                    Log.e("ProblemListScreen", "Cancelled: ${errorDb.message}", errorDb.toException())
                    error = "Failed to load problems: ${errorDb.message}"
                    loading = false
                }
            }

            dbRef.addValueEventListener(listener)
            onDispose { dbRef.removeEventListener(listener) }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
        when {
            loading -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                    Text("Loading problems...", Modifier.padding(top = 60.dp))
                }
            }
            error != null -> {
                Box(Modifier.fillMaxSize().padding(16.dp), Alignment.Center) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            problemsWithIds.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(16.dp), Alignment.Center) {
                    Text(
                        if (isUserSpecific) "No problems logged yet." else "No problems reported yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        items = problemsWithIds.sortedByDescending { it.entry.timestamp },
                        key = { it.id }
                    ) { problemItem ->
                        ProblemQueryRow(
                            problem = problemItem.entry,
                            problemId = problemItem.id,
                            currentLoggedInUserSanitizedEmail = currentLoggedInUserSanitizedEmail,
                            onProblemClick = { onProblemClick(problemItem) },
                            onLikeToggle = { problemIdToToggle, isCurrentlyLiked ->
                                toggleProblemLike(
                                    problemId = problemIdToToggle,
                                    problemOwnerEmail = problemItem.ownerEmail,
                                    currentLoggedInUserSanitizedEmail = currentLoggedInUserSanitizedEmail,
                                    isCurrentlyLiked = isCurrentlyLiked
                                ) { success, exception ->
                                    if (success) {
                                        Log.d("LikeToggle", "Like toggled for $problemIdToToggle")
                                    } else {
                                        Log.e("LikeToggle", "Failed to toggle like", exception)
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

fun toggleProblemLike(
    problemId: String,
    problemOwnerEmail: String,
    currentLoggedInUserSanitizedEmail: String?,
    isCurrentlyLiked: Boolean,
    onComplete: (Boolean, Exception?) -> Unit
) {
    if (currentLoggedInUserSanitizedEmail == null) {
        onComplete(false, IllegalArgumentException("User not logged in"))
        return
    }

    val problemRef = FirebaseDatabase.getInstance()
        .getReference("user_problems")
        .child(problemOwnerEmail)
        .child(problemId)

    problemRef.runTransaction(object : Transaction.Handler {
        override fun doTransaction(currentData: MutableData): Transaction.Result {
            val problem = currentData.getValue(ProblemEntry::class.java)
                ?: return Transaction.success(currentData)

            if (problem.userEmail == currentLoggedInUserSanitizedEmail) {
                Log.d("ToggleLike", "Reporter $currentLoggedInUserSanitizedEmail cannot like their own problem.")
                return Transaction.success(currentData)
            }

            var newLikeCount = problem.likeCount
            val newLikes = problem.likes.toMutableMap()

            if (isCurrentlyLiked) {
                if (newLikes.containsKey(currentLoggedInUserSanitizedEmail)) {
                    newLikes.remove(currentLoggedInUserSanitizedEmail)
                    newLikeCount = (newLikeCount - 1).coerceAtLeast(0)
                }
            } else {
                if (!newLikes.containsKey(currentLoggedInUserSanitizedEmail)) {
                    newLikes[currentLoggedInUserSanitizedEmail] = true
                    newLikeCount += 1
                }
            }

            currentData.child("likeCount").value = newLikeCount
            currentData.child("likes").value = newLikes
            return Transaction.success(currentData)
        }

        override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
            if (error != null) {
                onComplete(false, error.toException())
            } else {
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
    onProblemClick: () -> Unit,
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
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
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
        } else timestamp
    } catch (e: Exception) { timestamp }
}
