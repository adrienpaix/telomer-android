package health.telomer.android.feature.documents.data

import health.telomer.android.core.data.api.TelomerApi
import health.telomer.android.core.data.api.models.DocumentResponse
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentsRepository @Inject constructor(private val api: TelomerApi) {
    suspend fun getDocuments(): List<DocumentResponse> = api.getMyDocuments()
    suspend fun uploadDocument(file: MultipartBody.Part): DocumentResponse =
        api.uploadDocument(file)
}
