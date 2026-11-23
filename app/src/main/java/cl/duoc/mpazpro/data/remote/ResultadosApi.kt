package cl.duoc.mpazpro.data.remote

import cl.duoc.mpazpro.data.remote.dto.ResultadoDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Endpoints de ms-resultados (http://10.0.2.2:9081/api/resultados). */
interface ResultadosApi {

    @GET("api/resultados")
    suspend fun listar(
        @Query("estudianteId") estudianteId: Int? = null,
        @Query("oaId") oaId: Int? = null
    ): Response<List<ResultadoDto>>

    @GET("api/resultados/{id}")
    suspend fun obtener(@Path("id") id: Int): Response<ResultadoDto>

    @POST("api/resultados")
    suspend fun crear(@Body resultado: ResultadoDto): Response<ResultadoDto>

    @PUT("api/resultados/{id}")
    suspend fun actualizar(
        @Path("id") id: Int,
        @Body resultado: ResultadoDto
    ): Response<ResultadoDto>

    @DELETE("api/resultados/{id}")
    suspend fun eliminar(@Path("id") id: Int): Response<Unit>
}
