package com.example.phichartrender

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.IOException

class HttpServerManager : NanoHTTPD(9000) {
    companion object {
        private const val TAG = "HttpServerManager"
    }

    override fun start() {
        try {
            super.start(SOCKET_READ_TIMEOUT, false)
            Log.d(TAG, "HTTP Server started on 127.0.0.1:9000")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start HTTP server", e)
        }
    }

    override fun stop() {
        super.stop()
        Log.d(TAG, "HTTP Server stopped")
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        Log.d(TAG, "Received request for URI: $uri")

        return when (uri) {
            "/" -> {
                newFixedLengthResponse(
                    Response.Status.OK,
                    "text/html",
                    "<html><body><h1>PhiChartRender HTTP Server</h1><p>Server is running on port 9000</p></body></html>"
                )
            }
            "/version" -> {
                newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    "{\"app\": \"PhiChartRender\", \"version\": \"1.0.0\"}"
                )
            }
            else -> {
                newFixedLengthResponse(
                    Response.Status.NOT_FOUND,
                    "text/html",
                    "<html><body><h1>404 - Not Found</h1><p>The requested resource was not found.</p></body></html>"
                )
            }
        }
    }
}