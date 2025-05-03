package com.katsumasa.fishingcontrollerdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.katsumasa.fishingcontrollerdemo.ui.theme.FishingControllerDemoTheme
import kotlinx.coroutines.delay
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class MainActivity : ComponentActivity() {
    private var imu: IMU? = null
    private var ros: RosController? = null

    override fun onStart() {
        super.onStart()
        imu = IMU(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        imu?.close()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FishingControllerDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "釣り竿デモアプリケーション")
                        Spacer(modifier = Modifier.size(30.dp))
                        IpField()
                    }
                }
            }
        }
    }

    @Composable
    fun IpField() {
        var ip by rememberSaveable { mutableStateOf("") }
        var connectStateText by rememberSaveable { mutableStateOf("接続されていません") }
        var connectButtonEnable by rememberSaveable { mutableStateOf(true) }
        var disconnectButtonEnable by rememberSaveable { mutableStateOf(false) }
        var imuStateText by rememberSaveable { mutableStateOf("IMUは動作していません") }
        var subscribeText by rememberSaveable { mutableStateOf("何のメッセージも受信していません") }
        var rotationSpeed by rememberSaveable { mutableStateOf("0") }
        var id by rememberSaveable { mutableStateOf("") }

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                connectStateText = "接続しました"
                ros!!.publisher("/fish/ctrl/imu", "sensor_msgs/msg/Imu")
                ros!!.publisher("/fish/ctrl/out", "std_msgs/msg/Float32")
                ros!!.subscriber("/fish/ctrl/in", "std_msgs/msg/String")
                disconnectButtonEnable = true
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                connectStateText = "接続失敗しました: " + t.message
                connectButtonEnable = true
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                connectStateText = "切断しました"
                connectButtonEnable = true
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                subscribeText = "メッセージを受信しました\n$text"
            }
        }

        LaunchedEffect(Unit) {
            while (true) {
                imuStateText = "加速度：${imu!!.getLinearAccelerationText(3)}\n角速度：${imu!!.getAngularVelocityText(3)}"
                ros?.publishIMU(imu!!)
                ros?.publish("/fish/ctrl/out", rotationSpeed.toFloatOrNull() ?: 0f)
                delay(100)
            }
        }


        TextField(
            value = ip,
            onValueChange = { ip = it },
            label = { Text("ROS Server's IP") },
            singleLine = true,
            enabled = connectButtonEnable,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.size(30.dp))
        Row {
            Button(content = { Text(text = "Connect") }, enabled = connectButtonEnable, onClick = {
                connectStateText = "接続中..."
                connectButtonEnable = false
                try {
                    ros = RosController(ip, listener)
                } catch (e: Exception) {
                    connectStateText = "接続に失敗しました\n${e}"
                    connectButtonEnable = true
                }
            })
            Spacer(modifier = Modifier.size(30.dp))
            Button(content = { Text(text = "Disconnect") }, enabled = disconnectButtonEnable, onClick = {
                connectStateText = "切断中..."
                disconnectButtonEnable = false
                ros?.close(1000, "接続は正常に終了しました")
            })
        }
        Spacer(modifier = Modifier.size(30.dp))
        Text(text = connectStateText, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.size(30.dp))
        TextField(
            value = id,
            onValueChange = {
                id = it
                imu?.setID(id)
            },
            label = { Text(text  = "ID") }
        )
        Spacer(modifier = Modifier.size(30.dp))
        TextField(
            value = rotationSpeed,
            onValueChange = {
                if (it.isEmpty() || it.matches(Regex("^-?[0-9]*\\.?[0-9]*$"))) rotationSpeed = it
            },
            label = { Text(text  = "回転速度") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.size(30.dp))
        Text(text = imuStateText, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.size(30.dp))
        Text(text = subscribeText, textAlign = TextAlign.Center)
    }
}
