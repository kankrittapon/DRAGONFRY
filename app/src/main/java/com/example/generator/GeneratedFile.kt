package com.example.generator

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class GeneratedFileCategory(val labelTh: String) {
    ALL("ทั้งหมด"),
    GOOGLE_SCRIPT("Google Script"),
    FORGE_SERVER("Forge Server"),
    FORGE_CLIENT("Forge Client & Xaero"),
    GRADLE_CONFIG("Gradle & คู่มือ")
}

data class GeneratedFile(
    val path: String,
    val displayName: String,
    val category: GeneratedFileCategory,
    val language: String,
    val descriptionTh: String,
    val content: String
)

object ZipExporter {
    suspend fun exportToUri(
        context: Context,
        uri: Uri,
        files: List<GeneratedFile>
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                ZipOutputStream(out).use { zos ->
                    files.forEach { file ->
                        val entry = ZipEntry(file.path)
                        zos.putNextEntry(entry)
                        zos.write(file.content.toByteArray(Charsets.UTF_8))
                        zos.closeEntry()
                    }
                }
            } ?: error("ไม่สามารถเปิดไฟล์ปลายทางสำหรับบันทึก ZIP ได้")
            files.size
        }
    }
}
