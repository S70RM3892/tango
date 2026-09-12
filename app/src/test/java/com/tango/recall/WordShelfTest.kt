package com.tango.recall

import androidx.test.core.app.ApplicationProvider
import com.tango.recall.data.Deck
import com.tango.recall.data.Note
import com.tango.recall.data.NoteType
import com.tango.recall.data.Repository
import com.tango.recall.data.Seed
import com.tango.recall.data.TangoDb
import com.tango.recall.srs.Rating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The word shelf: vocabulary laid out along the one dimension it varies in.
 *
 * What matters is the order — the root that is slipping has to be at the top, and a
 * root that has simply not been started must not be mistaken for one that has been
 * forgotten.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WordShelfTest {

    private lateinit var repo: Repository
    private var deckId = 0L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        context.deleteDatabase(TangoDb.DB_NAME)
        repo = Repository(TangoDb(context))
        deckId = repo.saveDeck(
            Deck(
                name = "英単語",
                noteTypeId = NoteType.ENGLISH.id,
                enabledTemplates = setOf("en_ja", "ja_en"),
            )
        )
    }

    private fun word(word: String, root: String): Long = repo.saveNote(
        Note(
            deckId = deckId,
            typeId = NoteType.ENGLISH.id,
            fields = mapOf("word" to word, "meaning" to "意味：$word", "root" to root),
        )
    )

    private fun study(noteId: Long, rating: Rating, daysAgo: Int) {
        val at = System.currentTimeMillis() - daysAgo * 86_400_000L
        repo.cardsOfNote(noteId).forEach { repo.answer(it, rating, at) }
    }

    @Test
    fun wordsAreGroupedByRoot() {
        word("perspective", "spect（見る）")
        word("conspicuous", "spect（見る）")
        word("conduct", "duc（導く）")

        val shelf = repo.wordShelf()

        assertEquals(2, shelf.size)
        assertEquals(
            listOf("conspicuous", "perspective"),
            shelf.first { it.root == "spect（見る）" }.words.map { it.word },
        )
    }

    @Test
    fun theWeakestRootComesFirst() {
        val strongA = word("alpha", "strong（強い）")
        val strongB = word("beta", "strong（強い）")
        val weakA = word("gamma", "weak（弱い）")
        val weakB = word("delta", "weak（弱い）")
        // Answered well and recently: still fresh.
        study(strongA, Rating.EASY, 0)
        study(strongB, Rating.EASY, 0)
        // Answered badly and long ago: predicted recall has fallen away.
        study(weakA, Rating.AGAIN, 30)
        study(weakB, Rating.AGAIN, 30)

        val shelf = repo.wordShelf()

        assertEquals("weak（弱い）", shelf.first().root)
        assertTrue(shelf.first().meanAt(System.currentTimeMillis())!! < 0.5)
    }

    @Test
    fun rootsNotStartedGoLastRatherThanCountingAsForgotten() {
        val started = word("alpha", "started（着手）")
        word("beta", "untouched（未着手）")
        study(started, Rating.GOOD, 10)

        val shelf = repo.wordShelf()

        assertEquals("started（着手）", shelf.first().root)
        val last = shelf.last()
        assertEquals("untouched（未着手）", last.root)
        assertNull("未学習の語根に「定着率」はない", last.meanAt(System.currentTimeMillis()))
        assertEquals(1, last.newCount)
    }

    @Test
    fun theShelfLooksAheadLikeTheMapDoes() {
        val id = word("alpha", "root（語根）")
        study(id, Rating.GOOD, 0)
        val now = System.currentTimeMillis()
        val shelf = repo.wordShelf(now).single()

        val today = shelf.meanAt(now)!!
        val inAYear = shelf.meanAt(now + 365 * 86_400_000L)!!
        assertTrue("先の日付ほど弱く見えること", inAYear < today)
    }

    @Test
    fun aWholeRootCanBeStudiedAtOnce() {
        val a = word("alpha", "root（語根）")
        val b = word("beta", "root（語根）")
        val other = word("gamma", "another（別）")

        val queue = repo.queueForNotes(listOf(a, b))

        assertEquals(4, queue.size)
        assertTrue(queue.none { repo.card(it)!!.noteId == other })
    }

    @Test
    fun theShippedVocabularyFillsTheShelf() {
        Seed.populate(repo)
        val shelf = repo.wordShelf()
        assertTrue("語根の数", shelf.size >= 70)
        assertTrue("語数", shelf.sumOf { it.words.size } >= 300)
        assertTrue("1つの語根に語が並びすぎない", shelf.all { it.words.size <= 8 })
    }
}
