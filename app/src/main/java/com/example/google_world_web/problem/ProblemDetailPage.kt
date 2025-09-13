// file: com/example/google_world_web/problem/ProblemDetailPage.kt
package com.example.google_world_web.problem

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import com.example.google_world_web.ProblemEntry // Import if you moved ProblemEntry to its own file

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemDetailPage(problemEntry: ProblemEntry, onBack: () -> Unit) {
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
                },
                // Optional: Add actions like share or delete
                // actions = {
                //     IconButton(onClick = { /* TODO: Share action */ }) {
                //         Icon(Icons.Filled.Share, contentDescription = "Share Problem")
                //     }
                // }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // Make content scrollable if it overflows
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp) // Increased spacing
        ) {
            DetailItem(label = "Logged On:", value = problemEntry.timestamp)
            DetailItem(label = "Problem Description:", value = problemEntry.problemQuery)
            HorizontalDivider() // Visually separate main problem from device info
            DetailItem(label = "App Version:", value = problemEntry.appVersion.ifEmpty { "N/A" })
            DetailItem(label = "Android Version:", value = problemEntry.androidVersion.ifEmpty { "N/A" })
            DetailItem(label = "Device Model:", value = problemEntry.deviceModel.ifEmpty { "N/A" })
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge, // Can use labelLarge for more prominence
            fontWeight = FontWeight.SemiBold, // Slightly less than Bold
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary // Use primary color for labels
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 18.sp, // Kept this for value
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    // Removed HorizontalDivider from here to place it explicitly in the parent Column
}
