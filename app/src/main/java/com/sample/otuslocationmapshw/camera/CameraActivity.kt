package com.sample.otuslocationmapshw.camera

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.CAMERA
import android.Manifest.permission.HIGH_SAMPLING_RATE_SENSORS
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
import androidx.camera.core.ImageCapture.OnImageSavedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.common.util.concurrent.ListenableFuture
import com.sample.otuslocationmapshw.databinding.ActivityCameraBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs


class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding

//    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
//    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var imageCapture: ImageCapture

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var sensorManager: SensorManager
    private var sensorEventListener: SensorEventListener? = null
    private var tiltSensor: Sensor? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        getLastLocation { location ->
            Log.d("LAST LOCATION", location.toString())
        }

//        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // TODO("Получить экземпляр SensorManager")
        sensorManager = this.getSystemService(SensorManager::class.java)

        // TODO("Добавить проверку на наличие датчика акселерометра и присвоить значение tiltSensor")
        tiltSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (tiltSensor != null) {

            // TODO("Подписаться на получение событий обновления датчика")

            sensorEventListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val tilt = event.values[2]
                    binding.errorTextView.visibility = if (abs(tilt) > 2) View.VISIBLE else View.GONE
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                    //nothing to do
                }
            }

            sensorManager.registerListener(sensorEventListener, tiltSensor, SensorManager.SENSOR_DELAY_UI)
        }

//        cameraProviderFuture.addListener({
//            cameraProvider = cameraProviderFuture.get()
//        }, ContextCompat.getMainExecutor(this@CameraActivity))

        binding.takePhotoButton.setOnClickListener {
            takePhoto()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    //@SuppressLint("MissingPermission")
    private fun takePhoto() {
        getLastLocation { location ->
            Log.d("LOCATION", location.toString())

            val folder = File("${filesDir.absolutePath}/photos/").apply {
                if (!exists()) mkdirs()
            }

            val filePath = folder.path + "/img_" + SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault()).format(Date())

            val f = File("$filePath.jpg").apply {
                if (!exists()) createNewFile()
            }

//            val f = File.createTempFile(
//                "img_" + SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault()).format(Date()),
//                ".jpg",
//                folder
//            )
            Log.d("filename", f.path)

            // TODO("4. Добавить установку местоположения в метаданные фото")
            val outputFileOptions = ImageCapture.OutputFileOptions
                .Builder(f)
                //.Builder(File(filePath))
                .setMetadata(ImageCapture.Metadata().apply { this.location = location })
                .build()

            // TODO("Добавить вызов CameraX для фото")
            // TODO("Вывести Toast о том, что фото успешно сохранено и закрыть текущее активити c указанием кода результата SUCCESS_RESULT_CODE")
            imageCapture.takePicture(
                outputFileOptions,
                ContextCompat.getMainExecutor(this@CameraActivity),
                object: OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        Toast.makeText(this@CameraActivity, "New photo saved", Toast.LENGTH_SHORT).show()
                        setResult(SUCCESS_RESULT_CODE)
                        finish()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.d("TAKE PHOTO", exception.toString())
                        Toast.makeText(this@CameraActivity, "Something wrong :-(", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation(callback: (location: Location?) -> Unit) {
        // TODO("Добавить получение местоположения от fusedLocationClient и передать результат в callback после получения")

        if (permissionsGranted(arrayOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION))) {
            Log.d("PERMISSIONS", "Location access granted")

            // получение последнего известного местоположения
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                // результат может быть null
                location?.let {
                    callback.invoke(location)
                } ?: Log.d("LAST LOCATION", "unknown")
            }

//            val currentLocationRequest = CurrentLocationRequest.Builder()
//                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
//                .setGranularity(Granularity.GRANULARITY_COARSE)
//                .build()
//
//            fusedLocationClient.getCurrentLocation(currentLocationRequest, null).addOnSuccessListener { location ->
//                // результат может быть null
//                location?.let {
//                    callback.invoke(location)
//                } ?: Log.d("CURRENT LOCATION", "unknown")
//            }

        } else
            Log.d("PERMISSIONS", "Location access denied")
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this@CameraActivity)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()
//            imageCapture = ImageCapture
//                .Builder()
//                .setCaptureMode(CAPTURE_MODE_MINIMIZE_LATENCY)
//                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this@CameraActivity, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this@CameraActivity))
    }

    private fun permissionsGranted(permissions: Array<out String>) = permissions.all { ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED }

    private fun allPermissionsGranted() = permissionsGranted(REQUIRED_PERMISSIONS)

    override fun onDestroy() {
        super.onDestroy()
        // TODO("Остановить получение событий от датчика")
        if (sensorEventListener != null)
            sensorManager.unregisterListener(sensorEventListener)
    }

    companion object {

        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10

        // TODO("Указать набор требуемых разрешений")
        private val REQUIRED_PERMISSIONS = arrayOf(
            ACCESS_COARSE_LOCATION,
            ACCESS_FINE_LOCATION,
            CAMERA
        )

        const val SUCCESS_RESULT_CODE = 15
    }
}
