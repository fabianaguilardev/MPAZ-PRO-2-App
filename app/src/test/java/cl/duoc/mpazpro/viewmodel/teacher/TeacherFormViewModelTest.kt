package cl.duoc.mpazpro.viewmodel.teacher

import android.app.Application
import cl.duoc.mpazpro.data.local.database.entities.DocenteEntity
import cl.duoc.mpazpro.data.local.preferences.ConfiguracionEvaluacion
import cl.duoc.mpazpro.data.local.preferences.EvaluacionConfig
import cl.duoc.mpazpro.data.repository.AppRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TeacherFormViewModelTest {

    private lateinit var repository: AppRepository
    private lateinit var viewModel: TeacherFormViewModel

    @Before
    fun setUp() {
        // viewModelScope usa Dispatchers.Main: lo reemplazamos por uno de prueba.
        Dispatchers.setMain(UnconfinedTestDispatcher())

        // SharedPreferences no existe en JVM: simulamos el objeto de configuración.
        mockkObject(ConfiguracionEvaluacion)
        every { ConfiguracionEvaluacion.obtenerParametros(any(), any()) } returns null
        every { ConfiguracionEvaluacion.guardarParametros(any(), any()) } just runs

        repository = mockk(relaxed = true)
        val app = mockk<Application>(relaxed = true)
        viewModel = TeacherFormViewModel(app, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // --- Validaciones individuales ---

    @Test
    fun validarNombre_vacio_devuelveError() {
        assertNotNull(viewModel.validarNombre("  "))
    }

    @Test
    fun validarNombre_muyCorto_devuelveError() {
        assertNotNull(viewModel.validarNombre("Ana"))
    }

    @Test
    fun validarNombre_valido_devuelveNull() {
        assertNull(viewModel.validarNombre("Marta Soto"))
    }

    @Test
    fun validarCodigo_cortoDevuelveError_yValidoNull() {
        assertNotNull(viewModel.validarCodigo("D1"))
        assertNull(viewModel.validarCodigo("DOC-101"))
    }

    @Test
    fun validarIntentos_respetaRangoDe1a5() {
        assertNotNull(viewModel.validarIntentos(""))
        assertNotNull(viewModel.validarIntentos("0"))
        assertNotNull(viewModel.validarIntentos("6"))
        assertNull(viewModel.validarIntentos("1"))
        assertNull(viewModel.validarIntentos("5"))
    }

    @Test
    fun validarFecha_rechazaVaciaFormatoInvalidoYPasada() {
        assertNotNull(viewModel.validarFecha(""))
        assertNotNull(viewModel.validarFecha("31/12/2030"))
        assertNotNull(viewModel.validarFecha("2020-01-01"))
    }

    @Test
    fun validarFecha_futuraEsValida() {
        assertNull(viewModel.validarFecha(LocalDate.now().plusDays(3).toString()))
    }

    @Test
    fun validarConfirmacion_exigeMarcarCasilla() {
        assertNotNull(viewModel.validarConfirmacion(false))
        assertNull(viewModel.validarConfirmacion(true))
    }

    // --- Eventos de entrada ---

    @Test
    fun onMaxIntentosChange_filtraCaracteresNoNumericos() {
        viewModel.onMaxIntentosChange("a3b")
        assertEquals("3", viewModel.uiState.value.maxIntentos)
    }

    // --- Guardado ---

    @Test
    fun validarYGuardar_conFormularioVacio_marcaErroresYNoPersiste() {
        val resultado = viewModel.validarYGuardar()

        assertFalse(resultado)
        val estado = viewModel.uiState.value
        assertNotNull(estado.nombreDocenteError)
        assertNotNull(estado.maxIntentosError)
        assertFalse(estado.isSuccess)
        verify(exactly = 0) { ConfiguracionEvaluacion.guardarParametros(any(), any()) }
        coVerify(exactly = 0) { repository.insertarDocente(any()) }
    }

    @Test
    fun validarYGuardar_conDatosValidos_guardaConfigYDocente() {
        viewModel.onNombreDocenteChange("Marta Soto")
        viewModel.onCodigoDocenteChange("DOC-101")
        viewModel.onMaxIntentosChange("3")
        viewModel.onFechaLimiteChange(LocalDate.now().plusDays(7).toString())
        viewModel.onConfirmarParametrosChange(true)

        val config = slot<EvaluacionConfig>()
        val docente = slot<DocenteEntity>()
        coEvery { repository.insertarDocente(capture(docente)) } returns Unit

        val resultado = viewModel.validarYGuardar()

        assertTrue(resultado)
        assertTrue(viewModel.uiState.value.isSuccess)
        verify { ConfiguracionEvaluacion.guardarParametros(any(), capture(config)) }
        assertEquals(3, config.captured.maxIntentos)
        assertEquals("Marta Soto", docente.captured.nombre)
    }
}
