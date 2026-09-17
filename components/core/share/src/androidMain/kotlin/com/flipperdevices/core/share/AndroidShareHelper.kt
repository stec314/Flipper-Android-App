package com.flipperdevices.core.share

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.flipperdevices.core.di.AppGraph
import com.flipperdevices.core.ktx.jre.createClearNewFileWithMkDirs
import dev.zacsweers.metro.ContributesBinding
import okio.Path.Companion.toOkioPath
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

private const val DEFAULT_MIME_TYPE = "application/octet-stream"

@ContributesBinding(AppGraph::class, binding<PlatformShareHelper>())
class AndroidShareHelper @Inject constructor(
    private val context: Context
) : PlatformShareHelper {

    override fun provideSharableFile(fileName: String): PlatformSharableFile {
        val sharableFile = SharableFile(context, fileName)
        sharableFile.createClearNewFileWithMkDirs()
        return PlatformSharableFile(sharableFile.toOkioPath())
    }

    override fun shareFile(file: PlatformSharableFile, title: String) {
        ShareHelper.shareFile(
            context = context,
            file = file,
            text = title
        )
    }

    override fun saveToDownloads(file: PlatformSharableFile): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Pre-scoped-storage devices would need a runtime WRITE_EXTERNAL_STORAGE
            // permission request; the caller falls back to shareFile() instead.
            return false
        }
        val fileName = file.path.name
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, guessMimeType(fileName))
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val itemUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return false
        val success = runCatching {
            val outputStream = resolver.openOutputStream(itemUri) ?: error("Can't open output stream")
            outputStream.use { output ->
                file.path.toFile().inputStream().use { input -> input.copyTo(output) }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
        }.onFailure {
            resolver.delete(itemUri, null, null)
        }.isSuccess
        if (success) {
            val message = context.getString(R.string.file_saved_to_downloads, fileName)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        return success
    }

    private fun guessMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
        if (extension.isEmpty()) return DEFAULT_MIME_TYPE
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
            ?: DEFAULT_MIME_TYPE
    }
}
