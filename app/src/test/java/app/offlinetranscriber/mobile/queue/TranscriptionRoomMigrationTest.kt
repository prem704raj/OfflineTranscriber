package app.offlinetranscriber.mobile.queue

import app.offlinetranscriber.mobile.data.database.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TranscriptionRoomMigrationTest {

    @Test
    fun migration9To10IsDefined() {
        val migration = AppDatabase.MIGRATION_9_10
        assertNotNull(migration)
        assertEquals(9, migration.startVersion)
        assertEquals(10, migration.endVersion)
    }
}
