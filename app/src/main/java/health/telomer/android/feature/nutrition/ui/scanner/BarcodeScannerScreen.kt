package health.telomer.android.feature.nutrition.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import health.telomer.android.core.ui.theme.*
import health.telomer.android.feature.nutrition.domain.model.FoodItem
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(
    navController: NavController,
    viewModel: BarcodeScannerViewModel = hiltViewModel(),
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
                title = { Text("Scanner code-barres") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (state.scannedFood == null) Color.Transparent else Color.White,
                    titleContentColor = if (state.scannedFood == null) Color.White else Color.Unspecified,
                    navigationIconContentColor = if (state.scannedFood == null) Color.White else Color.Unspecified,
                ),
            )
        },
    ) { padding ->
        if (!hasCameraPermission) {
            // Permission denied state
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.QrCodeScanner, null, tint = TelomerGray500, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Permission caméra requise", color = TelomerGray900, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Autorisez l'accès à la caméra pour scanner les codes-barres", color = TelomerGray500)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Autoriser la caméra")
                    }
                }
            }
        } else if (state.scannedFood == null) {
            // Scanner view
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                        }.also { previewView ->
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }

                                    val analyzer = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                        .also {
                                            it.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                                processBarcode(imageProxy, viewModel)
                                            }
                                        }

                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        ctx as LifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview, analyzer,
                                    )
                                } catch (_: Exception) {}
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                // Scan overlay
                ScanOverlay()

                // Status
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Recherche du produit…", color = Color.White)
                    } else {
                        Text(
                            "Placez le code-barres dans le cadre",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                        )
                    }

                    state.error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = TelomerRed, fontSize = 13.sp)
                    }
                }
            }
        } else {
            // Product card
            ProductDetailView(
                food = state.scannedFood!!,
                quantityG = state.quantityG,
                onQuantityChanged = viewModel::updateQuantity,
                onAdd = { viewModel.addToMeal(); navController.popBackStack() },
                onRescan = viewModel::rescan,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun ScanOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scanLine",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val boxWidth = size.width * 0.7f
        val boxHeight = boxWidth * 0.5f
        val left = (size.width - boxWidth) / 2
        val top = (size.height - boxHeight) / 2

        // Dark overlay
        drawRect(Color.Black.copy(alpha = 0.5f))
        // Clear center
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(16f, 16f),
            blendMode = BlendMode.Clear,
        )
        // Border
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(16f, 16f),
            style = Stroke(width = 3f),
        )
        // Scan line
        val lineY = top + scanLineY * boxHeight
        drawLine(
            color = TelomerGreen,
            start = Offset(left + 8, lineY),
            end = Offset(left + boxWidth - 8, lineY),
            strokeWidth = 2f,
        )
    }
}

@Composable
private fun ProductDetailView(
    food: FoodItem,
    quantityG: Double,
    onQuantityChanged: (Double) -> Unit,
    onAdd: () -> Unit,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    food.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                food.barcode?.let {
                    Text("EAN : $it", fontSize = 12.sp, color = TelomerGray500)
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = TelomerGray100)
                Spacer(Modifier.height(16.dp))

                Text("Valeurs pour 100g", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    NutrientChip("Calories", "${food.caloriesKcal?.toInt() ?: "—"}", "kcal", TelomerBlue)
                    NutrientChip("Protéines", "${food.proteinsG?.toInt() ?: "—"}", "g", TelomerGreen)
                    NutrientChip("Glucides", "${food.carbsG?.toInt() ?: "—"}", "g", TelomerOrange)
                    NutrientChip("Lipides", "${food.fatsG?.toInt() ?: "—"}", "g", TelomerRed)
                }
            }
        }

        // Quantity
        OutlinedTextField(
            value = quantityG.toInt().toString(),
            onValueChange = { it.toDoubleOrNull()?.let(onQuantityChanged) },
            label = { Text("Quantité (g)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.weight(1f))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onRescan, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.QrCodeScanner, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Re-scanner")
            }
            Button(
                onClick = onAdd,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = TelomerGreen),
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Ajouter au repas")
            }
        }
    }
}

@Composable
private fun NutrientChip(label: String, value: String, unit: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        Text(unit, fontSize = 11.sp, color = TelomerGray500)
        Text(label, fontSize = 10.sp, color = TelomerGray500)
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processBarcode(imageProxy: ImageProxy, viewModel: BarcodeScannerViewModel) {
    val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    val scanner = BarcodeScanning.getClient()

    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            for (barcode in barcodes) {
                if (barcode.format in listOf(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A)) {
                    barcode.rawValue?.let { viewModel.onBarcodeDetected(it) }
                }
            }
        }
        .addOnCompleteListener { imageProxy.close() }
}
