package cl.duoc.mpazpro.data.remote.dto

/**
 * Resultado de una prueba tal como lo envia y devuelve ms-resultados.
 * Los nombres de los campos coinciden con el JSON del microservicio.
 * El id es null al crear: lo genera el servidor.
 */
data class ResultadoDto(
    val id: Int? = null,
    val estudianteFicticioId: Int,
    val oaId: Int,
    val puntaje: Int,
    val intentosUsados: Int,
    val fechaRegistro: Long
)
