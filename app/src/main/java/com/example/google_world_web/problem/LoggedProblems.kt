package com.example.google_world_web.problem

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoggedProblemsPage() {
    val problems = remember { mutableStateListOf<ProblemEntry>() }
    val loading = remember { mutableStateOf(true) }
    val error = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val dbRef = FirebaseDatabase.getInstance().getReference("problems")
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                problems.clear()
                for (child in snapshot.children) {
                    try {
                        val entry = child.getValue(ProblemEntry::class.java)
                        if (entry != null && (entry.problemQuery.isNotBlank() || entry.deviceModel.isNotBlank() || entry.timestamp.isNotBlank())) {
                            problems.add(entry)
                        }
                    } catch (e: Exception) {
                        // Skip malformed entries
                    }
                }
                loading.value = false
            }
            override fun onCancelled(errorDb: DatabaseError) {
                error.value = errorDb.message
                loading.value = false
            }
        })
    }

    Scaffold(
        topBar = {},
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text(
                text = "Logged Problems",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
            )
            when {
                loading.value -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error.value != null -> {
                    Text("Error: ${'$'}{error.value}", color = MaterialTheme.colorScheme.error)
                }
                problems.isEmpty() -> {
                    Text("No problems logged yet.", modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(problems) { problem ->
                            ProblemQueryRow(problem)
                        }
                    }
                }
            }
        }
    }
}

data class ProblemEntry(
    val timestamp: String = "",
    val problemQuery: String = "",
    val appVersion: String = "",
    val androidVersion: String = "",
    val deviceModel: String = ""
)


@Composable
fun ProblemQueryRow(problem: ProblemEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: User icon
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "User",
            modifier = Modifier.size(32.dp).weight(0.15f),
            tint = Color.Gray
        )
        // Center: Problem query
        Text(
            text = problem.problemQuery,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal, fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.7f).padding(horizontal = 8.dp)
        )
        // Right: Timestamp/date
        val displayTime = remember(problem.timestamp) { getDisplayTime(problem.timestamp) }
        Text(
            text = displayTime,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.weight(0.15f),
            maxLines = 1
        )
    }
}

fun getDisplayTime(timestamp: String): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = format.parse(timestamp)
        if (date != null) {
            val now = Date()
            val diff = now.time - date.time
            val hours = diff / (1000 * 60 * 60)
            return if (hours < 24) {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            } else {
                SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
            }
        } else {
            ""
        }
    } catch (e: Exception) {
        ""
    }
}
