package com.tango.recall.data

/**
 * 無機化学の続き — 気体の製法、沈殿、両性と錯イオン、金属の取り出し方。
 *
 * Inorganic chemistry looks like a list to be memorised and is not: the gases are
 * made by a handful of moves (a stronger acid driving out a weaker one, a carbonate
 * decomposing, an oxidising agent meeting concentrated hydrochloric acid), and the
 * precipitates are worth knowing as pairs — what dissolves again in excess, and in
 * what. The notes are written around those axes.
 *
 * Every equation is checked by ChemistryDataTest.
 */
internal fun seedInorganic(s: Seeder) {
    val substances = s.deck(
        name = Seed.INORGANIC_DECK,
        type = NoteType.CHEM_SUBSTANCE,
        templates = setOf("name_formula", "formula_name", "name_props"),
        newPerDay = 10,
        relationQuiz = true,
    )
    val reactions = s.deck(
        name = Seed.REACTION_DECK,
        type = NoteType.CHEM_REACTION,
        templates = setOf("title_eq", "eq_title", "title_cond"),
        newPerDay = 6,
        relationQuiz = true,
    )

    fun substance(
        name: String, formula: String, category: String, props: String, uses: String,
        vararg tags: String,
    ): Long = s.note(
        substances, NoteType.CHEM_SUBSTANCE,
        mapOf(
            "name" to name, "formula" to formula, "category" to category,
            "props" to props, "uses" to uses, "memo" to "",
        ),
        listOf("無機", category) + tags,
    )

    fun reaction(
        title: String, equation: String, condition: String, point: String,
        vararg tags: String,
    ): Long = s.note(
        reactions, NoteType.CHEM_REACTION,
        mapOf(
            "title" to title, "equation" to equation,
            "condition" to condition, "point" to point, "memo" to "",
        ),
        listOf("無機") + tags,
    )

    // ---- 気体 -----------------------------------------------------------------

    val hcl = substance(
        "塩化水素", "HCl", "酸性気体",
        "無色・刺激臭。水に極めてよく溶け、水溶液は塩酸（強酸）。空気より重い。",
        "下方置換で捕集し、濃硫酸で乾燥する。アンモニアと出会うと白煙（NH4Cl）。",
        "気体",
    )
    val co2 = substance(
        "二酸化炭素", "CO2", "酸性酸化物",
        "無色・無臭。空気より重く、水に少し溶けて弱い酸性を示す。",
        "下方置換で捕集。石灰水に通すと白濁し、さらに通し続けると溶けて澄む。",
        "気体",
    )
    val h2 = substance(
        "水素", "H2", "単体",
        "無色・無臭で最も軽い気体。水に溶けにくい。",
        "水上置換で捕集する。イオン化傾向が水素より大きい金属と酸から発生する。",
        "気体",
    )
    val o2 = substance(
        "酸素", "O2", "単体",
        "無色・無臭。水に溶けにくく、助燃性がある。",
        "水上置換で捕集する。過酸化水素水に酸化マンガン(IV)を触媒として加えて発生させる。",
        "気体",
    )
    val co = substance(
        "一酸化炭素", "CO", "中性酸化物",
        "無色・無臭で有毒。水に溶けにくい。強い還元性をもつ。",
        "水上置換で捕集する。溶鉱炉では鉄の酸化物を還元する役をになう。",
        "気体",
    )
    val ozone = substance(
        "オゾン", "O3", "単体",
        "淡青色・特異臭。酸素中の放電や紫外線で生じる。強い酸化作用をもつ。",
        "湿らせたヨウ化カリウムデンプン紙を青紫色にする（酸化力の検出）。",
        "気体",
    )
    val p4o10 = substance(
        "十酸化四リン", "P4O10", "酸性酸化物",
        "白色の粉末。強い吸湿性（潮解性）をもち、水と反応するとリン酸になる。",
        "中性・酸性気体の乾燥剤。アンモニアのような塩基性気体の乾燥には使えない。",
    )
    val cao = substance(
        "酸化カルシウム", "CaO", "塩基性酸化物",
        "生石灰。白色固体で、水と反応して激しく発熱し水酸化カルシウムになる。",
        "乾燥剤。ソーダ石灰としてアンモニアの乾燥にも使える（塩基性気体に酸性の乾燥剤は不可）。",
    )
    val caoh2 = substance(
        "水酸化カルシウム", "Ca(OH)2", "塩基",
        "消石灰。水に少し溶け、その飽和水溶液が石灰水。強塩基。",
        "二酸化炭素を通すと炭酸カルシウムの白濁。さらに通すと炭酸水素カルシウムになって溶ける。",
    )

    // ---- 沈殿 -----------------------------------------------------------------

    val agcl = substance(
        "塩化銀", "AgCl", "沈殿",
        "白色の沈殿。光が当たると分解して黒ずむ（感光性）。",
        "アンモニア水を加えると錯イオンをつくって溶ける。塩化物イオンの検出に使う。",
        "沈殿",
    )
    val baso4 = substance(
        "硫酸バリウム", "BaSO4", "沈殿",
        "白色の沈殿。水にも酸にもほとんど溶けない。",
        "硫酸イオンの検出。X線をよく吸収するので造影剤に使う。",
        "沈殿",
    )
    val aloh3 = substance(
        "水酸化アルミニウム", "Al(OH)3", "両性水酸化物",
        "白色のゲル状沈殿。酸にも強塩基にも溶ける両性。",
        "過剰の水酸化ナトリウム水溶液には溶けるが、過剰のアンモニア水には溶けない。",
        "沈殿", "両性",
    )
    val znoh2 = substance(
        "水酸化亜鉛", "Zn(OH)2", "両性水酸化物",
        "白色の沈殿。酸にも強塩基にも溶ける両性。",
        "過剰のアンモニア水にも錯イオンをつくって溶ける。ここがアルミニウムとの分かれ目。",
        "沈殿", "両性",
    )
    val feoh3 = substance(
        "水酸化鉄(III)", "Fe(OH)3", "沈殿",
        "赤褐色の沈殿。過剰の塩基にも過剰のアンモニア水にも溶けない。",
        "鉄(III)イオンの検出。チオシアン酸カリウムでは血赤色になる。",
        "沈殿",
    )
    val cus = substance(
        "硫化銅(II)", "CuS", "沈殿",
        "黒色の沈殿。溶解度が非常に小さく、酸性の水溶液中でも沈殿する。",
        "系統分離では、酸性で硫化水素を通した段階で落ちてくる組（Cu²⁺・Pb²⁺ など）。",
        "沈殿",
    )
    val zns = substance(
        "硫化亜鉛", "ZnS", "沈殿",
        "白色の沈殿。酸性では溶けてしまい、中性・塩基性でのみ沈殿する。",
        "硫化銅(II) との違いが、系統分離で硫化水素を2回に分けて通す理由そのもの。",
        "沈殿",
    )
    val cuAmmine = substance(
        "テトラアンミン銅(II)イオン", "[Cu(NH3)4]^2+", "錯イオン",
        "深青色。正方形の4配位。銅(II) イオンに過剰のアンモニア水を加えると生じる。",
        "水酸化銅(II) の青白色沈殿が、アンモニア水を加え続けると溶けて深青色になる。",
        "錯イオン",
    )
    val agAmmine = substance(
        "ジアンミン銀(I)イオン", "[Ag(NH3)2]^+", "錯イオン",
        "無色。直線形の2配位。銀イオンに過剰のアンモニア水を加えると生じる。",
        "アンモニア性硝酸銀水溶液の正体で、銀鏡反応の試薬になる。",
        "錯イオン",
    )

    // ---- 気体の製法 ------------------------------------------------------------

    val makeHcl = reaction(
        "塩化水素の製法",
        "NaCl + H2SO4 → NaHSO4 + HCl",
        "濃硫酸を加えて加熱",
        "不揮発性の濃硫酸が揮発性の酸を追い出す。「不揮発性の酸＋塩 → 揮発性の酸」の型。",
        "気体の製法",
    )
    val makeCo2 = reaction(
        "二酸化炭素の製法",
        "CaCO3 + 2HCl → CaCl2 + H2O + CO2",
        "石灰石に希塩酸、常温",
        "弱酸の塩に強酸を加えて弱酸を追い出す型。希硫酸だと CaSO4 が表面を覆って止まる。",
        "気体の製法",
    )
    val makeH2 = reaction(
        "水素の製法",
        "Zn + H2SO4 → ZnSO4 + H2",
        "亜鉛に希硫酸、常温",
        "イオン化傾向が水素より大きい金属を使う。銅では発生しない。",
        "気体の製法",
    )
    val makeO2 = reaction(
        "酸素の製法",
        "2H2O2 → 2H2O + O2",
        "過酸化水素水に酸化マンガン(IV) を触媒として加える、常温",
        "酸化マンガン(IV) は触媒なので反応の前後で変化しない。塩素の製法では酸化剤として働くのと対で覚える。",
        "気体の製法",
    )
    val makeNh3 = reaction(
        "アンモニアの製法（実験室）",
        "2NH4Cl + Ca(OH)2 → CaCl2 + 2NH3 + 2H2O",
        "塩化アンモニウムと水酸化カルシウムの混合物を加熱",
        "弱塩基の塩に強塩基を加えて弱塩基を追い出す型。上方置換で捕集し、ソーダ石灰で乾燥する。",
        "気体の製法",
    )
    val makeCl2 = reaction(
        "塩素の製法",
        "MnO2 + 4HCl → MnCl2 + Cl2 + 2H2O",
        "酸化マンガン(IV) に濃塩酸を加えて加熱",
        "ここでの酸化マンガン(IV) は酸化剤。発生した塩素は水と濃硫酸に通してから捕集する。",
        "気体の製法",
    )
    val makeH2s = reaction(
        "硫化水素の製法",
        "FeS + H2SO4 → FeSO4 + H2S",
        "硫化鉄(II) に希硫酸、常温",
        "弱酸の塩＋強酸の型。下方置換で捕集する。",
        "気体の製法",
    )
    val makeCo = reaction(
        "一酸化炭素の製法",
        "HCOOH → CO + H2O",
        "ギ酸に濃硫酸を加えて加熱（脱水）",
        "濃硫酸の脱水作用を使う。有毒なので水上置換で捕集する。",
        "気体の製法",
    )

    // ---- 沈殿と錯イオン ---------------------------------------------------------

    val limewater = reaction(
        "石灰水の白濁とその消失",
        "Ca(OH)2 + CO2 → CaCO3 + H2O\nCaCO3 + CO2 + H2O → Ca(HCO3)2",
        "石灰水に二酸化炭素を通し続ける、常温",
        "白濁してから澄むまでが1組。炭酸水素カルシウムは水に溶けるので濁りが消える。",
    )
    val amphoteric = reaction(
        "水酸化アルミニウムの両性",
        "Al(OH)3 + 3HCl → AlCl3 + 3H2O\nAl(OH)3 + NaOH → Na[Al(OH)4]",
        "常温。塩基側は過剰の水酸化ナトリウム水溶液",
        "酸にも強塩基にも溶けるのが両性。過剰のアンモニア水には溶けないところが亜鉛との差。",
        "両性",
    )
    val zincAmmine = reaction(
        "水酸化亜鉛とアンモニア水",
        "Zn(OH)2 + 4NH3 → [Zn(NH3)4]^2+ + 2OH^-",
        "過剰のアンモニア水、常温",
        "亜鉛はアンモニア水にも溶ける。アルミニウムはここで溶け残るので、両者を分けられる。",
        "錯イオン",
    )
    val copperAmmine = reaction(
        "水酸化銅(II)とアンモニア水",
        "Cu(OH)2 + 4NH3 → [Cu(NH3)4]^2+ + 2OH^-",
        "過剰のアンモニア水、常温",
        "青白色の沈殿がいったんでき、加え続けると溶けて深青色になる。",
        "錯イオン",
    )
    val silverChloride = reaction(
        "塩化銀の生成とアンモニア水への溶解",
        "AgNO3 + NaCl → AgCl + NaNO3\nAgCl + 2NH3 → [Ag(NH3)2]^+ + Cl^-",
        "常温。溶解側は過剰のアンモニア水",
        "白色沈殿ができ、アンモニア水で溶ける。塩化物イオンの検出と、銀鏡反応の試薬づくりが同じ流れ。",
        "沈殿",
    )
    val sulfate = reaction(
        "硫酸イオンの検出",
        "BaCl2 + H2SO4 → BaSO4 + 2HCl",
        "常温",
        "硫酸バリウムは酸にも溶けないので、白色沈殿が消えないことが確認になる。",
        "沈殿",
    )
    val ironHydroxide = reaction(
        "鉄(III)イオンと塩基",
        "FeCl3 + 3NaOH → Fe(OH)3 + 3NaCl",
        "常温",
        "赤褐色の沈殿。過剰の塩基にもアンモニア水にも溶けないので、両性の金属と区別できる。",
        "沈殿",
    )

    // ---- 金属の取り出し方 -------------------------------------------------------

    val blastFurnace = reaction(
        "溶鉱炉（鉄の製錬）",
        "Fe2O3 + 3CO → 2Fe + 3CO2",
        "コークスから生じた一酸化炭素で還元する、約1500℃",
        "還元剤は炭素そのものではなく一酸化炭素。得られる銑鉄は炭素を多く含み硬くてもろい。",
    )
    val electrolysisAl = reaction(
        "アルミニウムの溶融塩電解",
        "2Al2O3 → 4Al + 3O2",
        "氷晶石を加えて融点を下げ、約1000℃ で融解塩電解する",
        "アルミニウムはイオン化傾向が大きく水溶液の電解では取り出せない。氷晶石は融点を下げるために加える。",
    )
    val refiningCu = reaction(
        "銅の電解精錬",
        "Cu → Cu^2+ + 2e^-（陽極）\nCu^2+ + 2e^- → Cu（陰極）",
        "粗銅を陽極、純銅を陰極にして硫酸銅(II)水溶液を電解する。電圧は低く保つ",
        "銅よりイオン化傾向の小さい金・銀は溶けずに陽極泥として沈む。大きい鉄・亜鉛は溶けたまま残る。",
    )
    val ionExchange = reaction(
        "イオン交換膜法（水酸化ナトリウムの工業的製法）",
        "2NaCl + 2H2O → 2NaOH + H2 + Cl2",
        "陽イオン交換膜で仕切った電解槽で塩化ナトリウム水溶液を電解する",
        "陽極で塩素、陰極で水素と水酸化ナトリウム。膜が陽イオンだけを通すので、生成物が混ざらない。",
    )

    // ---- つながり ---------------------------------------------------------------

    s.link(makeHcl, hcl, LinkType.PRODUCES, "生成する気体")
    s.link(makeCo2, co2, LinkType.PRODUCES, "生成する気体")
    s.link(makeH2, h2, LinkType.PRODUCES, "生成する気体")
    s.link(makeO2, o2, LinkType.PRODUCES, "生成する気体")
    s.link(makeH2s, makeCo2, LinkType.SAME_GROUP, "どちらも「弱酸の塩＋強酸 → 弱酸」の型")
    s.link(makeHcl, makeCo2, LinkType.CONTRAST, "不揮発性で追い出す / 酸の強さで追い出す")
    s.link(makeCl2, makeO2, LinkType.CONFUSABLE, "どちらも酸化マンガン(IV)。塩素では酸化剤、酸素では触媒")
    s.link(makeCo, co, LinkType.PRODUCES, "生成する気体")
    s.link(makeNh3, makeCo2, LinkType.CONTRAST, "弱塩基を追い出す / 弱酸を追い出す")

    s.link(co2, caoh2, LinkType.REACTS_WITH, "石灰水を白濁させる")
    s.link(limewater, co2, LinkType.RELATED, "白濁と、その消失")
    s.link(cao, caoh2, LinkType.PRODUCES, "水と反応して水酸化カルシウムになる")
    s.link(p4o10, cao, LinkType.CONTRAST, "酸性の乾燥剤 ↔ 塩基性の乾燥剤。気体の性質で使い分ける")
    s.link(co, blastFurnace, LinkType.RELATED, "鉄の酸化物を還元する")
    s.link(ozone, o2, LinkType.SAME_GROUP, "酸素の同素体。酸化力はオゾンのほうが強い")

    s.link(aloh3, znoh2, LinkType.CONFUSABLE, "どちらも両性。過剰のアンモニア水に溶けるのは水酸化亜鉛だけ")
    s.link(amphoteric, aloh3, LinkType.RELATED, "両性の確認")
    s.link(zincAmmine, znoh2, LinkType.RELATED, "アンモニア水に溶ける")
    s.link(copperAmmine, cuAmmine, LinkType.PRODUCES, "深青色の錯イオン")
    s.link(silverChloride, agcl, LinkType.PRODUCES, "白色沈殿")
    s.link(silverChloride, agAmmine, LinkType.PRODUCES, "アンモニア水に溶けて生じる錯イオン")
    s.link(cus, zns, LinkType.CONTRAST, "酸性でも沈殿する ↔ 中性・塩基性でしか沈殿しない")
    s.link(feoh3, aloh3, LinkType.CONTRAST, "過剰の塩基に溶けない ↔ 溶ける（両性）")
    s.link(ironHydroxide, feoh3, LinkType.PRODUCES, "赤褐色の沈殿")
    s.link(sulfate, baso4, LinkType.PRODUCES, "白色沈殿")
    s.link(agcl, baso4, LinkType.SAME_GROUP, "代表的な白色沈殿。アンモニア水に溶けるかどうかで区別する")

    s.link(electrolysisAl, refiningCu, LinkType.CONTRAST, "融解塩電解でしか取り出せない金属 ↔ 水溶液の電解で精製できる金属")
    s.link(ionExchange, refiningCu, LinkType.SAME_GROUP, "どちらも電気分解を使う工業的製法")
    s.link(blastFurnace, electrolysisAl, LinkType.CONTRAST, "一酸化炭素で還元する ↔ 電気で還元する")
}
