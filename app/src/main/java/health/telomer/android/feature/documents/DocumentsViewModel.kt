package health.telomer.android.feature.documents

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.telomer.android.core.data.api.TelomerApi
import health.telomer.android.core.data.api.models.DocumentResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

data class DocumentsUiState(
    val isLoading: Boolean = true,
    val documents: List<DocumentResponse> = emptyList(),
    val error: String? = null,
    val isUploading: Boolean = false,
)

@HiltViewModel
class DocumentsViewModel @Inject constructor(
    private val api: TelomerApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentsUiState())
    val uiState: StateFlow<DocumentsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val docs = api.getMyDocuments()
                _uiState.value = DocumentsUiState(
                    isLoading = false,
                    documents = docs.sortedByDescending { it.uploadedAt ?: it.documentDate },
                )
            } catch (e: Exception) {
                _uiState.value = DocumentsUiState(isLoading = false, error = e.message)
            }
        }
    }

    fun uploadDocument(context: Context, uri: Uri) {
        _uiState.value = _uiState.value.copy(isUploading = true, error = null)
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("Fichier introuvable")
                val tempFile = File.createTempFile("upload", ".tmp", context.cacheDir)
                tempFile.outputStream().use { inputStream.copyTo(it) }

                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", tempFile.name, requestBody)

                api.uploadDocument(part)
                tempFile.delete()
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isUploading = false, error = "Erreur d'upload: ${e.message}")
            }
        }
    }
}
