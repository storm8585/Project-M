package io.nekohasekai.sfa.compose.screen.tools

import android.webkit.MimeTypeMap
import io.nekohasekai.libbox.TaildropDownloadHandler
import io.nekohasekai.sfa.Application
import io.nekohasekai.sfa.utils.CommandTarget
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

object TaildropFiles {
    private const val DIRECTORY_NAME = "Taildrop"
    private const val CACHE_DIRECTORY = "taildrop"
    private const val CACHE_LIFETIME = 24L * 60 * 60 * 1000

    fun mimeType(name: String): String {
        val extension = name.substringAfterLast('.', "").lowercase()
        if (extension.isEmpty()) return "application/octet-stream"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"
    }

    private fun daemonFile(name: String): File? {
        if (CommandTarget.isRemote) return null
        val workingDirectory = Application.application.getExternalFilesDir(null) ?: return null
        val file = File(File(workingDirectory, DIRECTORY_NAME), name)
        return if (file.isFile) file else null
    }

    suspend fun materialize(endpointTag: String, name: String): File = withContext(Dispatchers.IO) {
        daemonFile(name)?.let { return@withContext it }
        val cacheDirectory = File(Application.application.cacheDir, CACHE_DIRECTORY).also { it.mkdirs() }
        val destination = File(cacheDirectory, name)
        val finish = CompletableDeferred<String>()
        val session = CommandTarget.standaloneClient().downloadTaildropFile(
            endpointTag,
            name,
            destination.path,
            object : TaildropDownloadHandler {
                override fun onProgress(downloaded: Long, total: Long) {
                }

                override fun onFinish(errorMessage: String) {
                    finish.complete(errorMessage)
                }
            },
        )
        try {
            val errorMessage = finish.await()
            if (errorMessage.isNotEmpty()) {
                throw IOException(errorMessage)
            }
        } finally {
            session.close()
        }
        destination
    }

    fun cleanCache() {
        val directory = File(Application.application.cacheDir, CACHE_DIRECTORY)
        val expiry = System.currentTimeMillis() - CACHE_LIFETIME
        directory.listFiles()?.forEach { file ->
            if (file.lastModified() < expiry) {
                file.delete()
            }
        }
    }
}
