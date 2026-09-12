package com.tango.recall

import androidx.test.core.app.ApplicationProvider
import com.tango.recall.data.NoteType
import com.tango.recall.data.Repository
import com.tango.recall.data.Seed
import com.tango.recall.data.TangoDb
import com.tango.recall.data.wordCount
import com.tango.recall.data.wordsPerMinute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 速読: measuring a reading rate.
 *
 * The measurement has to mean something, which is why a read that was not understood
 * is logged but kept out of the average — otherwise the fastest way to a better number
 * is to stop reading.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ReadingTest {

    private lateinit var repo: Repository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        context.deleteDatabase(TangoDb.DB_NAME)
        repo = Repository(TangoDb(context))
    }

    @Test
    fun wordsAreCountedTheWayAReadingRateCountsThem() {
        assertEquals(5, wordCount("The cat sat on the"))
        assertEquals(4, wordCount("One, two; three — four."))
        assertEquals(1, wordCount("well-known"))
        assertEquals(2, wordCount("it's here"))
        assertEquals(0, wordCount("。、——"))
    }

    @Test
    fun theRateIsWordsOverMinutes() {
        assertEquals(120, wordsPerMinute(200, 100_000))
        assertEquals(60, wordsPerMinute(60, 60_000))
        assertEquals(0, wordsPerMinute(0, 60_000))
        assertEquals(0, wordsPerMinute(100, 0))
    }

    @Test
    fun onlyUnderstoodReadsCountTowardsTheAverage() {
        Seed.populate(repo)
        val passage = repo.readingPassages().first()
        // A slow read that was understood, and a very fast one that was not.
        repo.recordReading(passage.id, tookMs = 120_000, words = 200, understood = true)
        repo.recordReading(passage.id, tookMs = 20_000, words = 200, understood = false)

        val progress = repo.readingProgress()

        assertEquals(2, progress.sessions)
        assertEquals("読み飛ばした回は平均に入れない", 100, progress.meanWordsPerMinute)
        assertEquals(100, progress.bestWordsPerMinute)
        assertEquals(0.5, progress.understoodRate!!, 1e-9)
    }

    @Test
    fun theLastReadOfAPassageIsRemembered() {
        Seed.populate(repo)
        val passage = repo.readingPassages().first()
        assertNull(repo.lastReading(passage.id))

        repo.recordReading(passage.id, tookMs = 100_000, words = 200, understood = true)

        assertEquals(120, repo.lastReading(passage.id)!!.wordsPerMinute)
    }

    @Test
    fun theShippedPassagesAreLongEnoughToMeasure() {
        Seed.populate(repo)
        val passages = repo.readingPassages()
        assertTrue("英文が入っていること", passages.size >= 5)
        for (note in passages) {
            val words = wordCount(note["passage"])
            assertTrue("${note.title()} が短すぎる ($words 語)", words >= 150)
            assertTrue("${note.title()} に設問がない", note["question"].isNotBlank())
            assertTrue("${note.title()} に答えがない", note["answer"].isNotBlank())
            assertTrue("${note.title()} に全訳がない", note["ja"].length >= 200)
            assertTrue(
                "${note.title()} の押さえる点が足りない",
                note["points"].lines().count { it.isNotBlank() } >= 2,
            )
            assertNotNull(
                "${note.title()} が出題できること",
                repo.cardsOfNote(note.id).firstOrNull()?.let { repo.renderAnyCard(it, note) },
            )
        }
    }

    @Test
    fun passagesAreEnglishOnly() {
        Seed.populate(repo)
        for (note in repo.readingPassages()) {
            assertEquals(NoteType.READING, note.type)
            assertTrue(
                "${note.title()} の英文に日本語が混じっている",
                note["passage"].none { it.code in 0x3040..0x30FF || it.code in 0x4E00..0x9FFF },
            )
        }
    }
}
