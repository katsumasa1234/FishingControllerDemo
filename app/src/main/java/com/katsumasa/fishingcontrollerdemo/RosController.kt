package com.katsumasa.fishingcontrollerdemo

import okhttp3.WebSocketListener
import org.json.JSONObject

class RosController(ip: String, listener: WebSocketListener) {
    private val wsc: WebSocketController = WebSocketController(ip, listener)

    fun publishIMU(imu: IMU) {
        wsc.send(imu.getImuJson().toString())
    }

    fun publisher(topic: String, type:String) {
        val advertiseMsg = JSONObject().apply {
            put("op", "advertise")
            put("topic", topic)
            put("type", type)
        }
        wsc.send(advertiseMsg.toString())
    }

    fun publish(topic: String, data: Float) {
        val publishMsg = JSONObject().apply {
            put("op", "publish")
            put("topic", topic)
            put("msg", JSONObject().apply {
                put("data", data)
            })
        }

        wsc.send(publishMsg.toString())
    }

    fun subscriber(topic: String, type: String?) {
        val subscribeMessage = JSONObject().apply {
            put("op", "subscribe")
            put("topic", topic)
            if (type != null) put("type", type)
        }

        wsc.send(subscribeMessage.toString())
    }

    fun close(code: Int, reason: String?) {
        wsc.close(code, reason)
    }
}