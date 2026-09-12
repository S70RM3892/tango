package com.tango.recall

import androidx.test.core.app.ApplicationProvider
import com.tango.recall.data.Repository
import com.tango.recall.data.Seed
import com.tango.recall.data.TangoDb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Bundled content arrives in packs.
 *
 * The point is that a later version can add material to a phone that was set up
 * months ago: what the learner has not got yet is installed, what they already have
 * is left exactly as it is — edits, tags and schedules included.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SeedPackTest {

    private lateinit var repo: Repository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        context.deleteDatabase(TangoDb.DB_NAME)
        repo = Repository(TangoDb(context))
    }

    private fun noteCount() = repo.listNotes(null, "", Int.MAX_VALUE).size

    @Test
    fun aFreshInstallGetsEveryPackExactlyOnce() {
        Seed.populate(repo)
        val notes = noteCount()
        val packs = repo.installedSeedPacks
        assertTrue("every pack is recorded", packs.size >= 4)

        Seed.populate(repo)

        assertEquals("running it again must add nothing", notes, noteCount())
        assertEquals(packs, repo.installedSeedPacks)
    }

    @Test
    fun aPackTheLearnerDoesNotHaveYetIsInstalledLater() {
        Seed.populate(repo)
        val before = noteCount()
        // Translation into Japanese comes from exactly one pack, so removing those
        // notes reproduces a phone that was set up before that pack existed.
        assertTrue(repo.listNotes(null, "", Int.MAX_VALUE).any { it.typeId == "wayaku" })

        repo.raw().execSQL("DELETE FROM notes WHERE type='wayaku'")
        repo.installedSeedPacks = repo.installedSeedPacks - "wayaku"

        Seed.populate(repo)

        assertEquals("the missing pack comes back, the rest is untouched", before, noteCount())
    }

    @Test
    fun anInstallFromBeforePacksExistedIsNotSeededTwice() {
        Seed.populate(repo)
        val before = noteCount()
        // Exactly what such a phone holds: the content, the old flag, no pack list.
        repo.putSetting(Repository.KEY_SEED_PACKS, "[]")

        Seed.populate(repo)

        assertEquals(before, noteCount())
    }

    @Test
    fun reinstallingAPackKeepsTheLearnersOwnEdits() {
        Seed.populate(repo)
        val note = repo.listNotes(null, "abandon", Int.MAX_VALUE)
            .firstOrNull() ?: repo.listNotes(null, "", Int.MAX_VALUE).first { it.typeId == "english" }
        val edited = note.copy(fields = note.fields + ("memo" to "自分のメモ"))
        repo.saveNote(edited)
        val before = noteCount()

        repo.installedSeedPacks = repo.installedSeedPacks - "english"
        Seed.populate(repo)

        assertEquals("no duplicates", before, noteCount())
        assertEquals("自分のメモ", repo.note(note.id)!!["memo"])
    }
}
