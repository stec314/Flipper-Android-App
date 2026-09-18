package com.flipperdevices.core.share

interface PlatformShareHelper {
    /**
     * Provide file which can be shared later via [shareFile]
     *
     * If file with same name [fileName] exists, it will be deleted
     */
    fun provideSharableFile(fileName: String): PlatformSharableFile

    /**
     * @param file file to share
     * @param title text displayed as hint for user
     */
    fun shareFile(file: PlatformSharableFile, title: String)

    /**
     * Persist [file] into the device's public Downloads location, so it survives
     * outside the app and is visible to the user's Files/Downloads app.
     *
     * @return true if the file was actually persisted; false if this platform/OS
     * version doesn't support it and the caller should fall back to [shareFile]
     */
    fun saveToDownloads(file: PlatformSharableFile): Boolean
}
