package health.telomer.android.feature.nutrition.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import health.telomer.android.core.ui.theme.*

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
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }
                                    val imageCapture = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                        .build()
                                    viewModel.setImageCapture(imageCapture)
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview, imageCapture,
                                    )
                                } catch (e: Exception) {
                                    if (health.telomer.android.BuildConfig.DEBUG) android.util.Log.e("FoodCamera", "Camera binding failed", e)
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                // Demo mode banner
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 100.dp),
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(0.dp),
                ) {
                    Text(
                        text = "⚠️ Mode démo — Reconnaissance IA non activée",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = Color(0xFF92400E),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                }

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
            // Photo taken — show demo result (no AI)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (state.isAnalyzing) {
                    CircularProgressIndicator(color = TelomerBlue)
                    Spacer(Modifier.height(16.dp))
                    Text("Analyse en cours…", color = TelomerGray500)
                } else {
                    // Photo taken, no AI available
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = TelomerGreen,
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "📷 Photo prise !",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TelomerGray900,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "La reconnaissance automatique des aliments sera bientôt disponible.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TelomerGray500,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "En attendant, utilisez la recherche textuelle pour ajouter vos aliments.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TelomerGray500,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(32.dp))

                    Button(
                        onClick = { navController.navigate("nutrition/search") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = TelomerBlue),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Rechercher manuellement")
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { viewModel.retake() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Reprendre la photo")
                    }
                }
            }
        }
    }
}
