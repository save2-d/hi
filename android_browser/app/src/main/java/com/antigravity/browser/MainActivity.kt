package com.antigravity.browser

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import com.antigravity.browser.core.BrowserViewModel
import org.mozilla.geckoview.GeckoView

class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: BrowserViewModel
    private lateinit var geckoView: GeckoView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[BrowserViewModel::class.java]
        
        // Create simple GeckoView layout
        geckoView = GeckoView(this)
        
        // Set GeckoView as content
        val layout = FrameLayout(this)
        layout.addView(geckoView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        setContentView(layout)
        
        // Observe active session from ViewModel
        viewModel.activeSession.value?.let { session ->
            geckoView.setSession(session)
        }
    }
}
