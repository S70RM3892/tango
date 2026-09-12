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

/**
 * The subject a note belongs to.
 *
 * Note types are fine-grained on purpose — a chemical substance and a calculation are
 * asked in completely different ways — but nobody studies "化学・物質", they study
 * chemistry. The subject is what the connection map and the deck lists group by.
 */
enum class Subject(val id: String, val label: String) {
    ENGLISH("english", "英語"),
    CHEMISTRY("chemistry", "化学"),
    MATH("math", "数学"),
    PHYSICS("physics", "物理"),
    OTHER("other", "その他");

    companion object {
        fun fromId(id: String): Subject = entries.firstOrNull { it.id == id } ?: OTHER
    }
}

enum class NoteType(
    val id: String,
    val label: String,
    val subject: Subject,
    val fields: List<FieldDef>,
    val templates: List<CardTemplate>,
) {
    ENGLISH(
        id = "english",
        label = "英単語",
        subject = Subject.ENGLISH,
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

    /**
     * 熟語.
     *
     * A phrasal verb looks arbitrary until the particle is read as carrying its own
     * meaning — off is separation, up is completion, out is exhaustion — and then a
     * dozen of them stop being a dozen things. So the family is a field of its own and
     * the notes are grouped by it, exactly as the words are grouped by root. The
     * one-word equivalent is kept too, because that is what makes an idiom usable in
     * composition rather than only recognisable in reading.
     */
    IDIOM(
        id = "idiom",
        label = "熟語",
        subject = Subject.ENGLISH,
        fields = listOf(
            FieldDef("phrase", "熟語", "put off"),
            FieldDef("meaning", "意味", "延期する"),
            FieldDef("family", "芯（前置詞・型）", "off（分離）"),
            FieldDef("core", "なぜその意味になるか", "off は「離す」。予定から切り離して先へ送る", multiline = true),
            FieldDef("example", "例文", "They put off the meeting until Friday.", multiline = true),
            FieldDef("exampleJa", "例文訳", "彼らは会議を金曜まで延期した。", multiline = true),
            FieldDef("synonym", "1語で言い換えると", "defer, postpone"),
            FieldDef("memo", "メモ", "", multiline = true),
        ),
        templates = listOf(
            CardTemplate("idiom_ja", "熟語 → 意味", listOf("phrase"), listOf("meaning", "synonym"),
                AnswerMode.REVEAL, requires = listOf("phrase", "meaning")),
            CardTemplate("ja_idiom", "意味 → 熟語（入力）", listOf("meaning"), listOf("phrase"),
                AnswerMode.TYPE, requires = listOf("phrase", "meaning")),
            CardTemplate("idiom_cloze", "例文穴埋め", listOf("example"), listOf("phrase"),
                AnswerMode.CLOZE, requires = listOf("phrase", "example"),
                clozeSentenceField = "example", clozeAnswerField = "phrase"),
            CardTemplate("idiom_core", "熟語 → 芯", listOf("phrase"), listOf("core", "family"),
                AnswerMode.REVEAL, requires = listOf("phrase", "core"), defaultEnabled = false),
        ),
    ),

    CHEM_SUBSTANCE(
        id = "chem_substance",
        label = "化学・物質",
        subject = Subject.CHEMISTRY,
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
        subject = Subject.CHEMISTRY,
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
        subject = Subject.ENGLISH,
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

    /**
     * Translation into Japanese.
     *
     * The English paper at Kyoto University is built around translating a marked
     * passage, and what decides the mark is whether the structure was taken correctly
     * — the concessive, the inversion, the comparison — not whether every word was
     * looked up. So the note keeps those points as an explicit checklist, and the
     * answer is self-graded against them, exactly as with translation into English.
     */
    WAYAKU(
        id = "wayaku",
        label = "英文和訳",
        subject = Subject.ENGLISH,
        fields = listOf(
            FieldDef(
                "en", "英文（下線部）",
                "It is not that he cannot do the work, but that he will not.",
                multiline = true,
            ),
            FieldDef("ja", "模範訳", "彼にその仕事ができないのではなく、やろうとしないのだ。", multiline = true),
            FieldDef(
                "structures", "押さえる点（1行に1つ）",
                "It is not that A but that B を「AではなくBなのだ」と訳し分ける\nwill の「意志」を訳に出す",
                multiline = true,
            ),
            FieldDef("traps", "取りにくい構文・語義", "will not を単純未来として訳さない", multiline = true),
            FieldDef("memo", "メモ", "", multiline = true),
        ),
        templates = listOf(
            CardTemplate(
                "en_ja_write", "英文和訳を書く", listOf("en"), listOf("ja", "traps"), AnswerMode.SELF_CHECK,
                requires = listOf("en", "ja"), checklistField = "structures",
            ),
            CardTemplate(
                "structure_only", "構文の取り方だけ確認", listOf("en"), listOf("traps", "structures"),
                AnswerMode.REVEAL, requires = listOf("en", "traps"), defaultEnabled = false,
            ),
        ),
    ),

    /** A chemistry problem with a numeric answer, graded within a tolerance. */
    CHEM_CALC(
        id = "chem_calc",
        label = "化学・計算",
        subject = Subject.CHEMISTRY,
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

    /**
     * 速読用の長文.
     *
     * Reading speed is the one thing in this app that cannot be drilled by recall: it
     * has to be measured under time, against text long enough for the eye to settle
     * into a rhythm, and paired with a comprehension check — otherwise "faster" just
     * means "read less". The passage is therefore both a card (the question is asked
     * again on a schedule) and the material for the timed screen.
     */
    READING(
        id = "reading",
        label = "速読",
        subject = Subject.ENGLISH,
        fields = listOf(
            FieldDef("title", "見出し", "Why forgetting is useful"),
            FieldDef("passage", "英文", "", multiline = true),
            FieldDef("question", "設問", "What does the author claim about forgetting?", multiline = true),
            FieldDef("answer", "答え", "忘れることは記憶の失敗ではなく、必要な取捨選択だという主張。", multiline = true),
            FieldDef(
                "points", "押さえる点（1行に1つ）",
                "筆者の主張を1文で言えるか\n具体例が何の例かを言えるか",
                multiline = true,
            ),
            FieldDef("ja", "全訳", "", multiline = true),
            FieldDef("source", "出典", "自作"),
            FieldDef("memo", "メモ", "", multiline = true),
        ),
        templates = listOf(
            CardTemplate(
                "reading_answer", "読んで設問に答える", listOf("question", "passage"), listOf("answer"),
                AnswerMode.SELF_CHECK,
                requires = listOf("passage", "question", "answer"), checklistField = "points",
            ),
            CardTemplate(
                "reading_ja", "全訳で確認する", listOf("passage"), listOf("ja"),
                AnswerMode.REVEAL, requires = listOf("passage", "ja"), defaultEnabled = false,
            ),
        ),
    ),

    /**
     * 化学・理論.
     *
     * The laws rather than the substances: gas equations, equilibrium, thermochemistry,
     * cells. Each one is a relation plus the conditions it holds under, and it is
     * almost always the conditions that decide the mark — Henry's law needs a sparingly
     * soluble gas, the boiling-point elevation needs the particle count after
     * dissociation. So the note keeps the formula and its conditions as separate fields
     * and asks about them separately.
     */
    CHEM_THEORY(
        id = "chem_theory",
        label = "化学・理論",
        subject = Subject.CHEMISTRY,
        fields = listOf(
            FieldDef("title", "法則・項目", "気体の状態方程式"),
            FieldDef("formula", "式", "PV = nRT"),
            FieldDef(
                "meaning", "記号の意味",
                "P: 圧力、V: 体積、n: 物質量、R: 気体定数、T: 絶対温度",
                multiline = true,
            ),
            FieldDef(
                "condition", "成り立つ条件・使いどころ",
                "理想気体。高温・低圧ほどよく合う",
                multiline = true,
            ),
            FieldDef("point", "押さえる点", "", multiline = true),
            FieldDef("memo", "メモ", "", multiline = true),
        ),
        templates = listOf(
            CardTemplate(
                "theory_formula", "項目 → 式", listOf("title"), listOf("formula", "meaning"),
                AnswerMode.REVEAL, requires = listOf("title", "formula"),
            ),
            CardTemplate(
                "theory_name", "式 → 項目", listOf("formula"), listOf("title", "meaning"),
                AnswerMode.REVEAL, requires = listOf("title", "formula"),
            ),
            CardTemplate(
                "theory_condition", "項目 → 成り立つ条件", listOf("title"), listOf("condition", "point"),
                AnswerMode.REVEAL, requires = listOf("title", "condition"),
            ),
        ),
    ),

    /**
     * 数学.
     *
     * A maths problem is not memorised, but the move that opens it is: "classify by
     * remainder", "take the difference of the recurrence", "fix one variable and read
     * the rest as a function of it". So the card asks for the plan, not the answer,
     * and grading is a checklist of the steps that had to appear — the same
     * self-marking as translation, for the same reason.
     */
    MATH(
        id = "math",
        label = "数学",
        subject = Subject.MATH,
        fields = listOf(
            FieldDef(
                "question", "問題",
                "n を整数とする。n² + n + 1 が 3 の倍数になるのは、n を 3 で割った余りがいくつのときか。",
                multiline = true,
            ),
            FieldDef("approach", "方針（ひとことで）", "3 で割った余りで場合分けする", multiline = true),
            FieldDef(
                "steps", "押さえる手順（1行に1つ）",
                "n = 3k, 3k±1 に分ける\n各場合で n² + n + 1 を 3 で割った余りを計算する",
                multiline = true,
            ),
            FieldDef("tools", "使う道具・定理", "剰余による場合分け、合同式"),
            FieldDef("traps", "落とし穴", "「示せ」なので、すべての場合を尽くしたことを明記する", multiline = true),
            FieldDef("source", "出典", "自作 / 京大 2020 第2問 など"),
            FieldDef("memo", "メモ", "", multiline = true),
        ),
        templates = listOf(
            CardTemplate(
                "math_plan", "問題 → 方針を書く", listOf("question"), listOf("approach", "traps"),
                AnswerMode.SELF_CHECK,
                requires = listOf("question", "approach"), checklistField = "steps",
            ),
            CardTemplate(
                "math_tools", "問題 → 使う道具", listOf("question"), listOf("tools", "approach"),
                AnswerMode.REVEAL, requires = listOf("question", "tools"), defaultEnabled = false,
            ),
            CardTemplate(
                "math_trap", "方針 → 落とし穴", listOf("approach"), listOf("traps"),
                AnswerMode.REVEAL, requires = listOf("approach", "traps"), defaultEnabled = false,
            ),
        ),
    ),

    /**
     * 物理（原子）.
     *
     * Most of physics is derived rather than remembered, which is why this app stays
     * out of it. Atomic physics is the exception: the constants, the conditions each
     * relation holds under, and which experiment established what are simply things
     * you either know or do not.
     */
    PHYSICS(
        id = "physics",
        label = "物理・原子",
        subject = Subject.PHYSICS,
        fields = listOf(
            FieldDef("title", "項目・法則名", "光電効果（アインシュタインの式）"),
            FieldDef("formula", "式", "hν = W + K"),
            FieldDef(
                "meaning", "記号の意味",
                "h: プランク定数、ν: 光の振動数、W: 仕事関数、K: 光電子の最大運動エネルギー",
                multiline = true,
            ),
            FieldDef(
                "condition", "成り立つ条件・使いどころ",
                "限界振動数より大きい振動数のとき。光の強さではなく振動数で決まる",
                multiline = true,
            ),
            FieldDef("point", "押さえる点", "", multiline = true),
            FieldDef("memo", "メモ", "", multiline = true),
        ),
        templates = listOf(
            CardTemplate(
                "phys_formula", "項目 → 式", listOf("title"), listOf("formula", "meaning"),
                AnswerMode.REVEAL, requires = listOf("title", "formula"),
            ),
            CardTemplate(
                "phys_name", "式 → 項目", listOf("formula"), listOf("title", "meaning"),
                AnswerMode.REVEAL, requires = listOf("title", "formula"),
            ),
            CardTemplate(
                "phys_condition", "項目 → 使いどころ", listOf("title"), listOf("condition", "point"),
                AnswerMode.REVEAL, requires = listOf("title", "condition"),
            ),
        ),
    ),

    BASIC(
        id = "basic",
        label = "自由形式",
        subject = Subject.OTHER,
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
            NoteType.IDIOM -> listOf(SAME_GROUP, SYNONYM, ANTONYM, CONFUSABLE, CONTRAST, RELATED)
            NoteType.READING -> listOf(SAME_GROUP, CONTRAST, RELATED)
            NoteType.CHEM_SUBSTANCE -> listOf(REACTS_WITH, PRODUCES, SAME_GROUP, CONTRAST, CONFUSABLE, HYPERNYM, RELATED)
            NoteType.CHEM_REACTION -> listOf(PRODUCES, SAME_GROUP, CONTRAST, RELATED)
            NoteType.EISAKUBUN -> listOf(SAME_GROUP, CONTRAST, CONFUSABLE, RELATED)
            NoteType.WAYAKU -> listOf(SAME_GROUP, CONTRAST, CONFUSABLE, RELATED)
            NoteType.CHEM_CALC -> listOf(SAME_GROUP, CONTRAST, RELATED)
            NoteType.MATH -> listOf(SAME_GROUP, CONTRAST, CONFUSABLE, HYPERNYM, RELATED)
            NoteType.PHYSICS -> listOf(SAME_GROUP, CONTRAST, CONFUSABLE, PRODUCES, RELATED)
            NoteType.CHEM_THEORY -> listOf(SAME_GROUP, CONTRAST, CONFUSABLE, HYPERNYM, RELATED)
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
        NoteType.IDIOM -> this["phrase"]
        NoteType.CHEM_SUBSTANCE -> this["name"]
        NoteType.CHEM_REACTION -> this["title"]
        NoteType.EISAKUBUN -> this["ja"]
        NoteType.WAYAKU -> this["en"]
        NoteType.CHEM_CALC -> this["question"]
        NoteType.MATH -> this["question"]
        NoteType.PHYSICS -> this["title"]
        NoteType.CHEM_THEORY -> this["title"]
        NoteType.READING -> this["title"]
        NoteType.BASIC -> this["front"]
    }.ifBlank { type.fields.firstNotNullOfOrNull { fields[it.id]?.ifBlank { null } } ?: "(空)" }

    fun subtitle(): String = when (type) {
        NoteType.ENGLISH -> this["meaning"]
        NoteType.IDIOM -> this["meaning"]
        NoteType.CHEM_SUBSTANCE -> this["formula"]
        NoteType.CHEM_REACTION -> this["equation"]
        NoteType.EISAKUBUN -> this["en"]
        NoteType.WAYAKU -> this["ja"]
        NoteType.CHEM_CALC -> listOf(this["answer"], this["unit"]).filter { it.isNotBlank() }.joinToString(" ")
        NoteType.MATH -> this["approach"]
        NoteType.PHYSICS -> this["formula"]
        NoteType.CHEM_THEORY -> this["formula"]
        NoteType.READING -> this["question"]
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
