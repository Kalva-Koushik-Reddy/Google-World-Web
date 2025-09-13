package com.example.google_world_web.ui_common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun CenteredPageWithSearch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchDone: () -> Unit,
    onSubmitClicked: () -> Unit,
    modifier: Modifier = Modifier // Added modifier parameter
) {
    Column(
        modifier = modifier // Use the passed modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding(), // Handles keyboard intrusions
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top // Align content to the top
    ) {
        Spacer(Modifier.height(32.dp)) // Space from top, adjust as needed

        Icon(
            icon,
            contentDescription = title, // Use title for content description
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium, // Changed from bodyLarge for less emphasis
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant // Slightly less prominent color
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Describe your problem...") },
            modifier = Modifier
                .fillMaxWidth()
                    .weight(1f, fill = false), // TextField should not take all remaining space unless intended
                    singleLine = false,
            maxLines = 8,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done // Use ImeAction.Done for multi-line text fields
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    onSearchDone()
                }
            )
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onSubmitClicked,
            modifier = Modifier.fillMaxWidth(),
            enabled = searchQuery.isNotBlank()
        ) {
            Text("SUBMIT PROBLEM")
        }
        Spacer(Modifier.height(16.dp)) // Ensures some padding at the bottom
    }
}
