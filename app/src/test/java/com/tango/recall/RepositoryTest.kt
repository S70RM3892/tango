package com.tango.recall

import androidx.test.core.app.ApplicationProvider
import com.tango.recall.data.Deck
import com.tango.recall.data.ImportExport
import com.tango.recall.data.LinkType
import com.tango.recall.data.Note
import com.tango.recall.data.NoteType
import com.tango.recall.data.Repository
import com.tango.recall.data.Seed
import com.tango.recall.data.TangoDb
import com.tango.recall.data.renderCard
import com.tango.recall.srs.CardPhase
import com.tango.recall.srs.Rating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Exercises the real SQLite layer on the JVM, including first-launch seeding. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RepositoryTest {

    private lateinit var repo: Repository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        context.deleteDatabase(TangoDb.DB_NAME)
        repo = Repository(TangoDb(context))
    }

    private fun englishDeck(): Long = repo.saveDeck(
        Deck(
            name = "テスト",
            noteTypeId = NoteType.ENGLISH.id,
            enabledTemplates = setOf("en_ja", "ja_en", "cloze"),
            newPerDay = 20,
        )
    )

    private fun sampleNote(deckId: Long, word: String = "abandon") = Note(
        deckId = deckId,
        typeId = NoteType.ENGLISH.id,
        fields = mapOf(
            "word" to word,
            "meaning" to "〜を見捨てる",
            "pos" to "動",
            "root" to "ab + bandon",
            "example" to "He $word" + "ed his plan.",
            "exampleJa" to "彼は計画を放棄した。",
        ),
        tags = listOf("語源"),
    )

    @Test
    fun seedingProducesLinkedDecksAndRunsOnlyOnce() {
        Seed.populate(repo)
        val deckCount = repo.listDecks().size
        assertTrue("seed should create several decks", deckCount >= 3)

        val allNotes = repo.listNotes(null, "", Int.MAX_VALUE)
        assertTrue("seed should create notes", allNotes.size > 30)
        assertTrue(
            "seeded notes should already be linked to each other",
            allNotes.count { repo.related(it.id).isNotEmpty() } > 10,
        )

        Seed.populate(repo)
        assertEquals("re-seeding must be a no-op", deckCount, repo.listDecks().size)
    }

    @Test
    fun savingANoteGeneratesOneCardPerEnabledDirection() {
        val deckId = englishDeck()
        val noteId = repo.saveNote(sampleNote(deckId))
        val cards = repo.cardsOfNote(noteId)
        assertEquals(setOf("en_ja", "ja_en", "cloze"), cards.map { it.templateId }.toSet())
    }

    @Test
    fun clearingARequiredFieldRemovesOnlyThatDirection() {
        val deckId = englishDeck()
        val noteId = repo.saveNote(sampleNote(deckId))
        val note = repo.note(noteId)!!
        repo.saveNote(note.copy(fields = note.fields + ("example" to "")))
        assertEquals(setOf("en_ja", "ja_en"), repo.cardsOfNote(noteId).map { it.templateId }.toSet())
    }

    @Test
    fun disablingADirectionInDeckSettingsDropsItsCards() {
        val deckId = englishDeck()
        val noteId = repo.saveNote(sampleNote(deckId))
        val deck = repo.deck(deckId)!!
        repo.saveDeck(deck.copy(enabledTemplates = setOf("en_ja")))
        assertEquals(listOf("en_ja"), repo.cardsOfNote(noteId).map { it.templateId })
    }

    @Test
    fun everyGeneratedCardRenders() {
        Seed.populate(repo)
        val notes = repo.listNotes(null, "", Int.MAX_VALUE)
        var rendered = 0
        for (note in notes) {
            for (card in repo.cardsOfNote(note.id)) {
                val view = renderCard(card, note)
                assertNotNull("card ${card.templateId} of ${note.title()} failed to render", view)
                assertTrue(view!!.promptText.isNotBlank())
                assertTrue(view.answerParts.isNotEmpty())
                rendered++
            }
        }
        assertTrue("seed should produce a substantial number of cards", rendered > 80)
    }

    @Test
    fun queueRespectsTheDailyNewLimit() {
        val deckId = repo.saveDeck(
            Deck(
                name = "少しずつ",
                noteTypeId = NoteType.ENGLISH.id,
                enabledTemplates = setOf("en_ja"),
                newPerDay = 3,
            )
        )
        repeat(10) { repo.saveNote(sampleNote(deckId, "word$it")) }
        assertEquals(3, repo.buildQueue(deckId).size)
    }

    @Test
    fun answeringAdvancesTheScheduleAndIsLogged() {
        val deckId = englishDeck()
        val noteId = repo.saveNote(sampleNote(deckId))
        val card = repo.cardsOfNote(noteId).first()
        val now = System.currentTimeMillis()

        val after = repo.answer(card, Rating.GOOD, now)
        assertTrue(after.srs.due > now)
        assertEquals(1, after.srs.reps)
        assertTrue(after.srs.phase != CardPhase.NEW)

        val reloaded = repo.card(card.id)!!
        assertEquals(after.srs.due, reloaded.srs.due)
        assertEquals(1, repo.stats(now).reviewsToday)
    }

    @Test
    fun newCardsStopBeingOfferedOnceTheDailyLimitIsUsed() {
        val deckId = repo.saveDeck(
            Deck(
                name = "上限",
                noteTypeId = NoteType.ENGLISH.id,
                enabledTemplates = setOf("en_ja"),
                newPerDay = 2,
            )
        )
        repeat(5) { repo.saveNote(sampleNote(deckId, "w$it")) }
        val queue = repo.buildQueue(deckId)
        assertEquals(2, queue.size)
        queue.forEach { repo.answer(repo.card(it)!!, Rating.EASY) }
        assertEquals("the limit must survive a rebuild on the same day", 0, repo.buildQueue(deckId).size)
    }

    @Test
    fun relationsReadCorrectlyFromBothEnds() {
        val deckId = englishDeck()
        val a = repo.saveNote(sampleNote(deckId, "produce"))
        val b = repo.saveNote(sampleNote(deckId, "induce"))
        assertTrue(repo.addLink(a, b, LinkType.DERIVED, "同じ語根"))
        assertFalse("duplicate links must be rejected", repo.addLink(a, b, LinkType.DERIVED))

        val fromA = repo.related(a).single()
        val fromB = repo.related(b).single()
        assertEquals(LinkType.DERIVED.forward, fromA.label)
        assertEquals(LinkType.DERIVED.reverse, fromB.label)
        assertEquals("induce", fromA.other.title())
        assertEquals("produce", fromB.other.title())
    }

    @Test
    fun sameRootNotesAreSuggestedAsRelated() {
        val deckId = englishDeck()
        val a = repo.saveNote(sampleNote(deckId, "conduct"))
        repo.saveNote(sampleNote(deckId, "induce"))
        val suggestions = repo.suggestLinks(repo.note(a)!!)
        assertTrue(suggestions.any { it.first.title() == "induce" && it.second == LinkType.SAME_ROOT })
    }

    @Test
    fun deletingANoteRemovesItsCardsAndLinks() {
        val deckId = englishDeck()
        val a = repo.saveNote(sampleNote(deckId, "impose"))
        val b = repo.saveNote(sampleNote(deckId, "dispose"))
        repo.addLink(a, b, LinkType.SAME_ROOT)
        repo.deleteNote(a)
        assertTrue(repo.cardsOfNote(a).isEmpty())
        assertTrue(repo.related(b).isEmpty())
    }

    @Test
    fun tsvImportAddsThenUpdatesWithoutDuplicating() {
        val deckId = englishDeck()
        val tsv = """
            word	meaning	pos	root	example	exampleJa
            versatile	多才な	形	vert	Wood is versatile.	木材は用途が広い。
            divert	そらす	動	vert	Traffic was diverted.	流れがそらされた。
        """.trimIndent()

        val first = ImportExport.importDelimited(repo, deckId, tsv)
        assertEquals(2, first.added)
        assertEquals(2, repo.listNotes(deckId, "").size)

        val second = ImportExport.importDelimited(repo, deckId, tsv)
        assertEquals("re-importing must update in place", 0, second.added)
        assertEquals(2, second.updated)
        assertEquals(2, repo.listNotes(deckId, "").size)
    }

    @Test
    fun tsvImportWithoutAHeaderUsesFieldOrder() {
        val deckId = englishDeck()
        val result = ImportExport.importDelimited(repo, deckId, "tenacious\t粘り強い\t形")
        assertEquals(1, result.added)
        assertEquals("粘り強い", repo.listNotes(deckId, "").single()["meaning"])
    }

    @Test
    fun backupRoundTripsContentLinksAndProgress() {
        Seed.populate(repo)
        val deckId = repo.listDecks().first().id
        val cardBefore = repo.cardsOfNote(repo.listNotes(deckId, "").first().id).first()
        repo.answer(cardBefore, Rating.GOOD)

        val json = ImportExport.exportJson(repo)
        val decksBefore = repo.listDecks().size
        val notesBefore = repo.listNotes(null, "", Int.MAX_VALUE).size
        val linksBefore = repo.listNotes(null, "", Int.MAX_VALUE).sumOf { repo.related(it.id).size }

        ImportExport.importJson(repo, json)

        assertEquals(decksBefore, repo.listDecks().size)
        assertEquals(notesBefore, repo.listNotes(null, "", Int.MAX_VALUE).size)
        assertEquals(linksBefore, repo.listNotes(null, "", Int.MAX_VALUE).sumOf { repo.related(it.id).size })
        assertTrue(
            "scheduling progress must survive a backup",
            repo.listNotes(null, "", Int.MAX_VALUE)
                .flatMap { repo.cardsOfNote(it.id) }
                .any { it.srs.reps > 0 },
        )
    }

    @Test
    fun queueDoesNotPutTwoDirectionsOfTheSameNoteSideBySide() {
        val deckId = englishDeck()
        repeat(6) { repo.saveNote(sampleNote(deckId, "w$it")) }
        val queue = repo.buildQueue(deckId)
        val noteIds = queue.mapNotNull { repo.card(it)?.noteId }
        val adjacentDuplicates = noteIds.zipWithNext().count { (a, b) -> a == b }
        assertEquals(0, adjacentDuplicates)
    }

    @Test
    fun settingsPersist() {
        repo.desiredRetention = 0.95
        repo.maxReviewsPerDay = 120
        repo.showRelated = false
        val reopened = Repository(TangoDb(ApplicationProvider.getApplicationContext()))
        assertEquals(0.95, reopened.desiredRetention, 1e-9)
        assertEquals(120, reopened.maxReviewsPerDay)
        assertFalse(reopened.showRelated)
    }
}
