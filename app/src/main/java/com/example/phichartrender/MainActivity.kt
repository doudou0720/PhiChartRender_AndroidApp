package com.example.phichartrender

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.example.phichartrender.databinding.ActivityMainBinding
import android.os.Handler
import android.os.Looper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var httpServer: HttpServerManager
    private var isServerRunning = false
    private lateinit var openWebViewButton: Button
    private lateinit var versionSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // 初始化HTTP服务器但不立即启动
        httpServer = HttpServerManager()
        
        // 设置版本选择模块
        setupVersionSpinner()

        binding.fab.setOnClickListener { view ->
            toggleServer()
        }
        
        // 设置打开WebView按钮的点击事件
        openWebViewButton = findViewById<Button>(R.id.button_open_webview)
        openWebViewButton.setOnClickListener {
            openWebView()
        }
        
        // 初始设置为启动服务
        isServerRunning = false
        toggleServer()
    }

    private fun toggleServer() {
        if (isServerRunning) {
            // 停止服务器
            httpServer.stop()
            isServerRunning = false
            binding.fab.setImageResource(android.R.drawable.ic_media_play)
            Toast.makeText(this, "Server stopped", Toast.LENGTH_SHORT).show()
            
            // 服务器停止时，禁用打开WebView按钮
            openWebViewButton.isEnabled = false
            
            // 服务器停止时，启用版本选择
            versionSpinner.isEnabled = true
        } else {
            // 启动服务器
            httpServer.start()
            isServerRunning = true
            binding.fab.setImageResource(android.R.drawable.ic_media_pause)
            Toast.makeText(this, "Server started", Toast.LENGTH_SHORT).show()
            
            // 服务器运行时，启用打开WebView按钮
            openWebViewButton.isEnabled = true
            
            // 服务器运行时，禁用版本选择
            versionSpinner.isEnabled = false
        }
    }

    private fun openWebView() {
        // 使用Handler将启动WebView的操作放到消息队列中，避免阻塞UI线程
        Handler(Looper.getMainLooper()).post {
            val intent = Intent(this, WebViewActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupVersionSpinner() {
        // 创建版本选项
        val versions = arrayOf("Version 1.0", "Version 2.0", "Version 3.0")
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, versions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        versionSpinner = findViewById<Spinner>(R.id.versionSpinner)
        versionSpinner.adapter = adapter
        
        versionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedVersion = versions[position]
                Toast.makeText(this@MainActivity, "Selected: $selectedVersion", Toast.LENGTH_SHORT).show()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>) {
                // 空实现
            }
        }
        
        // 初始时启用版本选择
        versionSpinner.isEnabled = true
        
        // 初始时禁用打开WebView按钮
        openWebViewButton.isEnabled = false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                // 打开设置页面
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return super.onSupportNavigateUp()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 停止HTTP服务器
        if (isServerRunning) {
            httpServer.stop()
        }
    }
}