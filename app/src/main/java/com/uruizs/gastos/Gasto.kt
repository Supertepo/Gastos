package com.uruizs.gastos

data class Gasto(
    val id: Int = 0,
    val categoriaId: Int,
    val descripcion: String? = null,
    val monto: Long
)