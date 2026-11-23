package cl.duoc.mpazpro.viewmodel.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.mpazpro.data.remote.dto.ResultadoDto
import cl.duoc.mpazpro.data.repository.ResultadosRemoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estados de la lista de resultados traida desde ms-resultados. */
sealed interface ResultadosUiState {
    data object Cargando : ResultadosUiState
    data class Exito(val resultados: List<ResultadoDto>) : ResultadosUiState
    data class Error(val mensaje: String) : ResultadosUiState
}

/**
 * ViewModel del CRUD de resultados contra ms-resultados.
 * Expone el estado de la lista y un mensaje de una sola vez para avisar el resultado
 * de crear, actualizar o eliminar.
 */
class ResultadosRemotosViewModel(
    private val repository: ResultadosRemoteRepository = ResultadosRemoteRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ResultadosUiState>(ResultadosUiState.Cargando)
    val uiState: StateFlow<ResultadosUiState> = _uiState.asStateFlow()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    init {
        cargar()
    }

    /** Pide la lista al servidor (READ). */
    fun cargar() {
        viewModelScope.launch {
            _uiState.value = ResultadosUiState.Cargando
            val nuevoEstado: ResultadosUiState = repository.listar().fold(
                onSuccess = { ResultadosUiState.Exito(it) },
                onFailure = { ResultadosUiState.Error(it.message ?: "Error desconocido") }
            )
            _uiState.value = nuevoEstado
        }
    }

    /** CREATE: guarda un resultado nuevo en el servidor. */
    fun crear(resultado: ResultadoDto) =
        operar("Resultado guardado en el servidor") { repository.crear(resultado) }

    /** UPDATE: modifica un resultado existente. */
    fun actualizar(id: Int, resultado: ResultadoDto) =
        operar("Resultado actualizado") { repository.actualizar(id, resultado) }

    /** DELETE: elimina un resultado del servidor. */
    fun eliminar(id: Int) =
        operar("Resultado eliminado") { repository.eliminar(id) }

    fun mensajeMostrado() {
        _mensaje.value = null
    }

    private fun operar(mensajeExito: String, accion: suspend () -> Result<*>) {
        viewModelScope.launch {
            accion().fold(
                onSuccess = {
                    _mensaje.value = mensajeExito
                    cargar()
                },
                onFailure = { _mensaje.value = it.message ?: "Error desconocido" }
            )
        }
    }
}
