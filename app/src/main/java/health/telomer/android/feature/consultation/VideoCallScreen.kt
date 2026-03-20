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
import livekit.org.webrtc.EglBase
import livekit.org.webrtc.RendererCommon

@Composable
fun VideoCallScreen(
    navController: NavController,
    appointmentId: String,
    viewModel: VideoCallViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var permissionsGranted by remember { mutableStateOf(false) }

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

    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    LaunchedEffect(state.token, permissionsGranted) {
        if (state.token != null && permissionsGranted && !state.isConnected && !state.isConnecting) {
            viewModel.connect()
        }
    }

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
                            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
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
                    viewModel = viewModel,
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

/**
 * Composable that properly renders a LiveKit VideoTrack using a TextureViewRenderer.
 * Each instance owns its own EglBase and renderer lifecycle.
 * Track attach/detach is handled via DisposableEffect keyed on the track instance.
 */
@Composable
private fun VideoTrackRenderer(
    videoTrack: VideoTrack?,
    modifier: Modifier = Modifier,
    mirror: Boolean = false,
    scalingType: RendererCommon.ScalingType = RendererCommon.ScalingType.SCALE_ASPECT_FIT,
) {
    val eglBase = remember { EglBase.create() }
    val rendererRef = remember { mutableStateOf<TextureViewRenderer?>(null) }

    AndroidView(
        factory = { ctx ->
            TextureViewRenderer(ctx).apply {
                init(eglBase.eglBaseContext, null)
                setMirror(mirror)
                setScalingType(scalingType)
                rendererRef.value = this
            }
        },
        modifier = modifier,
        update = { renderer ->
            renderer.setMirror(mirror)
            renderer.setScalingType(scalingType)
        },
    )

    DisposableEffect(videoTrack) {
        val renderer = rendererRef.value
        if (renderer != null && videoTrack != null) {
            videoTrack.addRenderer(renderer)
        }
        onDispose {
            if (renderer != null && videoTrack != null) {
                videoTrack.removeRenderer(renderer)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            rendererRef.value?.release()
            eglBase.release()
        }
    }
}

@Composable
private fun VideoCallContent(
    state: VideoCallState,
    viewModel: VideoCallViewModel,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    onHangUp: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Remote video (full screen) or waiting screen
        if (state.remoteParticipantConnected && state.remoteVideoTrack != null) {
            VideoTrackRenderer(
                videoTrack = state.remoteVideoTrack,
                modifier = Modifier.fillMaxSize(),
                mirror = false,
                scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FIT,
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
                VideoTrackRenderer(
                    videoTrack = state.localVideoTrack,
                    modifier = Modifier.fillMaxSize(),
                    mirror = true,
                    scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL,
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
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (reconnecting) {
            Text("Reconnexion en cours...", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Text(formatDuration(duration), color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ControlBar(
    isMicOn: Boolean, isCameraOn: Boolean,
    onToggleMic: () -> Unit, onToggleCamera: () -> Unit, onHangUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
        ControlButton(if (isMicOn) Icons.Default.Mic else Icons.Default.MicOff, if (isMicOn) "Micro" else "Muet", isMicOn, onToggleMic)
        ControlButton(if (isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff, if (isCameraOn) "Cam\u00e9ra" else "Off", isCameraOn, onToggleCamera)
        IconButton(onClick = onHangUp, modifier = Modifier.size(64.dp).background(Color(0xFFEF4444), CircleShape)) {
            Icon(Icons.Default.CallEnd, "Raccrocher", tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun ControlButton(icon: ImageVector, label: String, isActive: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(onClick = onClick, modifier = Modifier.size(52.dp).background(Color.White.copy(alpha = if (isActive) 0.2f else 0.1f), CircleShape)) {
            Icon(icon, label, tint = Color.White.copy(alpha = if (isActive) 1f else 0.5f), modifier = Modifier.size(24.dp))
        }
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
    }
}

@Composable
private fun WaitingForDoctor() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(0.4f, 1f, infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse), label = "a")
    Box(Modifier.fillMaxSize().background(Color(0xFF1A1A2E)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.PersonSearch, null, tint = Color.White.copy(alpha = alpha), modifier = Modifier.size(64.dp))
            Text("En attente du praticien...", color = Color.White.copy(alpha = alpha), fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text("Vous serez connect\u00e9 automatiquement\nlorsque le praticien rejoindra la salle", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable private fun ConnectingOverlay() {
    Box(Modifier.fillMaxSize().background(Color(0xFF1A1A2E)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
            Text("Connexion \u00e0 la consultation...", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable private fun ErrorOverlay(error: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF1A1A2E)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFEF4444), modifier = Modifier.size(48.dp))
            Text(error, color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Text("Retour") }
                Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0596DE))) { Text("R\u00e9essayer") }
            }
        }
    }
}

@Composable private fun PermissionsOverlay(onRequest: () -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF1A1A2E)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.VideoCameraFront, null, tint = Color.White, modifier = Modifier.size(48.dp))
            Text("Autorisations requises", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("La cam\u00e9ra et le microphone sont n\u00e9cessaires pour la visioconf\u00e9rence.", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Text("Retour") }
                Button(onClick = onRequest, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0596DE))) { Text("Autoriser") }
            }
        }
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val h = totalSeconds / 3600; val m = (totalSeconds % 3600) / 60; val s = totalSeconds % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
