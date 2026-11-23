package cl.duoc.mpazpro

import io.mockk.every
import io.mockk.mockk
import retrofit2.Response

/** Respuesta HTTP exitosa (200) con el cuerpo indicado, para simular a ms-resultados. */
inline fun <reified T : Any> respuestaExitosa(cuerpo: T?): Response<T> = mockk {
    every { isSuccessful } returns true
    every { code() } returns 200
    every { body() } returns cuerpo
}

/** Respuesta HTTP con error (400, 404, 500...) y sin cuerpo. */
inline fun <reified T : Any> respuestaFallida(codigo: Int): Response<T> = mockk {
    every { isSuccessful } returns false
    every { code() } returns codigo
    every { body() } returns null
}
