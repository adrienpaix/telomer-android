package health.telomer.android.core.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Lance une coroutine dans viewModelScope avec gestion automatique
 * des \u00e9tats isLoading/error \u2014 r\u00e9duit le boilerplate dans les ViewModels.
 *
 * Usage:
 * ```
 * launchWithLoading(
 *     currentState = { _uiState.value },
 *     updateState = { _uiState.value = it },
 *     setLoading = { copy(isLoading = it) },
 *     setError = { copy(error = it) },
 * ) {
 *     val data = api.getData()
 *     _uiState.update { it.copy(data = data) }
 * }
 * ```
 */
fun <S> ViewModel.launchWithLoading(
    currentState: () -> S,
    updateState: (S) -> Unit,
    setLoading: S.(Boolean) -> S,
    setError: S.(String?) -> S,
    block: suspend CoroutineScope.() -> Unit,
) {
    viewModelScope.launch {
        updateState(currentState().setLoading(true).setError(null))
        try {
            block()
            updateState(currentState().setLoading(false))
        } catch (e: Exception) {
            updateState(currentState().setLoading(false).setError(e.message))
        }
    }
}
