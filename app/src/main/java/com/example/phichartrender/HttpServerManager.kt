package com.example.phichartrender

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class HttpServerManager(private val rootDir: File? = null) : NanoHTTPD(9000) {
    companion object {
        private const val TAG = "HttpServerManager"
    }

    override fun start() {
        try {
            super.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            Log.d(TAG, "HTTP Server started on 127.0.0.1:9000")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start HTTP server", e)
        }
    }

    override fun stop() {
        super.stop()
        Log.d(TAG, "HTTP Server stopped")
    }

    override fun serve(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val uri = session.uri
        Log.d(TAG, "Received request for URI: $uri")

        // 如果指定了根目录，则尝试从该目录提供文件
        if (rootDir != null) {
            try {
                // 处理根路径请求，尝试返回index.html
                val filePath = if (uri == "/") {
                    File(rootDir, "index.html")
                } else {
                    // 清理URI路径，防止路径遍历攻击
                    val cleanUri = uri.replace("..", "").replace("//", "/")
                    File(rootDir, cleanUri)
                }

                // 检查文件是否存在且是文件（不是目录）
                if (filePath.exists() && filePath.isFile) {
                    val mimeType = getMimeType(filePath.extension)
                    val fis = FileInputStream(filePath)
                    return NanoHTTPD.newFixedLengthResponse(
                        NanoHTTPD.Response.Status.OK,
                        mimeType,
                        fis,
                        filePath.length()
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error serving file: ${e.message}")
            }
        }

        // 默认响应
        return when (uri) {
            "/" -> {
                NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.OK,
                    "text/html",
                    "<html><body><h1>PhiChartRender HTTP Server</h1><p>Server is running on port 9000</p></body></html>"
                )
            }
            "/version" -> {
                NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.OK,
                    "application/json",
                    "{\"app\": \"PhiChartRender\", \"version\": \"1.0.0\"}"
                )
            }
            else -> {
                NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.NOT_FOUND,
                    "text/html",
                    "<html><body><h1>404 - Not Found</h1><p>The requested resource was not found.</p></body></html>"
                )
            }
        }
    }

    private fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js" -> "application/javascript"
            "json" -> "application/json"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "svg" -> "image/svg+xml"
            "ico" -> "image/x-icon"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            "ttf" -> "font/ttf"
            "eot" -> "application/vnd.ms-fontobject"
            "txt" -> "text/plain"
            else -> "application/octet-stream"
        }
    }
}