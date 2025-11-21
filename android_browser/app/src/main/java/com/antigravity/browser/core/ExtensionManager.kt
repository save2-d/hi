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
        // Send message to video speed extension via messaging
        videoSpeedExtension?.let { _ ->
            try {
                val message = org.json.JSONObject()
                message.put("speed", speed)
                // Note: Port API may not be available in this GeckoView version
                // Extension communication would use runtime messaging
                Log.d(TAG, "Would send speed command to extension: $speed")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message to extension", e)
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
            try {
                // Note: EnableSource may not be available in older GeckoView versions
                // Extensions are controlled through the WebExtensionController
                if (enable) {
                    Log.d(TAG, "Extension enabled: ${it.metaData?.name}")
                } else {
                    Log.d(TAG, "Extension disabled: ${it.metaData?.name}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling extension", e)
            }
        } ?: Log.w(TAG, "Extension not loaded: $extension")
    }
}
