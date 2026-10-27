package com.uruizs.gastos

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper

class ModeloGastos(context: Context) :
    SQLiteOpenHelper(context, "gastos.db", null, 1) {

    // Habilitar la integridad referencial.
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE categorias (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL UNIQUE
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE gastos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                categoria_id INTEGER NOT NULL,
                descripcion TEXT,
                monto INTEGER NOT NULL CHECK (monto > 0),
                FOREIGN KEY (categoria_id)
                    REFERENCES categorias(id)
                    ON DELETE RESTRICT
            )
        """.trimIndent())
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        // Estrategia simple para la práctica:
        // recrear las tablas al cambiar la versión.
        // ATENCIÓN: esto elimina los datos existentes.
        db.execSQL("DROP TABLE IF EXISTS gastos")
        db.execSQL("DROP TABLE IF EXISTS categorias")
        onCreate(db)
    }

    // Convierte la fila actual de un Cursor
    // en un objeto Categoria.
    private fun cursorCategoria(c: Cursor): Categoria {
        return Categoria(
            id = c.getInt(c.getColumnIndexOrThrow("id")),
            nombre = c.getString(
                c.getColumnIndexOrThrow("nombre")
            )
        )
    }

    // Convierte la fila actual de un Cursor
    // en un objeto Gasto.
    private fun cursorGasto(c: Cursor): Gasto {
        val indiceDescripcion =
            c.getColumnIndexOrThrow("descripcion")

        return Gasto(
            id = c.getInt(c.getColumnIndexOrThrow("id")),
            categoriaId = c.getInt(
                c.getColumnIndexOrThrow("categoria_id")
            ),
            descripcion = if (c.isNull(indiceDescripcion))
                null
            else
                c.getString(indiceDescripcion),
            monto = c.getLong(
                c.getColumnIndexOrThrow("monto")
            )
        )
    }

    // =========================================
    //              CATEGORÍAS
    // =========================================

    fun leeCategoria(id: Int): Categoria? {
        val db = readableDatabase

        db.query(
            "categorias",
            null,
            "id = ?",
            arrayOf(id.toString()),
            null, null, null
        ).use { cursor ->
            return if (cursor.moveToFirst())
                cursorCategoria(cursor)
            else
                null
        }
    }

    fun leeCategorias(): List<Categoria> {
        val categorias = mutableListOf<Categoria>()
        val db = readableDatabase

        db.query(
            "categorias",
            null,
            null, null, null, null,
            "nombre ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                categorias.add(cursorCategoria(cursor))
            }
        }

        return categorias
    }

    fun insertaCategoria(categoria: Categoria): Boolean {
        if (categoria.nombre.isBlank()) return false

        val valores = ContentValues().apply {
            put("nombre", categoria.nombre.trim())
        }

        return try {
            writableDatabase.insert(
                "categorias", null, valores
            ) != -1L
        } catch (_: SQLiteException) {
            false
        }
    }

    fun actualizaCategoria(categoria: Categoria): Boolean {
        if (categoria.nombre.isBlank()) return false

        val valores = ContentValues().apply {
            put("nombre", categoria.nombre.trim())
        }

        return try {
            writableDatabase.update(
                "categorias",
                valores,
                "id = ?",
                arrayOf(categoria.id.toString())
            ) > 0
        } catch (_: SQLiteException) {
            false
        }
    }

    fun eliminaCategoria(id: Int): Boolean {
        return try {
            writableDatabase.delete(
                "categorias",
                "id = ?",
                arrayOf(id.toString())
            ) > 0
        } catch (_: SQLiteException) {
            // Puede fallar por la llave foránea.
            false
        }
    }

    // =========================================
    //                 GASTOS
    // =========================================

    fun leeGasto(id: Int): Gasto? {
        val db = readableDatabase

        db.query(
            "gastos",
            null,
            "id = ?",
            arrayOf(id.toString()),
            null, null, null
        ).use { cursor ->
            return if (cursor.moveToFirst())
                cursorGasto(cursor)
            else
                null
        }
    }

    fun leeGastos(): List<Gasto> {
        val gastos = mutableListOf<Gasto>()
        val db = readableDatabase

        db.query(
            "gastos",
            null,
            null, null, null, null,
            "id DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                gastos.add(cursorGasto(cursor))
            }
        }

        return gastos
    }

    fun insertaGasto(gasto: Gasto): Boolean {
        if (gasto.monto <= 0L) return false

        val valores = ContentValues().apply {
            put("categoria_id", gasto.categoriaId)
            put("descripcion", gasto.descripcion)
            put("monto", gasto.monto)
        }

        return try {
            writableDatabase.insert(
                "gastos", null, valores
            ) != -1L
        } catch (_: SQLiteException) {
            false
        }
    }

    fun actualizaGasto(gasto: Gasto): Boolean {
        if (gasto.monto <= 0L) return false

        val valores = ContentValues().apply {
            put("categoria_id", gasto.categoriaId)
            put("descripcion", gasto.descripcion)
            put("monto", gasto.monto)
        }

        return try {
            writableDatabase.update(
                "gastos",
                valores,
                "id = ?",
                arrayOf(gasto.id.toString())
            ) > 0
        } catch (_: SQLiteException) {
            false
        }
    }

    fun eliminaGasto(id: Int): Boolean {
        return try {
            writableDatabase.delete(
                "gastos",
                "id = ?",
                arrayOf(id.toString())
            ) > 0
        } catch (_: SQLiteException) {
            false
        }
    }

    fun resumenCategorias(): List<ResumenCategoria> {
        val resumen = mutableListOf<ResumenCategoria>()

        val sql = """
			SELECT c.nombre,
				   COALESCE(SUM(g.monto), 0) AS total
			FROM categorias c
			LEFT JOIN gastos g
				ON c.id = g.categoria_id
			GROUP BY c.id, c.nombre
			ORDER BY c.nombre
		""".trimIndent()

        readableDatabase.rawQuery(sql, null).use { cursor ->
            while (cursor.moveToNext()) {
                resumen.add(
                    ResumenCategoria(
                        nombre = cursor.getString(0),
                        total = cursor.getLong(1)
                    )
                )
            }
        }

        return resumen
    }
}
