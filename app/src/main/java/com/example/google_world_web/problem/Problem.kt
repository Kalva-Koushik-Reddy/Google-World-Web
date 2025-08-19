package com.example.google_world_web.problem

import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalFocusManager
import com.example.google_world_web.CenteredPageWithSearch

@Composable
fun ProblemPage(onSearchSubmitted: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    CenteredPageWithSearch(
        icon = Icons.AutoMirrored.Filled.HelpOutline,
        title = "Report a Problem",
        subtitle = "Please describe the issue you are facing in detail.",
        searchQuery = searchQuery,
        onSearchQueryChange = { newValue -> searchQuery = newValue },
        onSearchDone = {
            focusManager.clearFocus()
        },
        onSubmitClicked = {
            if (searchQuery.isNotBlank()) {
                onSearchSubmitted(searchQuery)
                searchQuery = ""
                focusManager.clearFocus()
            }
        }
    )
}
