package com.example.a598_tumor_cnn

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class MainActivity : AppCompatActivity() {

    private lateinit var interpreter: Interpreter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Load the TensorFlow Lite model
        interpreter = Interpreter(loadModelFile())

        // Process the image and display the output
        processImage()
    }

    private fun loadModelFile(): MappedByteBuffer {
        val assetManager: AssetManager = assets
        val fileDescriptor = assetManager.openFd("model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun processImage() {
        // Load the image (for simplicity, assuming it's in the assets directory)
        val bitmap = BitmapFactory.decodeStream(assets.open("G_4.jpg"))

        // Perform inference with the TensorFlow Lite model
        val outputText = performInference(bitmap)

        // Display the output in the TextView
        val textView: TextView = findViewById(R.id.outputTextView)
        textView.text = outputText
    }

    private fun performInference(bitmap: Bitmap): String {
        // Preprocess the input image and perform inference with the TensorFlow Lite model
        // Replace this with your actual inference logic
        // For simplicity, let's assume the model takes a bitmap and outputs a string
        return "Output of the TensorFlow Lite model"
    }
}