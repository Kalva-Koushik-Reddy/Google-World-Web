package com.example.google_world_web.integrity

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object FileHasher {
    fun hashFile(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        val fis = FileInputStream(file)
        val dataBytes = ByteArray(1024)
        var nread: Int
        while (fis.read(dataBytes).also { nread = it } != -1) {
            md.update(dataBytes, 0, nread)
        }
        val mdbytes = md.digest()
        val sb = StringBuilder()
        for (i in mdbytes.indices) {
            sb.append(Integer.toString((mdbytes[i].toInt() and 0xff) + 0x100, 16).substring(1))
        }
        return sb.toString()
    }
}
