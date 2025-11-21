package com.antigravity.browser.core

import android.content.Context
import android.util.Log
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension
import java.io.File

class ExtensionManager(private val context: Context, private val runtime: GeckoRuntime) {

    private val TAG = "ExtensionManager"
    private val UBLOCK_ORIGIN_URL = "https://addons.mozilla.org/firefox/downloads/latest/ublock-origin/addon-11423598-latest.xpi"
    
    // Store loaded extensions
    private var videoSpeedExtension: WebExtension? = null
    private var ublockExtension: WebExtension? = null

    fun installExtensions() {
        // Install uBlock Origin
        runtime.webExtensionController.install(UBLOCK_ORIGIN_URL)
            .accept({ extension ->
                ublockExtension = extension
                Log.d(TAG, "uBlock Origin installed: ${extension?.metaData?.name}")
            }, { error ->
                Log.e(TAG, "Error installing uBlock Origin", error)
            })

        // Install Video Speed Extension from assets
        val extensionPath = "resource://android/assets/extensions/video-speed/"
        runtime.webExtensionController.installBuiltIn(extensionPath)
            .accept({ extension ->
                videoSpeedExtension = extension
                Log.d(TAG, "Video speed extension installed: ${extension?.metaData?.name}")
            }, { error ->
                Log.e(TAG, "Error installing video speed extension", error)
            })
    }
    
    fun setVideoSpeed(speed: Float) {
        // Send message to video speed extension
        videoSpeedExtension?.let { ext ->
            val port = ext.port
            if (port != null) {
                val message = org.json.JSONObject()
                message.put("speed", speed)
                port.postMessage(message)
                Log.d(TAG, "Sent speed command to extension: $speed")
            } else {
                Log.w(TAG, "Video speed extension port not available")
            }
        } ?: Log.w(TAG, "Video speed extension not loaded")
    }
    
    fun toggleExtension(extension: ExtensionType, enable: Boolean) {
        Log.d(TAG, "Toggle extension: $extension, enable: $enable")
        
        val ext = when (extension) {
            ExtensionType.UBLOCK -> ublockExtension
            ExtensionType.VIDEO_SPEED -> videoSpeedExtension
        }
        
        ext?.let {
            if (enable) {
                runtime.webExtensionController.enable(it, WebExtension.EnableSource.USER)
                Log.d(TAG, "Enabled extension: ${it.metaData?.name}")
            } else {
                runtime.webExtensionController.disable(it, WebExtension.EnableSource.USER)
                Log.d(TAG, "Disabled extension: ${it.metaData?.name}")
            }
        } ?: Log.w(TAG, "Extension not loaded: $extension")
    }
}
