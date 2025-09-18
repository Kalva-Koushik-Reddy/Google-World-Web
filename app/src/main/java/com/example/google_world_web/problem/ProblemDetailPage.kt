// file: com/example/google_world_web/problem/ProblemDetailPage.kt
package com.example.google_world_web.problem

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemDetailPage(
    problemEntryInitial: ProblemEntry,
    problemId: String,
    ownerEmail: String, // new: owner’s sanitized email
    currentLoggedInUserSanitizedEmail: String?,
    onBack: () -> Unit
) {
    var problemEntryState by remember { mutableStateOf(problemEntryInitial) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(problemId, ownerEmail) {
        val ref = FirebaseDatabase.getInstance()
            .getReference("user_problems")
            .child(ownerEmail)
            .child(problemId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isLoading = false
                snapshot.getValue(ProblemEntry::class.java)?.let { problemEntryState = it }
                    ?: run { error = "Problem not found." }
            }

            override fun onCancelled(dbError: DatabaseError) {
                isLoading = false
                error = dbError.message
            }
        }

        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }

    val isLiked = currentLoggedInUserSanitizedEmail?.let { problemEntryState.likes.containsKey(it) } ?: false
    val isReporter = problemEntryState.userEmail == currentLoggedInUserSanitizedEmail

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Problem Details") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(error!!, color = MaterialTheme.colorScheme.error) }
                else -> {
                    DetailItem("Logged On:", problemEntryState.timestamp)
                    DetailItem("Description:", problemEntryState.problemQuery)
                    DetailItem("Reported By:", problemEntryState.userEmail ?: "N/A")
                    HorizontalDivider()
                    DetailItem("App Version:", problemEntryState.appVersion.ifEmpty { "N/A" })
                    DetailItem("Android Version:", problemEntryState.androidVersion.ifEmpty { "N/A" })
                    DetailItem("Device Model:", problemEntryState.deviceModel.ifEmpty { "N/A" })
                    HorizontalDivider()
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Likes: ${problemEntryState.likeCount}", style = MaterialTheme.typography.titleMedium)
                        IconButton(
                            onClick = {
                                toggleProblemLike(
                                    problemId = problemId,
                                    problemOwnerEmail = ownerEmail,
                                    currentLoggedInUserSanitizedEmail = currentLoggedInUserSanitizedEmail,
                                    isCurrentlyLiked = isLiked
                                ) { success, e ->
                                    if (!success) Log.e("ProblemDetailPage", "Like failed", e)
                                }
                            },
                            enabled = !isReporter && currentLoggedInUserSanitizedEmail != null
                        ) {
                            Icon(
                                if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = null
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
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, fontSize = 18.sp)
    }
}
