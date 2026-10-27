package com.uruizs.gastos

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uruizs.gastos.ui.theme.GastosTheme
import java.util.Locale
import kotlin.math.round

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GastosTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: GastosViewModel = viewModel()
                    PantallaControlGastosSimple(viewModel = viewModel)
                }
            }
        }
    }
}

val PrimaryColor = Color(0xFF0D9488)
val BackgroundColor = Color(0xFFF8FAFC)
val CardColor = Color(0xFFFFFFFF)
val ErrorColor = Color(0xFFEF4444)

fun formatearMonto(centavos: Long): String {
    return String.format(Locale.getDefault(), "$%.2f", centavos / 100.0)
}

@SuppressLint("HardwareIds")
fun getAndroidId(context: Context): String {
    return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "Desconocido"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaControlGastosSimple(viewModel: GastosViewModel) {
    val context = LocalContext.current
    val modelo = remember { ModeloGastos(context) }

    // Estados para el Diálogo de Gasto (Nuevo o Edición)
    var mostrarDialogoGasto by remember { mutableStateOf(false) }
    var idGastoEnEdicion by remember { mutableStateOf<Int?>(null) }
    var descripcionInput by remember { mutableStateOf("") }
    var montoInput by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }
    var categoriaSeleccionadaMenu by remember { mutableStateOf("Selecciona una categoría...") }
    var categoriaIdSeleccionada by remember { mutableStateOf<Int?>(null) }

    // Estados para las Alertas generales (AlertDialog)
    var mostrarDialogoAlerta by remember { mutableStateOf(false) }
    var tituloAlerta by remember { mutableStateOf("") }
    var mensajeAlerta by remember { mutableStateOf("") }

    // Diálogo para Agregar Categoría separada
    var mostrarDialogoCrearCat by remember { mutableStateOf(false) }
    var nuevaCatInput by remember { mutableStateOf("") }

    // Diálogo para Editar Categoría separada
    var mostrarDialogoEditarCat by remember { mutableStateOf(false) }
    var catEnEdicion by remember { mutableStateOf<Categoria?>(null) }
    var nombreCatEditInput by remember { mutableStateOf("") }

    // Estado local para la lista de gastos
    var listaGastos by remember { mutableStateOf(modelo.leeGastos()) }

    fun actualizarDatos() {
        viewModel.actualizarDatos()
        listaGastos = modelo.leeGastos()
    }

    fun mostrarMensaje(titulo: String, mensaje: String) {
        tituloAlerta = titulo
        mensajeAlerta = mensaje
        mostrarDialogoAlerta = true
    }

    // ================= VENTANA FLOTANTE DE ALERTAS =================
    if (mostrarDialogoAlerta) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoAlerta = false },
            title = { Text(text = tituloAlerta) },
            text = { Text(text = mensajeAlerta) },
            confirmButton = {
                Button(
                    onClick = { mostrarDialogoAlerta = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, contentColor = Color.White)
                ) {
                    Text("Aceptar")
                }
            }
        )
    }

    // ================= VENTANA FLOTANTE: CREAR CATEGORÍA =================
    if (mostrarDialogoCrearCat) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoCrearCat = false },
            title = { Text("Nueva Categoría") },
            text = {
                OutlinedTextField(
                    value = nuevaCatInput,
                    onValueChange = { nuevaCatInput = it },
                    label = { Text("Nombre de la categoría") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nombre = nuevaCatInput.trim()
                        if (nombre.isNotBlank()) {
                            viewModel.agregarCategoria(nombre)
                            actualizarDatos()
                            nuevaCatInput = ""
                            mostrarDialogoCrearCat = false
                            mostrarMensaje("Éxito", "Categoría creada correctamente.")
                        } else {
                            mostrarMensaje("Error", "El nombre no puede estar vacío.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, contentColor = Color.White)
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { mostrarDialogoCrearCat = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // ================= VENTANA FLOTANTE: EDITAR CATEGORÍA =================
    if (mostrarDialogoEditarCat && catEnEdicion != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEditarCat = false },
            title = { Text("Editar Categoría") },
            text = {
                OutlinedTextField(
                    value = nombreCatEditInput,
                    onValueChange = { nombreCatEditInput = it },
                    label = { Text("Nuevo nombre") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nuevoNombre = nombreCatEditInput.trim()
                        if (nuevoNombre.isNotBlank()) {
                            val actualizada = viewModel.actualizarCategoria(catEnEdicion!!.id, nuevoNombre)
                            if (actualizada) {
                                actualizarDatos()
                                if (categoriaIdSeleccionada == catEnEdicion!!.id) {
                                    categoriaSeleccionadaMenu = nuevoNombre
                                }
                                mostrarDialogoEditarCat = false
                                mostrarMensaje("Éxito", "Categoría actualizada correctamente.")
                            } else {
                                mostrarMensaje("Error", "No se pudo actualizar la categoría.")
                            }
                        } else {
                            mostrarMensaje("Error", "El nombre no puede estar vacío.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, contentColor = Color.White)
                ) {
                    Text("Actualizar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { mostrarDialogoEditarCat = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // ================= VENTANA FLOTANTE: NUEVO / EDITAR GASTO =================
    if (mostrarDialogoGasto) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoGasto = false },
            title = { Text(if (idGastoEnEdicion == null) "Registrar Nuevo Gasto" else "Editar Gasto") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown }
                    ) {
                        OutlinedTextField(
                            value = categoriaSeleccionadaMenu,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("CATEGORÍA") },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            if (viewModel.categorias.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No hay categorías creadas") },
                                    onClick = { expandedDropdown = false }
                                )
                            } else {
                                viewModel.categorias.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.nombre) },
                                        onClick = {
                                            categoriaSeleccionadaMenu = cat.nombre
                                            categoriaIdSeleccionada = cat.id
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = descripcionInput,
                        onValueChange = { descripcionInput = it },
                        label = { Text("Descripción del gasto") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = montoInput,
                        onValueChange = { montoInput = it },
                        label = { Text("MONTO (ej. 85.50)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val catId = categoriaIdSeleccionada
                        val montoDouble = montoInput.toDoubleOrNull()

                        when {
                            catId == null -> {
                                mostrarMensaje("Validación", "Debe seleccionar una categoría obligatoriamente.")
                            }
                            montoDouble == null || montoDouble <= 0.0 -> {
                                mostrarMensaje("Validación", "Ingrese un monto válido mayor a 0.")
                            }
                            else -> {
                                val centavos = round(montoDouble * 100).toLong()

                                if (idGastoEnEdicion == null) {
                                    viewModel.agregarGasto(
                                        categoriaId = catId,
                                        descripcion = descripcionInput,
                                        monto = centavos
                                    )
                                    mostrarMensaje("Éxito", "Gasto registrado correctamente.")
                                } else {
                                    viewModel.actualizarGasto(
                                        id = idGastoEnEdicion!!,
                                        categoriaId = catId,
                                        descripcion = descripcionInput.takeIf { it.isNotBlank() },
                                        monto = centavos
                                    )
                                    idGastoEnEdicion = null
                                    mostrarMensaje("Éxito", "Gasto actualizado correctamente.")
                                }

                                descripcionInput = ""
                                montoInput = ""
                                categoriaSeleccionadaMenu = "Selecciona una categoría..."
                                categoriaIdSeleccionada = null
                                mostrarDialogoGasto = false
                                actualizarDatos()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, contentColor = Color.White)
                ) {
                    Text(if (idGastoEnEdicion == null) "Guardar" else "Actualizar")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        idGastoEnEdicion = null
                        descripcionInput = ""
                        montoInput = ""
                        categoriaSeleccionadaMenu = "Selecciona una categoría..."
                        categoriaIdSeleccionada = null
                        mostrarDialogoGasto = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Cálculos para el resumen general y desglosado
    val gastoTotalCentavos = viewModel.resumen.sumOf { it.total }
    val totalCategorias = viewModel.categorias.size
    val totalGastos = listaGastos.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CONTROL DE GASTOS", fontWeight = FontWeight.Bold, color = Color.Black) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundColor)
            )
        },
        containerColor = BackgroundColor
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // PANEL DE RESUMEN (TOTAL ARRIBA, DESGLOSE ABAJO)
            // ==========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryColor),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "TOTAL GENERAL",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatearMonto(gastoTotalCentavos),
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.3f))

                        Text(
                            text = "DESGLOSE POR CATEGORÍA",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (viewModel.resumen.isEmpty()) {
                            Text(
                                text = "No hay categorías registradas.",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        } else {
                            viewModel.resumen.forEach { itemResumen ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = itemResumen.nombre,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = formatearMonto(itemResumen.total),
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.3f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Categorías: $totalCategorias", color = Color.White, fontSize = 13.sp)
                            Text(text = "Registros: $totalGastos", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }

            // ==========================================
            // BOTONES SUPERIORES DE ACCIÓN
            // ==========================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            nuevaCatInput = ""
                            mostrarDialogoCrearCat = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, contentColor = Color.White)
                    ) {
                        Text("+ Categoría", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            idGastoEnEdicion = null
                            descripcionInput = ""
                            montoInput = ""
                            categoriaSeleccionadaMenu = "Selecciona una categoría..."
                            categoriaIdSeleccionada = null
                            mostrarDialogoGasto = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, contentColor = Color.White)
                    ) {
                        Text("+ Registrar Gasto", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ==========================================
            // LISTADO DE CATEGORÍAS
            // ==========================================
            item {
                Text(
                    text = "Categorías Registradas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1E293B)
                )
            }

            if (viewModel.categorias.isEmpty()) {
                item {
                    Text("No hay categorías creadas.", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                items(viewModel.categorias) { cat ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = CardColor),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cat.nombre,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = Color.Black
                            )

                            Row {
                                TextButton(onClick = {
                                    catEnEdicion = cat
                                    nombreCatEditInput = cat.nombre
                                    mostrarDialogoEditarCat = true
                                }) {
                                    Text("Editar", fontWeight = FontWeight.Bold, color = PrimaryColor)
                                }

                                TextButton(onClick = {
                                    val exito = viewModel.eliminarCategoria(cat.id)
                                    if (exito) {
                                        actualizarDatos()
                                        mostrarMensaje("Éxito", "Categoría eliminada correctamente.")
                                    } else {
                                        mostrarMensaje(
                                            "Operación Rechazada",
                                            "No se puede eliminar la categoría '${cat.nombre}' porque tiene gastos asociados."
                                        )
                                    }
                                }) {
                                    Text("Borrar", fontWeight = FontWeight.Bold, color = ErrorColor)
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // HISTORIAL GENERAL DE GASTOS
            // ==========================================
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Historial de Registros",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1E293B)
                )
            }

            if (listaGastos.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No hay gastos registrados todavía.", color = Color.Gray)
                    }
                }
            } else {
                items(listaGastos) { gasto ->
                    val categoriaAsociada = viewModel.categorias.find { it.id == gasto.categoriaId }?.nombre ?: "Sin categoría"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardColor),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = categoriaAsociada.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = PrimaryColor
                                )
                                if (!gasto.descripcion.isNullOrBlank()) {
                                    Text(
                                        text = "Tipo: ${gasto.descripcion}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Black
                                    )
                                } else {
                                    Text(
                                        text = "Sin descripción",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                }
                                Text(
                                    text = formatearMonto(gasto.monto),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black
                                )
                            }

                            Row {
                                TextButton(onClick = {
                                    idGastoEnEdicion = gasto.id
                                    categoriaIdSeleccionada = gasto.categoriaId
                                    categoriaSeleccionadaMenu = categoriaAsociada
                                    descripcionInput = gasto.descripcion ?: ""
                                    montoInput = (gasto.monto / 100.0).toString()
                                    mostrarDialogoGasto = true
                                }) {
                                    Text("Editar", fontWeight = FontWeight.Bold, color = PrimaryColor)
                                }

                                TextButton(onClick = {
                                    viewModel.eliminarGasto(gasto.id)
                                    actualizarDatos()
                                    mostrarMensaje("Éxito", "Gasto eliminado del historial.")
                                }) {
                                    Text("Borrar", fontWeight = FontWeight.Bold, color = ErrorColor)
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // PIE DE PÁGINA CON ANDROID ID
            // ==========================================
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val contexto = LocalContext.current
                    Text(
                        text = "ID: ${getAndroidId(contexto)}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}