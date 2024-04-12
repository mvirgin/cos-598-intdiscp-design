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
import java.nio.ByteBuffer


class MainActivity : AppCompatActivity() {

    private lateinit var interpreter: Interpreter
    private val imgSize = 256 // Input size of your model
    private val numClasses = 4

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

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * imgSize * imgSize * 3) // Assuming 3 channels (RGB)
        byteBuffer.order(java.nio.ByteOrder.nativeOrder())
        val intValues = IntArray(imgSize * imgSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until imgSize) {
            for (j in 0 until imgSize) {
                val `val` = intValues[pixel++]
                byteBuffer.putFloat(((`val` shr 16 and 0xFF)*(1f/1)))
                byteBuffer.putFloat(((`val` shr 8 and 0xFF)*(1f/1)))
                byteBuffer.putFloat(((`val` and 0xFF)*(1f/1)))
            }   //TODO issue was that I was resizing the images here (preprocessing), when my model already does that
        }       //TODO add upload from gallery support and we're done! ALso ^ https://www.youtube.com/watch?v=yV9nrRIC_R0 helpful
        return byteBuffer
    }

    private fun resizeBitmap(bitmap: Bitmap): Bitmap {
        // val imgSize = 224 // Input size of your model
        return Bitmap.createScaledBitmap(bitmap, imgSize, imgSize, false)
    }

    private fun getOutputLabel(output: FloatArray): String {
        // Implement logic to get the label corresponding to the highest probability class
        // This logic depends on how your labels are arranged and stored
        // For simplicity, let's assume labels are stored in an array where the index corresponds to class
        val labels = arrayOf("glioma_tumor", "meningioma_tumor", "no_tumor", "pituitary_tumor") // Replace with your actual labels
        val maxIndex = output.indices.maxByOrNull { output[it] } ?: 0
        return labels[maxIndex]
    }

    private fun performInference(bitmap: Bitmap): String {
        // Resize the input bitmap to match the input size of the model
        val resizedBitmap = resizeBitmap(bitmap)

        // Convert the resized bitmap to a ByteBuffer
        val inputBuffer = bitmapToByteBuffer(resizedBitmap)

        // Run inference
        val output = Array(1) { FloatArray(numClasses) }
        interpreter.run(inputBuffer, output)

        // Get the label corresponding to the highest probability class
        return getOutputLabel(output[0])    // TODO do both label print and probability print?
//        // Print the probabilities for each class
//        val probabilities = output[0]
//        val stringBuilder = StringBuilder()
//        for (i in probabilities.indices) {
//            stringBuilder.append("Class $i: ${probabilities[i]}\n")
//        }
//        return stringBuilder.toString()
    }

    private fun processImage() {
        // Load the image (for simplicity, assuming it's in the assets directory)
        val bitmap = BitmapFactory.decodeStream(assets.open("M_643.jpg"))

        // Perform inference with the TensorFlow Lite model
        val outputText = performInference(bitmap)

        // Display the output in the TextView
        val textView: TextView = findViewById(R.id.outputTextView)
        textView.text = outputText
    }
}