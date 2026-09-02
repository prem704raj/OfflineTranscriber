package com.example.transcriber.backup.restore

import androidx.sqlite.db.SupportSQLiteDatabase

class RoomForeignKeyChecker(
    private val db: SupportSQLiteDatabase
) {

    fun requireClean() {
        db.query("PRAGMA foreign_key_check").use { cursor ->
            require(!cursor.moveToFirst()) {
                "Restored database failed foreign key relationship validation."
            }
        }
    }
}
