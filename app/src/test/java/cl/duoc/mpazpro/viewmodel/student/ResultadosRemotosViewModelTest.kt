package cl.duoc.mpazpro.viewmodel.student

import cl.duoc.mpazpro.data.remote.ResultadosApi
import cl.duoc.mpazpro.data.remote.dto.ResultadoDto
import cl.duoc.mpazpro.data.repository.ResultadosRemoteRepository
import cl.duoc.mpazpro.respuestaExitosa
import cl.duoc.mpazpro.respuestaFallida
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.io.IOException

/**
 * Pruebas de los estados del ViewModel (Cargando, Exito, Error) y de los mensajes
 * de crear, actualizar y eliminar. Solo se mockea la API de red.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ResultadosRemotosViewModelTest : FunSpec({

    val api = mockk<ResultadosApi>()
    val resultado = ResultadoDto(
        id = 1,
        estudianteFicticioId = 1,
        oaId = 2,
        puntaje = 80,
        intentosUsados = 1,
        fechaRegistro = 1700000000000L
    )

    fun crearViewModel() = ResultadosRemotosViewModel(ResultadosRemoteRepository(api))

    beforeTest {
        clearAllMocks()
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    afterTest {
        Dispatchers.resetMain()
    }

    test("al iniciar carga la lista y queda en estado Exito") {
        coEvery { api.listar(any(), any()) } returns respuestaExitosa(listOf(resultado))

        val viewModel = crearViewModel()

        viewModel.uiState.value shouldBe ResultadosUiState.Exito(listOf(resultado))
    }

    test("si el servidor esta apagado queda en estado Error con el mensaje") {
        coEvery { api.listar(any(), any()) } throws IOException("sin red")

        val viewModel = crearViewModel()

        val error = viewModel.uiState.value.shouldBeInstanceOf<ResultadosUiState.Error>()
        error.mensaje shouldContain "No se pudo conectar"
    }

    test("cargar de nuevo despues de un error recupera la lista") {
        coEvery { api.listar(any(), any()) } throws IOException("sin red")
        val viewModel = crearViewModel()

        coEvery { api.listar(any(), any()) } returns respuestaExitosa(listOf(resultado))
        viewModel.cargar()

        viewModel.uiState.value shouldBe ResultadosUiState.Exito(listOf(resultado))
    }

    test("crear avisa que se guardo y vuelve a pedir la lista") {
        coEvery { api.listar(any(), any()) } returns respuestaExitosa(emptyList<ResultadoDto>())
        coEvery { api.crear(resultado) } returns respuestaExitosa(resultado)
        val viewModel = crearViewModel()

        viewModel.crear(resultado)

        viewModel.mensaje.value shouldBe "Resultado guardado en el servidor"
        coVerify(exactly = 2) { api.listar(any(), any()) }
    }

    test("crear con datos invalidos muestra el error 400 y no recarga la lista") {
        coEvery { api.listar(any(), any()) } returns respuestaExitosa(emptyList<ResultadoDto>())
        coEvery { api.crear(resultado) } returns respuestaFallida<ResultadoDto>(400)
        val viewModel = crearViewModel()

        viewModel.crear(resultado)

        viewModel.mensaje.value?.shouldContain("400")
        coVerify(exactly = 1) { api.listar(any(), any()) }
    }

    test("actualizar avisa que se actualizo el resultado") {
        coEvery { api.listar(any(), any()) } returns respuestaExitosa(listOf(resultado))
        coEvery { api.actualizar(1, resultado) } returns respuestaExitosa(resultado)
        val viewModel = crearViewModel()

        viewModel.actualizar(1, resultado)

        viewModel.mensaje.value shouldBe "Resultado actualizado"
        coVerify(exactly = 1) { api.actualizar(1, resultado) }
    }

    test("eliminar un resultado inexistente muestra el error 404") {
        coEvery { api.listar(any(), any()) } returns respuestaExitosa(listOf(resultado))
        coEvery { api.eliminar(9) } returns respuestaFallida<Unit>(404)
        val viewModel = crearViewModel()

        viewModel.eliminar(9)

        viewModel.mensaje.value?.shouldContain("404")
    }

    test("eliminar avisa que se elimino y recarga la lista") {
        coEvery { api.listar(any(), any()) } returns respuestaExitosa(listOf(resultado))
        coEvery { api.eliminar(1) } returns respuestaExitosa(Unit)
        val viewModel = crearViewModel()

        viewModel.eliminar(1)

        viewModel.mensaje.value shouldBe "Resultado eliminado"
        coVerify(exactly = 2) { api.listar(any(), any()) }
    }

    test("mensajeMostrado limpia el mensaje") {
        coEvery { api.listar(any(), any()) } returns respuestaExitosa(emptyList<ResultadoDto>())
        coEvery { api.crear(resultado) } returns respuestaExitosa(resultado)
        val viewModel = crearViewModel()
        viewModel.crear(resultado)

        viewModel.mensajeMostrado()

        viewModel.mensaje.value shouldBe null
    }
})
