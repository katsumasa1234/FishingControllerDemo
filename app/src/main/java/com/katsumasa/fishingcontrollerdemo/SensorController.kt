package com.katsumasa.fishingcontrollerdemo

import android.content.Context
import android.graphics.drawable.GradientDrawable.Orientation
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import org.json.JSONArray
import org.json.JSONObject

class SensorController(private val context: Context) : SensorEventListener {
    private var frameId = ""
    private val imuTopic = "/fish/ctrl/imu"
    private var orientation: Quaternion = Quaternion(0.0, 0.0, 0.0, -1.0)
    private var angularVelocity: Triple<Double, Double, Double> = Triple(0.0, 0.0, 0.0)
    private var linearAcceleration: Triple<Double, Double, Double> = Triple(0.0, 0.0, 0.0)

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    init {
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, rotation, SensorManager.SENSOR_DELAY_GAME)
    }

    fun setID(id: String) {
        frameId = id
    }

    fun getLinearAcceleration(): Triple<Double, Double, Double> {
        return linearAcceleration
    }

    fun getAngularVelocity(): Triple<Double, Double, Double> {
        return angularVelocity
    }

    fun close() {
        sensorManager.unregisterListener(this)
    }

    fun getLinearAccelerationText(digits: Int): String {
        val allDigits = 5 + digits
        return "(%${allDigits}.${digits}f, %${allDigits}.${digits}f, %${allDigits}.${digits}f)".format(linearAcceleration.first, linearAcceleration.second, linearAcceleration.third)
    }

    fun getAngularVelocityText(digits: Int): String {
        val allDigits = 5 + digits
        return "(%${allDigits}.${digits}f, %${allDigits}.${digits}f, %${allDigits}.${digits}f)".format(angularVelocity.first, angularVelocity.second, angularVelocity.third)
    }

    fun getOrientationText(digits: Int): String {
        val allDigits = 5 + digits
        return "(%${allDigits}.${digits}f, %${allDigits}.${digits}f, %${allDigits}.${digits}f, %${allDigits}.${digits}f)".format(orientation.x, orientation.y, orientation.z, orientation.w)
    }

    fun getImuJson(): JSONObject {
        val currentTimeMillis = System.currentTimeMillis()
        val currentNano = System.nanoTime()

        val imuMsg = JSONObject()
        imuMsg.put("op", "publish")
        imuMsg.put("topic", imuTopic)

        val msg = JSONObject()

        val header = JSONObject().apply {
            put("stamp", JSONObject().apply {
                put("sec", currentTimeMillis / 1000)
                put("nanosec", (currentNano % 1_000_000_000))
            })
            put("frame_id", frameId)
        }
        msg.put("header", header)

        val orientationCovariance = JSONArray(listOf(-1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))

        msg.put("orientation", JSONObject().apply {
            put("x", orientation.x)
            put("y", orientation.y)
            put("z", orientation.z)
            put("w", orientation.w)
        })
        msg.put("orientation_covariance", orientationCovariance)

        msg.put("angular_velocity", JSONObject().apply {
            put("x", angularVelocity.first)
            put("y", angularVelocity.second)
            put("z", angularVelocity.third)
        })
        msg.put("angular_velocity_covariance", JSONArray(List(9) { 0.0 }))

        msg.put("linear_acceleration", JSONObject().apply {
            put("x", linearAcceleration.first)
            put("y", linearAcceleration.second)
            put("z", linearAcceleration.third)
        })
        msg.put("linear_acceleration_covariance", JSONArray(List(9) { 0.0 }))

        imuMsg.put("msg", msg)
        return imuMsg
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                val value = event.values.clone()
                linearAcceleration = Triple(value[0].toDouble(), value[1].toDouble(), value[2].toDouble())
            }
            Sensor.TYPE_GYROSCOPE -> {
                val value = event.values.clone()
                angularVelocity = Triple(value[0].toDouble(), value[1].toDouble(), value[2].toDouble())
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                val value = event.values.clone()
                val q = FloatArray(4)
                SensorManager.getQuaternionFromVector(q, value)
                orientation = Quaternion(q[0].toDouble(), q[1].toDouble(), q[2].toDouble(), q[3].toDouble())
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}