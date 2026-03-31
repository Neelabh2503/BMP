package com.edgesite

import android.content.Intent
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import com.edgesite.yolov8tflite.LiveDetectionActivity
import com.edgesite.yolov8tflite.ImageDetectionActivity

class MainActivity: FlutterActivity() {

    private val CHANNEL = "yolo_channel"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->

                when (call.method) {

                    "startDetection" -> {
                        val intent = Intent(this, LiveDetectionActivity::class.java)
                        startActivity(intent)
                        result.success(null)
                    }

                    "detectImage" -> {
                        val intent = Intent(this, ImageDetectionActivity::class.java)
                        startActivity(intent)
                        result.success(null)
                    }

                    else -> result.notImplemented()
                }
            }
    }
}