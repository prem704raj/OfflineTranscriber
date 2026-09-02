package com.example.transcriber.meeting

import com.example.transcriber.data.database.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MeetingDatabaseMigrationTest {

    @Test
    fun migration7To8IsDefined() {
        val migration = AppDatabase.MIGRATION_7_8
        assertNotNull(migration)
        assertEquals(7, migration.startVersion)
        assertEquals(8, migration.endVersion)
    }
}
