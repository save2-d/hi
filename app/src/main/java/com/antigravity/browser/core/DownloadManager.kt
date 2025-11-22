package com.antigravity.browser.core

import java.util.concurrent.CopyOnWriteArrayList

object DownloadManager {
    private val downloads = CopyOnWriteArrayList<DownloadItem>()

    data class DownloadItem(
        val url: String,
        val fileName: String,
        val progress: Float = 0f,
        val status: DownloadStatus = DownloadStatus.PENDING
    )

    enum class DownloadStatus {
        PENDING, DOWNLOADING, COMPLETED, FAILED, CANCELLED
    }

    fun addDownload(url: String, fileName: String) {
        downloads.add(DownloadItem(url = url, fileName = fileName))
    }

    fun getDownloads(): List<DownloadItem> {
        return downloads.toList()
    }

    fun removeDownload(url: String) {
        downloads.removeIf { it.url == url }
    }

    fun clearCompleted() {
        downloads.removeIf { it.status == DownloadStatus.COMPLETED }
    }
}
