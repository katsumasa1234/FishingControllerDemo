package com.katsumasa.fishingcontrollerdemo

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class WebSocketController(ip: String, listener: WebSocketListener) {
    private val client = OkHttpClient()
    private val request = Request.Builder()
    private var webSocket: WebSocket

    init {
        request.url(ip)
        webSocket = client.newWebSocket(request.build(), listener)
    }

    fun send(msg: String) {
        webSocket.send(msg)
    }

    fun close(code: Int, reason: String?) {
        webSocket.close(code, reason)
    }
}