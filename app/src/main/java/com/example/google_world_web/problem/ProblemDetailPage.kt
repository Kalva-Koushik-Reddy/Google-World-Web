// file: com/example/google_world_web/problem/ProblemDetailPage.kt
package com.example.google_world_web.problem

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// If toggleProblemLike is in LoggedProblems.kt, you'd need to either move it
// to a common file and import it, or pass it as a lambda.
// For this example, I'm assuming it's accessible (e.g., in a ProblemUtils.kt file
// or you adjust the call if it's passed as a parameter).
// Example if it were in the same package (adjust if different):
// import com.example.google_world_web.problem.toggleProblemLike


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemDetailPage(
    problemEntryInitial: ProblemEntry, // Initial data from navigation
    problemId: String,                 // Firebase ID of the problem
    currentLoggedInUserSanitizedEmail: String?, // Current user's ID for liking
    onBack: () -> Unit
) {
    // State to hold the latest problem entry, including likes, fetched in real-time
    // Initialize with data from navigation, will be updated by Firebase listener
    var problemEntryState by remember { mutableStateOf(problemEntryInitial) }
    var isLoadingProblem by remember { mutableStateOf(true) }
    var errorLoadingProblem by remember { mutableStateOf<String?>(null) }

    // Use DisposableEffect to manage the Firebase listener lifecycle
    DisposableEffect(problemId) { // Re-attach listener if problemId changes
        isLoadingProblem = true
        errorLoadingProblem = null
        val problemRef = FirebaseDatabase.getInstance().getReference("universal_problems").child(problemId)
        Log.d("ProblemDetailPage", "Attaching listener to reference: $problemRef")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isLoadingProblem = false
                snapshot.getValue(ProblemEntry::class.java)?.let { fetchedProblem ->
                    problemEntryState = fetchedProblem
                    Log.d("ProblemDetailPage", "Data updated for $problemId: Likes = ${fetchedProblem.likeCount}")
                } ?: run {
                    Log.w("ProblemDetailPage", "Failed to parse problem $problemId from snapshot or snapshot is null.")
                    errorLoadingProblem = "Problem data could not be loaded."
                }
            }

            override fun onCancelled(error: DatabaseError) {
                isLoadingProblem = false
                errorLoadingProblem = "Failed to load problem: ${error.message}"
                Log.e("ProblemDetailPage", "Failed to load problem $problemId", error.toException())
            }
        }
        problemRef.addValueEventListener(listener)

        // Cleanup: remove the listener when the effect is disposed
        onDispose {
            Log.d("ProblemDetailPage", "Disposing listener for $problemId")
            problemRef.removeEventListener(listener)
        }
    }

    val isLikedByCurrentUser = currentLoggedInUserSanitizedEmail?.let {
        problemEntryState.likes.containsKey(it)
    } ?: false

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Problem Details") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                isLoadingProblem -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                        Text("Loading details...", modifier = Modifier.padding(top = 60.dp))
                    }
                }
                errorLoadingProblem != null -> {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = errorLoadingProblem!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                else -> {
                    // Display details using problemEntryState which is live from Firebase
                    DetailItem(label = "Logged On:", value = problemEntryState.timestamp)
                    DetailItem(label = "Problem Description:", value = problemEntryState.problemQuery)
                    problemEntryState.userEmail?.let { email ->
                        if (email.isNotBlank()) {
                            DetailItem(label = "Reported By:", value = email)
                        }
                    } ?: DetailItem(label = "Reported By:", value = "N/A") // If userEmail is null

                    HorizontalDivider()

                    DetailItem(label = "App Version:", value = problemEntryState.appVersion.ifEmpty { "N/A" })
                    DetailItem(label = "Android Version:", value = problemEntryState.androidVersion.ifEmpty { "N/A" })
                    DetailItem(label = "Device Model:", value = problemEntryState.deviceModel.ifEmpty { "N/A" })

                    HorizontalDivider()

                    // Like Button and Count
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Likes: ${problemEntryState.likeCount}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        val isReporter = problemEntryState.userEmail == currentLoggedInUserSanitizedEmail

                        Button(
                            onClick = {
                                // Ensure toggleProblemLike is accessible.
                                // If it's in another file (e.g., LoggedProblems.kt or ProblemUtils.kt),
                                // ensure it's correctly imported or passed.
                                // For example, if it's moved to a common utils file:
                                // com.example.google_world_web.utils.toggleProblemLike(...)

                                toggleProblemLike( // Assuming this function is globally accessible or imported
                                    problemId = problemId,
                                    currentLoggedInUserSanitizedEmail = currentLoggedInUserSanitizedEmail,
                                    isCurrentlyLiked = isLikedByCurrentUser
                                ) { success, exception ->
                                    if (!success) {
                                        Log.e("ProblemDetailPage", "Failed to toggle like for $problemId", exception)
                                    }
                                    // UI will update automatically via the ValueEventListener on problemEntryState
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
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}