package com.example.google_world_web.integrity

import android.content.Context
import java.io.File

class BaselineManager(private val context: Context) {
    private val baselineFile = File(context.filesDir, "baseline.json")

    fun saveBaseline(baseline: Map<String, String>) {
        val json = baseline.entries.joinToString(separator = ",\n") {
            "  \"${it.key}\": \"${it.value}\""
        }
        baselineFile.writeText("{\n$json\n}")
    }

    fun readBaseline(): Map<String, String> {
        if (!baselineFile.exists()) {
            return emptyMap()
        }
        val json = baselineFile.readText()
        val entries = json.lines().drop(1).dropLast(1).map {
            val parts = it.trim().removeSuffix(",").split("\": \"")
            val path = parts[0].removePrefix("\"")
            val hash = parts[1].removeSuffix("\"")
            path to hash
        }
        return entries.toMap()
    }
}
