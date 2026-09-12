package com.tango.recall.data

import com.tango.recall.srs.Rating
import java.text.Normalizer
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
    /** For [AnswerMode.SELF_CHECK]: the points the answer had to contain. */
    val checklist: List<String> = emptyList(),
    /** For [AnswerMode.NUMERIC]: shown beside the input so the unit need not be typed. */
    val unit: String = "",
    val tolerancePercent: Double = DEFAULT_TOLERANCE_PERCENT,
    /** True when the question came from the relation graph rather than the note's fields. */
    val isRelation: Boolean = false,
) {
    /** The single string a typed answer is compared against. */
    val expectedAnswer: String get() = answerParts.firstOrNull()?.second.orEmpty()
}

const val DEFAULT_TOLERANCE_PERCENT = 1.0

private val DEFAULT_CHECKLIST = listOf("意味が正しく伝わっているか", "文法・語法に誤りがないか")

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

    val checklist = if (template.mode == AnswerMode.SELF_CHECK) {
        template.checklistField
            ?.let { note[it] }
            ?.lines()
            ?.map { it.trim().removePrefix("・").removePrefix("-").trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
            .ifEmpty { DEFAULT_CHECKLIST }
    } else emptyList()

    return RenderedCard(
        card = card,
        note = note,
        template = template,
        promptLabel = type.label(promptField),
        promptText = promptText,
        promptExtras = extras,
        answerParts = answerParts,
        mode = template.mode,
        checklist = checklist,
        unit = template.unitField?.let { note[it] }.orEmpty().trim(),
        tolerancePercent = template.toleranceField
            ?.let { note[it] }
            ?.trim()
            ?.toDoubleOrNull()
            ?.takeIf { it > 0 }
            ?: DEFAULT_TOLERANCE_PERCENT,
    )
}

/**
 * Build a question out of the relation graph: "which notes hang off this one by
 * [linkType]?" The answer is every partner at once, which is both a fairer question
 * than picking one of them and far fewer cards than one per link.
 */
fun renderRelationCard(
    card: Card,
    note: Note,
    linkType: LinkType,
    reverse: Boolean,
    partners: List<RelatedNote>,
): RenderedCard? {
    if (partners.isEmpty()) return null
    val relationLabel = RelationCards.label(linkType, reverse)
    val template = CardTemplate(
        id = RelationCards.templateId(linkType, reverse),
        label = "つながり: $relationLabel",
        promptFields = emptyList(),
        answerFields = emptyList(),
        mode = AnswerMode.REVEAL,
        requires = emptyList(),
    )
    return RenderedCard(
        card = card,
        note = note,
        template = template,
        promptLabel = "「$relationLabel」でつながるものは？（${partners.size} 件）",
        promptText = note.title(),
        promptExtras = listOfNotNull(
            note.subtitle().takeIf { it.isNotBlank() }?.let { note.type.label(note.type.fields[1].id) to it },
        ),
        answerParts = partners.map { p ->
            val caption = p.link.memo.ifBlank { p.label }
            val body = listOf(p.other.title(), p.other.subtitle())
                .filter { it.isNotBlank() }
                .joinToString(" — ")
            caption to body
        },
        mode = AnswerMode.REVEAL,
        isRelation = true,
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

    val normTyped = normalizeAnswer(typed, chemistry)

    for (alt in alternatives) {
        if (typed == alt) return GradeResult(Grade.CORRECT, expected, "正解")
    }
    for (alt in alternatives) {
        if (normTyped == normalizeAnswer(alt, chemistry)) {
            val comment = if (chemistry) "正解（表記ゆれを許容：元素記号の大文字・小文字に注意）" else "正解"
            return GradeResult(Grade.CORRECT, expected, comment)
        }
    }

    val best = alternatives.minOf { levenshtein(normTyped, normalizeAnswer(it, chemistry)) }
    val tolerance = if (chemistry) 1 else min(2, max(1, normTyped.length / 4))
    return if (best <= tolerance) {
        GradeResult(Grade.CLOSE, expected, "惜しい（${best}文字違い）")
    } else {
        GradeResult(Grade.WRONG, expected, "不正解")
    }
}

internal fun normalizeAnswer(s: String, chemistry: Boolean): String {
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

// ---- numeric answers --------------------------------------------------------

/**
 * Read a number the way a chemistry answer is actually written: `2.8`, `1.2e-3`,
 * `1.2×10^-3`, `1.2*10^-3`, full-width digits, thousands separators.
 */
fun parseNumber(text: String): Double? {
    var t = Normalizer.normalize(text, Normalizer.Form.NFKC).trim()
        .replace(",", "")
        .replace("\\s+".toRegex(), "")
        .replace("−", "-")
        .replace("ー", "-")
    if (t.isEmpty()) return null
    // "1.2×10^-3" / "1.2*10-3" style scientific notation.
    t = t.replace("[×xX*・]10\\^?([+-]?\\d+)".toRegex(), "e$1")
    // A bare power of ten with no mantissa.
    t = t.replace("^10\\^([+-]?\\d+)$".toRegex(), "1e$1")
    t = t.replace("\\^".toRegex(), "e")
    return t.toDoubleOrNull()
}

/** Significant digits in a written number, used only to comment on the answer. */
internal fun significantDigits(text: String): Int {
    val mantissa = Normalizer.normalize(text, Normalizer.Form.NFKC).trim()
        .substringBefore("e").substringBefore("E").substringBefore("×").substringBefore("x")
        .replace("-", "").replace("+", "")
    val digits = mantissa.filter { it.isDigit() || it == '.' }
    if (digits.isEmpty()) return 0
    val stripped = digits.replace(".", "").trimStart('0')
    return if (stripped.isEmpty()) 1 else stripped.length
}

/**
 * Grade a numeric answer within [tolerancePercent].
 *
 * An answer that is right except for a power of ten is reported as wrong — in
 * chemistry it is — but the message says so explicitly, because the method was sound
 * and that is the useful thing to know.
 */
fun gradeNumeric(input: String, expectedText: String, tolerancePercent: Double, unit: String = ""): GradeResult {
    val expected = parseNumber(expectedText)
        ?: return gradeTyped(input, expectedText, chemistry = true)
    val shown = listOf(expectedText.trim(), unit).filter { it.isNotBlank() }.joinToString(" ")

    val got = parseNumber(input)
        ?: return GradeResult(Grade.WRONG, shown, if (input.isBlank()) "未入力" else "数値として読み取れません")

    val tolerance = (tolerancePercent / 100.0).coerceAtLeast(0.0)
    val error = if (expected == 0.0) abs(got) else abs(got - expected) / abs(expected)

    if (error <= tolerance) {
        val wanted = significantDigits(expectedText)
        val given = significantDigits(input)
        val note = if (wanted in 1..9 && given != wanted) "正解（有効数字は $wanted 桁で答えるのが自然です）" else "正解"
        return GradeResult(Grade.CORRECT, shown, note)
    }

    if (expected != 0.0 && got != 0.0 && (got > 0) == (expected > 0)) {
        val exponent = log10(abs(got / expected))
        val rounded = exponent.roundToInt()
        if (rounded != 0 && abs(exponent - rounded) < 0.02) {
            val sign = if (rounded > 0) "+" else ""
            return GradeResult(Grade.WRONG, shown, "数値は合っていますが桁が違います（10^$sign$rounded 倍）")
        }
    }

    return if (error <= tolerance * 10) {
        GradeResult(Grade.CLOSE, shown, "惜しい（誤差 ${"%.1f".format(error * 100)}%）")
    } else {
        GradeResult(Grade.WRONG, shown, "不正解（誤差 ${"%.0f".format(error * 100)}%）")
    }
}

// ---- self-graded answers ----------------------------------------------------

/** Turn "3 of the 4 points were covered" into a grade and a suggested button. */
fun gradeSelfCheck(checked: Int, total: Int): GradeResult {
    if (total <= 0) return GradeResult(Grade.CLOSE, "", "自分で評価してください")
    val ratio = checked.toDouble() / total
    val grade = when {
        ratio >= 1.0 -> Grade.CORRECT
        ratio >= 0.5 -> Grade.CLOSE
        else -> Grade.WRONG
    }
    return GradeResult(grade, "", "$total 点中 $checked 点を押さえられました")
}
