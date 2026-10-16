package com.nickallport.findmevinyl.backup

import org.junit.Assert.assertEquals
import org.junit.Test

class BackupMessagesTest {

    @Test
    fun `says what was backed up`() {
        assertEquals("Backed up 12 artists, 87 albums and 3 photos.", BackupMessages.backedUp(BackupSummary(12, 87, 3)))
    }

    @Test
    fun `uses the singular for one`() {
        assertEquals("Backed up 1 artist, 1 album and 1 photo.", BackupMessages.backedUp(BackupSummary(1, 1, 1)))
    }

    @Test
    fun `leaves out what there was none of`() {
        assertEquals("Backed up 2 artists and 5 albums.", BackupMessages.backedUp(BackupSummary(2, 5, 0)))
    }

    @Test
    fun `says so when there was nothing to back up`() {
        assertEquals("Backed up your settings. There were no artists, albums or photos yet.", BackupMessages.backedUp(BackupSummary(0, 0, 0)))
    }

    @Test
    fun `says what was restored, and the settings`() {
        assertEquals(
            "Restored 12 artists, 87 albums and 3 photos, and your settings.",
            BackupMessages.restored(RestoreResult.Done(12, 87, 3, settingsRestored = true))
        )
    }

    @Test
    fun `says what was restored when the file had no settings`() {
        assertEquals(
            "Restored 1 artist.",
            BackupMessages.restored(RestoreResult.Done(1, 0, 0, settingsRestored = false))
        )
    }

    @Test
    fun `says when the file added nothing`() {
        assertEquals(
            "Nothing new to restore. This phone already has everything in the file.",
            BackupMessages.restored(RestoreResult.Done(0, 0, 0, settingsRestored = false))
        )
    }

    @Test
    fun `settings alone are reported`() {
        assertEquals(
            "Restored your settings.",
            BackupMessages.restored(RestoreResult.Done(0, 0, 0, settingsRestored = true))
        )
    }

    @Test
    fun `a refused file gives its reason`() {
        assertEquals(
            "This is not a FindMeVinyl backup.",
            BackupMessages.restored(RestoreResult.Failed("This is not a FindMeVinyl backup."))
        )
    }
}
