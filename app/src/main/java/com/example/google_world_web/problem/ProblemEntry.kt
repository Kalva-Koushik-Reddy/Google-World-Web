package com.example.google_world_web.problem

data class ProblemEntry(
    val timestamp: String = "",
    val problemQuery: String = "",
    val appVersion: String = "",
    val androidVersion: String = "",
    val deviceModel: String = "",
    val userEmail: String? = null, // Who reported it
    val likeCount: Int = 0,        // New: Number of likes
    val likes: Map<String, Boolean> = emptyMap() // New: Map of user IDs (sanitized emails) who liked
)
