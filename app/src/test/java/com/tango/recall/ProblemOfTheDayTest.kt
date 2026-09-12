package com.tango.recall

import androidx.test.core.app.ApplicationProvider
import com.tango.recall.data.NoteType
import com.tango.recall.data.Repository
import com.tango.recall.data.Seed
import com.tango.recall.data.TangoDb
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
 * The one problem a day.
 *
 * It has to be the same problem all day — otherwise it cannot be turned over between
 * other things — and it has to move on the next day, or it stops being a rotation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProblemOfTheDayTest {

    private lateinit var repo: Repository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        context.deleteDatabase(TangoDb.DB_NAME)
        repo = Repository(TangoDb(context))
    }

    private val noon = 1_800_000_000_000L
    private val day = 86_400_000L

    @Test
    fun nothingToPoseWhenTheCollectionIsEmpty() {
        assertNull(repo.problemOfTheDay(noon))
    }

    @Test
    fun itIsTheSameProblemAllDayAndAnotherOneTomorrow() {
        Seed.populate(repo)
        val morning = repo.problemOfTheDay(noon)
        val evening = repo.problemOfTheDay(noon + 6 * 3_600_000L)
        assertNotNull(morning)
        assertEquals("re-opening the app must not re-roll it", morning!!.id, evening!!.id)

        val tomorrow = repo.problemOfTheDay(noon + day)
        assertTrue("it has to move on", tomorrow!!.id != morning.id)
    }

    @Test
    fun itIsAlwaysSomethingToWorkThrough() {
        Seed.populate(repo)
        val types = (0 until 30).mapNotNull { repo.problemOfTheDay(noon + it * day)?.type }.toSet()
        assertTrue("問題型のノートだけが選ばれること", types.all { it in Repository.PROBLEM_TYPES })
        assertTrue("数学が回ってくること", NoteType.MATH in types)
    }

    @Test
    fun theWholeProblemCanBeStudiedOnItsOwn() {
        Seed.populate(repo)
        val problem = repo.problemOfTheDay(noon)!!
        val queue = repo.queueForNote(problem.id)
        assertTrue("その1問のカードだけが並ぶこと", queue.isNotEmpty())
        assertTrue(queue.all { repo.card(it)!!.noteId == problem.id })
    }
}
