package com.tango.recall

import androidx.test.core.app.ApplicationProvider
import com.tango.recall.data.Deck
import com.tango.recall.data.LinkType
import com.tango.recall.data.Note
import com.tango.recall.data.NoteType
import com.tango.recall.data.Repository
import com.tango.recall.data.TangoDb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Turning "I wrote the wrong word" into a relation. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ConfusionTest {

    private lateinit var repo: Repository
    private var english = 0L
    private var chemistry = 0L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        context.deleteDatabase(TangoDb.DB_NAME)
        repo = Repository(TangoDb(context))
        english = repo.saveDeck(
            Deck(name = "英単語", noteTypeId = NoteType.ENGLISH.id, enabledTemplates = setOf("ja_en"))
        )
        chemistry = repo.saveDeck(
            Deck(
                name = "化学",
                noteTypeId = NoteType.CHEM_SUBSTANCE.id,
                enabledTemplates = setOf("name_formula"),
            )
        )
    }

    private fun word(w: String, meaning: String) = repo.saveNote(
        Note(deckId = english, typeId = NoteType.ENGLISH.id, fields = mapOf("word" to w, "meaning" to meaning))
    )

    private fun substance(name: String, formula: String) = repo.saveNote(
        Note(
            deckId = chemistry,
            typeId = NoteType.CHEM_SUBSTANCE.id,
            fields = mapOf("name" to name, "formula" to formula),
        )
    )

    @Test
    fun aWrongAnswerThatIsAnotherNotesAnswerIsIdentified() {
        val precede = word("precede", "先行する")
        word("concede", "認める")

        val hit = repo.findConfusion(repo.note(precede)!!, "ja_en", "concede")
        assertEquals("concede", hit?.title())
    }

    @Test
    fun theCorrectAnswerIsNotAConfusion() {
        val precede = word("precede", "先行する")
        word("concede", "認める")
        assertNull(repo.findConfusion(repo.note(precede)!!, "ja_en", "precede"))
    }

    @Test
    fun anAnswerMatchingNothingIsNotAConfusion() {
        val precede = word("precede", "先行する")
        assertNull(repo.findConfusion(repo.note(precede)!!, "ja_en", "banana"))
    }

    @Test
    fun matchingUsesTheSameLenienceAsGrading() {
        val precede = word("precede", "先行する")
        word("concede", "認める")
        // Capitalisation and a leading "to" are ignored when grading, so they must be
        // ignored here too or the same answer would be judged two different ways.
        assertEquals("concede", repo.findConfusion(repo.note(precede)!!, "ja_en", "To Concede")?.title())
    }

    @Test
    fun chemicalFormulaeAreMatchedThroughTheirNotation() {
        val a = substance("硫酸銅(II)五水和物", "CuSO4.5H2O")
        substance("硫酸鉄(II)七水和物", "FeSO4.7H2O")
        assertEquals(
            "硫酸鉄(II)七水和物",
            repo.findConfusion(repo.note(a)!!, "name_formula", "FeSO4・7H2O")?.title(),
        )
    }

    @Test
    fun aOneCharacterSlipIsIgnored() {
        val a = word("precede", "先行する")
        word("a", "ひとつの")
        assertNull(repo.findConfusion(repo.note(a)!!, "ja_en", "a"))
    }

    @Test
    fun notesOfADifferentTypeAreNotOffered() {
        val precede = word("precede", "先行する")
        substance("concede", "X")
        assertNull(repo.findConfusion(repo.note(precede)!!, "ja_en", "concede"))
    }

    @Test
    fun confusionsAreCountedAcrossBothDirections() {
        val a = word("precede", "先行する")
        val b = word("concede", "認める")

        assertEquals(1, repo.recordConfusion(a, b, "ja_en", "concede"))
        // The same pair the other way round is the same mix-up.
        assertEquals(2, repo.recordConfusion(b, a, "ja_en", "precede"))
        assertEquals(3, repo.recordConfusion(a, b, "ja_en", "concede"))
    }

    @Test
    fun separatePairsAreCountedSeparately() {
        val a = word("precede", "先行する")
        val b = word("concede", "認める")
        val c = word("recede", "退く")
        repo.recordConfusion(a, b, "ja_en", "concede")
        assertEquals(1, repo.recordConfusion(a, c, "ja_en", "recede"))
    }

    @Test
    fun linkedPairsAreReportedAsSuch() {
        val a = word("precede", "先行する")
        val b = word("concede", "認める")
        repo.recordConfusion(a, b, "ja_en", "concede")
        repo.recordConfusion(a, b, "ja_en", "concede")

        assertFalse(repo.areLinked(a, b))
        val before = repo.confusionPairs().single()
        assertEquals(2, before.times)
        assertFalse(before.linked)

        repo.addLink(a, b, LinkType.CONFUSABLE, "取り違え 2 回")

        assertTrue(repo.areLinked(a, b))
        assertTrue(repo.areLinked(b, a))
        assertTrue(repo.confusionPairs().single().linked)
    }

    @Test
    fun confusionPairsAreRankedByHowOftenTheyHappen() {
        val a = word("precede", "先行する")
        val b = word("concede", "認める")
        val c = word("recede", "退く")
        repeat(3) { repo.recordConfusion(a, b, "ja_en", "concede") }
        repo.recordConfusion(a, c, "ja_en", "recede")

        val pairs = repo.confusionPairs()
        assertEquals(2, pairs.size)
        assertEquals(3, pairs.first().times)
        assertEquals(setOf("precede", "concede"), setOf(pairs.first().a.title(), pairs.first().b.title()))
    }

    @Test
    fun deletingANoteRemovesItsConfusions() {
        val a = word("precede", "先行する")
        val b = word("concede", "認める")
        repo.recordConfusion(a, b, "ja_en", "concede")
        repo.deleteNote(b)
        assertTrue(repo.confusionPairs().isEmpty())
    }
}
