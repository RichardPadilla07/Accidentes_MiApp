package com.example.miapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.miapp.ui.theme.MiAppTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class AccidentActivity : ComponentActivity() {

    private val photoUris = mutableStateListOf<Uri>()
    private val locationState = mutableStateOf("Ubicación no obtenida")

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocation() {
        locationState.value = "Obteniendo ubicación..."
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                locationState.value = if (location != null) {
                    "Lat: ${location.latitude}, Lon: ${location.longitude}"
                } else {
                    "No se pudo obtener la ubicación actual."
                }
                Log.d("AccidentActivity", "Ubicación obtenida: ${locationState.value}")
            }
            .addOnFailureListener { e ->
                locationState.value = "Error al obtener ubicación."
                Log.e("AccidentActivity", "Error de ubicación", e)
            }
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                    permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                Log.d("AccidentActivity", "Permiso de ubicación concedido.")
                fetchCurrentLocation()
            }
            else -> {
                locationState.value = "Permiso de ubicación denegado."
                Log.d("AccidentActivity", "Permiso de ubicación denegado.")
            }
        }
    }

    private fun requestLocationOrFetch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation()
        } else {
            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra("image_uri")?.let { uriString ->
                val photoUri = Uri.parse(uriString)
                photoUris.add(photoUri)
                Log.d("AccidentActivity", "Foto capturada y recibida: $photoUri")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiAppTheme {
                AccidentRegistryScreen(
                    photoUris = photoUris,
                    locationText = locationState.value,
                    onLocationRequest = { requestLocationOrFetch() },
                    onTakePictureRequest = {
                        val intent = Intent(this, CameraActivity::class.java)
                        takePictureLauncher.launch(intent)
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccidentRegistryScreen(
    photoUris: List<Uri>,
    locationText: String,
    onLocationRequest: () -> Unit,
    onTakePictureRequest: () -> Unit
) {
    var accidentType by remember { mutableStateOf("") }
    var accidentDate by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }
    var driverName by remember { mutableStateOf("") }
    var driverId by remember { mutableStateOf("") }
    var observations by remember { mutableStateOf("") }
    val context = LocalContext.current

    // --- NUEVO: Estado para controlar la visibilidad del diálogo ---
    var showSuccessDialog by remember { mutableStateOf(false) }

    // --- NUEVO: Composable para el diálogo de éxito ---
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Éxito") },
            text = { Text("El registro del accidente se ha guardado correctamente.") },
            confirmButton = {
                Button(
                    onClick = { showSuccessDialog = false }
                ) {
                    Text("Aceptar")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Accidente de Tránsito a Registrar", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))

        // --- CAMPOS DEL FORMULARIO CON TÍTULOS AFUERA ---
        Text("Tipo accidente:", style = MaterialTheme.typography.labelLarge)
        TextField(value = accidentType, onValueChange = { accidentType = it }, placeholder = { Text("Choque, colisión, etc.") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))

        Text("Fecha del siniestro:", style = MaterialTheme.typography.labelLarge)
        TextField(value = accidentDate, onValueChange = { accidentDate = it }, placeholder = { Text("DD/MM/AAAA") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))

        Text("Matrícula del auto:", style = MaterialTheme.typography.labelLarge)
        TextField(value = licensePlate, onValueChange = { licensePlate = it }, placeholder = { Text("ABC-1234") }, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))

        Text("Nombre del conductor:", style = MaterialTheme.typography.labelLarge)
        TextField(value = driverName, onValueChange = { driverName = it }, placeholder = { Text("Nombres") }, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))

        Text("Cédula del conductor:", style = MaterialTheme.typography.labelLarge)
        TextField(value = driverId, onValueChange = { driverId = it }, placeholder = { Text("Número de cédula") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))

        Text("Observaciones:", style = MaterialTheme.typography.labelLarge)
        TextField(value = observations, onValueChange = { observations = it }, placeholder = { Text(" ") }, modifier = Modifier.fillMaxWidth().height(100.dp))
        Spacer(Modifier.height(16.dp))

        // --- BOTÓN TOMAR FOTO (AZUL) ---
        Button(
            onClick = { onTakePictureRequest() },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1976D2))
        ) {
            Text("Tomar Foto", color = androidx.compose.ui.graphics.Color.White)
        }
        Spacer(Modifier.height(8.dp))

        // --- MENSAJE DE ESTADO DE FOTO ---
        Text(
            text = if (photoUris.isNotEmpty()) "Se ha tomado con exito la foto" else "Tome una foto",
            style = MaterialTheme.typography.bodySmall,
            color = if (photoUris.isNotEmpty()) androidx.compose.ui.graphics.Color(0xFF4CAF50) else androidx.compose.ui.graphics.Color(0xFFD32F2F)
        )
        Spacer(Modifier.height(12.dp))

        // --- BOTÓN OBTENER UBICACIÓN (ROJO) ---
        Button(
            onClick = { onLocationRequest() },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFD32F2F))
        ) {
            Text("Obtener Ubicación", color = androidx.compose.ui.graphics.Color.White)
        }
        Spacer(Modifier.height(12.dp))

        // --- TEXTO DE UBICACIÓN ---
        Text(locationText, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(24.dp))

        // --- BOTÓN GUARDAR REGISTRO (AZUL) ---
        Button(
            onClick = {
                // 1. Lógica existente (sin cambios)
                Log.d("AccidentRegistry", "Guardando Accidente: $accidentType, Matrícula: $licensePlate, Fotos: ${photoUris.joinToString()}, Ubicación: $locationText")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(5000, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(5000)
                }

                showSuccessDialog = true
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1976D2))
        ) {
            Text("GUARDAR REGISTRO", color = androidx.compose.ui.graphics.Color.White)
        }
    }
}
