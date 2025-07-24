package com.example.datasimulator

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "DataSimulator.db"
        private const val DATABASE_VERSION = 2
        private const val TABLE_NAME = "data_items"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_VALUE = "value"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val TABLE_MEDICIONES = "mediciones"
        private const val COLUMN_RITMO = "ritmo_cardiaco"
        private const val COLUMN_OXIGENO = "oxigeno"
        private const val COLUMN_PASOS = "pasos"
        private const val COLUMN_ESTRES = "estres"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TYPE TEXT NOT NULL,
                $COLUMN_VALUE REAL NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL
            )
        """
        db.execSQL(createTable)
        val createMediciones = """
            CREATE TABLE $TABLE_MEDICIONES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_RITMO INTEGER NOT NULL,
                $COLUMN_OXIGENO INTEGER NOT NULL,
                $COLUMN_PASOS INTEGER NOT NULL,
                $COLUMN_ESTRES INTEGER NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL
            )
        """
        db.execSQL(createMediciones)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MEDICIONES")
        onCreate(db)
    }

    fun insertData(type: String, value: Double): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TYPE, type)
            put(COLUMN_VALUE, value)
            put(COLUMN_TIMESTAMP, System.currentTimeMillis())
        }
        
        return db.insert(TABLE_NAME, null, values)
    }

    fun getAllData(): List<DataItem> {
        val dataList = mutableListOf<DataItem>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_TIMESTAMP DESC"
        )

        with(cursor) {
            while (moveToNext()) {
                val id = getLong(getColumnIndexOrThrow(COLUMN_ID))
                val type = getString(getColumnIndexOrThrow(COLUMN_TYPE))
                val value = getDouble(getColumnIndexOrThrow(COLUMN_VALUE))
                val timestamp = getLong(getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                
                dataList.add(DataItem(id, type, value, timestamp))
            }
        }
        cursor.close()
        return dataList
    }

    fun insertMedicion(ritmo: Int, oxigeno: Int, pasos: Int, estres: Int): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_RITMO, ritmo)
            put(COLUMN_OXIGENO, oxigeno)
            put(COLUMN_PASOS, pasos)
            put(COLUMN_ESTRES, estres)
            put(COLUMN_TIMESTAMP, System.currentTimeMillis())
        }
        return db.insert(TABLE_MEDICIONES, null, values)
    }

    data class Medicion(
        val id: Long,
        val ritmo: Int,
        val oxigeno: Int,
        val pasos: Int,
        val estres: Int,
        val timestamp: Long
    )

    fun getAllMediciones(): List<Medicion> {
        val lista = mutableListOf<Medicion>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_MEDICIONES,
            null, null, null, null, null,
            "$COLUMN_TIMESTAMP DESC"
        )
        with(cursor) {
            while (moveToNext()) {
                val id = getLong(getColumnIndexOrThrow(COLUMN_ID))
                val ritmo = getInt(getColumnIndexOrThrow(COLUMN_RITMO))
                val oxigeno = getInt(getColumnIndexOrThrow(COLUMN_OXIGENO))
                val pasos = getInt(getColumnIndexOrThrow(COLUMN_PASOS))
                val estres = getInt(getColumnIndexOrThrow(COLUMN_ESTRES))
                val timestamp = getLong(getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                lista.add(Medicion(id, ritmo, oxigeno, pasos, estres, timestamp))
            }
        }
        cursor.close()
        return lista
    }

    fun clearAllData() {
        val db = this.writableDatabase
        db.delete(TABLE_NAME, null, null)
    }

    fun getDataCount(): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_NAME", null)
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }
} 