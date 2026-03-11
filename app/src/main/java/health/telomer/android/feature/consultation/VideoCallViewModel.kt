package health.telomer.android.feature.consultation

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.telomer.android.core.data.api.TelomerApi
import io.livekit.android.LiveKit
import io.livekit.android.ConnectOptions
import io.livekit.android.RoomOptions
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.participant.LocalParticipant
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.participant.RemoteParticipant
import io.livekit.android.room.track.CameraPosition
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VideoCallState(
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val isMicOn: Boolean = true,
    val isCameraOn: Boolean = true,
    val token: String? = null,
    val livekitUrl: String? = null,
    val roomName: String? = null,
    val error: String? = null,
    val callDurationSeconds: Long = 0,
    val remoteParticipantConnected: Boolean = false,
    val remoteVideoTrack: VideoTrack? = null,
    val localVideoTrack: VideoTrack? = null,
)

@HiltViewModel
class VideoCallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val api: TelomerApi,
    private val application: Application,
) : ViewModel() {

    private val appointmentId: String = savedStateHandle["appointmentId"] ?: ""

    private val _state = MutableStateFlow(VideoCallState())
    val state: StateFlow<VideoCallState> = _state.asStateFlow()

    var room: Room? = null
        private set

    private var timerJob: Job? = null
    private var eventJob: Job? = null

    init {
        fetchToken()
    }

    private fun fetchToken() {
        viewModelScope.launch {
            _state.update { it.copy(isConnecting = true, error = null) }
            try {
                val response = api.createConsultationRoom(appointmentId)
                _state.update {
                    it.copy(
                        token = response.patientToken,
                        livekitUrl = response.livekitUrl ?: "wss://livekit.telomer.health",
                        roomName = response.roomName,
                        isConnecting = false,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isConnecting = false,
                        error = "Impossible de rejoindre la consultation : ${e.message}",
                    )
                }
            }
        }
    }

    fun connect() {
        val currentState = _state.value
        val token = currentState.token ?: return
        val url = currentState.livekitUrl ?: return

        viewModelScope.launch {
            _state.update { it.copy(isConnecting = true, error = null) }
            try {
                val lkRoom = LiveKit.create(
                    appContext = application,
                    options = RoomOptions(
                        adaptiveStream = true,
                        dynacast = true,
                    ),
                )
                room = lkRoom

                // Listen to room events
                eventJob = viewModelScope.launch {
                    lkRoom.events.collect { event ->
                        when (event) {
                            is RoomEvent.ParticipantConnected -> {
                                updateRemoteTracks(lkRoom)
                            }
                            is RoomEvent.ParticipantDisconnected -> {
                                updateRemoteTracks(lkRoom)
                            }
                            is RoomEvent.TrackSubscribed -> {
                                updateRemoteTracks(lkRoom)
                            }
                            is RoomEvent.TrackUnsubscribed -> {
                                updateRemoteTracks(lkRoom)
                            }
                            is RoomEvent.Reconnecting -> {
                                _state.update { it.copy(error = "Reconnexion en cours...") }
                            }
                            is RoomEvent.Reconnected -> {
                                _state.update { it.copy(error = null) }
                            }
                            is RoomEvent.Disconnected -> {
                                _state.update {
                                    it.copy(
                                        isConnected = false,
                                        remoteParticipantConnected = false,
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                }

                lkRoom.connect(
                    url = url,
                    token = token,
                    options = ConnectOptions(autoSubscribe = true),
                )

                // Enable local audio/video
                val localParticipant = lkRoom.localParticipant
                localParticipant.setMicrophoneEnabled(true)
                localParticipant.setCameraEnabled(true)

                // Get local video track
                val localVideoTrack = localParticipant.getTrackPublication(Track.Source.CAMERA)
                    ?.track as? VideoTrack

                _state.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = true,
                        isMicOn = true,
                        isCameraOn = true,
                        localVideoTrack = localVideoTrack,
                    )
                }

                // Check for existing remote participants
                updateRemoteTracks(lkRoom)

                // Start call timer
                startTimer()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isConnecting = false,
                        error = "Erreur de connexion : ${e.message}",
                    )
                }
            }
        }
    }

    private fun updateRemoteTracks(lkRoom: Room) {
        val remoteParticipants = lkRoom.remoteParticipants.values
        val hasRemote = remoteParticipants.isNotEmpty()
        val remoteVideoTrack = remoteParticipants.firstOrNull()
            ?.getTrackPublication(Track.Source.CAMERA)
            ?.track as? VideoTrack

        // Also update local video track in case it changed
        val localVideoTrack = lkRoom.localParticipant
            .getTrackPublication(Track.Source.CAMERA)
            ?.track as? VideoTrack

        _state.update {
            it.copy(
                remoteParticipantConnected = hasRemote,
                remoteVideoTrack = remoteVideoTrack,
                localVideoTrack = localVideoTrack,
            )
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _state.update { it.copy(callDurationSeconds = it.callDurationSeconds + 1) }
            }
        }
    }

    fun toggleMic() {
        viewModelScope.launch {
            val newState = !_state.value.isMicOn
            room?.localParticipant?.setMicrophoneEnabled(newState)
            _state.update { it.copy(isMicOn = newState) }
        }
    }

    fun toggleCamera() {
        viewModelScope.launch {
            val newState = !_state.value.isCameraOn
            room?.localParticipant?.setCameraEnabled(newState)

            val localVideoTrack = if (newState) {
                room?.localParticipant
                    ?.getTrackPublication(Track.Source.CAMERA)
                    ?.track as? VideoTrack
            } else null

            _state.update { it.copy(isCameraOn = newState, localVideoTrack = localVideoTrack) }
        }
    }

    fun hangUp() {
        disconnect()
    }

    private fun disconnect() {
        timerJob?.cancel()
        eventJob?.cancel()
        viewModelScope.launch {
            try {
                room?.disconnect()
            } catch (_: Exception) {}
            room?.release()
            room = null
            _state.update {
                it.copy(
                    isConnected = false,
                    remoteParticipantConnected = false,
                    remoteVideoTrack = null,
                    localVideoTrack = null,
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        eventJob?.cancel()
        try {
            room?.disconnect()
            room?.release()
        } catch (_: Exception) {}
        room = null
    }
}
