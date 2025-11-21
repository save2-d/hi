package com.antigravity.browser.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.GeckoSession

/**
 * Handles browser automation through JavaScript injection
 */
class BrowserAutomation(private val context: Context) {
    private val TAG = "BrowserAutomation"
    
    /**
     * Execute JavaScript in the active session
     */
    suspend fun executeCommand(session: GeckoSession, command: BrowserCommand): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                when (command) {
                    is BrowserCommand.Scroll -> scrollPage(session, command.direction, command.amount)
                    is BrowserCommand.ScrollToTop -> scrollToTop(session)
                    is BrowserCommand.ScrollToBottom -> scrollToBottom(session)
                    is BrowserCommand.PlayPause -> playPauseVideo(session)
                    is BrowserCommand.Seek -> seekVideo(session, command.seconds)
                    is BrowserCommand.SetVideoSpeed -> setVideoSpeed(session, command.speed)
                    else -> {
                        Log.w(TAG, "Command not handled by automation: $command")
                        false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing command: $command", e)
                false
            }
        }
    }
    
    private fun scrollPage(session: GeckoSession, direction: ScrollDirection, amount: Float): Boolean {
        val (dx, dy) = when (direction) {
            ScrollDirection.UP -> 0 to -(300 * amount).toInt()
            ScrollDirection.DOWN -> 0 to (300 * amount).toInt()
            ScrollDirection.LEFT -> -(300 * amount).toInt() to 0
            ScrollDirection.RIGHT -> (300 * amount).toInt() to 0
        }
        
        val js = """
            window.scrollBy($dx, $dy);
            true;
        """.trimIndent()
        
        session.evaluateJS(js)
        return true
    }
    
    private fun scrollToTop(session: GeckoSession): Boolean {
        val js = """
            window.scrollTo(0, 0);
            true;
        """.trimIndent()
        
        session.evaluateJS(js)
        return true
    }
    
    private fun scrollToBottom(session: GeckoSession): Boolean {
        val js = """
            window.scrollTo(0, document.body.scrollHeight);
            true;
        """.trimIndent()
        
        session.evaluateJS(js)
        return true
    }
    
    private fun playPauseVideo(session: GeckoSession): Boolean {
        val js = """
            (function() {
                // Try YouTube player API
                if (typeof yt !== 'undefined' && yt.player && yt.player.getPlayerByElement) {
                    const player = yt.player.getPlayerByElement('movie_player');
                    if (player) {
                        const state = player.getPlayerState();
                        if (state === 1) { // Playing
                            player.pauseVideo();
                        } else {
                            player.playVideo();
                        }
                        return true;
                    }
                }
                
                // Fallback to HTML5 video
                const videos = document.querySelectorAll('video');
                if (videos.length > 0) {
                    const video = videos[0];
                    if (video.paused) {
                        video.play();
                    } else {
                        video.pause();
                    }
                    return true;
                }
                
                return false;
            })();
        """.trimIndent()
        
        session.evaluateJS(js)
        return true
    }
    
    private fun seekVideo(session: GeckoSession, seconds: Int): Boolean {
        val js = """
            (function() {
                const videos = document.querySelectorAll('video');
                if (videos.length > 0) {
                    const video = videos[0];
                    video.currentTime = Math.max(0, video.currentTime + $seconds);
                    return true;
                }
                return false;
            })();
        """.trimIndent()
        
        session.evaluateJS(js)
        return true
    }
    
    private fun setVideoSpeed(session: GeckoSession, speed: Float): Boolean {
        val js = """
            (function() {
                const videos = document.querySelectorAll('video');
                if (videos.length > 0) {
                    videos.forEach(video => {
                        video.playbackRate = $speed;
                    });
                    return true;
                }
                return false;
            })();
        """.trimIndent()
        
        session.evaluateJS(js)
        Log.d(TAG, "Set video speed to: $speed")
        return true
    }
    
    /**
     * Helper extension to evaluate JavaScript
     */
    private fun GeckoSession.evaluateJS(js: String) {
        this.evaluateJS(js)
    }
}
