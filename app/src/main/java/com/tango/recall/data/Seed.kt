package com.tango.recall.data

/**
 * Bundled content, in packs.
 *
 * Everything here is ordinary editable data — the point is to show what a
 * well-linked note looks like (shared etymological roots for English, reaction
 * chains for chemistry) so a new deck has something to imitate. Delete freely.
 *
 * It is split into packs so that a later version can add material to a phone that
 * has been in use for months: a pack the learner has not got yet is installed on the
 * next launch, and one already installed is never touched again.
 */
object Seed {

    /**
     * Every pack, in installation order.
     *
     * The ids are permanent. Renaming one would make the app think the learner has
     * never seen it and install a second copy.
     */
    private val PACKS: List<Pair<String, (Seeder) -> Unit>> = listOf(
        "english" to ::seedEnglish,
        "chemistry" to ::seedChemistry,
        "eisakubun" to ::seedEisakubun,
        "chem_calc" to ::seedChemCalc,
        "wayaku" to ::seedWayaku,
        "organic" to ::seedOrganic,
        "inorganic_extra" to ::seedInorganic,
        "vocabulary_extra" to ::seedMoreVocabulary,
        "eisakubun_extra" to ::seedMoreEisakubun,
        "chem_calc_extra" to ::seedMoreChemCalc,
        "math" to ::seedMath,
        "physics_atomic" to ::seedPhysics,
    )

    /** What the starter content consisted of before it was split into packs. */
    private val ORIGINAL_PACKS = setOf("english", "chemistry", "eisakubun", "chem_calc")

    fun populate(repo: Repository) {
        var installed = repo.installedSeedPacks
        // A phone set up before packs existed already holds the original content.
        if (installed.isEmpty() && repo.setting(Repository.KEY_SEEDED, "") == "1") {
            installed = ORIGINAL_PACKS
            repo.installedSeedPacks = installed
        }
        for ((id, install) in PACKS) {
            if (id in installed) continue
            repo.transaction { install(Seeder(repo)) }
            installed = installed + id
            repo.installedSeedPacks = installed
        }
        repo.putSetting(Repository.KEY_SEEDED, "1")
    }

    // ---- English ------------------------------------------------------------

    internal data class Word(
        val word: String, val meaning: String, val pos: String, val root: String,
        val example: String, val exampleJa: String, val collocation: String = "",
    )

    private val ROOT_GROUPS: List<Pair<String, List<Word>>> = listOf(
        "spect / spic（見る）" to listOf(
            Word("perspective", "観点、見通し", "名", "spect / spic（見る）",
                "Try to see the problem from a different perspective.",
                "その問題を別の観点から見てみなさい。", "from a ... perspective"),
            Word("conspicuous", "目立つ、際立った", "形", "spect / spic（見る）",
                "Her red coat was conspicuous in the crowd.",
                "彼女の赤いコートは人混みで目立っていた。", "conspicuous absence"),
            Word("speculate", "推測する、投機する", "動", "spect / spic（見る）",
                "Analysts speculate that prices will fall.",
                "アナリストは価格が下がると推測している。", "speculate about / on"),
            Word("retrospect", "回顧、振り返ること", "名", "spect / spic（見る）",
                "In retrospect, the decision was clearly wrong.",
                "振り返ってみれば、その決定は明らかに誤りだった。", "in retrospect"),
        ),
        "duc / duct（導く）" to listOf(
            Word("conduct", "行う、指揮する／行い", "動・名", "duc / duct（導く）",
                "The team conducted a careful experiment.",
                "チームは入念な実験を行った。", "conduct a survey / research"),
            Word("induce", "引き起こす、誘発する", "動", "duc / duct（導く）",
                "The drug may induce drowsiness.",
                "その薬は眠気を引き起こすことがある。", "induce sleep / labour"),
            Word("deduce", "推論する、演繹する", "動", "duc / duct（導く）",
                "From these facts we can deduce his motive.",
                "これらの事実から彼の動機を推論できる。", "deduce from"),
            Word("conducive", "（〜の）助けとなる", "形", "duc / duct（導く）",
                "A quiet room is conducive to concentration.",
                "静かな部屋は集中の助けになる。", "be conducive to"),
        ),
        "ced / cess（行く・譲る）" to listOf(
            Word("precede", "先行する、〜に先立つ", "動", "ced / cess（行く・譲る）",
                "A short introduction precedes each chapter.",
                "各章の前には短い導入がある。", "precede a meeting"),
            Word("concede", "認める、譲歩する", "動", "ced / cess（行く・譲る）",
                "He finally conceded that he was mistaken.",
                "彼はついに自分が誤っていたと認めた。", "concede defeat"),
            Word("unprecedented", "前例のない", "形", "ced / cess（行く・譲る）",
                "The country faced an unprecedented crisis.",
                "その国は前例のない危機に直面した。", "unprecedented scale / level"),
            Word("recession", "景気後退、後退", "名", "ced / cess（行く・譲る）",
                "The economy slipped into a deep recession.",
                "経済は深刻な景気後退に陥った。", "fall into recession"),
        ),
        "ten / tain（保つ）" to listOf(
            Word("sustain", "持続させる、支える", "動", "ten / tain（保つ）",
                "It is hard to sustain such rapid growth.",
                "これほど急速な成長を持続させるのは難しい。", "sustain growth / damage"),
            Word("retain", "保持する、覚えている", "動", "ten / tain（保つ）",
                "The soil retains water well.",
                "その土壌はよく水を保つ。", "retain information / heat"),
            Word("tenacious", "粘り強い、執拗な", "形", "ten / tain（保つ）",
                "She is tenacious in pursuing her goals.",
                "彼女は目標の追求に粘り強い。", "a tenacious grip"),
            Word("detain", "拘留する、引き止める", "動", "ten / tain（保つ）",
                "Police detained two suspects overnight.",
                "警察は容疑者2人を一晩拘留した。", "be detained by police"),
        ),
        "vert / vers（回る）" to listOf(
            Word("divert", "そらす、転換する", "動", "vert / vers（回る）",
                "Traffic was diverted to a side road.",
                "車の流れは脇道へそらされた。", "divert attention / funds"),
            Word("adverse", "不利な、逆の", "形", "vert / vers（回る）",
                "The plan had adverse effects on the environment.",
                "その計画は環境に悪影響を及ぼした。", "adverse effect / weather"),
            Word("versatile", "多才な、用途の広い", "形", "vert / vers（回る）",
                "Wood is a remarkably versatile material.",
                "木材は非常に用途の広い材料だ。", "a versatile player / tool"),
            Word("converse", "逆の、反対の／会話する", "形・動", "vert / vers（回る）",
                "The converse is not necessarily true.",
                "その逆が必ずしも成り立つとは限らない。", "the converse of"),
        ),
        "pos / pon（置く）" to listOf(
            Word("impose", "課す、押しつける", "動", "pos / pon（置く）",
                "The government imposed a new tax on imports.",
                "政府は輸入品に新たな税を課した。", "impose a tax / restriction"),
            Word("dispose", "処分する、配置する", "動", "pos / pon（置く）",
                "Please dispose of the waste properly.",
                "廃棄物は適切に処分してください。", "dispose of"),
            Word("component", "構成要素、部品", "名", "pos / pon（置く）",
                "Memory is a key component of learning.",
                "記憶は学習の重要な構成要素だ。", "a key component of"),
            Word("postpone", "延期する", "動", "pos / pon（置く）",
                "They postponed the launch until next spring.",
                "彼らは発売を来春まで延期した。", "postpone a decision"),
        ),
    )

    private fun seedEnglish(s: Seeder) {
        val deckId = s.deck(
            name = ENGLISH_DECK,
            type = NoteType.ENGLISH,
            templates = setOf("en_ja", "ja_en", "cloze"),
            newPerDay = 15,
            relationQuiz = true,
        )

        addWords(s, deckId, ROOT_GROUPS)

        // A couple of cross-group relations that are worth noticing.
        val byWord = s.repo.listNotes(deckId, "", Int.MAX_VALUE).associateBy { it.title() }
        fun link(a: String, b: String, t: LinkType, memo: String = "") {
            val x = byWord[a]?.id ?: return
            val y = byWord[b]?.id ?: return
            s.link(x, y, t, memo)
        }
        link("induce", "deduce", LinkType.CONTRAST, "induce=帰納的に引き出す / deduce=演繹して導く")
        link("adverse", "conducive", LinkType.ANTONYM, "不利に働く ↔ 助けとなる")
        link("concede", "precede", LinkType.CONFUSABLE, "つづりが近い。concede=認める / precede=先行する")
        link("retain", "sustain", LinkType.SYNONYM, "どちらも「保ち続ける」")
    }

    /** Add every word of every root group, linking each group into a clique. */
    internal fun addWords(s: Seeder, deckId: Long, groups: List<Pair<String, List<Word>>>) {
        for ((root, words) in groups) {
            val ids = words.map { w ->
                s.note(
                    deckId, NoteType.ENGLISH,
                    mapOf(
                        "word" to w.word,
                        "meaning" to w.meaning,
                        "pos" to w.pos,
                        "root" to w.root,
                        "example" to w.example,
                        "exampleJa" to w.exampleJa,
                        "collocation" to w.collocation,
                        "memo" to "",
                    ),
                    listOf("語源", root.substringBefore("（").trim().replace(" / ", "-")),
                )
            }
            // Every word in a root group is linked to every other one, so answering any
            // of them surfaces the rest.
            for (i in ids.indices) for (j in i + 1 until ids.size) {
                s.link(ids[i], ids[j], LinkType.SAME_ROOT, root)
            }
        }
    }

    // ---- Chemistry ----------------------------------------------------------

    private fun seedChemistry(s: Seeder) {
        val substanceDeck = s.deck(
            name = INORGANIC_DECK,
            type = NoteType.CHEM_SUBSTANCE,
            templates = setOf("name_formula", "formula_name", "name_props"),
            newPerDay = 10,
            relationQuiz = true,
        )
        val reactionDeck = s.deck(
            name = REACTION_DECK,
            type = NoteType.CHEM_REACTION,
            templates = setOf("title_eq", "eq_title", "title_cond"),
            newPerDay = 6,
            relationQuiz = true,
        )

        fun substance(
            name: String, formula: String, category: String, props: String, uses: String,
        ): Long = s.note(
            substanceDeck, NoteType.CHEM_SUBSTANCE,
            mapOf(
                "name" to name, "formula" to formula, "category" to category,
                "props" to props, "uses" to uses, "memo" to "",
            ),
            listOf("無機", category),
        )

        fun reaction(
            title: String, equation: String, condition: String, point: String,
        ): Long = s.note(
            reactionDeck, NoteType.CHEM_REACTION,
            mapOf(
                "title" to title, "equation" to equation,
                "condition" to condition, "point" to point, "memo" to "",
            ),
            listOf("無機", "製法"),
        )

        val h2so4 = substance(
            "硫酸", "H2SO4", "オキソ酸",
            "濃硫酸は不揮発性・吸湿性・脱水作用をもち、密度が大きい。希硫酸は強酸。",
            "乾燥剤、希硫酸は金属と反応して水素を発生。熱濃硫酸は酸化剤としてはたらく。",
        )
        val so2 = substance(
            "二酸化硫黄", "SO2", "酸性酸化物",
            "無色・刺激臭。水に溶けて亜硫酸となり弱酸性。還元性を示し漂白作用がある。",
            "接触法の原料。硫化水素と反応すると硫黄が析出する（SO2 が酸化剤側）。",
        )
        val so3 = substance(
            "三酸化硫黄", "SO3", "酸性酸化物",
            "水と激しく反応して硫酸になるため、濃硫酸に吸収させて発煙硫酸とする。",
            "接触法の中間生成物。",
        )
        val h2s = substance(
            "硫化水素", "H2S", "酸性・還元剤",
            "無色・腐卵臭・有毒。水溶液は弱酸性。強い還元性をもつ。",
            "多くの金属イオンと硫化物の沈殿をつくり、系統分析に使われる。",
        )
        val nh3 = substance(
            "アンモニア", "NH3", "塩基",
            "無色・刺激臭。水に極めてよく溶け弱塩基性を示す。三角錐形の極性分子。",
            "ハーバー・ボッシュ法で合成し、オストワルト法で硝酸の原料になる。",
        )
        val hno3 = substance(
            "硝酸", "HNO3", "オキソ酸",
            "強酸かつ強い酸化剤。光や熱で分解するため褐色びんに保存する。",
            "希硝酸は NO、濃硝酸は NO2 を発生。Al・Fe・Ni は濃硝酸で不動態となる。",
        )
        val no = substance(
            "一酸化窒素", "NO", "中性酸化物",
            "無色・水に溶けにくい。空気中で直ちに酸化されて二酸化窒素になる。",
            "水上置換で捕集する。",
        )
        val no2 = substance(
            "二酸化窒素", "NO2", "酸性酸化物",
            "赤褐色・刺激臭・有毒。水に溶けて硝酸と一酸化窒素を生じる。",
            "下方置換で捕集する。",
        )
        val naoh = substance(
            "水酸化ナトリウム", "NaOH", "塩基",
            "白色固体。潮解性があり空気中の水分を吸う。強塩基。",
            "空気中の CO2 を吸収して炭酸ナトリウムになるため密閉保存する。",
        )
        val na2co3 = substance(
            "炭酸ナトリウム", "Na2CO3", "塩",
            "白色粉末。水溶液は加水分解により塩基性。十水和物は風解する。",
            "アンモニアソーダ法で製造。ガラスの原料。",
        )
        val caco3 = substance(
            "炭酸カルシウム", "CaCO3", "塩",
            "水に溶けにくい白色固体（石灰石・大理石）。強熱すると CaO と CO2 に分解。",
            "塩酸を加えると CO2 が発生する。アンモニアソーダ法の原料。",
        )
        val cuso4 = substance(
            "硫酸銅(II)五水和物", "CuSO4.5H2O", "塩",
            "青色結晶。加熱すると白色の無水物になる（可逆）。",
            "白色無水物は水に触れると青変するので、水の検出に使える。",
        )
        val al2o3 = substance(
            "酸化アルミニウム", "Al2O3", "両性酸化物",
            "融点が非常に高い白色固体。酸にも強塩基にも溶ける両性酸化物。",
            "融解塩電解（氷晶石を加える）でアルミニウムの単体を得る。",
        )
        val cl2 = substance(
            "塩素", "Cl2", "ハロゲン単体",
            "黄緑色・刺激臭・有毒。水に溶けて塩化水素と次亜塩素酸を生じ、酸化作用を示す。",
            "下方置換で捕集し、水（塩化水素の除去）と濃硫酸（乾燥）に通す。",
        )

        val contact = reaction(
            "接触法（硫酸の工業的製法）",
            "S + O2 → SO2（硫黄を燃やす場合）\n" +
                "4FeS2 + 11O2 → 2Fe2O3 + 8SO2（黄鉄鉱を焙焼する場合）\n" +
                "2SO2 + O2 ⇄ 2SO3\n" +
                "SO3 + H2O → H2SO4",
            "第2段階は酸化バナジウム(V) V2O5 を触媒とし約400〜500℃",
            "SO3 は水と激しく反応するので、直接水に通さず濃硫酸に吸収させて発煙硫酸とし、希硫酸で薄める。",
        )
        val ostwald = reaction(
            "オストワルト法（硝酸の工業的製法）",
            "4NH3 + 5O2 → 4NO + 6H2O\n2NO + O2 → 2NO2\n3NO2 + H2O → 2HNO3 + NO",
            "第1段階は白金 Pt 触媒、約800℃",
            "第3段階で生じた NO は回収して第2段階に戻す。全体では NH3 + 2O2 → HNO3 + H2O。",
        )
        val haber = reaction(
            "ハーバー・ボッシュ法（アンモニアの合成）",
            "N2 + 3H2 ⇄ 2NH3",
            "四酸化三鉄 Fe3O4 を主成分とする触媒、約400〜600℃・約1〜3×10^7 Pa",
            "発熱・分子数減少の平衡反応。低温・高圧ほど収率は上がるが、低温では速度が落ちるため触媒と高温の妥協点をとる。",
        )
        val solvay = reaction(
            "アンモニアソーダ法（ソルベー法）",
            "NaCl + NH3 + CO2 + H2O → NaHCO3 + NH4Cl\n2NaHCO3 → Na2CO3 + H2O + CO2",
            "第2段階は加熱（熱分解）",
            "CO2 は CaCO3 → CaO + CO2 から供給し、NH3 は CaO と NH4Cl から回収して循環させる。",
        )
        val thermite = reaction(
            "テルミット反応",
            "2Al + Fe2O3 → Al2O3 + 2Fe",
            "点火（マグネシウムリボンなど）",
            "Al の酸素との親和力が大きいことを利用した還元。多量の熱が出て鉄が融解する。",
        )
        val cuHot = reaction(
            "銅と熱濃硫酸",
            "Cu + 2H2SO4 → CuSO4 + SO2 + 2H2O",
            "加熱した濃硫酸",
            "Cu はイオン化傾向が水素より小さく、希硫酸には溶けない。熱濃硫酸の酸化作用で溶ける。",
        )
        val cuDilNitric = reaction(
            "銅と希硝酸",
            "3Cu + 8HNO3 → 3Cu(NO3)2 + 2NO + 4H2O",
            "希硝酸、常温",
            "発生する気体は無色の NO。濃硝酸では赤褐色の NO2 になる点と対で覚える。",
        )
        val cuConcNitric = reaction(
            "銅と濃硝酸",
            "Cu + 4HNO3 → Cu(NO3)2 + 2NO2 + 2H2O",
            "濃硝酸、常温",
            "赤褐色の NO2 が発生。希硝酸との違いは「濃いほど窒素の酸化数が高いまま残る」と整理する。",
        )

        fun link(a: Long, b: Long, t: LinkType, memo: String) = s.link(a, b, t, memo)

        link(contact, so2, LinkType.PRODUCES, "接触法 第1段階の生成物")
        link(contact, so3, LinkType.PRODUCES, "接触法 第2段階の生成物")
        link(contact, h2so4, LinkType.PRODUCES, "最終生成物")
        link(so2, so3, LinkType.CONTRAST, "酸化されて SO3 へ。硫黄の酸化数 +4 → +6")
        link(so2, h2s, LinkType.REACTS_WITH, "SO2 + 2H2S → 3S + 2H2O：SO2 が酸化剤としてはたらく")

        link(ostwald, nh3, LinkType.PRODUCES, "原料")
        link(ostwald, no, LinkType.PRODUCES, "第1段階の生成物")
        link(ostwald, no2, LinkType.PRODUCES, "第2段階の生成物")
        link(ostwald, hno3, LinkType.PRODUCES, "最終生成物")
        link(no, no2, LinkType.CONTRAST, "無色・水に溶けにくい ↔ 赤褐色・水に溶ける")
        link(haber, nh3, LinkType.PRODUCES, "生成物")
        link(haber, ostwald, LinkType.RELATED, "ハーバー・ボッシュ法の NH3 がオストワルト法の原料になる")

        link(solvay, na2co3, LinkType.PRODUCES, "最終生成物")
        link(solvay, caco3, LinkType.RELATED, "CO2 の供給源かつ NH3 回収に使う CaO の原料")
        link(na2co3, naoh, LinkType.CONFUSABLE, "どちらもナトリウムの塩基性物質。潮解（NaOH）と風解（Na2CO3·10H2O）を対で覚える")

        link(cuHot, h2so4, LinkType.RELATED, "熱濃硫酸の酸化作用")
        link(cuHot, so2, LinkType.PRODUCES, "発生する気体")
        link(cuHot, cuso4, LinkType.PRODUCES, "生成する塩")
        link(cuDilNitric, no, LinkType.PRODUCES, "無色の NO が発生")
        link(cuConcNitric, no2, LinkType.PRODUCES, "赤褐色の NO2 が発生")
        link(cuDilNitric, cuConcNitric, LinkType.CONTRAST, "濃度で生成する窒素酸化物が変わる — 対で覚える")
        link(cuDilNitric, hno3, LinkType.RELATED, "希硝酸の酸化作用")
        link(cuConcNitric, hno3, LinkType.RELATED, "濃硝酸の酸化作用")

        link(thermite, al2o3, LinkType.PRODUCES, "生成する酸化物")
        link(al2o3, cl2, LinkType.RELATED, "いずれも工業的に電解・酸化還元と結びつく")
    }

    // ---- 和文英訳 -------------------------------------------------------------

    private fun seedEisakubun(s: Seeder) {
        val deckId = s.deck(
            name = EISAKUBUN_DECK,
            type = NoteType.EISAKUBUN,
            templates = setOf("ja_en_write"),
            newPerDay = 3,
        )

        fun sentence(ja: String, en: String, structures: String, traps: String) = s.note(
            deckId, NoteType.EISAKUBUN,
            mapOf(
                "ja" to ja, "en" to en, "structures" to structures, "traps" to traps, "memo" to "",
            ),
            listOf("和文英訳"),
        )

        sentence(
            "彼の言うことは、どうも腑に落ちない。",
            "Somehow what he says doesn't quite make sense to me.",
            "「腑に落ちない」を doesn't make sense / doesn't convince me と言い換える\n" +
                "「どうも」を somehow で受け、「どうも〜ない」に not quite を添える\n" +
                "「彼の言うこと」を what he says（関係代名詞 what）で出す",
            "「腑に落ちない」を慣用句のまま訳そうとしない。意味は「納得できない」。",
        )
        sentence(
            "本を読むのは、他人の頭で考えることだ。",
            "To read a book is to think with someone else's head.",
            "「〜のは…ことだ」を to 不定詞または動名詞でそろえる\n" +
                "「他人の頭で」を with someone else's head と前置詞で出す",
            "主語と補語の形をそろえる（To read 〜 is to think 〜）。片方だけ動名詞にしない。",
        )
        sentence(
            "知らないということを知っているだけ、彼はましだ。",
            "He is better off in that he at least knows that he knows nothing.",
            "「〜だけましだ」を be better off で出す\n" +
                "「〜という点で」を in that 節で補う\n" +
                "「知らないということを知っている」を knows that he knows nothing と入れ子にする",
            "「ましだ」に good の比較級を当てない。better off / at least で処理する。",
        )
        sentence(
            "若いうちの苦労は買ってでもせよ、とはよく言ったものだ。",
            "How true it is that you should seek out hardship while you are young.",
            "「とはよく言ったものだ」を How true it is that … で出す\n" +
                "「買ってでもせよ」を seek out と言い換える\n" +
                "「若いうち」を while you are young で出す",
            "「買う」を buy と訳さない。「進んで求めよ」の意味。",
        )
        sentence(
            "彼女は口を開けば人の悪口ばかりだ。",
            "She never opens her mouth without speaking ill of someone.",
            "「〜すれば必ず…」を never … without -ing で出す\n" +
                "「悪口を言う」を speak ill of で出す",
            "「ばかりだ」を only で処理しない。二重否定の構文に落とすと自然になる。",
        )

        val byJa = s.repo.listNotes(deckId, "", Int.MAX_VALUE).associateBy { it.title() }
        fun link(a: String, b: String, t: LinkType, memo: String) {
            val x = byJa[a]?.id ?: return
            val y = byJa[b]?.id ?: return
            s.link(x, y, t, memo)
        }
        link(
            "知らないということを知っているだけ、彼はましだ。",
            "若いうちの苦労は買ってでもせよ、とはよく言ったものだ。",
            LinkType.SAME_GROUP, "どちらも「日本語の慣用表現を意味に開いてから英語にする」型",
        )
        link(
            "彼女は口を開けば人の悪口ばかりだ。",
            "彼の言うことは、どうも腑に落ちない。",
            LinkType.CONTRAST, "否定構文で処理する / 婉曲表現で処理する の対比",
        )
    }

    // ---- 化学の計算 -----------------------------------------------------------

    private fun seedChemCalc(s: Seeder) {
        val deckId = s.deck(
            name = CALC_DECK,
            type = NoteType.CHEM_CALC,
            templates = setOf("calc"),
            newPerDay = 4,
        )

        fun problem(
            question: String, answer: String, unit: String, tolerance: String, solution: String,
        ) = s.note(
            deckId, NoteType.CHEM_CALC,
            mapOf(
                "question" to question, "answer" to answer, "unit" to unit,
                "tolerance" to tolerance, "solution" to solution, "memo" to "",
            ),
            listOf("計算"),
        )

        val mol = problem(
            "標準状態（0℃、1.013×10^5 Pa）で 5.6 L の酸素は何 mol か。",
            "0.25", "mol", "1",
            "気体 1 mol の体積は標準状態で 22.4 L。5.6 ÷ 22.4 = 0.25 mol",
        )
        val molarity = problem(
            "塩化ナトリウム（式量 58.5）11.7 g を水に溶かして 500 mL にした。モル濃度は何 mol/L か。",
            "0.400", "mol/L", "1",
            "物質量 = 11.7 ÷ 58.5 = 0.200 mol。0.200 ÷ 0.500 L = 0.400 mol/L",
        )
        val ideal = problem(
            "27℃、1.0×10^5 Pa で 2.0 mol の理想気体が占める体積は何 L か。R = 8.3×10^3 Pa·L/(mol·K)",
            "50", "L", "2",
            "V = nRT/P = 2.0 × 8.3×10^3 × 300 ÷ 1.0×10^5 ≒ 50 L。絶対温度に直すのを忘れない。",
        )
        val strongAcid = problem(
            "0.010 mol/L の塩酸の pH はいくらか。",
            "2.0", "", "1",
            "塩酸は強酸で完全に電離するので [H+] = 1.0×10^-2 mol/L。pH = 2.0",
        )
        val weakAcid = problem(
            "0.10 mol/L の酢酸水溶液の pH はいくらか。電離定数 Ka = 2.7×10^-5 mol/L",
            "2.8", "", "2",
            "弱酸なので [H+] = √(Ka·c) = √(2.7×10^-5 × 0.10) ≒ 1.6×10^-3。pH ≒ 2.8",
        )
        val combustion = problem(
            "メタン CH4 1.0 mol を完全燃焼させるのに必要な酸素は何 mol か。",
            "2.0", "mol", "1",
            "CH4 + 2O2 → CO2 + 2H2O。係数比より酸素は 2 倍の 2.0 mol",
        )
        val percent = problem(
            "質量パーセント濃度 20% の水酸化ナトリウム水溶液 200 g に含まれる NaOH は何 g か。",
            "40", "g", "1",
            "200 g × 0.20 = 40 g",
        )

        s.link(strongAcid, weakAcid, LinkType.CONTRAST, "強酸は [H+] = c、弱酸は [H+] = √(Ka·c)。ここを取り違えやすい")
        s.link(mol, ideal, LinkType.RELATED, "標準状態の 22.4 L は気体の状態方程式から出る特別な場合")
        s.link(molarity, percent, LinkType.CONTRAST, "モル濃度は体積あたり、質量パーセントは質量あたり")
        s.link(combustion, mol, LinkType.RELATED, "係数比から物質量を出す流れは共通")
    }

    // ---- 英文和訳 -------------------------------------------------------------

    /**
     * Translating a marked passage into Japanese.
     *
     * Every sentence here turns on a structure rather than on vocabulary — the
     * concessive, the inversion, the double negative — because that is what the marker
     * is looking at, and it is what the checklist makes explicit.
     */
    private fun seedWayaku(s: Seeder) {
        val deckId = s.deck(
            name = WAYAKU_DECK,
            type = NoteType.WAYAKU,
            templates = setOf("en_ja_write"),
            newPerDay = 3,
        )

        fun passage(en: String, ja: String, structures: String, traps: String, tag: String) = s.note(
            deckId, NoteType.WAYAKU,
            mapOf("en" to en, "ja" to ja, "structures" to structures, "traps" to traps, "memo" to ""),
            listOf("英文和訳", tag),
        )

        val notThat = passage(
            "It is not that he cannot do the work, but that he will not.",
            "彼にその仕事ができないのではなく、やろうとしないのだ。",
            "It is not that A but that B を「AではなくBなのだ」と訳し分ける\n" +
                "will に「意志」の意味を持たせて「やろうとしない」と訳す",
            "will not を単純未来（〜しないだろう）で訳さない。",
            "構文",
        )
        val doubleNegative = passage(
            "No one is so old that he cannot learn something new.",
            "何か新しいことを学べないほど年をとっている人はいない。",
            "so … that ~ not を「〜ないほど…」と後ろから訳す\n" +
                "No one … cannot の二重否定を、そのまま訳すか「誰でも学べる」と開くか決める",
            "否定が二重にかかっている。訳したあとに肯定文に開いて意味を検算する。",
            "否定",
        )
        val concession = passage(
            "What he says sounds reasonable enough, but it does not stand up to close examination.",
            "彼の言うことは一応もっともらしく聞こえるが、詳しく検討すると成り立たない。",
            "形容詞 + enough の「一応〜ではある」という譲歩の含みを訳に出す\n" +
                "stand up to を「〜に耐える」と処理する\n" +
                "What he says を「彼の言うこと」と名詞節のまま出す",
            "reasonable enough を「十分に合理的だ」と訳すと、後半の but と噛み合わなくなる。",
            "語義",
        )
        val comparison = passage(
            "The discovery of the new drug owes less to a flash of genius than to years of patient failure.",
            "その新薬の発見は、天才のひらめきによるものというより、何年もの辛抱強い失敗によるところが大きい。",
            "owe A to B「A は B のおかげである」を土台に置く\n" +
                "less … than ~ は「…というより〜」で、重いのは than の後ろ\n" +
                "無生物主語を「〜によるところが大きい」と日本語の主述に組み替える",
            "less A than B の軽重を逆にしない。強調されるのは B のほう。",
            "比較",
        )
        val inversion = passage(
            "Not until he had lost his health did he realize how much it had meant to him.",
            "健康を失って初めて、それが自分にとってどれほど大切だったかを悟った。",
            "Not until … + 倒置 を「…して初めて〜した」と訳す\n" +
                "過去完了を「失ってから悟った」の前後関係として出す\n" +
                "how much it had meant to him を「どれほど大切だったか」と意訳する",
            "倒置の did を訳に出さない。文頭の否定語に引きずられて全体を否定で訳さない。",
            "構文",
        )
        val insertion = passage(
            "Science, far from being a mere collection of facts, is a way of asking questions.",
            "科学は、単なる事実の寄せ集めどころか、問いの立て方そのものである。",
            "far from -ing を「〜どころか」と訳す\n" +
                "挿入句をいったん外し、Science is a way of asking questions の骨格を先に取る\n" +
                "a way of asking questions を「問いの立て方」と名詞化する",
            "far from を「〜から遠い」と直訳しない。挿入句を主語の修飾として訳し込まない。",
            "構文",
        )

        s.link(notThat, inversion, LinkType.SAME_GROUP, "どちらも「構文の形をそのまま訳の形にする」型")
        s.link(doubleNegative, concession, LinkType.CONTRAST, "否定を開いて訳す / 含みを足して訳す")
        s.link(comparison, doubleNegative, LinkType.CONFUSABLE, "so…that not と less…than。どちらも軽重を取り違えやすい")
        s.link(insertion, concession, LinkType.SAME_GROUP, "骨格をいったん取り出してから修飾を戻す")
    }

    // ---- deck names, shared by the packs that add to them --------------------

    internal const val ENGLISH_DECK = "英単語（語源でつなぐ）"
    internal const val INORGANIC_DECK = "化学・無機物質"
    internal const val REACTION_DECK = "化学・工業的製法と反応"
    internal const val EISAKUBUN_DECK = "和文英訳（直訳できない日本語）"
    internal const val CALC_DECK = "化学・計算"
    internal const val WAYAKU_DECK = "英文和訳（構文で取る）"
}

/**
 * Installs bundled content without ever overwriting the learner's own.
 *
 * A deck or a note that is already there is left exactly as it stands — packs are
 * installed onto phones that have been in use for months, and new material must
 * never clobber an edit, a tag or a schedule.
 */
class Seeder(val repo: Repository) {

    private val titles = HashMap<Long, MutableMap<String, Long>>()

    fun deck(
        name: String,
        type: NoteType,
        templates: Set<String>,
        newPerDay: Int,
        relationQuiz: Boolean = false,
    ): Long {
        repo.listDecks().firstOrNull { it.name == name }?.let { return it.id }
        return repo.saveDeck(
            Deck(
                name = name,
                noteTypeId = type.id,
                enabledTemplates = templates,
                newPerDay = newPerDay,
                relationQuiz = relationQuiz,
            )
        )
    }

    /** Add a note, or return the one already carrying that heading word. */
    fun note(deckId: Long, type: NoteType, fields: Map<String, String>, tags: List<String>): Long {
        val known = titles.getOrPut(deckId) {
            repo.listNotes(deckId, "", Int.MAX_VALUE).associateTo(HashMap()) { it.title() to it.id }
        }
        val candidate = Note(deckId = deckId, typeId = type.id, fields = fields, tags = tags)
        known[candidate.title()]?.let { return it }
        val id = repo.saveNote(candidate)
        known[candidate.title()] = id
        return id
    }

    fun link(a: Long, b: Long, type: LinkType, memo: String = "") {
        repo.addLink(a, b, type, memo)
    }
}
