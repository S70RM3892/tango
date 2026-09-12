package com.tango.recall.data

/**
 * 数学の定石.
 *
 * Maths is not memorised, and this is not an attempt to memorise it. What is worth
 * recalling on demand is the first move — "classify by remainder", "take the fixed
 * point and turn it into a geometric sequence", "the answer is a necessary condition,
 * so check sufficiency" — because that is what does not come when the paper is in
 * front of you. So the card shows the problem and asks for the plan, and marking is a
 * checklist of the steps that had to appear.
 *
 * The problems are written for this app rather than copied from a paper: an exam
 * question reproduced from memory would be subtly wrong, and wrong is worse than
 * absent. Add real past problems yourself — the 出典 field is there for it.
 */
internal fun seedMath(s: Seeder) {
    val deckId = s.deck(
        name = MATH_DECK,
        type = NoteType.MATH,
        templates = setOf("math_plan", "math_tools"),
        newPerDay = 4,
        relationQuiz = true,
    )

    fun plan(
        question: String, approach: String, steps: String, tools: String, traps: String,
        field: String,
    ): Long = s.note(
        deckId, NoteType.MATH,
        mapOf(
            "question" to question, "approach" to approach, "steps" to steps,
            "tools" to tools, "traps" to traps, "source" to "自作（出題の型に沿った練習）", "memo" to "",
        ),
        listOf("数学", field),
    )

    // ---- 整数 -----------------------------------------------------------------

    val residue = plan(
        "n を整数とする。n² + n + 1 が 3 の倍数になるのは、n を 3 で割った余りがいくつのときか。",
        "3 で割った余りで n を分類し、全部試す",
        "n = 3k, 3k+1, 3k+2 の3通りに分ける\n" +
            "それぞれ n² + n + 1 を 3 で割った余りを計算する\n" +
            "余り 1 のときだけ 3 の倍数になることを示す",
        "剰余による分類、合同式",
        "3通りで尽くされていることを明記する。1つの例で確かめただけで終えない。",
        "整数",
    )
    val consecutive = plan(
        "連続する3つの整数の積は 6 の倍数であることを示せ。",
        "2 の倍数であることと 3 の倍数であることを別々に示す",
        "連続2整数のうち少なくとも1つは偶数\n" +
            "連続3整数のうち少なくとも1つは 3 の倍数\n" +
            "2 と 3 は互いに素だから 6 の倍数",
        "剰余による分類、互いに素",
        "「2 の倍数かつ 3 の倍数 → 6 の倍数」は 2 と 3 が互いに素だから言える。ここを飛ばさない。",
        "整数",
    )
    val irrational = plan(
        "√2 が無理数であることを示せ。",
        "背理法。既約分数と仮定して矛盾を出す",
        "√2 = q/p（p, q は互いに素な自然数）と仮定する\n" +
            "2p² = q² から q は偶数、q = 2r とおく\n" +
            "p も偶数となり、互いに素としたことに矛盾する",
        "背理法、偶奇",
        "「互いに素」と置いたことを最後に必ず使う。置かずに始めると矛盾が出ない。",
        "整数",
    )
    val diophantine = plan(
        "整数 x, y について 7x + 5y = 1 の整数解をすべて求めよ。",
        "特殊解を1組見つけ、一般解に広げる",
        "互除法などで特殊解を1組求める（例: x = 3, y = -4）\n" +
            "7(x-3) + 5(y+4) = 0 から 7(x-3) = -5(y+4)\n" +
            "7 と 5 は互いに素なので x = 3 + 5t, y = -4 - 7t",
        "ユークリッドの互除法、一次不定方程式",
        "動かす幅は係数そのものではなく、最大公約数で割った値。",
        "整数",
    )

    // ---- 確率・場合の数 ---------------------------------------------------------

    val probRecurrence = plan(
        "表の出る確率が 1/3 のコインを n 回投げる。表の出る回数が偶数（0 回を含む）である確率 p_n を求めよ。",
        "n 回目で偶奇が入れ替わることに注目して漸化式を作る",
        "n-1 回目までが偶数なら n 回目は裏、奇数なら n 回目は表\n" +
            "p_n = (2/3)p_{n-1} + (1/3)(1 - p_{n-1}) = (1/3)p_{n-1} + 1/3 を立てる\n" +
            "不動点 1/2 を引いて等比型にし、p_0 = 1 から p_n = 1/2 + (1/2)(1/3)^n",
        "確率漸化式、等比数列への帰着",
        "p_0 = 1（0 回は偶数）を落とさない。初項をどこに取るかで答えがずれる。",
        "確率",
    )
    val complement = plan(
        "さいころを n 回投げるとき、少なくとも1回 6 の目が出る確率を求めよ。",
        "「少なくとも1回」は余事象で数える",
        "余事象は「1回も 6 が出ない」\n" +
            "その確率は (5/6)^n\n" +
            "求める確率は 1 - (5/6)^n",
        "余事象",
        "「少なくとも」を見たら、まず余事象を疑う。直接数えると場合が爆発する。",
        "確率",
    )
    val conditional = plan(
        "ある病気の検査がある。有病率 1%、病気なら 99% が陽性、健康でも 5% が陽性になる。陽性だった人が実際に病気である確率を求めよ。",
        "条件付き確率の定義に戻し、分母は全確率の公式で作る",
        "P(病気|陽性) = P(病気かつ陽性) ÷ P(陽性)\n" +
            "分子 = 0.01 × 0.99、分母 = 0.01 × 0.99 + 0.99 × 0.05\n" +
            "約 0.167、すなわち 17% 程度",
        "条件付き確率、全確率の公式",
        "P(陽性|病気) と P(病気|陽性) を取り違えない。値が大きく違う。",
        "確率",
    )
    val permutation = plan(
        "a, a, a, b, b, c の6文字を並べる方法は何通りか。",
        "全体の並べ方を、同じものの並べ方で割る",
        "6 文字の並べ方は 6!\n" +
            "a 3個、b 2個は区別できないので 3!·2! で割る\n" +
            "6! ÷ (3!·2!) = 60 通り",
        "同じものを含む順列",
        "「区別する／しない」を問題文から必ず読み取る。ここを間違えると全部ずれる。",
        "場合の数",
    )

    // ---- 数列・極限 -------------------------------------------------------------

    val recurrence = plan(
        "a_1 = 1、a_{n+1} = 2a_n + 3 で定まる数列の一般項を求めよ。",
        "不動点を引いて等比数列に直す",
        "α = 2α + 3 を解いて α = -3\n" +
            "a_{n+1} + 3 = 2(a_n + 3) と変形する\n" +
            "a_n + 3 = (a_1 + 3)·2^{n-1} = 4·2^{n-1}、a_n = 2^{n+1} - 3",
        "特性方程式、等比数列",
        "初項の添字に注意する。2^{n-1} か 2^n かでずれる。",
        "数列",
    )
    val difference = plan(
        "階差数列が b_n = 2n + 1 で、a_1 = 2 の数列 {a_n} の一般項を求めよ。",
        "階差の和をとる。ただし n = 1 は別に確認する",
        "n ≧ 2 のとき a_n = a_1 + Σ_{k=1}^{n-1} b_k\n" +
            "Σ_{k=1}^{n-1}(2k+1) = (n-1)n + (n-1) = n² - 1\n" +
            "a_n = n² + 1。n = 1 でも成り立つことを確かめる",
        "階差数列、Σ の公式",
        "n = 1 で成り立つかの確認を必ず書く。ここを落とすのが最も多い減点。",
        "数列",
    )
    val induction = plan(
        "すべての自然数 n について 1 + 2 + … + n = n(n+1)/2 が成り立つことを、数学的帰納法で示せ。",
        "n = 1 を示し、n = k を仮定して n = k+1 を示す",
        "n = 1 のとき両辺 1 で成立\n" +
            "n = k を仮定し、両辺に k+1 を足す\n" +
            "k(k+1)/2 + (k+1) = (k+1)(k+2)/2 となり n = k+1 でも成立",
        "数学的帰納法",
        "仮定をどこで使ったかを明示する。使っていなければ帰納法になっていない。",
        "数列",
    )
    val squeeze = plan(
        "lim_{n→∞} (sin n)/n を求めよ。",
        "絶対値で上から押さえてはさみうちにする",
        "|sin n| ≦ 1 より |(sin n)/n| ≦ 1/n\n" +
            "1/n → 0\n" +
            "はさみうちの原理より極限は 0",
        "はさみうちの原理",
        "sin n 自体は振動して極限を持たない。押さえてから極限を取る。",
        "極限",
    )
    val eLimit = plan(
        "lim_{n→∞} (1 + 2/n)^n を求めよ。",
        "(1 + 1/m)^m の形に持ち込む",
        "m = n/2 とおくと 1 + 2/n = 1 + 1/m\n" +
            "(1 + 1/m)^{2m} = {(1 + 1/m)^m}²\n" +
            "m → ∞ で e² に収束する",
        "e の定義、指数法則",
        "指数の対応（n = 2m）を合わせる。合わせずに e としてしまう誤りが多い。",
        "極限",
    )
    val riemann = plan(
        "lim_{n→∞} (1/n)Σ_{k=1}^{n} f(k/n) を定積分で表せ。",
        "区分求積法。幅 1/n、高さ f(k/n) の短冊の和と見る",
        "1/n を Δx、k/n を x と読み替える\n" +
            "k = 1 から n なので x は 0 から 1 まで\n" +
            "極限は ∫_0^1 f(x) dx",
        "区分求積法、定積分",
        "積分の上端・下端は k の動く範囲から決まる。k = 0 から n-1 でも同じ区間になる。",
        "極限",
    )

    // ---- 微分・積分 -------------------------------------------------------------

    val inequality = plan(
        "x ≧ 0 のとき e^x ≧ 1 + x を示せ。",
        "差を関数とおいて微分し、最小値が 0 以上であることを示す",
        "f(x) = e^x - 1 - x とおく\n" +
            "f'(x) = e^x - 1 ≧ 0（x ≧ 0）なので f は増加\n" +
            "f(0) = 0 より f(x) ≧ 0、等号は x = 0 のとき",
        "微分、増減表",
        "等号成立の条件まで書く。不等式の証明はそこまでが答え。",
        "微積分",
    )
    val tangentCount = plan(
        "点 (0, a) から曲線 y = x³ に引ける接線の本数を、a の値で分類せよ。",
        "接点を t とおき、通過条件を t の方程式にして実数解の個数を数える",
        "接点 (t, t³) での接線は y = 3t²x - 2t³\n" +
            "(0, a) を通る条件から a = -2t³\n" +
            "t の方程式の実数解の個数が接線の本数（a ≠ 0 なら1本、a = 0 なら1本）",
        "微分、方程式の実数解の個数",
        "「接線の本数＝接点の個数」であって、通る点の個数ではない。重解の扱いに注意する。",
        "微積分",
    )
    val area = plan(
        "2曲線で囲まれた部分の面積を求めるときの手順を述べよ。",
        "交点を求め、区間ごとにどちらが上かを確定してから積分する",
        "連立して交点の x 座標を求める\n" +
            "区間ごとに（上の関数 - 下の関数）を決める\n" +
            "上下が入れ替わる点で区間を分けて積分する",
        "定積分、グラフの上下関係",
        "絶対値のまま積分しない。上下が入れ替わる点で必ず分ける。",
        "微積分",
    )
    val symmetry = plan(
        "∫_{-a}^{a} f(x) dx を、f が偶関数・奇関数のときにどう簡単にできるか。",
        "区間が原点対称であることを使って対称性で消す",
        "奇関数なら 0\n" +
            "偶関数なら 2∫_0^a f(x) dx\n" +
            "一般の f は偶関数部分と奇関数部分に分けられる",
        "偶関数・奇関数、定積分の性質",
        "区間が原点対称であることが前提。そうでなければ使えない。",
        "微積分",
    )

    // ---- 図形 -------------------------------------------------------------------

    val locus = plan(
        "軌跡を求める問題の答案は、どのような形で締めるべきか。",
        "条件を式にして文字を消し、最後に逆（十分性）を確認する",
        "動点を (x, y) とおき、条件を式にする\n" +
            "媒介変数を消去して x, y の関係式を得る（ここまでは必要条件）\n" +
            "得られた図形上の点がすべて条件を満たすことを確認し、除外点を除く",
        "軌跡、同値変形",
        "消去して出てきた式は必要条件にすぎない。逆の確認と除外点で答案が完成する。",
        "図形と方程式",
    )
    val coplanar = plan(
        "空間の4点 A, B, C, P が同一平面上にある条件をベクトルで書け。",
        "始点をそろえて 1 次結合で表し、係数の和を見る",
        "AP = sAB + tAC と表せることが条件\n" +
            "原点 O を始点にすると OP = αOA + βOB + γOC、α + β + γ = 1\n" +
            "A, B, C が一直線上にない（AB と AC が 1 次独立）ことが前提",
        "位置ベクトル、1 次独立",
        "始点をそろえ忘れると係数の和の条件が使えない。",
        "ベクトル",
    )
    val pointPlane = plan(
        "点と平面の距離を求める手順を述べよ。",
        "平面の法線ベクトルに射影する",
        "平面の法線ベクトル n を求める\n" +
            "平面上の点 A と対象の点 P を結ぶ AP をとる\n" +
            "距離 = |AP·n| ÷ |n|",
        "内積、法線ベクトル、正射影",
        "|n| で割るのを忘れない。内積の絶対値だけでは長さにならない。",
        "ベクトル",
    )
    val rotation = plan(
        "複素数平面で、点 z を点 α のまわりに θ だけ回転した点 w を表せ。",
        "中心を原点に移して回し、戻す",
        "z - α で中心を原点に移す\n" +
            "e^{iθ}(z - α) で回転する\n" +
            "α を足して戻す: w = α + e^{iθ}(z - α)",
        "複素数の極形式、回転",
        "回転してから平行移動すると別の点になる。移す → 回す → 戻すの順を守る。",
        "複素数平面",
    )
    val pureImaginary = plan(
        "3点 A(α), B(β), P(z) について (z-α)/(z-β) が純虚数となる z の軌跡は何か。",
        "純虚数の条件は「共役を足すと 0」。幾何的には直角",
        "w = (z-α)/(z-β) とおき、w + conj(w) = 0 かつ w ≠ 0\n" +
            "偏角で見ると ∠APB = 90°\n" +
            "AB を直径とする円（ただし A, B は除く）",
        "共役複素数、偏角、軌跡",
        "w ≠ 0（z ≠ α）と z ≠ β の除外を落とさない。",
        "複素数平面",
    )
    val composition = plan(
        "a sinθ + b cosθ を1つの三角関数にまとめよ。",
        "三角関数の合成。振幅を先に決め、角を後から合わせる",
        "振幅は √(a² + b²)\n" +
            "a sinθ + b cosθ = √(a²+b²) sin(θ + α)\n" +
            "cos α = a/√(a²+b²), sin α = b/√(a²+b²) で α を決める",
        "三角関数の合成、加法定理",
        "sin にまとめるか cos にまとめるかで α が変わる。どちらに合わせたか明記する。",
        "三角関数",
    )

    // ---- つながり ---------------------------------------------------------------

    s.link(residue, consecutive, LinkType.SAME_GROUP, "どちらも剰余で分類して全部の場合を尽くす型")
    s.link(residue, diophantine, LinkType.RELATED, "整数問題の2本柱: 余りで分類する / 互除法で1組見つける")
    s.link(irrational, induction, LinkType.CONTRAST, "背理法（仮定して壊す）↔ 帰納法（1つ示して積み上げる）")
    s.link(probRecurrence, recurrence, LinkType.RELATED, "確率漸化式も、結局は不動点を引いて等比型にする")
    s.link(recurrence, difference, LinkType.SAME_GROUP, "漸化式から一般項へ。特性方程式と階差の和")
    s.link(complement, conditional, LinkType.SAME_GROUP, "数え方を変える型: 余事象で裏返す / 条件で絞る")
    s.link(complement, permutation, LinkType.CONTRAST, "確率は数え方の工夫、場合の数は重複の割り算")
    s.link(squeeze, eLimit, LinkType.SAME_GROUP, "極限の2大手法: 押さえて挟む / 既知の形に帰着させる")
    s.link(squeeze, riemann, LinkType.SAME_GROUP, "和や振動を、既知の極限に落とし込む")
    s.link(inequality, tangentCount, LinkType.RELATED, "どちらも「差を関数とみて増減を調べる」")
    s.link(area, symmetry, LinkType.SAME_GROUP, "定積分を計算する前に、図形の性質で簡単にする")
    s.link(locus, tangentCount, LinkType.RELATED, "必要条件で出した答えを、十分性・個数で締める")
    s.link(coplanar, pointPlane, LinkType.SAME_GROUP, "空間図形はベクトルの 1 次結合と内積に落とす")
    s.link(rotation, pureImaginary, LinkType.SAME_GROUP, "複素数平面は「回転」と「偏角の条件」で読む")
    s.link(rotation, composition, LinkType.RELATED, "回転も合成も、角をまとめる操作")
    s.link(difference, induction, LinkType.CONFUSABLE, "一般項を出したあと、n = 1 の確認と帰納法の仮定を混同しやすい")
}

internal const val MATH_DECK = "数学・定石"
