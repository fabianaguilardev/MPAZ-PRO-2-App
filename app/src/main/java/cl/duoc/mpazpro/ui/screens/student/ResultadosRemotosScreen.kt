package cl.duoc.mpazpro.ui.screens.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.mpazpro.data.remote.dto.ResultadoDto
import cl.duoc.mpazpro.ui.theme.AzulInstitucional
import cl.duoc.mpazpro.viewmodel.student.ResultadosRemotosViewModel
import cl.duoc.mpazpro.viewmodel.student.ResultadosUiState

/**
 * Pantalla del CRUD de resultados guardados en el microservicio ms-resultados.
 * Muestra la lista (READ) y permite crear, editar y eliminar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultadosRemotosScreen(
    onBack: () -> Unit,
    viewModel: ResultadosRemotosViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var mostrarFormulario by remember { mutableStateOf(false) }
    var enEdicion by remember { mutableStateOf<ResultadoDto?>(null) }
    var porEliminar by remember { mutableStateOf<ResultadoDto?>(null) }

    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.mensajeMostrado()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial en el servidor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.cargar() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar lista")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AzulInstitucional,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                enEdicion = null
                mostrarFormulario = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar resultado")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val estado = uiState) {
                is ResultadosUiState.Cargando -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                is ResultadosUiState.Error -> Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(estado.mensaje, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.cargar() }) { Text("Reintentar") }
                }

                is ResultadosUiState.Exito -> if (estado.resultados.isEmpty()) {
                    Text(
                        "Aun no hay resultados en el servidor. Usa + para agregar uno.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(estado.resultados) { resultado ->
                            ResultadoCard(
                                resultado = resultado,
                                onEditar = {
                                    enEdicion = resultado
                                    mostrarFormulario = true
                                },
                                onEliminar = { porEliminar = resultado }
                            )
                        }
                    }
                }
            }
        }
    }

    if (mostrarFormulario) {
        FormularioResultadoDialog(
            inicial = enEdicion,
            onDismiss = { mostrarFormulario = false },
            onGuardar = { dto ->
                val id = enEdicion?.id
                if (id == null) viewModel.crear(dto) else viewModel.actualizar(id, dto)
                mostrarFormulario = false
            }
        )
    }

    porEliminar?.let { resultado ->
        AlertDialog(
            onDismissRequest = { porEliminar = null },
            title = { Text("Eliminar resultado") },
            text = { Text("Se eliminara el resultado del objetivo OA-${resultado.oaId} del servidor.") },
            confirmButton = {
                TextButton(onClick = {
                    resultado.id?.let { viewModel.eliminar(it) }
                    porEliminar = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { porEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ResultadoCard(
    resultado: ResultadoDto,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Objetivo OA-${resultado.oaId}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("Puntaje: ${resultado.puntaje}  |  Intentos: ${resultado.intentosUsados}")
                Text(
                    "Estudiante: ${resultado.estudianteFicticioId}  |  Id servidor: ${resultado.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEditar) {
                Icon(Icons.Default.Edit, contentDescription = "Editar resultado")
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar resultado")
            }
        }
    }
}

@Composable
private fun FormularioResultadoDialog(
    inicial: ResultadoDto?,
    onDismiss: () -> Unit,
    onGuardar: (ResultadoDto) -> Unit
) {
    var estudiante by remember { mutableStateOf((inicial?.estudianteFicticioId ?: 1).toString()) }
    var oa by remember { mutableStateOf((inicial?.oaId ?: 1).toString()) }
    var puntaje by remember { mutableStateOf((inicial?.puntaje ?: 0).toString()) }
    var intentos by remember { mutableStateOf((inicial?.intentosUsados ?: 1).toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (inicial == null) "Nuevo resultado" else "Editar resultado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoNumerico("Id del estudiante", estudiante) { estudiante = it }
                CampoNumerico("Objetivo (oaId)", oa) { oa = it }
                CampoNumerico("Puntaje", puntaje) { puntaje = it }
                CampoNumerico("Intentos usados (1 a 5)", intentos) { intentos = it }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val e = estudiante.toIntOrNull()
                val o = oa.toIntOrNull()
                val p = puntaje.toIntOrNull()
                val i = intentos.toIntOrNull()
                val problema = when {
                    e == null || e <= 0 -> "El estudiante debe ser un numero mayor que 0"
                    o == null || o <= 0 -> "El objetivo debe ser un numero mayor que 0"
                    p == null || p < 0 -> "El puntaje debe ser 0 o mas"
                    i == null || i !in 1..5 -> "Los intentos deben estar entre 1 y 5"
                    else -> null
                }
                error = problema
                if (problema == null) {
                    onGuardar(
                        ResultadoDto(
                            id = inicial?.id,
                            estudianteFicticioId = e!!,
                            oaId = o!!,
                            puntaje = p!!,
                            intentosUsados = i!!,
                            fechaRegistro = inicial?.fechaRegistro ?: System.currentTimeMillis()
                        )
                    )
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun CampoNumerico(etiqueta: String, valor: String, onCambio: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = { nuevo -> onCambio(nuevo.filter { it.isDigit() }) },
        label = { Text(etiqueta) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}
