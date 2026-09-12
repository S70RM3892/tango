package com.tango.recall.data

/**
 * 有機化学.
 *
 * Organic chemistry is where relations earn their keep: almost nothing here is worth
 * memorising on its own. An alcohol is one oxidation step from an aldehyde and two
 * from a carboxylic acid; aniline is nitrobenzene reduced; salicylic acid is one
 * reagent away from aspirin and one away from the ointment smell. Those chains are
 * the content, so the notes are written to be linked rather than listed.
 *
 * Every equation here is checked by ChemistryDataTest, which counts the atoms.
 */
internal fun seedOrganic(s: Seeder) {
    val compounds = s.deck(
        name = ORGANIC_DECK,
        type = NoteType.CHEM_SUBSTANCE,
        templates = setOf("name_formula", "formula_name", "name_props"),
        newPerDay = 8,
        relationQuiz = true,
    )
    val reactions = s.deck(
        name = ORGANIC_REACTION_DECK,
        type = NoteType.CHEM_REACTION,
        templates = setOf("title_eq", "eq_title", "title_cond"),
        newPerDay = 5,
        relationQuiz = true,
    )

    fun compound(
        name: String, formula: String, category: String, props: String, uses: String,
        vararg tags: String,
    ): Long = s.note(
        compounds, NoteType.CHEM_SUBSTANCE,
        mapOf(
            "name" to name, "formula" to formula, "category" to category,
            "props" to props, "uses" to uses, "memo" to "",
        ),
        listOf("有機", category) + tags,
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
        listOf("有機") + tags,
    )

    // ---- 脂肪族 ---------------------------------------------------------------

    val methanol = compound(
        "メタノール", "CH3OH", "アルコール",
        "無色の液体。水と任意の割合で混ざる。有毒で、飲むと失明の危険がある。",
        "酸化するとホルムアルデヒド、さらに酸化するとギ酸になる。ヨードホルム反応は陰性。",
    )
    val ethanol = compound(
        "エタノール", "C2H5OH", "アルコール",
        "無色の液体。水と任意の割合で混ざる。中性でナトリウムと反応して水素を発生する。",
        "濃硫酸と 170℃ でエチレン、130〜140℃ でジエチルエーテル。酸化するとアセトアルデヒド。",
        "ヨードホルム",
    )
    val propanol2 = compound(
        "2-プロパノール", "CH3CH(OH)CH3", "アルコール",
        "第二級アルコール。酸化するとケトン（アセトン）になり、それ以上は酸化されない。",
        "CH3CH(OH)- の構造をもつのでヨードホルム反応は陽性。",
        "ヨードホルム",
    )
    val glycol = compound(
        "エチレングリコール", "C2H4(OH)2", "アルコール",
        "2価アルコール。粘性が高く、水によく溶ける。沸点が高い。",
        "不凍液。テレフタル酸と縮合重合させるとポリエチレンテレフタラート（PET）になる。",
    )
    val glycerol = compound(
        "グリセリン", "C3H5(OH)3", "アルコール",
        "3価アルコール。無色で粘性が高く、甘みがある。",
        "油脂をけん化すると必ず生じる。濃硝酸と濃硫酸でニトログリセリンになる。",
    )
    val formaldehyde = compound(
        "ホルムアルデヒド", "HCHO", "アルデヒド",
        "刺激臭のある気体。約 37% 水溶液がホルマリン。還元性を示す。",
        "メタノールの酸化で得る。銀鏡反応・フェーリング反応ともに陽性。フェノール樹脂の原料。",
        "還元性",
    )
    val acetaldehyde = compound(
        "アセトアルデヒド", "CH3CHO", "アルデヒド",
        "刺激臭のある液体（沸点 20℃）。還元性を示す。",
        "エタノールの酸化、またはエチレンの酸化（塩化パラジウム(II)触媒）で得る。酸化すると酢酸。",
        "還元性", "ヨードホルム",
    )
    val acetone = compound(
        "アセトン", "CH3COCH3", "ケトン",
        "無色の液体。水にも有機物にもよく溶ける溶媒。ケトンなので還元性はない。",
        "2-プロパノールの酸化、またはクメン法の副生成物。ヨードホルム反応は陽性。",
        "ヨードホルム",
    )
    val formicAcid = compound(
        "ギ酸", "HCOOH", "カルボン酸",
        "刺激臭のある液体。カルボキシ基とホルミル基（アルデヒド基）を併せ持つ。",
        "カルボン酸でありながら還元性を示し、銀鏡反応が陽性になる。濃硫酸で脱水すると CO。",
        "還元性",
    )
    val aceticAcid = compound(
        "酢酸", "CH3COOH", "カルボン酸",
        "刺激臭のある液体。弱酸。純粋なものは冬に凝固するので氷酢酸と呼ぶ。",
        "エタノールとエステル化すると酢酸エチル。還元性はない（ギ酸との違い）。",
    )
    val oxalicAcid = compound(
        "シュウ酸", "(COOH)2", "カルボン酸",
        "2価のカルボン酸。二水和物が安定な結晶で、正確な濃度の水溶液をつくれる。",
        "還元剤として過マンガン酸カリウムの滴定に使う。",
    )
    val maleic = compound(
        "マレイン酸", "C4H4O4", "カルボン酸",
        "シス形の不飽和ジカルボン酸。融点が低く、水に溶けやすい。",
        "2つのカルボキシ基が近いので、加熱すると分子内で脱水して無水マレイン酸になる。",
        "異性体",
    )
    val fumaric = compound(
        "フマル酸", "C4H4O4", "カルボン酸",
        "トランス形の不飽和ジカルボン酸。マレイン酸より融点が高く、水に溶けにくい。",
        "カルボキシ基が離れているため、加熱しても分子内脱水しない。",
        "異性体",
    )
    val ethylAcetate = compound(
        "酢酸エチル", "CH3COOC2H5", "エステル",
        "果実のような芳香をもつ液体。水に溶けにくい。",
        "酢酸とエタノールのエステル化で得る。水酸化ナトリウムで加水分解（けん化）すると酢酸ナトリウムとエタノール。",
    )
    val soap = compound(
        "セッケン", "RCOONa", "塩",
        "高級脂肪酸のナトリウム塩。弱酸と強塩基の塩なので、水溶液は加水分解して弱い塩基性を示す。",
        "硬水中では Ca²⁺・Mg²⁺ と難溶性の塩をつくって泡立たなくなる。",
    )
    val ethylene = compound(
        "エチレン", "CH2=CH2", "アルケン",
        "無色の気体。二重結合をもち、付加反応を起こしやすい。臭素水を脱色する。",
        "エタノールの分子内脱水で得る。付加重合するとポリエチレン。",
    )
    val acetylene = compound(
        "アセチレン", "C2H2", "アルキン",
        "無色の気体。三重結合をもつ。燃焼熱が大きく高温の炎になる。",
        "炭化カルシウムに水を加えて発生させる。赤熱した鉄管に通すと3分子重合してベンゼン。",
    )

    // ---- 芳香族 ---------------------------------------------------------------

    val benzene = compound(
        "ベンゼン", "C6H6", "芳香族",
        "正六角形の平面分子。炭素間の結合はすべて同等。水に溶けず、水より軽い。",
        "不飽和なのに付加より置換（ニトロ化・スルホン化・ハロゲン化）を起こしやすい。",
    )
    val toluene = compound(
        "トルエン", "C6H5CH3", "芳香族",
        "ベンゼンの水素1つをメチル基に置き換えた液体。",
        "硫酸酸性の過マンガン酸カリウムで側鎖が酸化され、安息香酸になる（環は酸化されない）。",
    )
    val phenol = compound(
        "フェノール", "C6H5OH", "芳香族",
        "特有のにおいをもつ固体。水に少し溶け、その水溶液は弱い酸性（炭酸より弱い）。",
        "塩化鉄(III)水溶液で紫色を呈する。臭素水で 2,4,6-トリブロモフェノールの白色沈殿。",
        "検出",
    )
    val benzoicAcid = compound(
        "安息香酸", "C6H5COOH", "芳香族",
        "白色の結晶。昇華性がある。カルボン酸なので炭酸より強い酸。",
        "トルエンの酸化で得る。炭酸水素ナトリウム水溶液に溶けて二酸化炭素を発生する（フェノールとの区別）。",
    )
    val salicylic = compound(
        "サリチル酸", "C6H4(OH)COOH", "芳香族",
        "フェノール性ヒドロキシ基とカルボキシ基を併せ持つ白色の結晶。",
        "塩化鉄(III)で紫色（フェノール性 OH）。無水酢酸でアセチルサリチル酸、メタノールでサリチル酸メチル。",
        "検出",
    )
    val aspirin = compound(
        "アセチルサリチル酸", "C6H4(OCOCH3)COOH", "エステル",
        "白色の結晶。サリチル酸のヒドロキシ基をアセチル化したもの。",
        "解熱鎮痛剤。フェノール性 OH が残っていないので塩化鉄(III)では呈色しない。",
    )
    val methylSalicylate = compound(
        "サリチル酸メチル", "C6H4(OH)COOCH3", "エステル",
        "特有の芳香をもつ液体。サリチル酸のカルボキシ基をエステル化したもの。",
        "消炎鎮痛の外用薬。フェノール性 OH が残るので塩化鉄(III)で紫色を示す。",
        "検出",
    )
    val nitrobenzene = compound(
        "ニトロベンゼン", "C6H5NO2", "芳香族",
        "淡黄色の油状の液体。水より重く、水に溶けない。",
        "ベンゼンの混酸によるニトロ化で得る。スズと塩酸で還元するとアニリンになる。",
    )
    val aniline = compound(
        "アニリン", "C6H5NH2", "芳香族",
        "無色の油状の液体。空気中で酸化されて褐色になる。水に溶けにくい弱い塩基。",
        "塩酸には塩をつくって溶ける。さらし粉水溶液で赤紫色。二クロム酸カリウムでアニリンブラック。",
        "検出",
    )

    // ---- 糖・アミノ酸 ---------------------------------------------------------

    val glucose = compound(
        "グルコース", "C6H12O6", "糖",
        "水溶液中では鎖状構造とα・β の環状構造が平衡にある。鎖状構造がホルミル基をもつ。",
        "還元性を示し、銀鏡反応・フェーリング反応ともに陽性。酵母でアルコール発酵する。",
        "還元性",
    )
    val sucrose = compound(
        "スクロース", "C12H22O11", "糖",
        "グルコースとフルクトースが還元性を示す部分どうしで結合しているため、還元性がない。",
        "希酸や転化酵素で加水分解すると、還元性のある転化糖（グルコースとフルクトースの等量混合物）になる。",
    )
    val glycine = compound(
        "グリシン", "H2NCH2COOH", "アミノ酸",
        "最も簡単なアミノ酸で、不斉炭素原子をもたない。分子内に酸の基と塩基の基を併せ持つ両性化合物。",
        "水溶液中では双性イオンとして存在する。電荷の総和が0になる pH が等電点。ニンヒドリン反応で紫色。",
    )

    // ---- 反応 -----------------------------------------------------------------

    val dehydration170 = reaction(
        "エタノールの分子内脱水（エチレンの生成）",
        "C2H5OH → CH2=CH2 + H2O",
        "濃硫酸、約170℃",
        "温度で生成物が変わる典型。高い温度では1分子から水が取れて二重結合ができる。",
    )
    val dehydration140 = reaction(
        "エタノールの分子間脱水（ジエチルエーテルの生成）",
        "2C2H5OH → C2H5OC2H5 + H2O",
        "濃硫酸、約130〜140℃",
        "低い温度では2分子から水1分子が取れて縮合する。170℃ との対で覚える。",
    )
    val oxidation1 = reaction(
        "第一級アルコールの酸化（エタノール → アセトアルデヒド）",
        "2C2H5OH + O2 → 2CH3CHO + 2H2O",
        "硫酸酸性の二クロム酸カリウムなどの酸化剤、または熱した銅",
        "第一級アルコール → アルデヒド → カルボン酸 と2段階で進む。ここで止めるには生成物を留去する。",
    )
    val oxidation2 = reaction(
        "アルデヒドの酸化（アセトアルデヒド → 酢酸）",
        "2CH3CHO + O2 → 2CH3COOH",
        "酸化剤、または空気中で徐々に",
        "アルデヒドが酸化されやすいこと自体が、還元性（銀鏡反応）の正体。",
    )
    val oxidationSecondary = reaction(
        "第二級アルコールの酸化（2-プロパノール → アセトン）",
        "2CH3CH(OH)CH3 + O2 → 2CH3COCH3 + 2H2O",
        "硫酸酸性の二クロム酸カリウムなどの酸化剤",
        "第二級はケトンで止まり、それ以上酸化されない。第一級との違いはここ。",
    )
    val esterification = reaction(
        "エステル化（酢酸エチルの生成）",
        "CH3COOH + C2H5OH ⇄ CH3COOC2H5 + H2O",
        "濃硫酸（触媒兼脱水剤）、加熱",
        "酸の OH とアルコールの H がとれて水になる。可逆反応なので、水を除くと右に進む。",
    )
    val saponification = reaction(
        "けん化（酢酸エチルの加水分解）",
        "CH3COOC2H5 + NaOH → CH3COONa + C2H5OH",
        "水酸化ナトリウム水溶液、加熱",
        "塩基による加水分解は不可逆。酸による加水分解はエステル化の逆反応で可逆。",
    )
    val fatSaponification = reaction(
        "油脂のけん化（セッケンの製造）",
        "C57H110O6 + 3NaOH → 3C17H35COONa + C3H8O3",
        "水酸化ナトリウム水溶液、加熱。飽和食塩水を加えて塩析する",
        "油脂はグリセリンと高級脂肪酸のエステル。けん化すると必ずグリセリンが残る。",
    )
    val sodiumAlcohol = reaction(
        "アルコールとナトリウム",
        "2C2H5OH + 2Na → 2C2H5ONa + H2",
        "常温",
        "水素が発生するのはヒドロキシ基がある証拠。エーテルでは起こらない。",
    )
    val nitration = reaction(
        "ベンゼンのニトロ化",
        "C6H6 + HNO3 → C6H5NO2 + H2O",
        "濃硝酸と濃硫酸の混酸、約60℃",
        "ベンゼンは不飽和だが付加ではなく置換で反応する。濃硫酸は触媒としてはたらく。",
    )
    val sulfonation = reaction(
        "ベンゼンのスルホン化",
        "C6H6 + H2SO4 → C6H5SO3H + H2O",
        "濃硫酸、加熱",
        "生成するベンゼンスルホン酸は強酸。ニトロ化と並ぶ代表的な置換反応。",
    )
    val anilineFromNitro = reaction(
        "ニトロベンゼンの還元（アニリンの製法）",
        "2C6H5NO2 + 3Sn + 14HCl → 2C6H5NH3Cl + 3SnCl4 + 4H2O\n" +
            "C6H5NH3Cl + NaOH → C6H5NH2 + NaCl + H2O",
        "スズと濃塩酸で還元し、生じた塩に水酸化ナトリウムを加えてアニリンを遊離させる",
        "還元しただけでは塩（アニリン塩酸塩）で止まる。塩基を加えて遊離させるところまでが1組。",
    )
    val diazotization = reaction(
        "アニリンのジアゾ化",
        "C6H5NH2 + 2HCl + NaNO2 → C6H5N2Cl + NaCl + 2H2O",
        "亜硝酸ナトリウムと塩酸、5℃以下に氷冷",
        "冷やすのは、生じる塩化ベンゼンジアゾニウムが温度を上げると分解してフェノールになるため。",
    )
    val coupling = reaction(
        "ジアゾカップリング（アゾ染料の生成）",
        "C6H5N2Cl + C6H5ONa → C6H5N=NC6H4OH + NaCl",
        "ナトリウムフェノキシドの水溶液、氷冷",
        "生じる p-ヒドロキシアゾベンゼンは橙赤色。アゾ基 -N=N- が発色の中心。",
    )
    val cumene = reaction(
        "クメン法（フェノールの工業的製法）",
        "C6H5CH(CH3)2 + O2 → C6H5OH + CH3COCH3",
        "クメンを空気酸化してクメンヒドロペルオキシドとし、希硫酸で分解する",
        "フェノールとアセトンが同時に得られるのが要点。原料のクメンはベンゼンとプロペンから作る。",
    )
    val phenolWeakAcid = reaction(
        "フェノールの酸としての弱さ",
        "C6H5ONa + CO2 + H2O → C6H5OH + NaHCO3",
        "ナトリウムフェノキシドの水溶液に二酸化炭素を通す",
        "炭酸より弱い酸なので、炭酸に追い出される。安息香酸との分離はここを使う。",
    )
    val bromophenol = reaction(
        "フェノールの臭素化",
        "C6H5OH + 3Br2 → C6H2Br3OH + 3HBr",
        "臭素水、常温",
        "2,4,6-トリブロモフェノールの白色沈殿。ヒドロキシ基がオルト・パラを活性化するので一気に3置換される。",
    )
    val tolueneOxidation = reaction(
        "トルエンの酸化（安息香酸の生成）",
        "5C6H5CH3 + 6KMnO4 + 9H2SO4 → 5C6H5COOH + 3K2SO4 + 6MnSO4 + 14H2O",
        "硫酸酸性の過マンガン酸カリウム水溶液、加熱",
        "酸化されるのは側鎖だけで、ベンゼン環は残る。側鎖の長さによらず -COOH になる。",
    )
    val acetylation = reaction(
        "サリチル酸のアセチル化（アセチルサリチル酸の生成）",
        "C6H4(OH)COOH + (CH3CO)2O → C6H4(OCOCH3)COOH + CH3COOH",
        "無水酢酸、少量の濃硫酸",
        "反応するのはフェノール性ヒドロキシ基のほう。カルボキシ基は残る。",
    )
    val methylation = reaction(
        "サリチル酸のエステル化（サリチル酸メチルの生成）",
        "C6H4(OH)COOH + CH3OH → C6H4(OH)COOCH3 + H2O",
        "メタノール、濃硫酸、加熱",
        "こちらが反応するのはカルボキシ基。アセチル化と「どちらの基が反応するか」で対になる。",
    )
    val iodoform = reaction(
        "ヨードホルム反応",
        "CH3CHO + 3I2 + 4NaOH → CHI3 + HCOONa + 3NaI + 3H2O",
        "ヨウ素と水酸化ナトリウム水溶液、加温",
        "CH3CO- または CH3CH(OH)- をもつ物質で、黄色のヨードホルムが沈殿する。",
        "検出",
    )
    val silverMirror = reaction(
        "銀鏡反応",
        "CH3CHO + 2[Ag(NH3)2]OH → CH3COONH4 + 2Ag + 3NH3 + H2O",
        "アンモニア性硝酸銀水溶液、湯浴で加温",
        "還元性の検出。銀が容器の内壁に析出して鏡になる。アルデヒドとギ酸・還元糖で陽性。",
        "検出",
    )
    val fehling = reaction(
        "フェーリング液の還元",
        "CH3CHO + 2Cu(OH)2 → CH3COOH + Cu2O + 2H2O",
        "フェーリング液（酒石酸塩で錯体にした銅(II)）、加熱",
        "赤色の酸化銅(I) が沈殿する。銀鏡反応と同じ「還元性」を別の試薬で見ている。",
        "検出",
    )
    val acetyleneMaking = reaction(
        "アセチレンの製法",
        "CaC2 + 2H2O → C2H2 + Ca(OH)2",
        "炭化カルシウムに水を加える、常温",
        "水上置換で捕集する。炭化カルシウムは吸湿性が強いので密閉保存する。",
    )
    val benzeneFromAcetylene = reaction(
        "アセチレンの3分子重合",
        "3C2H2 → C6H6",
        "赤熱した鉄管に通す",
        "三重結合が開いて環になる。ベンゼンが「不飽和の集まり」であることが見える反応。",
    )
    val fermentation = reaction(
        "アルコール発酵",
        "C6H12O6 → 2C2H5OH + 2CO2",
        "酵母のもつ酵素群（チマーゼ）",
        "糖からエタノールと二酸化炭素が同じ物質量ずつ生じる。",
    )
    val hydrolysisSucrose = reaction(
        "スクロースの加水分解",
        "C12H22O11 + H2O → C6H12O6 + C6H12O6（グルコースとフルクトース）",
        "希硫酸で加熱、またはインベルターゼ",
        "生じる転化糖は還元性を示す。スクロース自体は還元性がないので、前後で性質が変わる。",
    )
    val bromineAddition = reaction(
        "エチレンへの臭素の付加",
        "CH2=CH2 + Br2 → CH2BrCH2Br",
        "臭素水、常温",
        "臭素水の色が消えることが二重結合の検出になる。ベンゼンは置換なのでこれでは脱色しない。",
    )
    val polyethylene = reaction(
        "ポリエチレンの付加重合",
        "n CH2=CH2 → [-CH2-CH2-]n",
        "触媒、加圧・加熱",
        "二重結合が開いて次々につながる。単量体1種類から鎖ができるのが付加重合。",
        "重合",
    )
    val pet = reaction(
        "ポリエチレンテレフタラート（PET）の縮合重合",
        "n HOOC-C6H4-COOH + n C2H4(OH)2 → [-OC-C6H4-COO-C2H4-O-]n + 2n H2O",
        "加熱",
        "2種類の単量体から水がとれてつながる縮合重合。エステル結合でつながるのでポリエステル。",
        "重合",
    )

    // ---- つながり -------------------------------------------------------------

    // 酸化の段階は有機の背骨。1段ずつ登れることが分かっていれば、個々は覚えなくていい。
    s.link(ethanol, acetaldehyde, LinkType.PRODUCES, "酸化するとアセトアルデヒド（第一級アルコール → アルデヒド）")
    s.link(acetaldehyde, aceticAcid, LinkType.PRODUCES, "さらに酸化すると酢酸")
    s.link(propanol2, acetone, LinkType.PRODUCES, "第二級アルコールの酸化はケトンで止まる")
    s.link(methanol, formaldehyde, LinkType.PRODUCES, "酸化するとホルムアルデヒド")
    s.link(formaldehyde, formicAcid, LinkType.PRODUCES, "さらに酸化するとギ酸")
    s.link(oxidation1, acetaldehyde, LinkType.PRODUCES, "生成物")
    s.link(oxidation2, aceticAcid, LinkType.PRODUCES, "生成物")
    s.link(oxidationSecondary, acetone, LinkType.PRODUCES, "生成物")
    s.link(oxidation1, oxidationSecondary, LinkType.CONTRAST, "第一級はアルデヒドへ、第二級はケトンで止まる")

    s.link(ethanol, ethylene, LinkType.PRODUCES, "170℃ の分子内脱水でエチレン")
    s.link(dehydration170, dehydration140, LinkType.CONTRAST, "170℃ は分子内（エチレン）、130〜140℃ は分子間（エーテル）")
    s.link(dehydration170, ethylene, LinkType.PRODUCES, "生成物")
    s.link(ethylene, polyethylene, LinkType.RELATED, "付加重合の単量体")
    s.link(acetylene, benzeneFromAcetylene, LinkType.RELATED, "3分子が重合してベンゼンになる")
    s.link(acetyleneMaking, acetylene, LinkType.PRODUCES, "生成物")
    s.link(bromineAddition, ethylene, LinkType.RELATED, "二重結合の検出")

    s.link(esterification, ethylAcetate, LinkType.PRODUCES, "生成物")
    s.link(esterification, saponification, LinkType.CONTRAST, "エステル化は可逆、塩基によるけん化は不可逆")
    s.link(aceticAcid, ethylAcetate, LinkType.PRODUCES, "エタノールとのエステル化で生成")
    s.link(fatSaponification, soap, LinkType.PRODUCES, "生成物")
    s.link(fatSaponification, glycerol, LinkType.PRODUCES, "油脂をけん化すると必ず残る")
    s.link(glycol, pet, LinkType.RELATED, "テレフタル酸との縮合重合で PET")
    s.link(polyethylene, pet, LinkType.CONTRAST, "単量体1種の付加重合 ↔ 単量体2種から水がとれる縮合重合")

    s.link(benzene, nitrobenzene, LinkType.PRODUCES, "ニトロ化で生成")
    s.link(nitration, nitrobenzene, LinkType.PRODUCES, "生成物")
    s.link(nitrobenzene, aniline, LinkType.PRODUCES, "スズと塩酸で還元するとアニリン")
    s.link(anilineFromNitro, aniline, LinkType.PRODUCES, "最終生成物")
    s.link(aniline, diazotization, LinkType.RELATED, "ジアゾ化の原料")
    s.link(diazotization, coupling, LinkType.PRODUCES, "生じたジアゾニウム塩がカップリングの原料になる")
    s.link(coupling, phenol, LinkType.RELATED, "ナトリウムフェノキシドと結合してアゾ染料になる")
    s.link(nitration, sulfonation, LinkType.SAME_GROUP, "どちらもベンゼンの置換反応")
    s.link(benzene, toluene, LinkType.SAME_GROUP, "芳香族の基本骨格と、その側鎖つき")
    s.link(toluene, benzoicAcid, LinkType.PRODUCES, "側鎖の酸化で安息香酸")
    s.link(tolueneOxidation, benzoicAcid, LinkType.PRODUCES, "生成物")
    s.link(cumene, phenol, LinkType.PRODUCES, "フェノール（工業的製法）")
    s.link(cumene, acetone, LinkType.PRODUCES, "同時に得られる副生成物")
    s.link(phenol, bromophenol, LinkType.RELATED, "臭素水で白色沈殿（検出）")
    s.link(phenol, phenolWeakAcid, LinkType.RELATED, "炭酸より弱い酸であることの確認")
    s.link(phenol, benzoicAcid, LinkType.CONTRAST, "炭酸より弱い酸 ↔ 炭酸より強い酸。分離はこの差を使う")
    s.link(salicylic, aspirin, LinkType.PRODUCES, "無水酢酸でアセチル化")
    s.link(salicylic, methylSalicylate, LinkType.PRODUCES, "メタノールでエステル化")
    s.link(acetylation, methylation, LinkType.CONTRAST, "アセチル化は OH 側、エステル化は COOH 側が反応する")
    s.link(aspirin, methylSalicylate, LinkType.CONFUSABLE, "どちらもサリチル酸から。塩化鉄(III)で呈色するのはサリチル酸メチルのほう")
    s.link(salicylic, phenol, LinkType.SAME_GROUP, "フェノール性ヒドロキシ基をもち、塩化鉄(III)で呈色する")

    s.link(glucose, sucrose, LinkType.CONTRAST, "還元性あり ↔ なし。スクロースは加水分解して初めて還元性が出る")
    s.link(hydrolysisSucrose, glucose, LinkType.PRODUCES, "加水分解の生成物")
    s.link(fermentation, ethanol, LinkType.PRODUCES, "生成物")
    s.link(glucose, fermentation, LinkType.RELATED, "アルコール発酵の原料")

    s.link(silverMirror, fehling, LinkType.SAME_GROUP, "どちらも還元性の検出。銀が析出する / 赤色の酸化銅(I) が沈殿する")
    s.link(silverMirror, acetaldehyde, LinkType.RELATED, "還元性の検出")
    s.link(formicAcid, aceticAcid, LinkType.CONFUSABLE, "ギ酸は還元性を示し銀鏡反応が陽性、酢酸は陰性")
    s.link(glucose, silverMirror, LinkType.RELATED, "還元糖なので陽性")
    s.link(iodoform, acetone, LinkType.RELATED, "CH3CO- をもつので陽性")
    s.link(acetone, acetaldehyde, LinkType.CONFUSABLE, "ヨードホルム反応はどちらも陽性。還元性はアセトアルデヒドだけ")
    s.link(ethanol, propanol2, LinkType.SAME_GROUP, "CH3CH(OH)- をもつのでヨードホルム反応が陽性")
    s.link(maleic, fumaric, LinkType.CONFUSABLE, "シス形とトランス形。加熱で無水物になるのはマレイン酸だけ")
    s.link(sodiumAlcohol, ethanol, LinkType.RELATED, "ヒドロキシ基の確認")
    s.link(glycine, soap, LinkType.RELATED, "どちらも水溶液の液性が加水分解で決まる")
    s.link(oxalicAcid, aceticAcid, LinkType.SAME_GROUP, "カルボン酸。シュウ酸は2価で還元剤にもなる")
}

internal const val ORGANIC_DECK = "化学・有機化合物"
internal const val ORGANIC_REACTION_DECK = "化学・有機反応"
