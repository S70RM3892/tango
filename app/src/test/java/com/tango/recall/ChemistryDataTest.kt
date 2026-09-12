package com.tango.recall

import androidx.test.core.app.ApplicationProvider
import com.tango.recall.data.NoteType
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
 * Every reaction shipped with the app has to balance.
 *
 * A wrong equation in the starter content is worse than no equation at all — it is
 * memorised as written — and a coefficient is exactly the sort of thing that is easy
 * to get wrong by hand and impossible to spot by reading. So the equations are parsed
 * and counted here rather than trusted.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ChemistryDataTest {

    private lateinit var repo: Repository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        context.deleteDatabase(TangoDb.DB_NAME)
        repo = Repository(TangoDb(context))
        Seed.populate(repo)
    }

    @Test
    fun everySeededEquationBalances() {
        val reactions = repo.listNotes(null, "", Int.MAX_VALUE)
            .filter { it.type == NoteType.CHEM_REACTION }
        assertTrue("there should be reactions to check", reactions.size >= 8)

        val problems = mutableListOf<String>()
        var checked = 0
        for (note in reactions) {
            // Polymerisation is written with n repeating units, which is not something
            // atom counting can check; those notes say so with a tag.
            if ("重合" in note.tags) continue
            for (line in note["equation"].lines()) {
                val equation = line.trim()
                if (equation.isEmpty() || !equation.contains(Regex("→|⇄|->"))) continue
                checked++
                val (left, right) = Equation.sides(equation)
                if (left != right) {
                    problems += "${note.title()}: $equation\n  左 $left\n  右 $right"
                }
            }
        }
        assertTrue("every line should be an equation", checked >= 10)
        assertTrue("釣り合っていない式:\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun theParserItselfIsRight() {
        assertEquals(mapOf("H" to 2, "O" to 1), Equation.atoms("H2O"))
        assertEquals(mapOf("H" to 4, "O" to 2), Equation.atoms("2H2O"))
        assertEquals(mapOf("Ca" to 1, "O" to 2, "H" to 2), Equation.atoms("Ca(OH)2"))
        assertEquals(mapOf("Al" to 2, "S" to 3, "O" to 12), Equation.atoms("Al2(SO4)3"))
        assertEquals(mapOf("Cu" to 1, "S" to 1, "O" to 9, "H" to 10), Equation.atoms("CuSO4.5H2O"))
        assertEquals(mapOf("C" to 2, "H" to 4), Equation.atoms("CH2=CH2"))
        assertEquals(mapOf("C" to 2, "H" to 6, "O" to 1), Equation.atoms("CH3-CH2-OH"))
        assertEquals(mapOf("C" to 4, "H" to 8, "O" to 2), Equation.atoms("CH3COOC2H5"))
        assertEquals(mapOf("C" to 6, "H" to 6, "O" to 1), Equation.atoms("C6H5OH"))
        assertEquals(mapOf("N" to 1, "H" to 4, "Cl" to 1), Equation.atoms("NH4Cl"))
        assertEquals(mapOf("Cu" to 1, "N" to 4, "H" to 12, "charge" to 2), Equation.atoms("[Cu(NH3)4]^2+"))
        assertEquals(mapOf("Na" to 1, "charge" to 1), Equation.atoms("Na^+"))
    }

    @Test
    fun anUnbalancedEquationIsCaught() {
        val (left, right) = Equation.sides("H2 + O2 → H2O")
        assertTrue("the check has to actually fail on a wrong equation", left != right)
    }
}

/** Atom counting, just enough to check the equations this app ships. */
object Equation {

    /** Total atoms (and charge) on each side of [equation]. */
    fun sides(equation: String): Pair<Map<String, Int>, Map<String, Int>> {
        val halves = equation.split("→", "⇄", "->").map { it.trim() }
        require(halves.size == 2) { "not a single equation: $equation" }
        return total(halves[0]) to total(halves[1])
    }

    private fun total(side: String): Map<String, Int> {
        val out = HashMap<String, Int>()
        for (species in side.split(" + ")) {
            val term = species.trim().substringBefore("（").trim()
            if (term.isEmpty()) continue
            for ((element, n) in atoms(term)) out[element] = (out[element] ?: 0) + n
        }
        return out.filterValues { it != 0 }
    }

    /** Atoms in one species, including any leading coefficient. */
    fun atoms(species: String): Map<String, Int> {
        val text = species.replace(" ", "").replace("−", "-")
        val out = HashMap<String, Int>()
        var i = 0

        fun number(): Int? {
            val start = i
            while (i < text.length && text[i].isDigit()) i++
            return if (i == start) null else text.substring(start, i).toInt()
        }

        val coefficient = number() ?: 1

        fun add(element: String, n: Int) {
            out[element] = (out[element] ?: 0) + n
        }

        fun parse(multiplier: Int, stopAtClose: Boolean) {
            while (i < text.length) {
                val c = text[i]
                when {
                    c == '(' || c == '[' -> {
                        i++
                        val inner = HashMap<String, Int>()
                        val outer = HashMap(out)
                        out.clear()
                        parse(1, stopAtClose = true)
                        inner.putAll(out)
                        out.clear()
                        out.putAll(outer)
                        val group = number() ?: 1
                        for ((e, n) in inner) add(e, n * group * multiplier)
                    }

                    c == ')' || c == ']' -> {
                        i++
                        if (stopAtClose) return
                    }

                    // A hydrate: everything after the dot has its own coefficient.
                    c == '.' || c == '·' || c == '・' -> {
                        i++
                        val each = number() ?: 1
                        parse(multiplier * each, stopAtClose)
                        return
                    }

                    c == '^' -> {
                        i++
                        val size = number() ?: 1
                        val sign = if (i < text.length && text[i] == '-') -1 else 1
                        if (i < text.length && (text[i] == '+' || text[i] == '-')) i++
                        add("charge", size * sign * multiplier)
                    }

                    c == '+' || c == '-' -> {
                        // A trailing sign with no caret is a single charge.
                        if (i == text.length - 1) add("charge", (if (c == '+') 1 else -1) * multiplier)
                        i++
                    }

                    c.isUpperCase() -> {
                        val start = i
                        i++
                        while (i < text.length && text[i].isLowerCase()) i++
                        val element = text.substring(start, i)
                        add(element, (number() ?: 1) * multiplier)
                    }

                    else -> i++ // bonds, brackets of other kinds, stray marks
                }
            }
        }

        parse(coefficient, stopAtClose = false)
        return out.filterValues { it != 0 }
    }
}
