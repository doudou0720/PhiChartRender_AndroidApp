package com.example.phichartrender

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import com.example.phichartrender.databinding.ActivityMainBinding
import android.os.Handler
import android.os.Looper
import android.util.Log

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var httpServer: HttpServerManager
    private lateinit var archiveManager: ArchiveManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // 初始化并启动HTTP服务器
        httpServer = HttpServerManager()
        httpServer.start()
        
        // 初始化存档管理器
        archiveManager = ArchiveManager(this)

        // 显示服务器启动提示
        showServerStartedDialog()

        // 设置版本选择模块
        setupVersionSpinner()

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }
    }

    private fun showServerStartedDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.server_started_title)
        builder.setMessage(R.string.server_started_message)
        
        builder.setPositiveButton(R.string.open_webview) { _, _ ->
            openWebView()
        }
        
        builder.setNegativeButton(R.string.close) { dialog, _ ->
            dialog.dismiss()
        }
        
        builder.setCancelable(false)
        builder.show()
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
        binding.versionSpinner.adapter = adapter
        
        binding.versionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedVersion = versions[position]
                Toast.makeText(this@MainActivity, "Selected: $selectedVersion", Toast.LENGTH_SHORT).show()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>) {
                // 空实现
            }
        }
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
                openSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        return super.onSupportNavigateUp()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 停止HTTP服务器
        httpServer.stop()
    }
}