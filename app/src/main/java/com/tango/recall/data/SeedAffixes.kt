package com.tango.recall.data

/**
 * 接頭辞・接尾辞.
 *
 * The hard words in the paper arrive without a gloss, so the question is never "do you
 * know this word" but "can you place it". A prefix gives the direction (ex- out, sub-
 * under, counter- against), a suffix gives the part of speech (-ate a verb, -ous an
 * adjective), and the root gives the rest. Learned together they turn an unknown word
 * into a guess with a reason behind it.
 *
 * Each affix is linked to the words in the collection that carry it, so the card is
 * not an abstract rule but a pointer at words already half-known.
 *
 * Columns: 接辞 | 意味 | 種類 | 語にどう効くか | 語例
 */
internal fun seedAffixes(s: Seeder) {
    val deckId = s.deck(
        name = AFFIX_DECK,
        type = NoteType.AFFIX,
        templates = setOf("affix_meaning", "meaning_affix"),
        newPerDay = 5,
        relationQuiz = true,
    )

    val byKind = LinkedHashMap<String, MutableList<Long>>()
    val ids = LinkedHashMap<String, Long>()
    for (line in AFFIX_TABLE.lines()) {
        val row = line.trim()
        if (row.isEmpty() || row.startsWith("#")) continue
        val cells = row.split("|").map { it.trim() }
        require(cells.size == 5) { "列が 5 つではありません: $row" }
        val id = s.note(
            deckId, NoteType.AFFIX,
            mapOf(
                "affix" to cells[0], "meaning" to cells[1], "kind" to cells[2],
                "effect" to cells[3], "examples" to cells[4], "memo" to "",
            ),
            listOf("接辞", cells[2]),
        )
        ids[cells[0]] = id
        byKind.getOrPut(cells[2]) { mutableListOf() } += id
    }

    // Point each affix at the words already in the collection that carry it. The words
    // are named here rather than matched by spelling: "in-" starts plenty of words that
    // have nothing to do with negation.
    val notes = s.repo.listNotes(null, "", Int.MAX_VALUE).associateBy { it.title() }
    for ((affix, words) in AFFIX_WORDS) {
        val affixId = ids[affix] ?: continue
        for (word in words) {
            val noteId = notes[word]?.id ?: continue
            s.link(affixId, noteId, LinkType.HYPERNYM, "$affix を持つ語")
        }
    }

    // Affixes are learned in contrasting sets — the negatives together, the four that
    // build adjectives together — so each family is linked, and kept small enough that
    // "which others belong to this set?" stays a question rather than a list.
    for ((family, members) in AFFIX_FAMILIES) {
        val group = members.mapNotNull { ids[it] }
        for (i in group.indices) for (j in i + 1 until group.size) {
            s.link(group[i], group[j], LinkType.SAME_GROUP, family)
        }
    }

    fun link(a: String, b: String, type: LinkType, memo: String) {
        val x = ids[a] ?: return
        val y = ids[b] ?: return
        s.link(x, y, type, memo)
    }
    link("in-（否定）", "in-（中へ）", LinkType.CONFUSABLE, "同じつづりで正反対。incredible（信じられない）と include（含む）")
    link("un-", "in-（否定）", LinkType.SAME_GROUP, "どちらも否定。un- は英語本来語、in- はラテン語系につく")
    link("ex-", "in-（中へ）", LinkType.ANTONYM, "外へ ↔ 中へ")
    link("pre-", "post-", LinkType.ANTONYM, "前 ↔ 後")
    link("sub-", "super-", LinkType.ANTONYM, "下 ↔ 上")
    link("micro-", "macro-", LinkType.ANTONYM, "小さい ↔ 大きい")
    link("pro-", "contra- / counter-", LinkType.ANTONYM, "前へ・賛成 ↔ 逆らって")
    link("-able / -ible", "-ive", LinkType.SAME_GROUP, "どちらも形容詞をつくる")
    link("-tion / -sion", "-ment", LinkType.SAME_GROUP, "どちらも動詞から名詞をつくる")
    link("-ize", "-fy", LinkType.SAME_GROUP, "どちらも「〜化する」動詞をつくる")
    link("-less", "-ful", LinkType.ANTONYM, "欠く ↔ 満ちた")
}

/** Affixes worth learning against each other. */
private val AFFIX_FAMILIES: List<Pair<String, List<String>>> = listOf(
    "否定をつくる接頭辞" to listOf("un-", "in-（否定）", "dis-", "a- / ab-"),
    "外と中を示す接頭辞" to listOf("ex- / e-", "in-（中へ）", "intra- / intro-", "inter-"),
    "上下を示す接頭辞" to listOf("sub- / sup-", "super- / sur-", "de-"),
    "前後・繰り返しの接頭辞" to listOf("pre-", "post-", "pro-", "re-"),
    "向き・対立の接頭辞" to listOf("ad-", "ob- / op-", "counter- / contra-", "trans-"),
    "強め・貫通の接頭辞" to listOf("com- / con-", "per-"),
    "数を示す接頭辞" to listOf("mono- / uni-", "bi- / di-", "multi- / poly-", "omni-", "semi-"),
    "規模・自律の接頭辞" to listOf("micro-", "macro-", "auto-"),
    "形容詞をつくる接尾辞（性質）" to listOf("-able / -ible", "-ive", "-ous / -ious", "-al / -ial"),
    "形容詞をつくる接尾辞（有無・程度）" to listOf("-ful", "-less", "-ish"),
    "名詞をつくる接尾辞（こと・状態）" to listOf("-tion / -sion", "-ment", "-ity / -ty", "-ness", "-ance / -ence"),
    "名詞をつくる接尾辞（人・主義）" to listOf("-ism", "-ist"),
    "動詞をつくる接尾辞" to listOf("-ate", "-fy / -ify", "-ize / -ise", "-en"),
    "方向・様態の接尾辞" to listOf("-ward(s)", "-wise"),
)

/**
 * Which of the collection's words carry which affix.
 *
 * Curated rather than matched on spelling: "insist" begins with the letters of the
 * negative in- and means nothing of the kind.
 */
private val AFFIX_WORDS: List<Pair<String, List<String>>> = listOf(
    "un-" to listOf("undermine", "unprecedented"),
    "in-（否定）" to listOf("infinite", "indifferent", "indispensable", "invincible"),
    "in-（中へ）" to listOf("inject", "inquire", "intercept", "induce", "inclusive"),
    "dis-" to listOf("dismiss", "disrupt", "distract", "dissolve"),
    "a- / ab-" to listOf("abstract", "abrupt"),
    "ad-" to listOf("advocate", "adequate", "adapt to"),
    "com- / con-" to listOf("confer", "consist", "contribute", "conform", "conspire"),
    "de-" to listOf("deduce", "degenerate", "deplete", "deviate", "defer"),
    "ex- / e-" to listOf("extract", "exclude", "expire", "expedite", "extend", "explicit"),
    "inter-" to listOf("intervene", "intercept", "interfere"),
    "ob- / op-" to listOf("obstruct", "obvious", "obligation"),
    "per-" to listOf("persist", "pervade", "perceive", "pervasive"),
    "post-" to listOf("postpone"),
    "pre-" to listOf("predict", "prescribe", "previous", "precise", "presume"),
    "pro-" to listOf("progress", "project", "provoke", "prosper"),
    "re-" to listOf("retract", "resolve", "revive", "renounce", "reform", "resume"),
    "sub- / sup-" to listOf("submit", "subject", "subscribe", "subconscious", "susceptible"),
    "super- / sur-" to listOf("superfluous", "superficial"),
    "trans-" to listOf("transmit", "transform", "transient"),
    "counter- / contra-" to listOf("contradict", "contend"),
    "-able / -ible" to listOf("durable", "credible", "eligible", "susceptible", "invincible"),
    "-ive" to listOf("collective", "cognitive", "decisive", "aggressive", "evasive"),
    "-ous / -ious" to listOf("conspicuous", "notorious", "obvious", "superfluous", "ambiguous"),
    "-al / -ial" to listOf("structural", "marginal", "fundamental", "incidental"),
    "-ful" to listOf("grateful"),
    "-less" to listOf("relentless"),
    "-tion / -sion" to listOf("solution", "obligation", "expedition", "discrimination"),
    "-ment" to listOf("sentiment"),
    "-ity / -ty" to listOf("fidelity"),
    "-ance / -ence" to listOf("consequence"),
    "-ate" to listOf("designate", "evaluate", "equate", "manipulate", "delegate"),
    "-fy / -ify" to listOf("modify", "testify", "gratify"),
    "-ize / -ise" to listOf("recognize", "specialize in"),
)

private val AFFIX_TABLE = """
# 接辞 | 意味 | 種類 | 語にどう効くか | 語例
un-|〜でない|接頭辞|英語本来の語につく否定。品詞は変えない|unable, unaware, unlikely
in-（否定）|〜でない|接頭辞|ラテン語系の語につく否定。続く音でim-/il-/ir- に変わる（impossible, illegal, irregular）|infinite, indifferent, incredible
in-（中へ）|中へ・上に|接頭辞|方向を与える。否定の in- と同じつづりなので、語根の意味で見分ける|include, inject, insist
dis-|離れて・打ち消し|接頭辞|分離させるか、反対にする|dismiss, disrupt, dissolve
a- / ab-|離れて|接頭辞|そこから離す方向|abandon, abnormal, abstract
ad-|〜の方へ|接頭辞|近づける方向。続く音に同化する（affect, accept, arrive）|adapt, advocate, adequate
com- / con-|共に・すっかり|接頭辞|いっしょにする、または強める|confer, consist, contribute
de-|下へ・離れて・逆に|接頭辞|下げる、取り去る、逆にする|deduce, deplete, degenerate
ex- / e-|外へ|接頭辞|外に出す方向|extract, exclude, expire
inter-|間に|接頭辞|2つのものの間で起こることにする|intervene, interfere, international
intra- / intro-|内部に|接頭辞|内側で完結することにする|introduce, introvert, intramural
ob- / op-|〜に向かって・逆らって|接頭辞|向かい合う、立ちはだかる|obstruct, oppose, object
per-|through・すっかり|接頭辞|貫いて最後まで|persist, pervade, perfect
post-|後ろ・後で|接頭辞|時間か位置を後ろにずらす|postpone, postwar, postscript
pre-|前もって|接頭辞|時間的に先に置く|predict, prescribe, prepare
pro-|前へ・賛成して|接頭辞|前方へ出す、支持する|progress, promote, propose
re-|再び・元へ・後ろへ|接頭辞|繰り返すか、戻す|retract, revive, resume
sub- / sup-|下に・副の|接頭辞|下に置く、下位にする|submit, subject, support
super- / sur-|上に・超えて|接頭辞|上を行く、余分である|superfluous, superficial, surpass
trans-|越えて・移して|接頭辞|向こう側へ移す|transmit, transform, translate
counter- / contra-|逆らって|接頭辞|反対の向きにする|contradict, counterargument, contrast
micro-|小さい|接頭辞|規模を小さい側に限定する。顕微鏡でしか見えない大きさ|microbe, microscope, microcosm
macro-|大きい|接頭辞|規模を大きい側に限定する|macroeconomics, macroscopic
mono- / uni-|1つ|接頭辞|数を1に限定する|monopoly, monotonous, uniform
bi- / di-|2つ|接頭辞|数を2に限定する|bilingual, bilateral, dioxide
multi- / poly-|多くの|接頭辞|数を多に限定する|multiple, multitude, polygon
omni-|すべての|接頭辞|範囲を全体に広げる|omnipotent, omnivorous
semi-|半分・やや|接頭辞|程度を半分にする|semicircle, semiconductor
auto-|自分で|接頭辞|自力で起こることにする|automatic, autonomy, autobiography
-able / -ible|〜できる・〜されうる|接尾辞|動詞から形容詞をつくる。受け身の意味になりやすい|durable, credible, visible
-ive|〜の性質をもつ|接尾辞|動詞から形容詞をつくる|decisive, cognitive, aggressive
-ous / -ious|〜に満ちた|接尾辞|名詞から形容詞をつくる|conspicuous, notorious, ambiguous
-al / -ial|〜に関する|接尾辞|名詞から形容詞をつくる|structural, marginal, essential
-ful|〜に満ちた|接尾辞|名詞から形容詞をつくる|grateful, powerful, meaningful
-less|〜がない|接尾辞|名詞から形容詞をつくる。-ful の反対|regardless, endless, priceless
-ish|〜じみた・やや〜|接尾辞|程度を弱める形容詞をつくる|childish, reddish, foolish
-tion / -sion|〜すること|接尾辞|動詞から名詞をつくる|solution, obligation, decision
-ment|〜すること・結果|接尾辞|動詞から名詞をつくる|sentiment, judgment, argument
-ity / -ty|〜であること|接尾辞|形容詞から名詞をつくる|fidelity, ability, density
-ness|〜であること|接尾辞|形容詞から名詞をつくる。英語本来語につく|awareness, kindness, darkness
-ance / -ence|〜すること・状態|接尾辞|動詞から名詞をつくる|resistance, difference, persistence
-ism|主義・特徴|接尾辞|考え方や傾向の名前をつくる|criticism, realism, mechanism
-ist|〜する人|接尾辞|人を表す名詞をつくる|scientist, specialist, novelist
-ate|〜にする|接尾辞|名詞・形容詞から動詞をつくる。形容詞のこともある（adequate）|evaluate, designate, manipulate
-fy / -ify|〜にする|接尾辞|動詞をつくる。-ate より「変える」色が濃い|modify, testify, clarify
-ize / -ise|〜化する|接尾辞|動詞をつくる|recognize, specialize, criticize
-en|〜にする・〜になる|接尾辞|形容詞から動詞をつくる|strengthen, widen, sharpen
-ward(s)|〜の方へ|接尾辞|方向を表す|toward, backward, outward
-wise|〜のように・〜の点で|接尾辞|様態か観点を表す|likewise, otherwise, clockwise
""".trimIndent()

internal const val AFFIX_DECK = "接頭辞・接尾辞（語形から見当をつける）"
