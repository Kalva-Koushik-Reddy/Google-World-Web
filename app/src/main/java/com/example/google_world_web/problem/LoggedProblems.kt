// file: com/example/google_world_web/problem/LoggedProblemsPage.kt
package com.example.google_world_web.problem

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Correct import for items in LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport // Changed icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Locale
// import com.example.google_world_web.ProblemEntry // Import if you moved ProblemEntry to its own file

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoggedProblemsPage(onProblemClick: (ProblemEntry) -> Unit) {
    val problems = remember { mutableStateListOf<ProblemEntry>() }
    val loading = remember { mutableStateOf(true) }
    val error = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading.value = true // Explicitly set loading to true at the start
        error.value = null   // Clear previous errors
        val dbRef = FirebaseDatabase.getInstance().getReference("problems")
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                problems.clear() // Clear previous list
                snapshot.children.mapNotNullTo(problems) { child ->
                    try {
                        // Ensure ProblemEntry has a no-argument constructor for Firebase deserialization
                        child.getValue(ProblemEntry::class.java)?.takeIf {
                            // Basic validation, adjust as needed
                            it.timestamp.isNotBlank() && it.problemQuery.isNotBlank()
                        }
                    } catch (e: Exception) {
                        Log.e("LoggedProblemsPage", "Error parsing problem: ${child.key}", e)
                        // Optionally, you could add this error to a list of parsing errors to display
                        null
                    }
                }
                loading.value = false
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("LoggedProblemsPage", "Firebase load cancelled: ${databaseError.message}", databaseError.toException())
                error.value = "Failed to load problems: ${databaseError.message}"
                loading.value = false
            }
        })
    }


    // No Scaffold here, as it's part of the main NavHost content area which has its own Scaffold
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        when {
            loading.value -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                    Text("Loading problems...", modifier = Modifier.padding(top = 60.dp)) // User feedback
                }
            }
            error.value != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = error.value!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            problems.isEmpty() && !loading.value -> { // Ensure not to show "no problems" while loading
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No problems logged yet.",
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
                        items = problems.sortedByDescending { it.timestamp },
                        key = { problem -> problem.timestamp + problem.problemQuery }
                    ) { problem ->
                        ProblemQueryRow(
                            problem = problem,
                            onClick = { onProblemClick(problem) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}


@Composable
fun ProblemQueryRow(problem: ProblemEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp), // Removed horizontal here, parent LazyColumn has it
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.BugReport, // More specific icon
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
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text( // Displaying app version and device model can be useful in the list
                text = "v${problem.appVersion.ifEmpty{"-"}} on ${problem.deviceModel.ifEmpty{"Unknown Device"}}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = getDisplayTime(problem.timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.End // Align time to the end
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
                days < 2 -> "Yesterday" // More specific "Yesterday"
                days < 7 -> SimpleDateFormat("EEE", Locale.getDefault()).format(date) // "Mon", "Tue"
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date) // "Jul 4"
            }
        } else {
            timestamp // Fallback to raw timestamp
        }
    } catch (e: Exception) {
        timestamp // Fallback in case of parsing error
    }
}
