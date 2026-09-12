package com.tango.recall

import androidx.test.core.app.ApplicationProvider
import com.tango.recall.data.Deck
import com.tango.recall.data.LinkType
import com.tango.recall.data.Note
import com.tango.recall.data.NoteType
import com.tango.recall.data.RelationCards
import com.tango.recall.data.Repository
import com.tango.recall.data.TangoDb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Cards generated from the relation graph rather than from a note's own fields. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RelationCardTest {

    private lateinit var repo: Repository
    private var deckId = 0L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        context.deleteDatabase(TangoDb.DB_NAME)
        repo = Repository(TangoDb(context))
        deckId = repo.saveDeck(
            Deck(
                name = "語源",
                noteTypeId = NoteType.ENGLISH.id,
                enabledTemplates = setOf("en_ja"),
                newPerDay = 50,
                relationQuiz = true,
            )
        )
    }

    private fun word(w: String, root: String = "spect") = repo.saveNote(
        Note(
            deckId = deckId,
            typeId = NoteType.ENGLISH.id,
            fields = mapOf("word" to w, "meaning" to "$w の意味", "root" to root),
        )
    )

    private fun relationCards(noteId: Long) =
        repo.cardsOfNote(noteId).filter { RelationCards.isRelationCard(it.templateId) }

    @Test
    fun linkingTwoNotesGivesBothOfThemARelationCard() {
        val a = word("perspective")
        val b = word("conspicuous")
        assertTrue(relationCards(a).isEmpty())

        repo.addLink(a, b, LinkType.SAME_ROOT, "spect")

        assertEquals(listOf("rel:same_root"), relationCards(a).map { it.templateId })
        assertEquals(listOf("rel:same_root"), relationCards(b).map { it.templateId })
    }

    @Test
    fun allPartnersOfOneRelationShareASingleCard() {
        val a = word("perspective")
        val b = word("conspicuous")
        val c = word("speculate")
        repo.addLink(a, b, LinkType.SAME_ROOT)
        repo.addLink(a, c, LinkType.SAME_ROOT)

        // Three same-root words, still one question: "which words share this root?"
        assertEquals(1, relationCards(a).size)

        val card = relationCards(a).single()
        val rendered = repo.renderAnyCard(card, repo.note(a)!!)
        assertNotNull(rendered)
        assertEquals("perspective", rendered!!.promptText)
        assertEquals(2, rendered.answerParts.size)
        assertTrue(rendered.isRelation)
        assertTrue(rendered.promptLabel.contains("同語根"))
    }

    @Test
    fun asymmetricRelationsAskADifferentQuestionFromEachEnd() {
        val reaction = word("接触法", root = "")
        val product = word("硫酸", root = "")
        repo.addLink(reaction, product, LinkType.PRODUCES, "最終生成物")

        assertEquals(listOf("rel:produces"), relationCards(reaction).map { it.templateId })
        assertEquals(listOf("rel:produces:r"), relationCards(product).map { it.templateId })

        val forward = repo.renderAnyCard(relationCards(reaction).single(), repo.note(reaction)!!)!!
        val backward = repo.renderAnyCard(relationCards(product).single(), repo.note(product)!!)!!
        assertTrue(forward.promptLabel.contains(LinkType.PRODUCES.forward))
        assertTrue(backward.promptLabel.contains(LinkType.PRODUCES.reverse))
    }

    @Test
    fun differentRelationTypesAreSeparateCards() {
        val a = word("adverse")
        repo.addLink(a, word("conducive"), LinkType.ANTONYM)
        repo.addLink(a, word("hostile"), LinkType.SYNONYM)
        assertEquals(
            setOf("rel:antonym", "rel:synonym"),
            relationCards(a).map { it.templateId }.toSet(),
        )
    }

    @Test
    fun removingTheLastLinkRemovesTheRelationCard() {
        val a = word("precede")
        val b = word("concede")
        repo.addLink(a, b, LinkType.CONFUSABLE)
        assertEquals(1, relationCards(a).size)

        val link = repo.related(a).single().link
        repo.deleteLink(link.id)

        assertTrue(relationCards(a).isEmpty())
        assertTrue(relationCards(b).isEmpty())
    }

    @Test
    fun turningTheDeckOptionOffRemovesRelationCardsAndOnBringsThemBack() {
        val a = word("impose")
        repo.addLink(a, word("dispose"), LinkType.SAME_ROOT)
        assertEquals(1, relationCards(a).size)

        repo.saveDeck(repo.deck(deckId)!!.copy(relationQuiz = false))
        assertTrue(relationCards(a).isEmpty())
        assertEquals(listOf("en_ja"), repo.cardsOfNote(a).map { it.templateId })

        repo.saveDeck(repo.deck(deckId)!!.copy(relationQuiz = true))
        assertEquals(1, relationCards(a).size)
    }

    @Test
    fun aDeckWithoutTheOptionNeverGetsRelationCards() {
        val plainDeck = repo.saveDeck(
            Deck(
                name = "ふつう",
                noteTypeId = NoteType.ENGLISH.id,
                enabledTemplates = setOf("en_ja"),
                relationQuiz = false,
            )
        )
        val a = repo.saveNote(Note(deckId = plainDeck, typeId = NoteType.ENGLISH.id,
            fields = mapOf("word" to "retain", "meaning" to "保つ")))
        val b = repo.saveNote(Note(deckId = plainDeck, typeId = NoteType.ENGLISH.id,
            fields = mapOf("word" to "sustain", "meaning" to "支える")))
        repo.addLink(a, b, LinkType.SYNONYM)
        assertTrue(relationCards(a).isEmpty())
    }

    @Test
    fun relationCardsJoinTheStudyQueue() {
        val a = word("perspective")
        repo.addLink(a, word("retrospect"), LinkType.SAME_ROOT)
        val queue = repo.buildQueue(deckId)
        val templates = queue.mapNotNull { repo.card(it)?.templateId }
        assertTrue("relation card missing from queue: $templates", templates.any { it.startsWith("rel:") })
    }

    @Test
    fun deletingANoteTakesItsPartnersRelationCardWithIt() {
        val a = word("versatile")
        val b = word("divert")
        repo.addLink(a, b, LinkType.SAME_ROOT)
        assertEquals(1, relationCards(b).size)

        repo.deleteNote(a)
        // The link cascaded away, so b's relation question has no answers left.
        assertTrue(repo.related(b).isEmpty())
        assertEquals(0, repo.relationGroups(b).size)
    }

    @Test
    fun eisakubunChecklistComesFromTheStructuresField() {
        val deck = repo.saveDeck(
            Deck(
                name = "和文英訳",
                noteTypeId = NoteType.EISAKUBUN.id,
                enabledTemplates = setOf("ja_en_write"),
            )
        )
        val noteId = repo.saveNote(
            Note(
                deckId = deck,
                typeId = NoteType.EISAKUBUN.id,
                fields = mapOf(
                    "ja" to "彼の言うことは腑に落ちない。",
                    "en" to "What he says doesn't convince me.",
                    "structures" to "・「腑に落ちない」を言い換える\n- what 節で主語を作る\n\n",
                ),
            )
        )
        val card = repo.cardsOfNote(noteId).single()
        val rendered = repo.renderAnyCard(card, repo.note(noteId)!!)!!
        assertEquals(
            listOf("「腑に落ちない」を言い換える", "what 節で主語を作る"),
            rendered.checklist,
        )
        assertFalse(rendered.isRelation)
    }

    @Test
    fun calculationCardCarriesItsUnitAndTolerance() {
        val deck = repo.saveDeck(
            Deck(name = "計算", noteTypeId = NoteType.CHEM_CALC.id, enabledTemplates = setOf("calc"))
        )
        val noteId = repo.saveNote(
            Note(
                deckId = deck,
                typeId = NoteType.CHEM_CALC.id,
                fields = mapOf(
                    "question" to "5.6 L の酸素は何 mol か。",
                    "answer" to "0.25", "unit" to "mol", "tolerance" to "2",
                ),
            )
        )
        val rendered = repo.renderAnyCard(repo.cardsOfNote(noteId).single(), repo.note(noteId)!!)!!
        assertEquals("mol", rendered.unit)
        assertEquals(2.0, rendered.tolerancePercent, 1e-9)
        assertEquals("0.25", rendered.expectedAnswer)
    }
}
