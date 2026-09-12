package com.tango.recall

import androidx.test.core.app.ApplicationProvider
import com.tango.recall.data.AnswerMode
import com.tango.recall.data.NoteType
import com.tango.recall.data.Repository
import com.tango.recall.data.Seed
import com.tango.recall.data.TangoDb
import com.tango.recall.data.parseNumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The shape of the bundled content.
 *
 * Not a count for its own sake: each of these is a property the app depends on. A
 * word with no sentence produces no cloze card, a calculation whose answer cannot be
 * read as a number can never be marked right, a self-graded question with no
 * checklist has nothing to grade against — and a note with no relation is exactly
 * what this app exists to avoid.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SeedContentTest {

    private lateinit var repo: Repository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        context.deleteDatabase(TangoDb.DB_NAME)
        repo = Repository(TangoDb(context))
        Seed.populate(repo)
    }

    private fun notesOf(type: NoteType) =
        repo.listNotes(null, "", Int.MAX_VALUE).filter { it.type == type }

    @Test
    fun everySubjectIsCoveredInDepth() {
        assertTrue("英単語", notesOf(NoteType.ENGLISH).size >= 70)
        assertTrue("化学・物質", notesOf(NoteType.CHEM_SUBSTANCE).size >= 55)
        assertTrue("化学・反応", notesOf(NoteType.CHEM_REACTION).size >= 50)
        assertTrue("化学・計算", notesOf(NoteType.CHEM_CALC).size >= 15)
        assertTrue("和文英訳", notesOf(NoteType.EISAKUBUN).size >= 10)
        assertTrue("英文和訳", notesOf(NoteType.WAYAKU).size >= 5)
    }

    @Test
    fun organicChemistryIsThere() {
        val organic = repo.listNotes(null, "", Int.MAX_VALUE).filter { "有機" in it.tags }
        assertTrue("有機化学がまるごと抜けていないこと", organic.size >= 45)
        // The oxidation ladder is the backbone; if it is missing, so is the point.
        val titles = organic.map { it.title() }
        for (expected in listOf("エタノール", "アセトアルデヒド", "酢酸", "アセトン", "フェノール", "アニリン")) {
            assertTrue("$expected がない", expected in titles)
        }
    }

    @Test
    fun everyWordCarriesASentenceAndARoot() {
        for (note in notesOf(NoteType.ENGLISH)) {
            assertTrue("${note.title()} に例文がない", note["example"].isNotBlank())
            assertTrue("${note.title()} に例文訳がない", note["exampleJa"].isNotBlank())
            assertTrue("${note.title()} に語根がない", note["root"].isNotBlank())
            assertTrue(
                "${note.title()} の例文にその語が出てこない",
                note["example"].lowercase().contains(note["word"].take(4).lowercase()),
            )
        }
    }

    @Test
    fun everyCalculationHasAReadableAnswer() {
        for (note in notesOf(NoteType.CHEM_CALC)) {
            assertNotNull("${note.title()} の答えが数値として読めない", parseNumber(note["answer"]))
            assertTrue("${note.title()} に解き方がない", note["solution"].isNotBlank())
        }
    }

    @Test
    fun everySelfGradedQuestionHasPointsToCheck() {
        val selfGraded = repo.listNotes(null, "", Int.MAX_VALUE)
            .filter { it.type == NoteType.EISAKUBUN || it.type == NoteType.WAYAKU }
        for (note in selfGraded) {
            val card = repo.cardsOfNote(note.id)
                .firstOrNull { note.type.template(it.templateId)?.mode == AnswerMode.SELF_CHECK }
            assertNotNull("${note.title()} に自己採点のカードがない", card)
            val rendered = repo.renderAnyCard(card!!, note)!!
            assertTrue("${note.title()} の押さえる点が足りない", rendered.checklist.size >= 2)
        }
    }

    @Test
    fun almostNothingIsLeftUnconnected() {
        val notes = repo.listNotes(null, "", Int.MAX_VALUE)
        val isolated = notes.filter { repo.related(it.id).isEmpty() }
        assertTrue(
            "関係のないノートが多すぎる: ${isolated.map { it.title() }}",
            isolated.size <= notes.size / 10,
        )
    }

    @Test
    fun theContentIsInstalledOnceAndStaysPut() {
        val before = repo.listNotes(null, "", Int.MAX_VALUE).size
        Seed.populate(repo)
        assertEquals(before, repo.listNotes(null, "", Int.MAX_VALUE).size)
    }
}
