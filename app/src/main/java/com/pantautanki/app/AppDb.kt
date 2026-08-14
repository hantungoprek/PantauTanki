package com.pantautanki.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Vehicle(
    val id: Long,
    val name: String,
    val odometer: Int,
    val fuelType: String,
    val tankCapacity: Double
)

data class FuelEntry(
    val id: Long,
    val vehicleId: Long,
    val timestamp: Long,
    val odometer: Int,
    val liters: Double,
    val pricePerLiter: Long,
    val total: Long
)

class AppDb(context: Context) : SQLiteOpenHelper(context, "pantautanki.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE vehicles(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            odometer INTEGER NOT NULL,
            fuelType TEXT NOT NULL,
            tankCapacity REAL NOT NULL
        )""")
        db.execSQL("""CREATE TABLE fuel_entries(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            vehicleId INTEGER NOT NULL,
            timestamp INTEGER NOT NULL,
            odometer INTEGER NOT NULL,
            liters REAL NOT NULL,
            pricePerLiter INTEGER NOT NULL,
            total INTEGER NOT NULL,
            FOREIGN KEY(vehicleId) REFERENCES vehicles(id) ON DELETE CASCADE
        )""")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    suspend fun vehicles(): List<Vehicle> = withContext(Dispatchers.IO) {
        val c = readableDatabase.query("vehicles", null, null, null, null, null, "id")
        c.use {
            buildList {
                while (it.moveToNext()) add(
                    Vehicle(
                        it.getLong(it.getColumnIndexOrThrow("id")),
                        it.getString(it.getColumnIndexOrThrow("name")),
                        it.getInt(it.getColumnIndexOrThrow("odometer")),
                        it.getString(it.getColumnIndexOrThrow("fuelType")),
                        it.getDouble(it.getColumnIndexOrThrow("tankCapacity"))
                    )
                )
            }
        }
    }

    suspend fun addVehicle(name: String, odo: Int, tank: Double): Long = withContext(Dispatchers.IO) {
        val v = ContentValues().apply {
            put("name", name); put("odometer", odo); put("fuelType", "Bensin"); put("tankCapacity", tank)
        }
        writableDatabase.insertOrThrow("vehicles", null, v)
    }

    suspend fun updateOdometer(id: Long, odo: Int) = withContext(Dispatchers.IO) {
        writableDatabase.update("vehicles", ContentValues().apply { put("odometer", odo) }, "id=?", arrayOf(id.toString()))
    }

    suspend fun deleteVehicle(id: Long) = withContext(Dispatchers.IO) {
        writableDatabase.beginTransaction()
        try {
            writableDatabase.delete("fuel_entries", "vehicleId=?", arrayOf(id.toString()))
            writableDatabase.delete("vehicles", "id=?", arrayOf(id.toString()))
            writableDatabase.setTransactionSuccessful()
        } finally { writableDatabase.endTransaction() }
    }

    suspend fun addFuel(vehicleId: Long, odo: Int, liters: Double, price: Long): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val v = ContentValues().apply {
            put("vehicleId", vehicleId); put("timestamp", now); put("odometer", odo)
            put("liters", liters); put("pricePerLiter", price); put("total", (liters * price).toLong())
        }
        val id = writableDatabase.insertOrThrow("fuel_entries", null, v)
        updateOdometer(vehicleId, odo)
        id
    }

    suspend fun entries(vehicleId: Long): List<FuelEntry> = withContext(Dispatchers.IO) {
        val c = readableDatabase.query("fuel_entries", null, "vehicleId=?", arrayOf(vehicleId.toString()), null, null, "timestamp ASC")
        c.use {
            buildList {
                while (it.moveToNext()) add(
                    FuelEntry(
                        it.getLong(it.getColumnIndexOrThrow("id")),
                        it.getLong(it.getColumnIndexOrThrow("vehicleId")),
                        it.getLong(it.getColumnIndexOrThrow("timestamp")),
                        it.getInt(it.getColumnIndexOrThrow("odometer")),
                        it.getDouble(it.getColumnIndexOrThrow("liters")),
                        it.getLong(it.getColumnIndexOrThrow("pricePerLiter")),
                        it.getLong(it.getColumnIndexOrThrow("total"))
                    )
                )
            }
        }
    }

    suspend fun allEntries(): List<FuelEntry> = withContext(Dispatchers.IO) {
        val c = readableDatabase.query("fuel_entries", null, null, null, null, null, "timestamp ASC")
        c.use {
            buildList {
                while (it.moveToNext()) add(
                    FuelEntry(
                        it.getLong(it.getColumnIndexOrThrow("id")),
                        it.getLong(it.getColumnIndexOrThrow("vehicleId")),
                        it.getLong(it.getColumnIndexOrThrow("timestamp")),
                        it.getInt(it.getColumnIndexOrThrow("odometer")),
                        it.getDouble(it.getColumnIndexOrThrow("liters")),
                        it.getLong(it.getColumnIndexOrThrow("pricePerLiter")),
                        it.getLong(it.getColumnIndexOrThrow("total"))
                    )
                )
            }
        }
    }
}
