package app.offlinetranscriber.mobile.ask

import app.offlinetranscriber.mobile.data.database.AppDatabase
import org.junit.Assert.assertNotNull
import org.junit.Test

class AskDatabaseMigrationTest {

    @Test
    fun migration6To7IsDefined() {
        val migration = AppDatabase.MIGRATION_6_7
        assertNotNull(migration)
        org.junit.Assert.assertEquals(6, migration.startVersion)
        org.junit.Assert.assertEquals(7, migration.endVersion)
    }
}
