package health.telomer.android.feature.nutrition.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import health.telomer.android.core.ui.theme.*
import health.telomer.android.feature.nutrition.engine.RecognizedFood

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodCameraScreen(
    navController: NavController,
    viewModel: FoodCameraViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Camera permission handling
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photo du repas") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (state.capturedBitmap == null) Color.Transparent else Color.White,
                ),
            )
        },
    ) { padding ->
        if (!hasCameraPermission) {
            // Permission denied state
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, null, tint = TelomerGray500, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Permission caméra requise", color = TelomerGray900, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Autorisez l'accès à la caméra pour photographier vos repas", color = TelomerGray500)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Autoriser la caméra")
                    }
                }
            }
        } else if (state.capturedBitmap == null) {
            // Camera preview
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }.also { previewView ->
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    val imageCapture = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                        .build()
                                    viewModel.setImageCapture(imageCapture)
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        ctx as LifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview, imageCapture,
                                    )
                                } catch (_: Exception) {}
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                // Capture button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                ) {
                    IconButton(
                        onClick = { viewModel.capturePhoto(context) },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f)),
                    ) {
                        Icon(
                            Icons.Default.CameraAlt, "Capturer",
                            tint = TelomerBlue, modifier = Modifier.size(36.dp),
                        )
                    }
                }
            }
        } else {
            // Results view
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (state.isAnalyzing) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = TelomerBlue)
                            Spacer(Modifier.height(16.dp))
                            Text("Analyse en cours…", color = TelomerGray500)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                    ) {
                        item {
                            Text(
                                "Aliments détectés",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Ajustez les quantités si nécessaire",
                                style = MaterialTheme.typography.bodySmall,
                                color = TelomerGray500,
                            )
                        }

                        items(state.recognizedFoods) { food ->
                            RecognizedFoodCard(
                                food = food,
                                portionG = state.portions[food.name] ?: food.estimatedPortionG,
                                onPortionChanged = { viewModel.updatePortion(food.name, it) },
                            )
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.retake() },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reprendre")
                        }
                        Button(
                            onClick = { viewModel.addToMeal(); navController.popBackStack() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = TelomerGreen),
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Ajouter au repas")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecognizedFoodCard(
    food: RecognizedFood,
    portionG: Double,
    onPortionChanged: (Double) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(food.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        "Confiance : ${(food.confidence * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = TelomerGray500,
                    )
                }
                food.estimatedCalories?.let {
                    Text(
                        "${it.toInt()} kcal",
                        fontWeight = FontWeight.Bold,
                        color = TelomerBlue,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // Macro row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MacroChip("P", food.estimatedProteins, TelomerGreen)
                MacroChip("G", food.estimatedCarbs, TelomerOrange)
                MacroChip("L", food.estimatedFats, TelomerRed)
            }

            Spacer(Modifier.height(12.dp))

            // Portion field
            OutlinedTextField(
                value = portionG.toInt().toString(),
                onValueChange = { it.toDoubleOrNull()?.let(onPortionChanged) },
                label = { Text("Quantité (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
    }
}

@Composable
private fun MacroChip(label: String, value: Double?, color: Color) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            "$label: ${value?.toInt() ?: "—"}g",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Medium,
        )
    }
}
