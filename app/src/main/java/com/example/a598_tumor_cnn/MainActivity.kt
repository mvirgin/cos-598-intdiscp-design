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
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Button

class MainActivity : AppCompatActivity() {

    private lateinit var interpreter: Interpreter
    private val imgSize = 256 // Input size of your model
    private val numClasses = 4
    private val imageReqCode = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Load the TensorFlow Lite model
        interpreter = Interpreter(loadModelFile())

        // Find button by ID
        val selectImageButton = findViewById<Button>(R.id.button)

        // Set a click listener on the button
        selectImageButton.setOnClickListener {
            // Open the gallery to select an image
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, imageReqCode)
        }
    }

    // when user clicks button, image gets processed by model
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == imageReqCode && resultCode == Activity.RESULT_OK) {
            // Get the URI of the selected image
            val selectedImageUri = data?.data
            // Pass the URI to your processing function
            if (selectedImageUri != null) {
                processImage(selectedImageUri)
            }
        }
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
            }   //issue was that I was resizing the images here (preprocessing), when my model already does that
        }
        return byteBuffer
    }

    private fun resizeBitmap(bitmap: Bitmap): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, imgSize, imgSize, false)
    }

    private fun getOutputLabel(output: FloatArray): String {
        // get the label corresponding to the highest probability class
        val labels = arrayOf("glioma tumor", "meningioma tumor", "no tumor", "pituitary tumor")
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
        return getOutputLabel(output[0])
    }

    private fun processImage(imageUri: Uri) {
        val inputStream = contentResolver.openInputStream(imageUri)
        // Load the image
        val bitmap = BitmapFactory.decodeStream(inputStream)

        // Perform inference with the TensorFlow Lite model
        val outputText = performInference(bitmap)

        // Display the output in the TextView
        val textView: TextView = findViewById(R.id.outputTextView)
        textView.text = outputText
    }
}