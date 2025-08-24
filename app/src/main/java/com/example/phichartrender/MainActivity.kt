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
import androidx.preference.PreferenceManager
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import android.util.Log
import android.widget.TextView
import androidx.core.content.ContextCompat
import android.graphics.Color
import android.graphics.Typeface
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat as CoreContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.util.Locale
import android.widget.ProgressBar
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.FileInputStream
import java.util.zip.ZipInputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var httpServer: HttpServerManager
    private var isServerRunning = false
    private lateinit var openWebViewButton: Button
    private lateinit var downloadButton: Button
    private lateinit var downloadProgress: ProgressBar
    private lateinit var downloadStatus: TextView
    private lateinit var versionSpinner: Spinner
    private var versions = mutableListOf<VersionInfo>()
    private lateinit var versionAdapter: ArrayAdapter<String>
    private val PERMISSION_REQUEST_CODE = 1001
    private lateinit var archiveDownloadReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        // 应用设置的主题
        applyTheme()
        
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
        
        // 设置下载按钮的点击事件
        downloadButton = findViewById<Button>(R.id.button_download)
        downloadProgress = findViewById<ProgressBar>(R.id.download_progress)
        downloadStatus = findViewById<TextView>(R.id.download_status)
        downloadButton.setOnClickListener {
            downloadSelectedVersion()
        }
        
        // 注册广播接收器以接收下载完成的通知
        archiveDownloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // 当收到来自ArchiveManager的下载完成广播时，刷新版本列表
                fetchVersionsFromGitHub()
                Toast.makeText(context, "已同步新下载的版本", Toast.LENGTH_SHORT).show()
            }
        }
        
        val filter = IntentFilter("ARCHIVE_DOWNLOADED")
        LocalBroadcastManager.getInstance(this).registerReceiver(archiveDownloadReceiver, filter)
        
        // 初始设置为启动服务
        isServerRunning = false
        toggleServer()
    }

    private fun applyTheme() {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        
        // 应用主题设置，处理可能的类型不匹配问题
        val theme = try {
            // 尝试获取字符串值（新的ListPreference）
            sharedPref.getString("dark_mode", "system")
        } catch (e: ClassCastException) {
            // 如果失败，尝试获取布尔值（旧的SwitchPreferenceCompat）并转换
            val isDarkMode = sharedPref.getBoolean("dark_mode", false)
            if (isDarkMode) "dark" else "light"
        }
        
        val mode = when (theme) {
            "light" -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            else -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        delegate.localNightMode = mode
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
            // 检查是否有选中的版本
            val selectedPosition = versionSpinner.selectedItemPosition
            if (selectedPosition < 0 || selectedPosition >= versions.size) {
                Toast.makeText(this, "请选择一个版本启动服务器", Toast.LENGTH_SHORT).show()
                return
            }
            
            val selectedVersion = versions[selectedPosition]
            
            // 解压选中版本到app目录
            val success = extractVersionToAppDir(selectedVersion)
            if (!success) {
                Toast.makeText(this, "解压版本失败，请先下载该版本", Toast.LENGTH_LONG).show()
                return
            }
            
            // 获取app目录作为服务器根目录
            val appDir = File(filesDir, "app")
            
            // 启动服务器
            httpServer = HttpServerManager(appDir)
            httpServer.start()
            isServerRunning = true
            binding.fab.setImageResource(android.R.drawable.ic_media_pause)
            Toast.makeText(this, "Server started with version: ${selectedVersion.commitId}", Toast.LENGTH_SHORT).show()
            
            // 服务器运行时，启用打开WebView按钮
            openWebViewButton.isEnabled = true
            
            // 服务器运行时，禁用版本选择
            versionSpinner.isEnabled = false
        }
    }

    private fun extractVersionToAppDir(version: VersionInfo): Boolean {
        try {
            // 获取zip文件路径
            val archivesDir = File(filesDir, "archives")
            val zipFile = File(archivesDir, "${version.commitId}.zip")
            
            // 检查文件是否存在
            if (!zipFile.exists()) {
                return false
            }
            
            // 获取目标app目录
            val appDir = File(filesDir, "app")
            
            // 如果app目录已存在，先删除
            if (appDir.exists()) {
                appDir.deleteRecursively()
            }
            
            // 创建新的app目录
            appDir.mkdirs()
            
            // 解压zip文件
            val fis = FileInputStream(zipFile)
            val zis = ZipInputStream(fis)
            
            var entry = zis.nextEntry
            while (entry != null) {
                // 跳过根目录（通常是phi-chart-render-commitId）
                val name = entry.name
                val relativePath = name.substring(name.indexOf('/') + 1)
                
                if (relativePath.isNotEmpty()) {
                    val outputFile = File(appDir, relativePath)
                    
                    if (entry.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        // 确保父目录存在
                        outputFile.parentFile?.mkdirs()
                        
                        // 写入文件
                        val fos = FileOutputStream(outputFile)
                        val buffer = ByteArray(1024)
                        var length: Int
                        while (zis.read(buffer).also { length = it } > 0) {
                            fos.write(buffer, 0, length)
                        }
                        fos.close()
                    }
                }
                
                zis.closeEntry()
                entry = zis.nextEntry
            }
            
            zis.close()
            fis.close()
            
            return true
        } catch (e: Exception) {
            Log.e("MainActivity", "解压版本时出错", e)
            return false
        }
    }

    private fun openWebView() {
        // 使用Handler将启动WebView的操作放到消息队列中，避免阻塞UI线程
        Handler(Looper.getMainLooper()).post {
            val intent = Intent(this, WebViewActivity::class.java)
            startActivity(intent)
        }
    }

    private fun downloadSelectedVersion() {
        // 检查存储权限
        if (CoreContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
            != PackageManager.PERMISSION_GRANTED) {
            // 请求权限
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
            return
        }
        
        // 检查是否有选中的版本
        val selectedPosition = versionSpinner.selectedItemPosition
        if (selectedPosition < 0 || selectedPosition >= versions.size) {
            Toast.makeText(this, "请选择一个版本进行下载", Toast.LENGTH_SHORT).show()
            return
        }
        
        val selectedVersion = versions[selectedPosition]
        startDownload(selectedVersion)
    }

    private fun startDownload(version: VersionInfo) {
        Thread {
            try {
                // 显示开始下载提示
                runOnUiThread {
                    downloadStatus.text = "开始下载版本: ${version.commitId}"
                    downloadStatus.visibility = View.VISIBLE
                    downloadProgress.visibility = View.VISIBLE
                    downloadProgress.progress = 0
                    downloadButton.isEnabled = false
                }
                
                // 构建下载URL (这里假设从GitHub下载)
                val originalUrl = "https://github.com/doudou0720/phi-chart-render/archive/${version.commitId}.zip"
                
                // 获取偏好设置
                val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
                val useAcceleration = sharedPref.getBoolean("download_acceleration", false)
                val accelerationSource = sharedPref.getString("acceleration_source", "ghfast.top")
                
                // 构建最终URL
                val downloadUrl = if (useAcceleration && !accelerationSource.isNullOrEmpty()) {
                    "https://$accelerationSource/$originalUrl"
                } else {
                    originalUrl
                }
                
                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()
                
                // 检查响应码
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    runOnUiThread {
                        downloadStatus.text = "下载失败: HTTP ${connection.responseCode}"
                        downloadButton.isEnabled = true
                        Toast.makeText(this, "下载失败: HTTP ${connection.responseCode}", Toast.LENGTH_LONG).show()
                    }
                    return@Thread
                }
                
                // 获取文件大小
                val fileLength = connection.contentLength
                
                // 获取文件名
                val fileName = "${version.commitId}.zip"
                
                // 获取应用数据目录下的archives目录
                val archivesDir = File(filesDir, "archives")
                if (!archivesDir.exists()) {
                    archivesDir.mkdirs()
                }
                
                val file = File(archivesDir, fileName)
                val inputStream: InputStream = connection.inputStream
                val outputStream = FileOutputStream(file)
                
                // 写入文件并更新进度
                val buffer = ByteArray(1024)
                var totalBytesRead: Long = 0
                var bytesRead: Int
                
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    
                    // 更新进度
                    if (fileLength > 0) {
                        val progress = (totalBytesRead * 100 / fileLength).toInt()
                        runOnUiThread {
                            downloadProgress.progress = progress
                            downloadStatus.text = "下载中... $progress%"
                        }
                    }
                }
                
                // 关闭流
                inputStream.close()
                outputStream.close()
                connection.disconnect()
                
                // 下载完成提示
                runOnUiThread {
                    downloadStatus.text = "下载完成: $fileName"
                    downloadButton.isEnabled = true
                    Toast.makeText(this, "下载完成: $fileName", Toast.LENGTH_LONG).show()
                    
                    // 将下载的版本添加到版本列表中（如果尚未存在）
                    addDownloadedVersionToList(version)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "下载出错", e)
                runOnUiThread {
                    downloadStatus.text = "下载失败: ${e.message}"
                    downloadButton.isEnabled = true
                    Toast.makeText(this, "下载失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
    
    private fun addDownloadedVersionToList(version: VersionInfo) {
        // 检查版本是否已存在于列表中
        val exists = versions.any { it.commitId == version.commitId }
        
        if (!exists) {
            // 如果版本不存在于列表中，则添加到列表开头（作为最新版本）
            versions.add(0, version)
            
            // 更新Spinner适配器
            val versionNames = versions.map { it.displayName }
            runOnUiThread {
                versionAdapter.clear()
                versionAdapter.addAll(versionNames)
                versionAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 权限已授予，重新尝试下载
                    downloadSelectedVersion()
                } else {
                    // 权限被拒绝
                    Toast.makeText(this, "需要存储权限才能下载文件", Toast.LENGTH_LONG).show()
                }
                return
            }
        }
    }

    private fun setupVersionSpinner() {
        // 初始化空的版本列表
        versions = mutableListOf()
        
        versionAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mutableListOf()) {
            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view as TextView
                
                // 为最新版本添加特殊标记
                if (position == 0 && versions.isNotEmpty()) {
                    textView.setTypeface(null, Typeface.BOLD)
                    textView.setTextColor(CoreContextCompat.getColor(context, android.R.color.holo_green_dark))
                } else {
                    textView.setTypeface(null, Typeface.NORMAL)
                    textView.setTextColor(CoreContextCompat.getColor(context, android.R.color.primary_text_dark))
                }
                
                return view
            }
        }
        
        versionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        versionSpinner = findViewById<Spinner>(R.id.versionSpinner)
        versionSpinner.adapter = versionAdapter
        
        versionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (versions.isNotEmpty() && position < versions.size) {
                    val selectedVersion = versions[position]
                    Toast.makeText(this@MainActivity, "Selected: ${selectedVersion.commitId}", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>) {
                // 空实现
            }
        }
        
        // 初始时启用版本选择
        versionSpinner.isEnabled = true
        
        // 初始时禁用打开WebView按钮（如果已初始化）
        if (::openWebViewButton.isInitialized) {
            openWebViewButton.isEnabled = false
        }
        
        // 初始时启用下载按钮
        if (::downloadButton.isInitialized) {
            downloadButton.isEnabled = true
        }
        
        // 隐藏进度条和状态文本
        if (::downloadProgress.isInitialized) {
            downloadProgress.visibility = View.GONE
        }
        
        if (::downloadStatus.isInitialized) {
            downloadStatus.visibility = View.GONE
        }
        
        // 从GitHub获取版本信息
        fetchVersionsFromGitHub()
        
        // 添加本地已下载的版本到列表
        addLocalArchivesToVersionList()
    }
    
    private fun addLocalArchivesToVersionList() {
        Thread {
            try {
                // 获取本地存档目录
                val archivesDir = File(filesDir, "archives")
                if (!archivesDir.exists()) {
                    archivesDir.mkdirs()
                    return@Thread
                }
                
                // 获取所有.zip文件
                val archiveFiles = archivesDir.listFiles { file -> 
                    file.extension == "zip" 
                } ?: return@Thread
                
                // 解析文件名获取commitId
                val localVersions = archiveFiles.mapNotNull { file ->
                    // 文件名格式为: commitId.zip
                    val commitId = file.nameWithoutExtension
                    if (commitId.length == 7 && commitId.all { it.isLetterOrDigit() }) {
                        VersionInfo(commitId, commitId, "Local", false)
                    } else {
                        null
                    }
                }
                
                // 将本地版本添加到版本列表中（如果尚未存在）
                runOnUiThread {
                    for (localVersion in localVersions) {
                        val exists = versions.any { it.commitId == localVersion.commitId }
                        if (!exists) {
                            versions.add(localVersion)
                        }
                    }
                    
                    // 更新Spinner适配器
                    val versionNames = versions.map { it.displayName }
                    versionAdapter.clear()
                    versionAdapter.addAll(versionNames)
                    versionAdapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error adding local archives to version list", e)
            }
        }.start()
    }
    
    private fun fetchVersionsFromGitHub() {
        Thread {
            try {
                // 获取提交历史
                val commitsUrl = URL("https://api.github.com/repos/doudou0720/phi-chart-render/commits")
                val commitsConnection = commitsUrl.openConnection() as HttpsURLConnection
                commitsConnection.requestMethod = "GET"
                commitsConnection.connect()
                
                val commitsResponse = commitsConnection.inputStream.bufferedReader().use { it.readText() }
                val commitsJson = JSONArray(commitsResponse)
                
                // 获取最新提交ID作为最新版本
                val latestCommitId = if (commitsJson.length() > 0) {
                    val latestCommit = commitsJson.getJSONObject(0)
                    latestCommit.getJSONObject("sha").toString()
                } else {
                    "latest"
                }
                
                runOnUiThread {
                    // 保存当前选中的版本
                    val selectedCommitId = if (versionSpinner.selectedItemPosition >= 0 && versionSpinner.selectedItemPosition < versions.size) {
                        versions[versionSpinner.selectedItemPosition].commitId
                    } else {
                        null
                    }
                    
                    versions.clear()
                    
                    // 添加最新版本
                    if (commitsJson.length() > 0) {
                        val latestCommit = commitsJson.getJSONObject(0)
                        val commitId = latestCommit.getString("sha").substring(0, 7) // 取前7位作为短ID
                        val commitDate = latestCommit.getJSONObject("commit").getJSONObject("author").getString("date")
                        versions.add(VersionInfo("Latest ($commitId)", commitId, commitDate, true))
                    }
                    
                    // 添加历史版本（最近几个提交）
                    for (i in 1 until minOf(commitsJson.length(), 10)) { // 最多显示10个历史版本
                        val commit = commitsJson.getJSONObject(i)
                        val commitId = commit.getString("sha").substring(0, 7)
                        val commitDate = commit.getJSONObject("commit").getJSONObject("author").getString("date")
                        versions.add(VersionInfo(commitId, commitId, commitDate, false))
                    }
                    
                    // 更新Spinner适配器
                    val versionNames = versions.map { it.displayName }
                    versionAdapter.clear()
                    versionAdapter.addAll(versionNames)
                    versionAdapter.notifyDataSetChanged()
                    
                    // 恢复之前选中的版本（如果仍然存在）
                    if (selectedCommitId != null) {
                        val newIndex = versions.indexOfFirst { it.commitId == selectedCommitId }
                        if (newIndex >= 0) {
                            versionSpinner.setSelection(newIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error fetching versions from GitHub", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to fetch versions: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
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
        // 注销广播接收器
        LocalBroadcastManager.getInstance(this).unregisterReceiver(archiveDownloadReceiver)
    }
}

data class VersionInfo(
    val displayName: String,
    val commitId: String,
    val commitDate: String,
    val isLatest: Boolean
)