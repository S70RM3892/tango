package com.tango.recall.data

/**
 * 化学・理論.
 *
 * The half of chemistry that is not a substance: gases, solutions, heat, equilibrium,
 * cells, rates, and the structures underneath them. What gets marked wrong here is
 * almost never the formula — it is the condition attached to it, so every note keeps
 * the two apart and asks about them separately.
 */
internal fun seedChemTheory(s: Seeder) {
    val deckId = s.deck(
        name = THEORY_DECK,
        type = NoteType.CHEM_THEORY,
        templates = setOf("theory_formula", "theory_name", "theory_condition"),
        newPerDay = 6,
        relationQuiz = true,
    )

    fun law(
        title: String, formula: String, meaning: String, condition: String, point: String,
        field: String,
    ): Long = s.note(
        deckId, NoteType.CHEM_THEORY,
        mapOf(
            "title" to title, "formula" to formula, "meaning" to meaning,
            "condition" to condition, "point" to point, "memo" to "",
        ),
        listOf("理論", field),
    )

    // ---- 気体 -----------------------------------------------------------------

    val idealGas = law(
        "気体の状態方程式",
        "PV = nRT",
        "P: 圧力、V: 体積、n: 物質量、R: 気体定数 8.31×10³ Pa·L/(mol·K)、T: 絶対温度",
        "理想気体。高温・低圧ほどよく合う。温度は必ず絶対温度（K）に直す。",
        "分子自身の体積と分子間力を無視した式。実在気体は低温・高圧でずれる。",
        "気体",
    )
    val boyleCharles = law(
        "ボイル・シャルルの法則",
        "PV/T = 一定",
        "同じ物質量の気体について、状態1と状態2で PV/T が等しい",
        "物質量が変わらないとき。状態方程式から n と R を消した形。",
        "「何が一定か」を最初に決める。物質量が変わる問題では使えない。",
        "気体",
    )
    val partialPressure = law(
        "分圧の法則（ドルトン）",
        "P = P_A + P_B、P_A = P × (n_A / n)",
        "P: 全圧、P_A: 成分 A の分圧、n_A/n: モル分率",
        "混合気体。互いに反応しないこと。",
        "分圧の比 = 物質量の比 = 体積比。気体では比がそのまま使えるのが強い。",
        "気体",
    )
    val henry = law(
        "ヘンリーの法則",
        "溶ける気体の物質量 ∝ その気体の分圧",
        "一定温度で、溶解量は分圧に比例する",
        "溶解度の小さい気体（酸素・窒素・二酸化炭素）。アンモニアや塩化水素のようによく溶ける気体では成り立たない。",
        "「物質量は分圧に比例、体積はその圧力のもとで測れば一定」の言い換えに注意する。",
        "気体",
    )

    // ---- 溶液 -----------------------------------------------------------------

    val boilingPoint = law(
        "沸点上昇・凝固点降下",
        "Δt = K·m",
        "Δt: 沸点上昇度（凝固点降下度）、K: モル沸点上昇（凝固点降下）、m: 質量モル濃度 mol/kg",
        "希薄溶液で、溶質が不揮発性のとき。電解質では電離後の粒子の物質量で数える。",
        "濃度は質量モル濃度（溶媒 1 kg あたり）。モル濃度と取り違えない。",
        "溶液",
    )
    val osmotic = law(
        "浸透圧（ファントホッフの法則）",
        "ΠV = nRT",
        "Π: 浸透圧、V: 溶液の体積、n: 溶質の物質量",
        "希薄溶液。電解質は電離後の粒子数で数える。",
        "気体の状態方程式と同じ形。分子量の測定に使える（高分子でも測れるのが利点）。",
        "溶液",
    )
    val vaporPressure = law(
        "蒸気圧降下",
        "純溶媒の蒸気圧 > 溶液の蒸気圧",
        "不揮発性の溶質を溶かすと、溶媒の蒸気圧が下がる",
        "希薄溶液。溶質が不揮発性であること。",
        "沸点上昇の原因。蒸気圧が下がるぶん、外圧に達するまで温度を上げる必要がある。",
        "溶液",
    )
    val solubilityProduct = law(
        "溶解度積",
        "Ksp = [A⁺][B⁻]",
        "難溶性の塩 AB が飽和しているときのイオン濃度の積",
        "飽和溶液。温度が決まれば一定。",
        "イオン濃度の積が Ksp を超えると沈殿する。共通イオンを加えると溶解度が下がる。",
        "溶液",
    )
    val colloid = law(
        "コロイドの性質",
        "チンダル現象・ブラウン運動・電気泳動",
        "コロイド粒子は 10⁻⁹〜10⁻⁷ m 程度。光を散乱し、水分子の衝突で不規則に動き、電荷をもつ",
        "疎水コロイドは少量の電解質で沈殿（凝析）、親水コロイドは多量の電解質で沈殿（塩析）。",
        "保護コロイドは疎水コロイドを親水コロイドで包んで沈殿しにくくしたもの。",
        "溶液",
    )

    // ---- 熱 -------------------------------------------------------------------

    val hess = law(
        "ヘスの法則",
        "反応熱は、最初と最後の状態だけで決まる",
        "途中の経路によらず、反応熱の総和は等しい",
        "同じ状態（物質・状態・温度・圧力）どうしを比べるとき。",
        "直接測れない反応熱を、測れる反応の組み合わせから出せる。熱化学方程式を足し引きする根拠。",
        "熱",
    )
    val bondEnergy = law(
        "結合エネルギーと反応熱",
        "反応熱 = （切る結合の和）−（できる結合の和）の符号を逆にしたもの",
        "気体状態で、結合を切るには必ずエネルギーが要る",
        "すべて気体で、結合エネルギーが与えられているとき。",
        "「切るのに要る」ー「できて出る」で符号を間違えやすい。発熱なら反応熱は正。",
        "熱",
    )

    // ---- 平衡 -----------------------------------------------------------------

    val equilibriumConstant = law(
        "化学平衡の法則（質量作用の法則）",
        "K = [生成物]^係数 ÷ [反応物]^係数",
        "K: 平衡定数。濃度は平衡状態での値",
        "温度が一定なら K は一定。触媒や濃度では変わらない。",
        "固体や純溶媒は式に入れない。K が変わるのは温度を変えたときだけ。",
        "平衡",
    )
    val leChatelier = law(
        "ルシャトリエの原理",
        "条件を変えると、その変化を和らげる向きに平衡が移動する",
        "濃度・圧力・温度を変えたときの移動の向き",
        "平衡に達している系。触媒は平衡の位置を変えない（到達を速めるだけ）。",
        "圧力は「気体の分子数が減る向き」、温度は「吸熱の向き」に移動する。定性的な予測の道具。",
        "平衡",
    )
    val ionization = law(
        "弱酸の電離平衡",
        "[H⁺] = √(Ka·c)、pH = -log[H⁺]",
        "Ka: 電離定数、c: 酸のモル濃度",
        "電離度が小さい弱酸（1 - α ≒ 1 と近似できるとき）。強酸では使えない。",
        "強酸は [H⁺] = c、弱酸は √(Ka·c)。ここを取り違えると桁が変わる。",
        "平衡",
    )
    val buffer = law(
        "緩衝液",
        "pH = pKa + log([塩] / [酸])",
        "弱酸とその塩（または弱塩基とその塩）の混合溶液",
        "弱酸と共役塩基が同程度あるとき。大量の酸・塩基を加えれば緩衝能を超える。",
        "少量の酸や塩基を加えても pH がほとんど変わらない。酸と塩が等量なら pH = pKa。",
        "平衡",
    )
    val hydrolysis = law(
        "塩の加水分解",
        "弱酸＋強塩基の塩 → 塩基性、強酸＋弱塩基の塩 → 酸性",
        "塩を水に溶かしたときの液性",
        "弱い側のイオンが水と反応するとき。強酸＋強塩基の塩は中性。",
        "酢酸ナトリウムは塩基性、塩化アンモニウムは酸性。「弱いほうが勝つ」と覚える。",
        "平衡",
    )

    // ---- 反応速度 ---------------------------------------------------------------

    val rate = law(
        "反応速度式",
        "v = k[A]^a[B]^b",
        "v: 反応速度、k: 速度定数、指数は実験で決まる",
        "指数は化学反応式の係数とは限らない。実験で決めるもの。",
        "係数をそのまま指数にしてはいけない。多段階反応では最も遅い段階（律速段階）が全体を決める。",
        "反応速度",
    )
    val activation = law(
        "活性化エネルギーと触媒",
        "触媒は活性化エネルギーを下げる",
        "Ea: 活性化エネルギー。越えるべき山の高さ",
        "触媒は反応熱も平衡の位置も変えない。速度だけを変える。",
        "温度を上げると速くなるのは、山を越えられる分子の割合が急に増えるため。",
        "反応速度",
    )

    // ---- 電池・電気分解 ---------------------------------------------------------

    val cellBasics = law(
        "電池の基本",
        "負極 = 酸化される（イオン化傾向が大きい）、正極 = 還元される",
        "電子は負極から導線を通って正極へ流れる",
        "2種類の金属と電解液。イオン化傾向の差が起電力を生む。",
        "「電流の向き」と「電子の向き」は逆。負極で酸化、正極で還元は電池でも電気分解でも同じ向きの対応。",
        "電池",
    )
    val danielCell = law(
        "ダニエル電池",
        "負極: Zn → Zn²⁺ + 2e⁻ / 正極: Cu²⁺ + 2e⁻ → Cu",
        "亜鉛板を硫酸亜鉛水溶液に、銅板を硫酸銅(II)水溶液に浸し、素焼き板で仕切る",
        "2つの溶液が混ざらないよう仕切ること。仕切りはイオンだけを通す。",
        "亜鉛のほうがイオン化傾向が大きいので負極。素焼き板がないと直接反応して電池にならない。",
        "電池",
    )
    val leadBattery = law(
        "鉛蓄電池",
        "放電: Pb + PbO2 + 2H2SO4 → 2PbSO4 + 2H2O",
        "負極 Pb、正極 PbO2、電解液は希硫酸",
        "充電すれば元に戻る二次電池。放電すると電解液の密度が下がる。",
        "両極とも硫酸鉛(II) になって重くなり、硫酸が薄くなる。ここが問われる。",
        "電池",
    )
    val fuelCell = law(
        "燃料電池（リン酸型）",
        "負極: H2 → 2H⁺ + 2e⁻ / 正極: O2 + 4H⁺ + 4e⁻ → 2H2O",
        "水素と酸素から直接電気を取り出す",
        "全体では 2H2 + O2 → 2H2O。生成物は水だけ。",
        "燃焼させて熱にしてから電気にするより、直接取り出すほうが効率がよい。",
        "電池",
    )
    val faraday = law(
        "ファラデーの法則",
        "電気量 Q = It、電子の物質量 = Q / F",
        "F: ファラデー定数 9.65×10⁴ C/mol、I: 電流 A、t: 時間 s",
        "電気分解・電池のどちらでも。電子 1 mol あたりの電気量が F。",
        "「電子何 mol が流れたか」を出してから、反応式の係数比で物質量に直す。時間は秒に直す。",
        "電気分解",
    )
    val electrolysisRule = law(
        "電気分解で何が起こるか",
        "陽極: 酸化（陰イオンか電極が電子を失う）/ 陰極: 還元",
        "水溶液では、水が反応することもある",
        "陽極が銅・銀なら電極自身が溶ける。白金・炭素なら溶けない。" +
            "陰極でイオン化傾向の大きい金属（Na, K, Ca, Al）は析出せず、水素が出る。",
        "「電極が溶けるか」「水が反応するか」の2点で場合分けする。ここを外すと全部ずれる。",
        "電気分解",
    )

    // ---- 結合と結晶 -------------------------------------------------------------

    val bondTypes = law(
        "化学結合の種類",
        "イオン結合 / 共有結合 / 金属結合 / 分子間力",
        "金属と非金属 → イオン結合、非金属どうし → 共有結合、金属どうし → 金属結合",
        "結合の強さは おおむね 共有・イオン・金属 ≫ 水素結合 > ファンデルワールス力。",
        "融点の高さや電気伝導性は、どの結合でできているかで決まる。まず結合を見分ける。",
        "結合",
    )
    val hydrogenBond = law(
        "水素結合",
        "F, O, N に結合した H と、隣の分子の F, O, N の間にはたらく",
        "分子間力のなかでは特に強い",
        "電気陰性度が大きい原子に H が直接ついているとき。",
        "HF・H2O・NH3 の沸点が同族の中で異常に高いのはこのため。氷が水より軽いのも水素結合の配置による。",
        "結合",
    )
    val polarity = law(
        "分子の極性",
        "結合の極性をベクトルとして足し合わせる",
        "電気陰性度の差で結合に極性が生じ、分子の形で打ち消し合うかが決まる",
        "分子の形（直線・折れ線・三角錐・正四面体）を先に決めること。",
        "CO2 は直線形で打ち消して無極性、H2O は折れ線形で打ち消さず極性。形が答えを決める。",
        "結合",
    )
    val crystalLattice = law(
        "金属結晶の単位格子",
        "体心立方: 配位数 8・原子 2 個 / 面心立方: 配位数 12・原子 4 個 / 六方最密: 配位数 12",
        "単位格子に含まれる原子数と配位数",
        "頂点は 1/8、面の中心は 1/2、辺の中心は 1/4 として数える。",
        "充填率は面心立方・六方最密が約 74%、体心立方が約 68%。密度の計算はここから始まる。",
        "結晶",
    )
    val ionicCrystal = law(
        "イオン結晶の構造",
        "NaCl 型: 配位数 6 / CsCl 型: 配位数 8",
        "陽イオンと陰イオンの大きさの比で配位数が決まる",
        "イオン半径比が大きいほど配位数も大きい。",
        "イオン結晶は硬いがもろい。ずれると同符号のイオンが隣り合って反発するため。",
        "結晶",
    )
    val stateChange = law(
        "状態変化と蒸気圧",
        "蒸気圧 = 外圧 となる温度が沸点",
        "飽和蒸気圧は温度だけで決まり、体積や気体の量にはよらない",
        "気液平衡に達しているとき。",
        "山の上で沸点が下がるのは外圧が下がるから。圧力鍋はその逆。",
        "状態変化",
    )

    // ---- つながり -----------------------------------------------------------------

    s.link(idealGas, boyleCharles, LinkType.RELATED, "状態方程式から n と R を消すとボイル・シャルル")
    s.link(idealGas, osmotic, LinkType.SAME_GROUP, "ΠV = nRT は形が同じ。溶液でも「粒子の数」が効く")
    s.link(idealGas, partialPressure, LinkType.RELATED, "分圧はモル分率、つまり物質量の比で決まる")
    s.link(henry, partialPressure, LinkType.RELATED, "溶ける量は「その気体の分圧」に比例する")
    s.link(henry, solubilityProduct, LinkType.CONTRAST, "気体の溶解は分圧で、難溶性塩の溶解はイオン濃度の積で決まる")
    s.link(boilingPoint, osmotic, LinkType.SAME_GROUP, "どちらも希薄溶液の束一的性質。効くのは粒子の数")
    s.link(boilingPoint, vaporPressure, LinkType.PRODUCES, "蒸気圧が下がることが沸点上昇の原因")
    s.link(vaporPressure, stateChange, LinkType.RELATED, "沸点は蒸気圧と外圧が等しくなる温度")
    s.link(boilingPoint, ionization, LinkType.CONFUSABLE, "どちらも電解質では「電離後の粒子数」を数えるかどうかで答えが変わる")
    s.link(hess, bondEnergy, LinkType.RELATED, "結合エネルギーからの計算はヘスの法則の応用")
    s.link(equilibriumConstant, leChatelier, LinkType.CONTRAST, "K は量的に決める / ルシャトリエは向きだけを定性的に見る")
    s.link(equilibriumConstant, ionization, LinkType.HYPERNYM, "電離定数は平衡定数の特別な場合")
    s.link(ionization, buffer, LinkType.RELATED, "緩衝液の pH は電離平衡から出る")
    s.link(buffer, hydrolysis, LinkType.RELATED, "弱酸とその塩の組み合わせという点で共通")
    s.link(leChatelier, activation, LinkType.CONFUSABLE, "触媒は平衡を動かさず速度だけ変える。ここが最も取り違えやすい")
    s.link(rate, activation, LinkType.RELATED, "速度定数 k は活性化エネルギーと温度で決まる")
    s.link(cellBasics, danielCell, LinkType.HYPERNYM, "電池の基本と、その最も基本的な実例")
    s.link(danielCell, leadBattery, LinkType.CONTRAST, "一次電池的な基本形 ↔ 充電できる二次電池")
    s.link(leadBattery, fuelCell, LinkType.CONTRAST, "電解液が薄くなる ↔ 生成物が水だけ")
    s.link(cellBasics, electrolysisRule, LinkType.CONTRAST, "電池は自発的に進む / 電気分解は電気を加えて進める")
    s.link(faraday, electrolysisRule, LinkType.RELATED, "何 mol の電子が流れたかを、何が析出するかに結びつける")
    s.link(bondTypes, hydrogenBond, LinkType.HYPERNYM, "分子間力のうち特に強いもの")
    s.link(bondTypes, polarity, LinkType.RELATED, "極性は結合の性質と分子の形の両方で決まる")
    s.link(polarity, hydrogenBond, LinkType.RELATED, "電気陰性度の差が大きいほど強くはたらく")
    s.link(crystalLattice, ionicCrystal, LinkType.SAME_GROUP, "単位格子から原子数と配位数を数える型")
    s.link(bondTypes, crystalLattice, LinkType.RELATED, "金属結合でできた結晶の詰まり方")
    s.link(stateChange, idealGas, LinkType.RELATED, "気体として扱えるのは、どの状態にあるかを確かめてから")
    s.link(colloid, solubilityProduct, LinkType.CONTRAST, "電解質を加えて沈殿させる（凝析・塩析）↔ 共通イオンで溶解度を下げる")
    s.link(colloid, polarity, LinkType.RELATED, "疎水コロイドか親水コロイドかは、粒子の表面が水と親和するかで決まる")
}

internal const val THEORY_DECK = "化学・理論"
