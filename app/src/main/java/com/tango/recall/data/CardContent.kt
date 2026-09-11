package com.tango.recall.data

import com.tango.recall.srs.Rating
import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min

const val CLOZE_BLANK = "______"

/** Everything the review screen needs to draw one card. */
data class RenderedCard(
    val card: Card,
    val note: Note,
    val template: CardTemplate,
    val promptLabel: String,
    val promptText: String,
    /** Extra context shown with the question (品詞, 分類 …). */
    val promptExtras: List<Pair<String, String>>,
    val answerParts: List<Pair<String, String>>,
    val mode: AnswerMode,
) {
    /** The single string a typed answer is compared against. */
    val expectedAnswer: String get() = answerParts.firstOrNull()?.second.orEmpty()
}

fun renderCard(card: Card, note: Note): RenderedCard? {
    val template = note.type.template(card.templateId) ?: return null
    val type = note.type

    val answerParts = template.answerFields
        .mapNotNull { id -> note[id].takeIf { it.isNotBlank() }?.let { type.label(id) to it } }

    if (template.mode == AnswerMode.CLOZE) {
        val sentence = note[template.clozeSentenceField ?: return null]
        val target = note[template.clozeAnswerField ?: return null]
        if (sentence.isBlank() || target.isBlank()) return null
        return RenderedCard(
            card = card,
            note = note,
            template = template,
            promptLabel = "空所に入る語は？",
            promptText = blankOut(sentence, target),
            promptExtras = listOfNotNull(
                note["exampleJa"].takeIf { it.isNotBlank() }?.let { "訳" to it },
                note["meaning"].takeIf { it.isNotBlank() }?.let { "意味" to it },
            ),
            answerParts = listOf(type.label(template.clozeAnswerField) to target) +
                (note["exampleJa"].takeIf { it.isNotBlank() }?.let { listOf("訳" to it) } ?: emptyList()),
            mode = AnswerMode.CLOZE,
        )
    }

    val promptField = template.promptFields.first()
    val promptText = note[promptField]
    if (promptText.isBlank() || answerParts.isEmpty()) return null

    val extras = template.promptFields.drop(1)
        .mapNotNull { id -> note[id].takeIf { it.isNotBlank() }?.let { type.label(id) to it } }

    return RenderedCard(
        card = card,
        note = note,
        template = template,
        promptLabel = type.label(promptField),
        promptText = promptText,
        promptExtras = extras,
        answerParts = answerParts,
        mode = template.mode,
    )
}

/**
 * Replace [target] inside [sentence] with a blank.
 *
 * Falls back to stem matching so "abandon" still blanks "abandoned" / "abandoning".
 */
fun blankOut(sentence: String, target: String): String {
    val t = target.trim()
    if (t.isEmpty()) return sentence

    val direct = Regex("(?i)\\b${Regex.escape(t)}\\b")
    if (direct.containsMatchIn(sentence)) return direct.replace(sentence, CLOZE_BLANK)

    val stemLength = max(4, t.length - 3)
    if (t.length >= 4) {
        val stem = t.take(stemLength).lowercase()
        val tokens = Regex("[A-Za-z]+")
        if (tokens.findAll(sentence).any { it.value.lowercase().startsWith(stem) }) {
            return tokens.replace(sentence) { m ->
                if (m.value.lowercase().startsWith(stem)) CLOZE_BLANK else m.value
            }
        }
    }
    // Non-alphabetic content (e.g. Japanese or a formula): plain substring removal.
    return if (sentence.contains(t)) sentence.replace(t, CLOZE_BLANK)
    else "$sentence\n（$CLOZE_BLANK）"
}

enum class Grade { CORRECT, CLOSE, WRONG }

data class GradeResult(
    val grade: Grade,
    val expected: String,
    val comment: String,
) {
    /** Pre-selected button; the learner can still override it. */
    val suggestedRating: Rating
        get() = when (grade) {
            Grade.CORRECT -> Rating.GOOD
            Grade.CLOSE -> Rating.HARD
            Grade.WRONG -> Rating.AGAIN
        }
}

/**
 * Grade a typed answer.
 *
 * Accepts several alternatives separated by `/`, `,` or `；`, ignores surrounding
 * articles and punctuation, and reports a near-miss separately so a one-letter typo
 * isn't scored the same as a blank.
 */
fun gradeTyped(input: String, expected: String, chemistry: Boolean = false): GradeResult {
    val alternatives = expected.split("/", ",", "、", "；", ";")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .ifEmpty { listOf(expected.trim()) }

    val typed = input.trim()
    if (typed.isEmpty()) return GradeResult(Grade.WRONG, expected, "未入力")

    val normTyped = normalize(typed, chemistry)

    for (alt in alternatives) {
        if (typed == alt) return GradeResult(Grade.CORRECT, expected, "正解")
    }
    for (alt in alternatives) {
        if (normTyped == normalize(alt, chemistry)) {
            val comment = if (chemistry) "正解（表記ゆれを許容：元素記号の大文字・小文字に注意）" else "正解"
            return GradeResult(Grade.CORRECT, expected, comment)
        }
    }

    val best = alternatives.minOf { levenshtein(normTyped, normalize(it, chemistry)) }
    val tolerance = if (chemistry) 1 else min(2, max(1, normTyped.length / 4))
    return if (best <= tolerance) {
        GradeResult(Grade.CLOSE, expected, "惜しい（${best}文字違い）")
    } else {
        GradeResult(Grade.WRONG, expected, "不正解")
    }
}

private fun normalize(s: String, chemistry: Boolean): String {
    var t = Normalizer.normalize(s, Normalizer.Form.NFKC).trim()
    if (chemistry) {
        // Subscript digits, centre dots and arrows all have several common spellings.
        t = t.replace("[·・•∙]".toRegex(), ".")
            .replace("[→⟶=]+>?".toRegex(), "->")
            .replace("[₀₁₂₃₄₅₆₇₈₉]".toRegex()) { m -> ('0' + ("₀₁₂₃₄₅₆₇₈₉".indexOf(m.value[0]))).toString() }
            .replace("\\s+".toRegex(), "")
        return t
    }
    t = t.lowercase()
        .replace("[.,!?;:\"'’”“()\\[\\]]".toRegex(), "")
        .replace("\\s+".toRegex(), " ")
        .trim()
    // "to abandon" and "abandon" are the same answer.
    for (prefix in listOf("to ", "a ", "an ", "the ")) {
        if (t.startsWith(prefix)) t = t.removePrefix(prefix)
    }
    return t
}

internal fun levenshtein(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length
    var prev = IntArray(b.length + 1) { it }
    var cur = IntArray(b.length + 1)
    for (i in 1..a.length) {
        cur[0] = i
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            cur[j] = minOf(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
        }
        val tmp = prev; prev = cur; cur = tmp
    }
    return prev[b.length]
}

/** "3日", "10分" … used on the grading buttons and in card lists. */
fun humanDelay(ms: Long): String {
    val minutes = ms / 60_000.0
    return when {
        minutes < 1 -> "<1分"
        minutes < 60 -> "${minutes.toInt()}分"
        minutes < 60 * 24 -> "${(minutes / 60).toInt()}時間"
        minutes < 60 * 24 * 30 -> "${(minutes / (60 * 24)).toInt()}日"
        minutes < 60 * 24 * 365 -> "${"%.1f".format(minutes / (60 * 24 * 30))}か月"
        else -> "${"%.1f".format(minutes / (60 * 24 * 365))}年"
    }
}
