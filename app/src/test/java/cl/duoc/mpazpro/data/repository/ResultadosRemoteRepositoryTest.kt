package cl.duoc.mpazpro.data.repository

import cl.duoc.mpazpro.data.remote.ResultadosApi
import cl.duoc.mpazpro.data.remote.dto.ResultadoDto
import cl.duoc.mpazpro.respuestaExitosa
import cl.duoc.mpazpro.respuestaFallida
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException

/**
 * Pruebas del repositorio remoto. Se mockea ResultadosApi (la red) para probar
 * como el repositorio traduce cada respuesta HTTP a un Result.
 */
class ResultadosRemoteRepositoryTest : FunSpec({

    val api = mockk<ResultadosApi>()
    val repository = ResultadosRemoteRepository(api)
    val resultado = ResultadoDto(
        id = 1,
        estudianteFicticioId = 1,
        oaId = 2,
        puntaje = 80,
        intentosUsados = 1,
        fechaRegistro = 1700000000000L
    )

    test("listar devuelve la lista cuando el servidor responde 200") {
        coEvery { api.listar(any(), any()) } returns respuestaExitosa(listOf(resultado))

        val resultadoLlamada = repository.listar()

        resultadoLlamada.isSuccess shouldBe true
        resultadoLlamada.getOrNull() shouldBe listOf(resultado)
    }

    test("listar entrega un fallo con el codigo cuando el servidor responde 500") {
        coEvery { api.listar(any(), any()) } returns respuestaFallida<List<ResultadoDto>>(500)

        val resultadoLlamada = repository.listar()

        resultadoLlamada.isFailure shouldBe true
        resultadoLlamada.exceptionOrNull()?.message shouldContain "500"
    }

    test("crear informa que el servidor rechazo los datos cuando responde 400") {
        coEvery { api.crear(resultado) } returns respuestaFallida<ResultadoDto>(400)

        val resultadoLlamada = repository.crear(resultado)

        resultadoLlamada.isFailure shouldBe true
        resultadoLlamada.exceptionOrNull()?.message shouldContain "400"
    }

    test("obtener informa que el resultado no existe cuando responde 404") {
        coEvery { api.obtener(99) } returns respuestaFallida<ResultadoDto>(404)

        val resultadoLlamada = repository.obtener(99)

        resultadoLlamada.isFailure shouldBe true
        resultadoLlamada.exceptionOrNull()?.message shouldContain "404"
    }

    test("actualizar envia el id y el cuerpo a la API y devuelve el resultado") {
        coEvery { api.actualizar(1, resultado) } returns respuestaExitosa(resultado)

        val resultadoLlamada = repository.actualizar(1, resultado)

        resultadoLlamada.getOrNull() shouldBe resultado
        coVerify(exactly = 1) { api.actualizar(1, resultado) }
    }

    test("eliminar devuelve exito cuando el servidor responde bien") {
        coEvery { api.eliminar(5) } returns respuestaExitosa(Unit)

        repository.eliminar(5).isSuccess shouldBe true
    }

    test("una respuesta exitosa sin cuerpo se trata como fallo") {
        coEvery { api.obtener(1) } returns respuestaExitosa<ResultadoDto>(null)

        val resultadoLlamada = repository.obtener(1)

        resultadoLlamada.isFailure shouldBe true
        resultadoLlamada.exceptionOrNull()?.message shouldContain "sin datos"
    }

    test("una excepcion de red se convierte en un mensaje de conexion") {
        coEvery { api.listar(any(), any()) } throws IOException("sin red")

        val resultadoLlamada = repository.listar()

        resultadoLlamada.isFailure shouldBe true
        resultadoLlamada.exceptionOrNull()?.message shouldContain "No se pudo conectar"
    }
})
