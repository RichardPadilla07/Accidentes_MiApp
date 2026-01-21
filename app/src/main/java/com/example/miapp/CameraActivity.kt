package com.example.miapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.miapp.ui.theme.MiAppTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CameraActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiAppTheme {
                CameraScreen(onImageCaptured = { uri ->
                    val resultIntent = Intent().apply {
                        putExtra("image_uri", uri.toString())
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                })
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(onImageCaptured: (Uri) -> Unit) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri?.let { uri ->
                onImageCaptured(uri)
            }
        }
    }

    fun createImageFile(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.externalCacheDir ?: context.cacheDir
        val imageFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Captura de Foto",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )


            Button(
                onClick = {
                    if (cameraPermissionState.status.isGranted) {
                        imageUri = createImageFile()
                        takePictureLauncher.launch(imageUri!!)
                    } else {
                        cameraPermissionState.launchPermissionRequest()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (cameraPermissionState.status.isGranted)
                        "Tomar Foto y Guardar"
                    else
                        "Solicitar Permiso de Cámara"
                )
            }
        }
    }
}
