package com.tango.recall.data

/**
 * More of the two kinds of card that need a pen: translation into English, and
 * calculation.
 *
 * Both are graded by working, not by recognition — the translation against a
 * checklist of the points that had to appear, the calculation against a number with a
 * tolerance — so a few well-chosen items are worth more than a long list.
 */
internal fun seedMoreEisakubun(s: Seeder) {
    val deckId = s.deck(
        name = Seed.EISAKUBUN_DECK,
        type = NoteType.EISAKUBUN,
        templates = setOf("ja_en_write"),
        newPerDay = 3,
    )

    fun sentence(ja: String, en: String, structures: String, traps: String): Long = s.note(
        deckId, NoteType.EISAKUBUN,
        mapOf("ja" to ja, "en" to en, "structures" to structures, "traps" to traps, "memo" to ""),
        listOf("和文英訳"),
    )

    val kuchibeta = sentence(
        "彼は口下手だが、言うことには重みがある。",
        "He is not good at expressing himself, but what he says carries weight.",
        "「口下手」を be not good at expressing oneself と説明に開く\n" +
            "「重みがある」を carry weight で出す\n" +
            "「言うこと」を what he says（関係代名詞 what）で受ける",
        "「口下手」に一語で対応する単語を探しにいかない。説明に開くほうが速い。",
    )
    val narau = sentence(
        "習うより慣れろ、というのは本当だと思う。",
        "I think there is some truth in the saying that practice teaches more than instruction does.",
        "ことわざは訳さずに、意味を説明する文に開く\n" +
            "「〜というのは本当だ」を there is some truth in the saying that … で受ける\n" +
            "比較の相手を does で受け直して文を締める",
        "ことわざをそのまま英語に移そうとしない。何を言っているかだけを訳す。",
    )
    val toshi = sentence(
        "年をとるにつれて、時間の経つのが速く感じられる。",
        "The older you get, the faster time seems to pass.",
        "「〜につれて…」を the 比較級 …, the 比較級 … で出す\n" +
            "一般論の主語に you を使う\n" +
            "「感じられる」を seem to do で処理する",
        "「感じられる」を受動態（is felt）にしない。英語では seem / appear で受ける。",
    )
    val kikeba = sentence(
        "彼女の話は、聞けば聞くほど分からなくなった。",
        "The more I listened to her, the less I understood.",
        "「〜ば〜ほど」を the 比較級, the 比較級 で出す\n" +
            "「分からなくなる」を the less I understood と否定方向の比較で出す",
        "「分からなくなった」を became unable to understand と重く訳さない。",
    )
    val shidai = sentence(
        "彼が来るかどうかは、天気次第だ。",
        "Whether he will come or not depends on the weather.",
        "「〜かどうか」を whether 節にして主語に立てる\n" +
            "「〜次第だ」を depend on で出す",
        "「次第」を according to としない。主語が節になることに注意する。",
    )
    val tashikameru = sentence(
        "自分の目で確かめないうちは、信じない方がいい。",
        "You had better not believe it until you have seen it for yourself.",
        "「〜しないうちは…ない」を not … until … で出す\n" +
            "had better の否定は had better not の語順\n" +
            "「自分の目で」を for yourself で出す",
        "until 節の中は未来のことでも現在形（have seen）で書く。",
    )
    val unnoyosa = sentence(
        "彼は成功したが、それは運がよかったからにすぎない。",
        "He did succeed, but only because he was lucky.",
        "「成功はした」の譲歩を助動詞 did の強調で出す\n" +
            "「〜にすぎない」を only because … で軽く処理する",
        "「〜にすぎない」に no more than を当てると文が重くなる。理由を限定するだけでよい。",
    )

    s.link(toshi, kikeba, LinkType.SAME_GROUP, "どちらも the 比較級, the 比較級。片方を思い出せばもう片方も出る")
    s.link(narau, kuchibeta, LinkType.SAME_GROUP, "日本語を説明に開いてから英語にする型")
    s.link(unnoyosa, shidai, LinkType.CONTRAST, "理由を限定する / 条件を主語に立てる")
    s.link(tashikameru, kikeba, LinkType.CONFUSABLE, "not … until と the less …。どちらも否定の方向を取り違えやすい")
}

/** Calculation, with the arithmetic spelled out in the solution field. */
internal fun seedMoreChemCalc(s: Seeder) {
    val deckId = s.deck(
        name = Seed.CALC_DECK,
        type = NoteType.CHEM_CALC,
        templates = setOf("calc"),
        newPerDay = 4,
    )

    fun problem(
        question: String, answer: String, unit: String, tolerance: String, solution: String,
    ): Long = s.note(
        deckId, NoteType.CHEM_CALC,
        mapOf(
            "question" to question, "answer" to answer, "unit" to unit,
            "tolerance" to tolerance, "solution" to solution, "memo" to "",
        ),
        listOf("計算"),
    )

    val titration = problem(
        "0.100 mol/L の水酸化ナトリウム水溶液 20.0 mL を中和するのに必要な 0.200 mol/L 硫酸は何 mL か。",
        "5.00", "mL", "1",
        "中和は「酸の物質量×価数 = 塩基の物質量×価数」。\n" +
            "塩基: 0.100 × 0.0200 × 1 = 2.00×10^-3 mol\n" +
            "硫酸は2価なので 0.200 × V × 2 = 2.00×10^-3、V = 5.00×10^-3 L = 5.00 mL",
    )
    val molarMass = problem(
        "標準状態で 1.00 L の質量が 1.25 g である気体の分子量はいくらか。",
        "28.0", "", "2",
        "標準状態の気体 1 mol は 22.4 L。1.00 L が 1.25 g なので、1 mol では 1.25 × 22.4 = 28.0 g",
    )
    val electrolysis = problem(
        "1.00 A の電流を 32分10秒 流したとき、流れた電子は何 mol か。ファラデー定数 F = 9.65×10^4 C/mol",
        "0.0200", "mol", "2",
        "電気量 Q = It = 1.00 × 1930 s = 1930 C\n" +
            "電子の物質量 = 1930 ÷ 9.65×10^4 = 2.00×10^-2 mol",
    )
    val heat = problem(
        "メタンの燃焼熱を 891 kJ/mol とすると、メタン 8.0 g を完全燃焼させたときに発生する熱量は何 kJ か。",
        "4.5×10^2", "kJ", "3",
        "CH4 の分子量は 16 なので 8.0 ÷ 16 = 0.50 mol\n" +
            "891 × 0.50 = 445.5 ≒ 4.5×10^2 kJ",
    )
    val solubility = problem(
        "60℃ で溶解度 110、20℃ で溶解度 32 の物質がある。60℃ の飽和水溶液 210 g を 20℃ まで冷やすと何 g 析出するか。",
        "78", "g", "2",
        "溶解度は水 100 g に溶ける最大の質量。60℃ の飽和水溶液 210 g は水 100 g と溶質 110 g。\n" +
            "20℃ では水 100 g に 32 g しか溶けないので、110 - 32 = 78 g が析出する。",
    )
    val freezing = problem(
        "水 100 g にグルコース（分子量 180）1.8 g を溶かした溶液の凝固点降下度は何 K か。Kf = 1.85 K·kg/mol",
        "0.185", "K", "3",
        "グルコース: 1.8 ÷ 180 = 0.010 mol。質量モル濃度 = 0.010 ÷ 0.100 kg = 0.10 mol/kg\n" +
            "ΔT = Kf × m = 1.85 × 0.10 = 0.185 K（非電解質なので粒子数はそのまま）",
    )
    val equilibrium = problem(
        "1.0 L の容器で H2 + I2 ⇄ 2HI が平衡に達し、H2 0.20 mol、I2 0.20 mol、HI 1.6 mol であった。平衡定数 K はいくらか。",
        "64", "", "2",
        "K = [HI]² ÷ ([H2][I2]) = 1.6² ÷ (0.20 × 0.20) = 2.56 ÷ 0.040 = 64（単位はつかない）",
    )
    val mixing = problem(
        "0.10 mol/L の塩酸 100 mL に 0.10 mol/L の水酸化ナトリウム水溶液 60 mL を混ぜた溶液の pH はいくらか。log2 = 0.30",
        "1.6", "", "3",
        "HCl: 1.0×10^-2 mol、NaOH: 6.0×10^-3 mol。差の 4.0×10^-3 mol の H+ が残る。\n" +
            "全体積 160 mL なので [H+] = 4.0×10^-3 ÷ 0.160 = 2.5×10^-2 mol/L\n" +
            "pH = -log(2.5×10^-2) = 2 - log2.5 ≒ 1.6",
    )
    val hydrate = problem(
        "硫酸銅(II)五水和物（式量 250）25 g を水に溶かして 500 mL にした。モル濃度は何 mol/L か。",
        "0.20", "mol/L", "2",
        "25 ÷ 250 = 0.10 mol。水和水も含めた式量で割るのがポイント。\n" +
            "0.10 ÷ 0.500 L = 0.20 mol/L",
    )

    val byTitle = s.repo.listNotes(deckId, "", Int.MAX_VALUE).associateBy { it.title() }
    fun link(a: String, b: Long, t: LinkType, memo: String) {
        val x = byTitle.entries.firstOrNull { it.key.startsWith(a) }?.value?.id ?: return
        s.link(x, b, t, memo)
    }

    s.link(titration, mixing, LinkType.RELATED, "どちらも「酸と塩基の物質量の差」で考える。中和点か、余りが出るかの違い")
    s.link(equilibrium, mixing, LinkType.CONTRAST, "平衡定数は濃度の比、pH は残った H+ の濃度")
    s.link(freezing, hydrate, LinkType.RELATED, "溶質の物質量をどう出すかが要。分子量か式量か")
    s.link(solubility, hydrate, LinkType.RELATED, "結晶に水が含まれるかどうかで質量の扱いが変わる")
    s.link(electrolysis, molarMass, LinkType.CONTRAST, "電気量から物質量へ / 体積から物質量へ。どちらも定数で橋渡しする")
    link("0.10 mol/L の酢酸水溶液", equilibrium, LinkType.RELATED, "電離定数も平衡定数の一種")
    link("メタン CH4", heat, LinkType.RELATED, "同じ燃焼を、物質量で見るか熱量で見るか")
    link("標準状態（0℃", molarMass, LinkType.RELATED, "22.4 L/mol を、体積から物質量へ / 質量から分子量へ")
}
