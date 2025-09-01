package com.example.google_world_web.integrity

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.io.File

class IntegrityChecker(private val context: Context) {
    private val baselineManager = BaselineManager(context)

    fun generateBaseline() {
        val baseline = mutableMapOf<String, String>()
        val sourceDir = getAppSourceDir()
        if (sourceDir != null) {
            File(sourceDir).walk().forEach { file ->
                if (file.isFile) {
                    val hash = FileHasher.hashFile(file)
                    baseline[file.absolutePath] = hash
                }
            }
        }
        baselineManager.saveBaseline(baseline)
        Log.d("IntegrityChecker", "Baseline generated successfully.")
    }

    fun verifyIntegrity(): Boolean {
        val currentHashes = mutableMapOf<String, String>()
        val sourceDir = getAppSourceDir()
        if (sourceDir != null) {
            File(sourceDir).walk().forEach { file ->
                if (file.isFile) {
                    val hash = FileHasher.hashFile(file)
                    currentHashes[file.absolutePath] = hash
                }
            }
        }

        val baseline = baselineManager.readBaseline()
        if (baseline.isEmpty()) {
            Log.d("IntegrityChecker", "No baseline found. Generating a new one.")
            generateBaseline()
            return true
        }

        if (currentHashes.size != baseline.size) {
            Log.e("IntegrityChecker", "Tampering detected: Number of files has changed.")
            return false
        }

        for ((path, hash) in baseline) {
            if (currentHashes[path] != hash) {
                Log.e("IntegrityChecker", "Tampering detected: File has been modified: $path")
                return false
            }
        }

        Log.d("IntegrityChecker", "Integrity check passed.")
        return true
    }

    private fun getAppSourceDir(): String? {
        return try {
            val pm = context.packageManager
            val pi = pm.getPackageInfo(context.packageName, 0)
            pi.applicationInfo.sourceDir
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("IntegrityChecker", "Failed to get app source directory.", e)
            null
        }
    }
}
