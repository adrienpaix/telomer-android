package health.telomer.android.feature.consultation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.track.VideoTrack

@Composable
fun VideoCallScreen(
    navController: NavController,
    appointmentId: String,
    viewModel: VideoCallViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Permission state tracking
    var permissionsGranted by remember { mutableStateOf(false) }

    // Check initial permission state
    LaunchedEffect(Unit) {
        permissionsGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
    }

    // Request permissions on first compose
    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                )
            )
        }
    }

    // Connect when we have permissions + token
    LaunchedEffect(state.token, permissionsGranted) {
        if (state.token != null && permissionsGranted && !state.isConnected && !state.isConnecting) {
            viewModel.connect()
        }
    }

    // Handle back press
    BackHandler {
        viewModel.hangUp()
        navController.popBackStack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding(),
    ) {
        when {
            state.error != null && !state.isConnected -> {
                ErrorOverlay(
                    error = state.error!!,
                    onRetry = { viewModel.connect() },
                    onBack = { navController.popBackStack() },
                )
            }
            !permissionsGranted -> {
                PermissionsOverlay(
                    onRequest = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO,
                            )
                        )
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            state.isConnecting || (state.token == null && state.error == null) -> {
                ConnectingOverlay()
            }
            state.isConnected -> {
                VideoCallContent(
                    state = state,
                    onToggleMic = viewModel::toggleMic,
                    onToggleCamera = viewModel::toggleCamera,
                    onHangUp = {
                        viewModel.hangUp()
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}

@Composable
private fun LiveKitVideoView(
    videoTrack: VideoTrack?,
    modifier: Modifier = Modifier,
    mirror: Boolean = false,
) {
    val rendererRef = remember { mutableStateOf<TextureViewRenderer?>(null) }
    val currentTrack = remember { mutableStateOf<VideoTrack?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            currentTrack.value?.removeRenderer(rendererRef.value ?: return@onDispose)
            rendererRef.value?.release()
        }
    }

    // Handle track changes
    LaunchedEffect(videoTrack) {
        val renderer = rendererRef.value ?: return@LaunchedEffect
        // Remove old track
        currentTrack.value?.removeRenderer(renderer)
        // Add new track
        videoTrack?.addRenderer(renderer)
        currentTrack.value = videoTrack
    }

    AndroidView(
        factory = { ctx ->
            TextureViewRenderer(ctx).apply {
                rendererRef.value = this
                setMirror(mirror)
                // Add current track if available
                videoTrack?.addRenderer(this)
                currentTrack.value = videoTrack
            }
        },
        modifier = modifier,
        update = { renderer ->
            renderer.setMirror(mirror)
        },
    )
}

@Composable
private fun VideoCallContent(
    state: VideoCallState,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    onHangUp: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Remote video (full screen) or waiting screen
        if (state.remoteParticipantConnected && state.remoteVideoTrack != null) {
            LiveKitVideoView(
                videoTrack = state.remoteVideoTrack,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            WaitingForDoctor()
        }

        // Status bar at top
        StatusBar(
            roomName = state.roomName,
            duration = state.callDurationSeconds,
            reconnecting = state.error?.contains("Reconnexion") == true,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 8.dp),
        )

        // Local video PiP (bottom right)
        if (state.isCameraOn && state.localVideoTrack != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 100.dp)
                    .width(120.dp)
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            ) {
                LiveKitVideoView(
                    videoTrack = state.localVideoTrack,
                    modifier = Modifier.fillMaxSize(),
                    mirror = true,
                )
            }
        }

        // Control buttons at bottom
        ControlBar(
            isMicOn = state.isMicOn,
            isCameraOn = state.isCameraOn,
            onToggleMic = onToggleMic,
            onToggleCamera = onToggleCamera,
            onHangUp = onHangUp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun StatusBar(
    roomName: String?,
    duration: Long,
    reconnecting: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (reconnecting) {
            Text(
                text = "Reconnexion en cours...",
                color = Color(0xFFF59E0B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = formatDuration(duration),
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        roomName?.let {
            Text(
                text = it,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun ControlBar(
    isMicOn: Boolean,
    isCameraOn: Boolean,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    onHangUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlButton(
            icon = if (isMicOn) Icons.Default.Mic else Icons.Default.MicOff,
            label = if (isMicOn) "Micro" else "Muet",
            isActive = isMicOn,
            onClick = onToggleMic,
        )
        ControlButton(
            icon = if (isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
            label = if (isCameraOn) "Caméra" else "Off",
            isActive = isCameraOn,
            onClick = onToggleCamera,
        )
        IconButton(
            onClick = onHangUp,
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFFEF4444), CircleShape),
        ) {
            Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = "Raccrocher",
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(52.dp)
                .background(
                    if (isActive) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                    CircleShape,
                ),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.White else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun WaitingForDoctor() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.PersonSearch,
                contentDescription = null,
                tint = Color.White.copy(alpha = alpha),
                modifier = Modifier.size(64.dp),
            )
            Text(
                text = "En attente du praticien...",
                color = Color.White.copy(alpha = alpha),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Vous serez connecté automatiquement\nlorsque le praticien rejoindra la salle",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ConnectingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
            )
            Text(
                text = "Connexion à la consultation...",
                color = Color.White,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun ErrorOverlay(
    error: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = error,
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onBack,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                ) {
                    Text("Retour")
                }
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0596DE)),
                ) {
                    Text("Réessayer")
                }
            }
        }
    }
}

@Composable
private fun PermissionsOverlay(
    onRequest: () -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.VideoCameraFront,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = "Autorisations requises",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "La caméra et le microphone sont nécessaires pour la visioconférence.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onBack,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                ) {
                    Text("Retour")
                }
                Button(
                    onClick = onRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0596DE)),
                ) {
                    Text("Autoriser")
                }
            }
        }
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
