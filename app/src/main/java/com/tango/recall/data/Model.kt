package com.tango.recall.data

import com.tango.recall.srs.SrsState

/** One editable field on a note. */
data class FieldDef(
    val id: String,
    val label: String,
    val hint: String = "",
    val multiline: Boolean = false,
)

/** How the learner is asked to produce the answer. */
enum class AnswerMode {
    /** Think, then tap to reveal. Cheap, good for recognition. */
    REVEAL,

    /** Type the answer; graded automatically. Forces production, not just recognition. */
    TYPE,

    /** The answer is blanked out inside a sentence and must be typed back in. */
    CLOZE,

    /**
     * Write a free answer, then grade it yourself against a model answer and a
     * checklist of the points that had to appear. Used for translation into English,
     * where no automatic comparison is honest.
     */
    SELF_CHECK,

    /** Enter a number; graded against the expected value within a tolerance. */
    NUMERIC,
}

/**
 * One direction a note can be asked in.
 *
 * A single note spawns several of these — that is the "互換" idea: 英→和 and 和→英
 * are scheduled as separate cards, because recognising a word and producing it are
 * genuinely different memories and decay at different rates.
 */
data class CardTemplate(
    val id: String,
    val label: String,
    val promptFields: List<String>,
    val answerFields: List<String>,
    val mode: AnswerMode,
    /** Field ids that must be non-blank for this card to exist. */
    val requires: List<String> = promptFields + answerFields,
    /** For [AnswerMode.CLOZE]: the sentence field shown with a blank in it. */
    val clozeSentenceField: String? = null,
    /** For [AnswerMode.CLOZE]: the field whose value is removed from the sentence. */
    val clozeAnswerField: String? = null,
    /** For [AnswerMode.SELF_CHECK]: field whose lines become the self-grading checklist. */
    val checklistField: String? = null,
    /** For [AnswerMode.NUMERIC]: fields holding the unit and the allowed error in percent. */
    val unitField: String? = null,
    val toleranceField: String? = null,
    /** Off by default — enabled per deck by the user. */
    val defaultEnabled: Boolean = true,
)

enum class NoteType(
    val id: String,
    val label: String,
    val fields: List<FieldDef>,
    val templates: List<CardTemplate>,
) {
    ENGLISH(
        id = "english",
        label = "英単語",
        fields = listOf(
            FieldDef("word", "語", "abandon"),
            FieldDef("meaning", "意味", "〜を見捨てる／放棄する"),
            FieldDef("pos", "品詞", "動 / 名 / 形 …"),
            FieldDef("root", "語根・語源", "ab-(離れて) + bandon(支配)"),
            FieldDef("example", "例文", "He abandoned his plan.", multiline = true),
            FieldDef("exampleJa", "例文訳", "彼は計画を放棄した。", multiline = true),
            FieldDef("collocation", "コロケーション", "abandon a plan / ship"),
            FieldDef("memo", "メモ", "", multiline = true),
        ),
        templates = listOf(
            CardTemplate("en_ja", "英 → 和", listOf("word"), listOf("meaning", "pos"), AnswerMode.REVEAL,
                requires = listOf("word", "meaning")),
            CardTemplate("ja_en", "和 → 英（入力）", listOf("meaning"), listOf("word"), AnswerMode.TYPE,
                requires = listOf("word", "meaning")),
            CardTemplate("cloze", "例文穴埋め", listOf("example"), listOf("word"), AnswerMode.CLOZE,
                requires = listOf("word", "example"),
                clozeSentenceField = "example", clozeAnswerField = "word"),
            CardTemplate("root_word", "語源 → 語", listOf("root"), listOf("word", "meaning"), AnswerMode.REVEAL,
                requires = listOf("root", "word"), defaultEnabled = false),
            CardTemplate("collo", "コロケーション → 語", listOf("collocation"), listOf("word"), AnswerMode.TYPE,
                requires = listOf("collocation", "word"), defaultEnabled = false),
            CardTemplate("ja_en_sentence", "例文の和訳 → 英訳を書く", listOf("exampleJa"), listOf("example"),
                AnswerMode.SELF_CHECK,
                requires = listOf("example", "exampleJa"), defaultEnabled = false),
        ),
    ),

    CHEM_SUBSTANCE(
        id = "chem_substance",
        label = "化学・物質",
        fields = listOf(
            FieldDef("name", "名称", "硫酸銅(II)五水和物"),
            FieldDef("formula", "化学式", "CuSO4·5H2O"),
            FieldDef("category", "分類", "無機 / 塩 / 錯体 …"),
            FieldDef("props", "性質・特徴", "青色結晶。加熱で白色無水物。", multiline = true),
            FieldDef("uses", "反応・用途", "水の検出、電気分解の電解液", multiline = true),
            FieldDef("memo", "メモ", "", multiline = true),
        ),
        templates = listOf(
            CardTemplate("name_formula", "名称 → 化学式（入力）", listOf("name"), listOf("formula"), AnswerMode.TYPE,
                requires = listOf("name", "formula")),
            CardTemplate("formula_name", "化学式 → 名称", listOf("formula"), listOf("name"), AnswerMode.REVEAL,
                requires = listOf("name", "formula")),
            CardTemplate("name_props", "名称 → 性質・特徴", listOf("name"), listOf("props", "uses"), AnswerMode.REVEAL,
                requires = listOf("name", "props")),
        ),
    ),

    CHEM_REACTION(
        id = "chem_reaction",
        label = "化学・反応",
        fields = listOf(
            FieldDef("title", "反応名", "接触法（硫酸の製造）"),
            FieldDef("equation", "反応式", "2SO2 + O2 → 2SO3", multiline = true),
            FieldDef("condition", "条件・触媒", "V2O5 触媒、約450℃"),
            FieldDef("point", "ポイント", "SO3 は濃硫酸に吸収させる", multiline = true),
            FieldDef("memo", "メモ", "", multiline = true),
        ),
        templates = listOf(
            CardTemplate("title_eq", "反応名 → 反応式（入力）", listOf("title"), listOf("equation"), AnswerMode.TYPE,
                requires = listOf("title", "equation")),
            CardTemplate("eq_title", "反応式 → 反応名", listOf("equation"), listOf("title", "condition"), AnswerMode.REVEAL,
                requires = listOf("title", "equation")),
            CardTemplate("title_cond", "反応名 → 条件・触媒", listOf("title"), listOf("condition", "point"), AnswerMode.REVEAL,
                requires = listOf("title", "condition")),
        ),
    ),

    /**
     * Translation into English.
     *
     * Kyoto University's paper has carried a 和文英訳 question every single year, and
     * the hard part is never vocabulary — it is rephrasing Japanese that cannot be
     * translated literally. So the note keeps that rephrasing explicit, and grading is
     * a checklist of the points that had to appear rather than a string comparison.
     */
    EISAKUBUN(
        id = "eisakubun",
        label = "和文英訳",
        fields = listOf(
            FieldDef("ja", "日本語文", "彼の言うことは、どうも腑に落ちない。", multiline = true),
            FieldDef("en", "模範英訳", "Something about what he says doesn't quite convince me.", multiline = true),
            FieldDef(
                "structures", "押さえる点（1行に1つ）",
                "「腑に落ちない」→ doesn't convince me と言い換える\nsomething about 〜 を使う",
                multiline = true,
            ),
            FieldDef("traps", "直訳できない箇所", "「腑に落ちない」をそのまま訳そうとしない", multiline = true),
            FieldDef("memo", "メモ", "", multiline = true),
        ),
        templates = listOf(
            CardTemplate(
                "ja_en_write", "和文英訳を書く", listOf("ja"), listOf("en", "traps"), AnswerMode.SELF_CHECK,
                requires = listOf("ja", "en"), checklistField = "structures",
            ),
            CardTemplate(
                "trap_only", "言い換えのポイントだけ確認", listOf("ja"), listOf("traps", "structures"),
                AnswerMode.REVEAL, requires = listOf("ja", "traps"), defaultEnabled = false,
            ),
        ),
    ),

    /** A chemistry problem with a numeric answer, graded within a tolerance. */
    CHEM_CALC(
        id = "chem_calc",
        label = "化学・計算",
        fields = listOf(
            FieldDef("question", "問題", "0.10 mol/L の酢酸水溶液の pH。Ka = 2.7×10^-5", multiline = true),
            FieldDef("answer", "答え（数値）", "2.8"),
            FieldDef("unit", "単位", "mol/L, g, pH など（無次元なら空欄）"),
            FieldDef("tolerance", "許容誤差（%）", "既定は 1"),
            FieldDef("solution", "解き方", "弱酸の電離: [H+] = √(Ka·c)", multiline = true),
            FieldDef("memo", "メモ", "", multiline = true),
        ),
        templates = listOf(
            CardTemplate(
                "calc", "計算して答える", listOf("question"), listOf("answer", "solution"), AnswerMode.NUMERIC,
                requires = listOf("question", "answer"),
                unitField = "unit", toleranceField = "tolerance",
            ),
            CardTemplate(
                "method", "解き方を思い出す", listOf("question"), listOf("solution"), AnswerMode.REVEAL,
                requires = listOf("question", "solution"), defaultEnabled = false,
            ),
        ),
    ),

    BASIC(
        id = "basic",
        label = "自由形式",
        fields = listOf(
            FieldDef("front", "表", "", multiline = true),
            FieldDef("back", "裏", "", multiline = true),
            FieldDef("hint", "ヒント", ""),
            FieldDef("memo", "メモ", "", multiline = true),
        ),
        templates = listOf(
            CardTemplate("fb", "表 → 裏", listOf("front"), listOf("back"), AnswerMode.REVEAL),
            CardTemplate("bf", "裏 → 表", listOf("back"), listOf("front"), AnswerMode.REVEAL,
                defaultEnabled = false),
        ),
    );

    fun field(id: String): FieldDef? = fields.firstOrNull { it.id == id }
    fun template(id: String): CardTemplate? = templates.firstOrNull { it.id == id }
    fun label(fieldId: String): String = field(fieldId)?.label ?: fieldId

    companion object {
        fun fromId(id: String): NoteType = entries.firstOrNull { it.id == id } ?: BASIC
    }
}

/**
 * A typed relation between two notes.
 *
 * Relations are the point of the app: recall gets much easier when an item hangs off
 * something you already know, so every review shows the neighbours of the card you
 * just answered.
 */
enum class LinkType(
    val id: String,
    val forward: String,
    val reverse: String,
    val symmetric: Boolean = true,
) {
    SAME_ROOT("same_root", "同語根", "同語根"),
    SYNONYM("synonym", "類義", "類義"),
    ANTONYM("antonym", "対義", "対義"),
    DERIVED("derived", "派生語", "派生元", symmetric = false),
    CONFUSABLE("confusable", "混同注意", "混同注意"),
    HYPERNYM("hypernym", "上位概念", "具体例", symmetric = false),
    REACTS_WITH("reacts_with", "反応する相手", "反応する相手"),
    PRODUCES("produces", "生成する", "から作られる", symmetric = false),
    SAME_GROUP("same_group", "同族・同分類", "同族・同分類"),
    CONTRAST("contrast", "対比", "対比"),
    RELATED("related", "関連", "関連");

    companion object {
        fun fromId(id: String): LinkType = entries.firstOrNull { it.id == id } ?: RELATED

        fun forNoteType(type: NoteType): List<LinkType> = when (type) {
            NoteType.ENGLISH -> listOf(SAME_ROOT, SYNONYM, ANTONYM, DERIVED, CONFUSABLE, CONTRAST, RELATED)
            NoteType.CHEM_SUBSTANCE -> listOf(REACTS_WITH, PRODUCES, SAME_GROUP, CONTRAST, CONFUSABLE, HYPERNYM, RELATED)
            NoteType.CHEM_REACTION -> listOf(PRODUCES, SAME_GROUP, CONTRAST, RELATED)
            NoteType.EISAKUBUN -> listOf(SAME_GROUP, CONTRAST, CONFUSABLE, RELATED)
            NoteType.CHEM_CALC -> listOf(SAME_GROUP, CONTRAST, RELATED)
            NoteType.BASIC -> entries
        }
    }
}

data class Deck(
    val id: Long = 0,
    val name: String,
    val noteTypeId: String,
    val enabledTemplates: Set<String>,
    val newPerDay: Int = 20,
    /** Whether this deck also drills the relations its notes take part in. */
    val relationQuiz: Boolean = false,
    val created: Long = System.currentTimeMillis(),
) {
    val noteType: NoteType get() = NoteType.fromId(noteTypeId)
}

data class Note(
    val id: Long = 0,
    val deckId: Long,
    val typeId: String,
    val fields: Map<String, String>,
    val tags: List<String> = emptyList(),
    val created: Long = System.currentTimeMillis(),
    val modified: Long = System.currentTimeMillis(),
) {
    val type: NoteType get() = NoteType.fromId(typeId)

    operator fun get(fieldId: String): String = fields[fieldId].orEmpty()

    /** Short label used in lists, link chips and search results. */
    fun title(): String = when (type) {
        NoteType.ENGLISH -> this["word"]
        NoteType.CHEM_SUBSTANCE -> this["name"]
        NoteType.CHEM_REACTION -> this["title"]
        NoteType.EISAKUBUN -> this["ja"]
        NoteType.CHEM_CALC -> this["question"]
        NoteType.BASIC -> this["front"]
    }.ifBlank { type.fields.firstNotNullOfOrNull { fields[it.id]?.ifBlank { null } } ?: "(空)" }

    fun subtitle(): String = when (type) {
        NoteType.ENGLISH -> this["meaning"]
        NoteType.CHEM_SUBSTANCE -> this["formula"]
        NoteType.CHEM_REACTION -> this["equation"]
        NoteType.EISAKUBUN -> this["en"]
        NoteType.CHEM_CALC -> listOf(this["answer"], this["unit"]).filter { it.isNotBlank() }.joinToString(" ")
        NoteType.BASIC -> this["back"]
    }
}

data class Card(
    val id: Long = 0,
    val noteId: Long,
    val deckId: Long,
    val templateId: String,
    val srs: SrsState = SrsState(),
    val suspended: Boolean = false,
    /**
     * True when the app suspended this card itself — its direction was switched off,
     * or the field it asks about was emptied. Such a card keeps its review history and
     * comes back the moment the direction applies again; a card the learner suspended
     * by hand stays suspended.
     */
    val autoSuspended: Boolean = false,
)

data class NoteLink(
    val id: Long = 0,
    val fromNoteId: Long,
    val toNoteId: Long,
    val typeId: String,
    val memo: String = "",
) {
    val type: LinkType get() = LinkType.fromId(typeId)
}

/** A link as seen from one particular note, with the correct direction label. */
data class RelatedNote(
    val link: NoteLink,
    val other: Note,
    val label: String,
)

data class ReviewEntry(
    val cardId: Long,
    val noteId: Long,
    val deckId: Long,
    val rating: Int,
    val ts: Long,
    val phase: String,
    val stability: Double,
    val difficulty: Double,
    val tookMs: Long,
)

/**
 * Cards generated from the relation graph rather than from a note's own fields.
 *
 * One card per (note, relation type) rather than one per link: asking "which words
 * share this root?" and listing all of them is both a fairer question and far fewer
 * cards than asking about each pair separately.
 */
object RelationCards {
    const val PREFIX = "rel:"
    private const val REVERSE_SUFFIX = ":r"

    /**
     * Asymmetric relations read differently from each end — "生成する" vs "から作られる" —
     * so they get a card per direction. Symmetric ones read the same either way and
     * get a single card.
     */
    fun templateId(type: LinkType, reverse: Boolean): String =
        PREFIX + type.id + if (reverse && !type.symmetric) REVERSE_SUFFIX else ""

    fun isRelationCard(templateId: String): Boolean = templateId.startsWith(PREFIX)

    fun parse(templateId: String): Pair<LinkType, Boolean>? {
        if (!isRelationCard(templateId)) return null
        val body = templateId.removePrefix(PREFIX)
        val reverse = body.endsWith(REVERSE_SUFFIX)
        return LinkType.fromId(body.removeSuffix(REVERSE_SUFFIX)) to reverse
    }

    fun label(type: LinkType, reverse: Boolean): String = if (reverse) type.reverse else type.forward
}
