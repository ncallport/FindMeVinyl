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
        assertEquals("Backed up your settings. There were no artists, albums, photos or shops yet.", BackupMessages.backedUp(BackupSummary(0, 0, 0)))
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

    @Test
    fun `says how many shops were backed up`() {
        assertEquals(
            "Backed up 2 artists, 5 albums, 1 photo and 3 shops.",
            BackupMessages.backedUp(BackupSummary(2, 5, 1, shops = 3))
        )
    }

    @Test
    fun `a backup of one shop on its own is in the singular`() {
        assertEquals("Backed up 1 shop.", BackupMessages.backedUp(BackupSummary(0, 0, 0, shops = 1)))
    }

    @Test
    fun `says how many shops were restored`() {
        assertEquals(
            "Restored 4 albums and 2 shops, and your settings.",
            BackupMessages.restored(RestoreResult.Done(0, 4, 0, settingsRestored = true, shops = 2))
        )
    }

    @Test
    fun `says how many groups were backed up, after the artists`() {
        assertEquals(
            "Backed up 12 artists, 2 groups, 87 albums and 3 photos.",
            BackupMessages.backedUp(BackupSummary(12, 87, 3, shops = 0, groups = 2))
        )
        assertEquals("Backed up 1 group.", BackupMessages.backedUp(BackupSummary(0, 0, 0, shops = 0, groups = 1)))
    }

    @Test
    fun `says how many groups were restored`() {
        assertEquals(
            "Restored 3 artists and 1 group.",
            BackupMessages.restored(RestoreResult.Done(3, 0, 0, settingsRestored = false, shops = 0, groups = 1))
        )
    }

    @Test
    fun `a restore that only brought back groups is not nothing new`() {
        assertEquals(
            "Restored 2 groups.",
            BackupMessages.restored(RestoreResult.Done(0, 0, 0, settingsRestored = false, shops = 0, groups = 2))
        )
    }
}
