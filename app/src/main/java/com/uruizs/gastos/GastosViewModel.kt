package com.uruizs.gastos

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

class GastosViewModel(application: Application) : AndroidViewModel(application) {
    private val modelo = ModeloGastos(application)

    var categorias by mutableStateOf(modelo.leeCategorias())
        private set

    var resumen by mutableStateOf(modelo.resumenCategorias())
        private set

    fun agregarCategoria(nombre: String) {
        if (modelo.insertaCategoria(Categoria(nombre = nombre))) {
            actualizarDatos()
        }
    }

    // NUEVA: Centraliza la actualización de categorías
    fun actualizarCategoria(id: Int, nuevoNombre: String): Boolean {
        val exito = modelo.actualizaCategoria(Categoria(id = id, nombre = nuevoNombre))
        if (exito) {
            actualizarDatos()
        }
        return exito
    }

    fun eliminarCategoria(id: Int): Boolean {
        val exito = modelo.eliminaCategoria(id)
        if (exito) {
            actualizarDatos()
        }
        return exito
    }

    fun agregarGasto(categoriaId: Int, descripcion: String, monto: Long) {
        if (modelo.insertaGasto(Gasto(categoriaId = categoriaId, descripcion = descripcion, monto = monto))) {
            actualizarDatos()
        }
    }

    // NUEVA: Centraliza la actualización de gastos (para cuando editas)
    fun actualizarGasto(id: Int, categoriaId: Int, descripcion: String?, monto: Long) {
        val exito = modelo.actualizaGasto(Gasto(id = id, categoriaId = categoriaId, descripcion = descripcion, monto = monto))
        if (exito) {
            actualizarDatos()
        }
    }

    fun eliminarGasto(id: Int) {
        if (modelo.eliminaGasto(id)) {
            actualizarDatos()
        }
    }

    fun actualizarDatos() {
        categorias = modelo.leeCategorias()
        resumen = modelo.resumenCategorias()
    }
}