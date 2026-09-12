package com.tango.recall.data

/**
 * 物理・原子.
 *
 * Mechanics and electromagnetism are derived on the paper, which is why this app
 * leaves them alone. Atomic physics is the part that is genuinely remembered: which
 * experiment showed what, which relation holds under which condition, and the handful
 * of constants. It is also the part that is most often left until last and then never
 * revisited — which is exactly what spaced repetition is for.
 */
internal fun seedPhysics(s: Seeder) {
    val deckId = s.deck(
        name = PHYSICS_DECK,
        type = NoteType.PHYSICS,
        templates = setOf("phys_formula", "phys_name", "phys_condition"),
        newPerDay = 5,
        relationQuiz = true,
    )

    fun item(
        title: String, formula: String, meaning: String, condition: String, point: String,
        vararg tags: String,
    ): Long = s.note(
        deckId, NoteType.PHYSICS,
        mapOf(
            "title" to title, "formula" to formula, "meaning" to meaning,
            "condition" to condition, "point" to point, "memo" to "",
        ),
        listOf("原子") + tags,
    )

    // ---- 光の粒子性 -------------------------------------------------------------

    val photoelectric = item(
        "光電効果（アインシュタインの光電方程式）",
        "hν = W + K",
        "h: プランク定数 6.6×10⁻³⁴ J·s、ν: 光の振動数、W: 仕事関数、K: 飛び出す電子の最大運動エネルギー",
        "振動数が限界振動数 ν₀ を超えたときだけ電子が飛び出す。強さではなく振動数で決まる。",
        "光を波と考えると「弱い光でも当て続ければ飛び出す」はずだが、実際は振動数が足りなければ何も起きない。" +
            "ここが光を粒子と考える根拠になる。",
        "光の粒子性",
    )
    val workFunction = item(
        "仕事関数と限界振動数",
        "W = hν₀",
        "W: 金属から電子を1個取り出すのに必要な最小のエネルギー、ν₀: 限界振動数",
        "金属ごとに決まる値。ν < ν₀ ではどれだけ強い光を当てても光電子は出ない。",
        "光電子の最大運動エネルギーは振動数の1次関数（傾き h、切片 -W）。グラフの傾きから h が求まる。",
        "光の粒子性",
    )
    val photon = item(
        "光子のエネルギーと運動量",
        "E = hν = hc/λ、p = h/λ = E/c",
        "E: 光子1個のエネルギー、p: 光子の運動量、c: 光速 3.0×10⁸ m/s",
        "光を粒子として扱うときの基本式。質量は 0 でも運動量をもつ。",
        "波長が短いほどエネルギーも運動量も大きい。X線が物質を透過し電子を弾き飛ばせるのはこのため。",
        "光の粒子性",
    )
    val compton = item(
        "コンプトン効果",
        "λ' - λ = (h/mc)(1 - cosθ)",
        "λ: 入射X線の波長、λ': 散乱後の波長、m: 電子の質量、θ: 散乱角",
        "X線を電子に当てたとき。散乱角が大きいほど波長の伸びが大きい（θ = 180° で最大）。",
        "光子と電子の弾性衝突として、エネルギー保存と運動量保存から導ける。h/mc = 2.4×10⁻¹² m はコンプトン波長。",
        "光の粒子性",
    )

    // ---- 電子の波動性 -----------------------------------------------------------

    val deBroglie = item(
        "ド・ブロイ波長（物質波）",
        "λ = h/(mv) = h/p",
        "λ: 粒子に伴う波の波長、m: 質量、v: 速さ、p: 運動量",
        "あらゆる粒子について成り立つ。質量が大きいと波長が極端に短く、波として観測できない。",
        "電子線が結晶で回折することが実験的な裏づけ（デビソン・ガーマーの実験）。" +
            "光が粒子なら粒子も波、という対称性がここで閉じる。",
        "電子の波動性",
    )
    val bohrQuantum = item(
        "ボーアの量子条件",
        "2πr = nλ、すなわち mvr = nh/(2π)",
        "r: 円軌道の半径、n: 量子数（1, 2, 3, …）、λ: 電子のド・ブロイ波長",
        "円軌道の1周が電子波の波長の整数倍になる軌道だけが許される。",
        "定常波として閉じる軌道だけが安定に存在する、という読み方をする。ここでド・ブロイ波が効いてくる。",
        "原子模型",
    )
    val bohrFrequency = item(
        "ボーアの振動数条件",
        "hν = E_n' - E_n",
        "E_n, E_n': 遷移前後のエネルギー準位、ν: 放出または吸収される光の振動数",
        "電子が軌道間を移るときだけ光を出し入れする。軌道上を回っている間は放射しない。",
        "スペクトルが線状（とびとび）になる理由そのもの。連続でないことが量子化の証拠になる。",
        "原子模型",
    )
    val energyLevel = item(
        "水素原子のエネルギー準位",
        "E_n = -13.6/n² eV",
        "n: 量子数、E_n: n 番目の軌道にある電子のエネルギー",
        "水素原子（原子核が陽子1個）の場合。負の値なのは、無限遠を基準（0）にとっているため。",
        "n = 1 が基底状態で -13.6 eV。13.6 eV 与えると電子は無限遠へ飛ぶ（イオン化エネルギー）。",
        "原子模型",
    )
    val spectrum = item(
        "水素の輝線スペクトルの系列",
        "1/λ = R(1/n'² - 1/n²)",
        "R: リュードベリ定数 1.1×10⁷ /m、n': 遷移先の量子数、n: 遷移元（n > n'）",
        "n' = 1 がライマン系列（紫外）、n' = 2 がバルマー系列（可視光）、n' = 3 がパッシェン系列（赤外）",
        "可視光で見えるのはバルマー系列だけ。「見えるのは n = 2 に落ちるとき」と結びつけて覚える。",
        "原子模型",
    )

    // ---- X線 ---------------------------------------------------------------------

    val xrayTube = item(
        "X線の発生と最短波長",
        "λ_min = hc/(eV)",
        "V: 加速電圧、e: 電気素量 1.6×10⁻¹⁹ C、λ_min: 連続X線の最短波長",
        "加速した電子を金属に当てたとき。電子の運動エネルギーがすべて1個の光子になった場合が最短波長。",
        "光電効果の逆過程。最短波長は加速電圧だけで決まり、金属の種類にはよらない" +
            "（固有X線の波長は金属で決まる）。",
        "X線",
    )
    val bragg = item(
        "ブラッグの条件",
        "2d sinθ = nλ",
        "d: 結晶の格子面の間隔、θ: 格子面とX線のなす角、n: 整数",
        "結晶にX線を当てて、反射が強め合う条件。θ は入射角ではなく格子面からの角度。",
        "X線の波動性を示す実験。同じ式で、波長が分かれば d が、d が分かれば波長が求まる。",
        "X線",
    )

    // ---- 原子核 -------------------------------------------------------------------

    val nucleus = item(
        "原子核の構成と統一原子質量単位",
        "質量数 A = 陽子数 Z + 中性子数 N",
        "Z: 原子番号（陽子の数）、A: 質量数、1 u = 1.66×10⁻²⁷ kg",
        "統一原子質量単位 u は、質量数 12 の炭素原子1個の質量の 1/12 と定める。",
        "同位体は Z が同じで N が異なるもの。化学的性質はほぼ同じで、質量と核の安定性が違う。",
        "原子核",
    )
    val massDefect = item(
        "質量欠損と結合エネルギー",
        "E = Δm c²",
        "Δm: 核子がばらばらのときとの質量の差、E: 核子を結びつけているエネルギー",
        "原子核の質量は、構成する核子の質量の和より必ず小さい。その差がエネルギーに対応する。",
        "核子1個あたりの結合エネルギーは質量数 60 付近（鉄）で最大。" +
            "だから軽い核は融合、重い核は分裂でエネルギーを出す。",
        "原子核",
    )
    val decay = item(
        "α崩壊・β崩壊・γ線",
        "α: A が 4 減り Z が 2 減る / β⁻: A は不変で Z が 1 増える / γ: どちらも不変",
        "α線: ヘリウム原子核の流れ、β線: 電子の流れ、γ線: 波長の短い電磁波",
        "α線は紙で止まり、β線は薄いアルミ板、γ線は鉛でようやく弱まる（透過力は α < β < γ）。",
        "電離作用の強さは透過力と逆で α > β > γ。β崩壊は中性子が陽子と電子に変わる現象。",
        "原子核",
    )
    val halfLife = item(
        "半減期",
        "N = N₀(1/2)^(t/T)",
        "N₀: はじめの原子数、T: 半減期、t: 経過時間",
        "崩壊は1個1個は偶然に起こるが、多数集まると必ずこの形になる。温度や圧力では変わらない。",
        "「何分の1になったか」を 2 の何乗かで読む。t = 3T なら 1/8。",
        "原子核",
    )
    val fission = item(
        "核分裂と核融合",
        "²³⁵U + n → 核分裂片 + 数個の n + エネルギー",
        "核分裂: 重い核が2つに割れる、核融合: 軽い核が結びつく",
        "どちらも、反応の前後で核子1個あたりの結合エネルギーが大きくなる方向に進む。",
        "核分裂で出た中性子が次の分裂を起こすのが連鎖反応。減速材は中性子を遅くして反応を起こしやすくする。",
        "原子核",
    )

    // ---- 決定的な実験 -------------------------------------------------------------

    val millikan = item(
        "ミリカンの油滴実験",
        "",
        "帯電した油滴にはたらく重力と静電気力をつり合わせ、電気量を測る",
        "測定した電気量がすべて 1.6×10⁻¹⁹ C の整数倍になったことから、電気素量が定まった。",
        "電気量が連続ではなく、とびとびの値をとることを示した実験。電子1個の電荷が決まる。",
        "実験",
    )
    val rutherford = item(
        "ラザフォードの散乱実験",
        "",
        "薄い金箔にα線を当て、散乱の様子を調べる",
        "ほとんどは素通りするのに、ごく一部が大角度で跳ね返された。",
        "正電荷と質量が中心の極めて小さい領域に集中している——原子核の発見。" +
            "原子は「詰まった球」ではなく、ほとんど空であることが分かる。",
        "実験",
    )

    // ---- つながり -----------------------------------------------------------------

    s.link(photoelectric, workFunction, LinkType.RELATED, "限界振動数 ν₀ と仕事関数 W は W = hν₀ で結ばれる")
    s.link(photoelectric, photon, LinkType.RELATED, "光を E = hν の粒子と見るのが前提")
    s.link(photoelectric, compton, LinkType.SAME_GROUP, "どちらも光の粒子性を示す。エネルギーだけ / 運動量まで")
    s.link(compton, photon, LinkType.RELATED, "光子が運動量 p = h/λ をもつことから導ける")
    s.link(photoelectric, xrayTube, LinkType.CONTRAST, "光 → 電子（光電効果）↔ 電子 → 光（X線の発生）。互いに逆の過程")
    s.link(deBroglie, compton, LinkType.CONTRAST, "粒子が波の性質をもつ ↔ 波が粒子の性質をもつ")
    s.link(deBroglie, bohrQuantum, LinkType.PRODUCES, "量子条件は、電子波が1周で閉じる条件として導かれる")
    s.link(bohrQuantum, energyLevel, LinkType.PRODUCES, "量子条件とクーロン力のつり合いから準位が決まる")
    s.link(bohrFrequency, energyLevel, LinkType.RELATED, "準位の差がそのまま光の振動数になる")
    s.link(bohrFrequency, spectrum, LinkType.PRODUCES, "振動数条件が輝線スペクトルの正体")
    s.link(spectrum, energyLevel, LinkType.RELATED, "系列は遷移先 n' で分かれる")
    s.link(xrayTube, bragg, LinkType.CONTRAST, "X線の粒子性（最短波長）↔ X線の波動性（回折）")
    s.link(bragg, deBroglie, LinkType.RELATED, "電子線回折も同じ条件で説明できる")
    s.link(nucleus, massDefect, LinkType.RELATED, "核子の質量の和との差が結合エネルギー")
    s.link(massDefect, fission, LinkType.PRODUCES, "結合エネルギーの差が、分裂・融合で取り出せるエネルギー")
    s.link(decay, halfLife, LinkType.RELATED, "崩壊の速さを表すのが半減期")
    s.link(nucleus, decay, LinkType.RELATED, "崩壊で A と Z がどう変わるか")
    s.link(rutherford, nucleus, LinkType.PRODUCES, "原子核の存在が分かった実験")
    s.link(millikan, photoelectric, LinkType.RELATED, "電気素量 e が決まって初めて、光電効果の測定から h が出せる")
    s.link(rutherford, millikan, LinkType.SAME_GROUP, "原子の姿を決めた古典的な実験")
}

internal const val PHYSICS_DECK = "物理・原子"
