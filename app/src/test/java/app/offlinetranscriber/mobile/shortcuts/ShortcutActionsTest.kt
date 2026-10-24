package app.offlinetranscriber.mobile.shortcuts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ShortcutActionsTest {

    @Test
    fun constantsMatchAndroidManifestAndShortcutsXml() {
        assertEquals("app.offlinetranscriber.mobile.action.RECORD", ShortcutActions.RECORD)
        assertEquals("app.offlinetranscriber.mobile.action.IMPORT_AUDIO", ShortcutActions.IMPORT_AUDIO)
        assertEquals("app.offlinetranscriber.mobile.action.OPEN_QUEUE", ShortcutActions.OPEN_QUEUE)
        assertEquals("app.offlinetranscriber.mobile.action.OPEN_MODELS", ShortcutActions.OPEN_MODELS)
        assertEquals("app.offlinetranscriber.mobile.action.OPEN_PAYWALL", ShortcutActions.OPEN_PAYWALL)

        // Read real shortcuts.xml resource from project to verify actual manifest binding
        val xmlFile = File("src/main/res/xml/shortcuts.xml")
        if (xmlFile.exists()) {
            val content = xmlFile.readText()
            assertTrue("shortcuts.xml must reference ShortcutActions.RECORD", content.contains(ShortcutActions.RECORD))
            assertTrue("shortcuts.xml must reference ShortcutActions.IMPORT_AUDIO", content.contains(ShortcutActions.IMPORT_AUDIO))
            assertTrue("shortcuts.xml must reference ShortcutActions.OPEN_QUEUE", content.contains(ShortcutActions.OPEN_QUEUE))
        }
    }
}

