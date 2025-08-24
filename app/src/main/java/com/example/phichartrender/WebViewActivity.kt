package com.example.phichartrender

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.os.Build
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.content.res.Configuration

class WebViewActivity : Activity() {
    private lateinit var webView: WebView
    
    companion object {
        private const val TAG = "WebViewActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        
        try {
            // 检查当前屏幕方向，如果为竖屏则切换到横屏并锁定
            if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                Log.d(TAG, "Device is in portrait mode, switching to landscape")
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                Log.d(TAG, "Device is already in landscape mode")
            }
            
            // 创建无边框窗口
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            
            webView = WebView(this)
            Log.d(TAG, "WebView created")
            
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.useWideViewPort = true
            webView.settings.loadWithOverviewMode = true
            Log.d(TAG, "WebView settings configured")
            
            webView.webViewClient = object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    super.onReceivedError(view, request, error)
                    // 处理WebView加载错误，避免闪退
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Log.e(TAG, "WebView error: ${error.errorCode} - ${error.description}")
                    }
                }

                // 为API级别低于M的设备保留旧的错误处理方法
                @Suppress("DEPRECATION")
                override fun onReceivedError(
                    view: WebView,
                    errorCode: Int,
                    description: String,
                    failingUrl: String
                ) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    // 处理WebView加载错误，避免闪退
                    Log.e(TAG, "WebView error: $errorCode - $description")
                }
                
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "WebView page finished loading: $url")
                }
            }
            
            setContentView(webView)
            Log.d(TAG, "WebView set as content view")
            
            // 在setContent之后设置全屏模式
            setupFullScreenMode()
            
            // 加载服务器页面
            try {
                Log.d(TAG, "Loading URL: http://127.0.0.1:9000")
                webView.loadUrl("http://127.0.0.1:9000")
            } catch (e: Exception) {
                // 捕获可能的异常，避免闪退
                Log.e(TAG, "Error loading URL", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
        }
    }
    
    private fun setupFullScreenMode() {
        try {
            // 根据API级别使用不同的全屏实现方式
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11及以上版本使用WindowInsetsController
                // 确保decorView不为空
                if (window.decorView != null) {
                    window.insetsController?.let {
                        it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                        it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }
            } else {
                // Android 11以下版本使用旧的FLAG_FULLSCREEN
                @Suppress("DEPRECATION")
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
            }
            Log.d(TAG, "Full screen mode setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up full screen mode", e)
        }
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "onConfigurationChanged called, orientation: ${newConfig.orientation}")
        // 处理配置变化，但不退出应用
        // 这里我们只需要确保Activity不被重启即可
    }
    
    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        try {
            webView.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying WebView", e)
        }
        super.onDestroy()
    }
}