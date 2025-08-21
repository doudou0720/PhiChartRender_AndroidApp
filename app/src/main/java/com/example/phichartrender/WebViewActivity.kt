package com.example.phichartrender

import android.app.Activity
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient

class WebViewActivity : Activity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 创建无边框窗口
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        
        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        
        setContentView(webView)
        
        // 加载服务器页面
        webView.loadUrl("http://127.0.0.1:9000")
    }
}