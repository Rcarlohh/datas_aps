package com.example.datasimulator

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "DataSimulator.db"
        private const val DATABASE_VERSION = 3

        // Tabla de usuarios
        private const val TABLE_USERS = "users"
        private const val COLUMN_USER_ID = "id"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_FULL_NAME = "full_name"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_CREATED_AT = "created_at"

        // Tabla de datos (modificada para incluir user_id)
        private const val TABLE_NAME = "data_items"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_VALUE = "value"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_USER_ID_FK = "user_id"

        // Tabla de mediciones (modificada para incluir user_id)
        private const val TABLE_MEDICIONES = "mediciones"
        private const val COLUMN_RITMO = "ritmo_cardiaco"
        private const val COLUMN_OXIGENO = "oxigeno"
        private const val COLUMN_PASOS = "pasos"
        private const val COLUMN_ESTRES = "estres"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Crear tabla de usuarios
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USERNAME TEXT UNIQUE NOT NULL,
                $COLUMN_PASSWORD TEXT NOT NULL,
                $COLUMN_FULL_NAME TEXT NOT NULL,
                $COLUMN_EMAIL TEXT NOT NULL,
                $COLUMN_CREATED_AT INTEGER NOT NULL
            )
        """
        db.execSQL(createUsersTable)

        // Crear tabla de datos con referencia a usuario
        val createDataTable = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TYPE TEXT NOT NULL,
                $COLUMN_VALUE REAL NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_USER_ID_FK INTEGER NOT NULL,
                FOREIGN KEY($COLUMN_USER_ID_FK) REFERENCES $TABLE_USERS($COLUMN_USER_ID)
            )
        """
        db.execSQL(createDataTable)

        // Crear tabla de mediciones con referencia a usuario
        val createMedicionesTable = """
            CREATE TABLE $TABLE_MEDICIONES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_RITMO INTEGER NOT NULL,
                $COLUMN_OXIGENO INTEGER NOT NULL,
                $COLUMN_PASOS INTEGER NOT NULL,
                $COLUMN_ESTRES INTEGER NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_USER_ID_FK INTEGER NOT NULL,
                FOREIGN KEY($COLUMN_USER_ID_FK) REFERENCES $TABLE_USERS($COLUMN_USER_ID)
            )
        """
        db.execSQL(createMedicionesTable)

        // Insertar usuario principal
        insertDefaultUser(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MEDICIONES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    private fun insertDefaultUser(db: SQLiteDatabase) {
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, "Acosta")
            put(COLUMN_PASSWORD, hashPassword("admin123"))
            put(COLUMN_FULL_NAME, "Usuario Principal")
            put(COLUMN_EMAIL, "acosta@respira.com")
            put(COLUMN_CREATED_AT, System.currentTimeMillis())
        }
        db.insert(TABLE_USERS, null, values)
    }

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest(password.toByteArray())
        return hashBytes.fold("", { str, it -> str + "%02x".format(it) })
    }

    // Métodos para usuarios
    fun insertUser(username: String, password: String, fullName: String, email: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, username)
            put(COLUMN_PASSWORD, hashPassword(password))
            put(COLUMN_FULL_NAME, fullName)
            put(COLUMN_EMAIL, email)
            put(COLUMN_CREATED_AT, System.currentTimeMillis())
        }
        return db.insert(TABLE_USERS, null, values)
    }

    fun authenticateUser(username: String, password: String): User? {
        val db = this.readableDatabase
        val hashedPassword = hashPassword(password)
        val cursor = db.query(
            TABLE_USERS,
            null,
            "$COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?",
            arrayOf(username, hashedPassword),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            val user = User(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)),
                fullName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FULL_NAME)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT))
            )
            cursor.close()
            user
        } else {
            cursor.close()
            null
        }
    }

    fun userExists(username: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_USER_ID),
            "$COLUMN_USERNAME = ?",
            arrayOf(username),
            null, null, null
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun getUserById(userId: Long): User? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            null,
            "$COLUMN_USER_ID = ?",
            arrayOf(userId.toString()),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            val user = User(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)),
                fullName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FULL_NAME)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT))
            )
            cursor.close()
            user
        } else {
            cursor.close()
            null
        }
    }

    // Métodos para datos (modificados para incluir user_id)
    fun insertData(type: String, value: Double, userId: Long): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TYPE, type)
            put(COLUMN_VALUE, value)
            put(COLUMN_TIMESTAMP, System.currentTimeMillis())
            put(COLUMN_USER_ID_FK, userId)
        }
        return db.insert(TABLE_NAME, null, values)
    }

    fun getAllData(userId: Long): List<DataItem> {
        val dataList = mutableListOf<DataItem>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            "$COLUMN_USER_ID_FK = ?",
            arrayOf(userId.toString()),
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

    fun insertMedicion(ritmo: Int, oxigeno: Int, pasos: Int, estres: Int, userId: Long): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_RITMO, ritmo)
            put(COLUMN_OXIGENO, oxigeno)
            put(COLUMN_PASOS, pasos)
            put(COLUMN_ESTRES, estres)
            put(COLUMN_TIMESTAMP, System.currentTimeMillis())
            put(COLUMN_USER_ID_FK, userId)
        }
        return db.insert(TABLE_MEDICIONES, null, values)
    }

    data class User(
        val id: Long,
        val username: String,
        val fullName: String,
        val email: String,
        val createdAt: Long
    )

    data class Medicion(
        val id: Long,
        val ritmo: Int,
        val oxigeno: Int,
        val pasos: Int,
        val estres: Int,
        val timestamp: Long,
        val userId: Long
    )

    fun getAllMediciones(userId: Long): List<Medicion> {
        val lista = mutableListOf<Medicion>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_MEDICIONES,
            null,
            "$COLUMN_USER_ID_FK = ?",
            arrayOf(userId.toString()),
            null,
            null,
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
                val userIdFromDb = getLong(getColumnIndexOrThrow(COLUMN_USER_ID_FK))
                lista.add(Medicion(id, ritmo, oxigeno, pasos, estres, timestamp, userIdFromDb))
            }
        }
        cursor.close()
        return lista
    }

    fun clearAllData(userId: Long) {
        val db = this.writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_USER_ID_FK = ?", arrayOf(userId.toString()))
        db.delete(TABLE_MEDICIONES, "$COLUMN_USER_ID_FK = ?", arrayOf(userId.toString()))
    }

    fun getDataCount(userId: Long): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_NAME WHERE $COLUMN_USER_ID_FK = ?",
            arrayOf(userId.toString())
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }
}