package cl.duoc.mpazpro.data.repository

import cl.duoc.mpazpro.data.remote.ResultadosApi
import cl.duoc.mpazpro.data.remote.RetrofitClient
import cl.duoc.mpazpro.data.remote.dto.ResultadoDto
import kotlinx.coroutines.CancellationException
import retrofit2.Response

/**
 * Repositorio de resultados que viven en ms-resultados.
 *
 * Convierte cada respuesta HTTP en un Result: exito con el dato, o fallo con un
 * mensaje legible (400, 404, error de servidor o servidor apagado).
 */
class ResultadosRemoteRepository(
    private val api: ResultadosApi = RetrofitClient.resultados.create(ResultadosApi::class.java)
) {

    suspend fun listar(estudianteId: Int? = null, oaId: Int? = null): Result<List<ResultadoDto>> =
        ejecutar { api.listar(estudianteId, oaId) }

    suspend fun obtener(id: Int): Result<ResultadoDto> = ejecutar { api.obtener(id) }

    suspend fun crear(resultado: ResultadoDto): Result<ResultadoDto> =
        ejecutar { api.crear(resultado) }

    suspend fun actualizar(id: Int, resultado: ResultadoDto): Result<ResultadoDto> =
        ejecutar { api.actualizar(id, resultado) }

    suspend fun eliminar(id: Int): Result<Unit> = ejecutar { api.eliminar(id) }

    private suspend fun <T> ejecutar(llamada: suspend () -> Response<T>): Result<T> {
        return try {
            val respuesta = llamada()
            val cuerpo = respuesta.body()
            when {
                respuesta.isSuccessful && cuerpo != null -> Result.success(cuerpo)
                respuesta.isSuccessful -> Result.failure(Exception("El servidor respondio sin datos"))
                else -> Result.failure(Exception(mensajeHttp(respuesta.code())))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo conectar con ms-resultados. Revisa que este encendido."))
        }
    }

    private fun mensajeHttp(codigo: Int): String = when (codigo) {
        400 -> "El servidor rechazo los datos (400). Revisa los valores ingresados."
        404 -> "El resultado no existe en el servidor (404)."
        else -> "Error del servidor ($codigo)."
    }
}
