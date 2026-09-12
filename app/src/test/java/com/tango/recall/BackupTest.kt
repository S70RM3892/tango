package com.tango.recall

import androidx.test.core.app.ApplicationProvider
import com.tango.recall.data.Deck
import com.tango.recall.data.ImportExport
import com.tango.recall.data.LinkType
import com.tango.recall.data.Note
import com.tango.recall.data.NoteType
import com.tango.recall.data.RelationCards
import com.tango.recall.data.Repository
import com.tango.recall.data.TangoDb
import com.tango.recall.srs.Rating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * What has to come back when a backup is restored on a new phone.
 *
 * "Everything" is the promise, so whatever the file leaves out is lost for good: the
 * relation questions and their schedules, the exam date the whole countdown is built
 * on, the shortlist, and the record of which pairs get mixed up.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BackupTest {

    private lateinit var repo: Repository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        context.deleteDatabase(TangoDb.DB_NAME)
        repo = Repository(TangoDb(context))
    }

    private fun linkedDeck(): Pair<Long, Long> {
        val deckId = repo.saveDeck(
            Deck(
                name = "語源",
                noteTypeId = NoteType.ENGLISH.id,
                enabledTemplates = setOf("en_ja", "ja_en"),
                relationQuiz = true,
            )
        )
        fun word(word: String, meaning: String) = repo.saveNote(
            Note(
                deckId = deckId,
                typeId = NoteType.ENGLISH.id,
                fields = mapOf("word" to word, "meaning" to meaning, "root" to "spect（見る）"),
            )
        )
        val a = word("perspective", "観点")
        val b = word("conspicuous", "目立つ")
        repo.addLink(a, b, LinkType.SAME_ROOT, "spect（見る）")
        return a to b
    }

    private fun roundTrip() {
        ImportExport.importJson(repo, ImportExport.exportJson(repo))
    }

    @Test
    fun theRelationQuizSettingSurvives() {
        linkedDeck()
        roundTrip()
        assertTrue("restoring must not quietly switch it off", repo.listDecks().single().relationQuiz)
    }

    @Test
    fun theScheduleOfARelationQuestionSurvives() {
        val (first, _) = linkedDeck()
        val relation = repo.cardsOfNote(first).single { RelationCards.isRelationCard(it.templateId) }
        repo.answer(relation, Rating.GOOD)
        val reps = repo.card(relation.id)!!.srs.reps
        assertTrue(reps > 0)

        roundTrip()

        val restored = repo.listNotes(null, "", Int.MAX_VALUE)
            .flatMap { repo.cardsOfNote(it.id) }
            .filter { RelationCards.isRelationCard(it.templateId) }
        assertEquals("both ends of the link keep their question", 2, restored.size)
        assertEquals("and the one that was studied keeps its place", reps, restored.maxOf { it.srs.reps })
    }

    @Test
    fun theExamDateAndShortlistSurvive() {
        linkedDeck()
        val wanted = setOf(SHORTLISTED)
        repo.examDate = 1_800_000_000_000L
        repo.shortlist = wanted

        roundTrip()

        assertEquals(1_800_000_000_000L, repo.examDate)
        assertEquals(wanted, repo.shortlist)
    }

    @Test
    fun theMixUpsThatDriveAutomaticLinkingSurvive() {
        val (first, second) = linkedDeck()
        repo.recordConfusion(first, second, "ja_en", "conspicuous")
        repo.recordConfusion(first, second, "ja_en", "conspicuous")

        roundTrip()

        val pair = repo.confusionPairs().single()
        assertEquals(2, pair.times)
        assertEquals(setOf("perspective", "conspicuous"), setOf(pair.a.title(), pair.b.title()))
    }

    @Test
    fun ordinarySettingsStillSurvive() {
        linkedDeck()
        repo.desiredRetention = 0.95
        repo.maxReviewsPerDay = 120
        repo.showRelated = false

        roundTrip()

        assertEquals(0.95, repo.desiredRetention, 1e-9)
        assertEquals(120, repo.maxReviewsPerDay)
        assertFalse(repo.showRelated)
    }

    private companion object {
        /** Shaped like Department.key: university, faculty and department, joined. */
        const val SHORTLISTED = "京都大学\u0001工学部\u0001情報学科"
    }
}
