package com.tango.recall.data

/**
 * 速読用の英文.
 *
 * Written for this app. A passage has to be long enough for the eye to settle into a
 * rhythm — two hundred words or so — and the question has to be answerable only by
 * someone who followed the argument, not by someone who caught a keyword. Each one
 * also carries a full translation, because the point of measuring speed is to raise it
 * without losing what the sentences actually said.
 *
 * The subjects are the ones the papers keep returning to: memory, judgement, models,
 * cities, automation, language.
 */
internal fun seedReading(s: Seeder) {
    val deckId = s.deck(
        name = READING_DECK,
        type = NoteType.READING,
        templates = setOf("reading_answer"),
        newPerDay = 2,
        relationQuiz = true,
    )

    fun passage(
        title: String, passage: String, question: String, answer: String,
        points: String, ja: String,
    ): Long = s.note(
        deckId, NoteType.READING,
        mapOf(
            "title" to title, "passage" to passage.trimIndent(), "question" to question,
            "answer" to answer, "points" to points, "ja" to ja.trimIndent(),
            "source" to "自作", "memo" to "",
        ),
        listOf("速読"),
    )

    val forgetting = passage(
        "Why forgetting is useful",
        """
        Forgetting is usually described as a failure. We speak of memory as a container
        that leaks, and of names and dates as things that slip out of it. Yet a memory
        that kept everything would be close to useless. Consider what it would mean to
        recall every route you have ever taken to work: the one you need today would be
        buried under thousands of others that differ from it only slightly. What makes
        recall possible is not storage but selection, and selection means letting most
        of the record go.

        Experiments on retrieval point the same way. When people are asked to remember
        one item from a set, the items they do not retrieve become harder to reach
        afterwards, as though recall actively suppressed its competitors. Forgetting, on
        this view, is not the opposite of remembering but part of its machinery.

        This has a consequence for anyone who studies. If the difficulty of recalling
        something is what strengthens it, then a method that removes the difficulty —
        rereading a page until it feels familiar — removes the benefit as well. The
        fluency that rereading produces is not evidence of learning. It is evidence that
        the page is in front of you.
        """,
        "Why does the author say that a memory which kept everything would be useless?",
        "必要な記憶が、ごくわずかしか違わない大量の記憶に埋もれてしまうから。想起を可能にするのは貯蔵ではなく選択であり、選択とは記録の大部分を手放すことだから。",
        "「忘却は想起の失敗ではなく、その仕組みの一部だ」という主張をつかむ\n" +
            "通勤経路の例が何を示すための例かを言える\n" +
            "最終段落の「流暢さは学習の証拠ではない」を訳せる",
        """
        忘れることはたいてい失敗として語られる。私たちは記憶を漏れのある容器のように言い、
        名前や日付をそこからこぼれ落ちるものとして扱う。しかし、すべてを保持する記憶は
        ほとんど役に立たないだろう。これまで職場へ向かうのに通ったすべての道順を思い出せる、
        ということが何を意味するか考えてみればよい。今日必要な一本は、わずかしか違わない
        何千もの道順の下に埋もれてしまう。想起を可能にしているのは貯蔵ではなく選択であり、
        選択するとは記録の大半を手放すということである。

        想起についての実験も同じ方向を指している。ある組の中から1つを思い出すよう求められると、
        思い出さなかった項目はその後かえって取り出しにくくなる。まるで想起が競合相手を
        積極的に抑え込んでいるかのようである。この見方に立てば、忘却は記憶することの反対では
        なく、その装置の一部だということになる。

        これは学ぶ者にとって帰結を持つ。思い出しにくさそのものが記憶を強くするのなら、
        その難しさを取り除く方法——なじみが出るまでページを読み返すこと——は、効果もろとも
        取り除いてしまう。読み返しが生む流暢さは、学習の証拠ではない。それはページが目の前に
        あることの証拠にすぎない。
        """,
    )

    val intuition = passage(
        "The limits of intuition",
        """
        Intuition is fast, and in most of daily life it is right. We judge distances,
        read faces and finish other people's sentences without deliberate thought, and
        we are seldom badly wrong. That success, however, comes from a narrow range of
        problems: the ones our senses evolved to handle, and the ones we meet often
        enough to learn from.

        Probability is neither. Asked which is more likely — that a quiet, orderly
        person is a librarian, or that he is a farmer — most people choose the
        librarian, because the description resembles the stereotype. But farmers
        outnumber librarians by a wide margin, and no amount of resemblance can outweigh
        that difference. The mistake is not carelessness. It is a rule that works
        elsewhere, applied where it does not.

        What follows is not that intuition should be distrusted in general, but that it
        should be checked wherever the answer depends on numbers we cannot see. A doctor
        reading a test result, a jury weighing evidence, a student choosing what to
        revise — each is in a position where the feeling of obviousness carries no
        information at all. The remedy is dull: write the numbers down, and let them,
        rather than the impression, decide.
        """,
        "According to the passage, in what situations should intuition be checked, and why?",
        "答えが目に見えない数値に左右される場面。直観が当たるのは、感覚が進化的に扱ってきた問題と、繰り返し出会って学習できた問題に限られ、そこから外れると「他所で通用する規則の誤用」が起きるから。",
        "直観がよく当たる範囲を限定している2つの条件を言える\n" +
            "図書館員と農場経営者の例が示す誤りの正体（不注意ではない）を言える\n" +
            "最終文の「印象ではなく数字に決めさせる」を訳せる",
        """
        直観は速く、日常生活の大半では正しい。私たちは距離を測り、表情を読み、他人の文の
        続きを口にする——どれも意識的に考えることなくやってのけ、大きく外すことはめったにない。
        だがその成功は、狭い範囲の問題から来ている。すなわち、私たちの感覚が進化の中で
        扱ってきた問題と、学習できるほど頻繁に出会う問題である。

        確率はそのどちらでもない。「物静かで几帳面な人物は、図書館員か農場経営者か」と
        問われると、多くの人は図書館員を選ぶ。その記述が典型像に似ているからだ。
        しかし農場経営者の数は図書館員をはるかに上回っており、似ているという事実は、その差を
        覆すには足りない。この誤りは不注意によるものではない。他の場面では有効な規則を、
        有効でない場面に当てはめた結果である。

        ここから導かれるのは、直観一般を疑えということではなく、答えが目に見えない数値に
        左右される場面では必ず点検せよ、ということである。検査結果を読む医師、証拠を量る陪審、
        何を復習するか選ぶ受験生——いずれも、「明らかだ」という感じが何の情報も持たない
        場面にいる。処方は退屈なものだ。数字を書き出し、印象にではなく数字に決めさせればよい。
        """,
    )

    val model = passage(
        "What a map leaves out",
        """
        Every map is a claim about what matters. A road map shows distances and
        junctions and says nothing about the height of the hills; a geological map shows
        the rock beneath and ignores the roads. Neither is false. What makes a map
        useful is precisely that it leaves things out, and the skill of making one lies
        in choosing what to discard.

        The same holds for the models used in science. A model of a falling body that
        ignores air resistance is wrong, in the sense that no real body falls that way,
        and right, in the sense that it predicts how quickly a stone reaches the ground
        to within the accuracy anyone needs. Adding air resistance improves the
        prediction for a feather and destroys the simplicity that made the model worth
        having for the stone.

        Trouble begins when the discarded parts are forgotten rather than chosen. A
        financial model that treats rare events as impossible is not simplified but
        misleading, because the thing left out is the thing that eventually decides the
        outcome. The question to ask of any model, then, is not whether it is true, but
        what it omits, and whether that omission is safe here.
        """,
        "What distinguishes a useful simplification from a misleading one?",
        "省いた部分を選んで省いたのか、忘れているのかの違い。省略したものが結果を決めてしまう場面では、その簡略化は「単純化」ではなく「誤導」になる。問うべきは真偽ではなく、何を省いたか、その省略はここで安全か。",
        "地図と科学の模型が同じ構造だという対応をつかむ\n" +
            "空気抵抗の例で「単純さ自体に価値がある」と言っていることを読み取る\n" +
            "最終文の問いの立て方（真偽ではなく省略）を訳せる",
        """
        どんな地図も「何が重要か」についての主張である。道路地図は距離と分岐を示し、丘の高さに
        ついては何も言わない。地質図は地下の岩を示し、道路を無視する。どちらも偽ではない。
        地図を有用にしているのは、まさに何かを省いていることであり、地図を作る技術とは
        何を捨てるかを選ぶ技術にほかならない。

        科学で使われる模型も同じである。空気抵抗を無視した落体の模型は、現実の物体が
        そんなふうには落ちないという意味では誤っており、石が地面に届くまでの速さを、必要な
        精度の範囲で予測できるという意味では正しい。空気抵抗を加えれば羽根についての予測は
        改善するが、石にとってその模型を持つ価値そのものだった単純さは失われる。

        問題が生じるのは、捨てた部分が「選ばれた」のではなく「忘れられた」ときである。
        まれな出来事を起こりえないものとして扱う金融モデルは、単純化されているのではなく
        誤導している。省かれたものこそが、最終的に結果を決めるからだ。したがって、どんな
        模型に対しても問うべきは、それが真かどうかではなく、何を省いているか、そしてその
        省略はこの場面で安全か、である。
        """,
    )

    val cities = passage(
        "Cities and the species that stay",
        """
        A city is often treated as the opposite of nature, a place from which wildlife
        has been pushed out. The record is more interesting than that. Some species
        disappear as a city grows, but others arrive and thrive, and the ones that stay
        are not a random sample. They tend to be generalists: animals that eat many
        kinds of food, tolerate disturbance, and breed in places resembling the ledges
        and cavities of their original habitat. A pigeon on a window ledge is doing what
        its ancestors did on a cliff.

        This filtering has measurable effects. Studies comparing urban and rural
        populations of the same bird find differences in boldness, in the timing of
        breeding, and even in song: where traffic noise is low in frequency, some birds
        sing higher, and the change persists in city birds across generations.

        Whether such changes should be called adaptation is disputed, and the dispute
        matters. If cities are only filters, conservation within them is a matter of
        protecting whatever survives. If they are also engines of change, then a city is
        producing populations that differ from their rural relatives, and the question
        becomes what we are selecting for without intending to.
        """,
        "What kind of species tend to remain as a city grows, and what does the author conclude from the dispute about adaptation?",
        "残るのはジェネラリスト（多様な餌を食べ、撹乱に耐え、元の生息地の岩棚や穴に似た場所で繁殖する種）。都市が単なるフィルターなのか、変化を生む装置でもあるのかで話が変わり、後者なら「意図せずに何を選択してしまっているのか」が問題になる、と結論している。",
        "generalist の条件を3つ挙げられる\n" +
            "鳩と崖の対応が何のための例かを言える\n" +
            "最終段落の二分（フィルターか、変化の原動力か）と、その帰結を訳せる",
        """
        都市はしばしば自然の対極として、野生生物が追い出された場所として扱われる。だが実際の
        記録はそれよりも興味深い。都市が大きくなるにつれて姿を消す種がある一方で、やって来て
        繁栄する種もあり、残る種は無作為な標本ではない。残るのはたいていジェネラリストである。
        多様な餌を食べ、撹乱に耐え、もとの生息地の岩棚や穴に似た場所で繁殖する動物たちだ。
        窓の縁にいる鳩は、その祖先が崖でしていたことをしている。

        この選別には測定可能な効果がある。同じ鳥の都市個体群と農村個体群を比べた研究は、
        大胆さ、繁殖の時期、さらには鳴き声にまで違いを見いだしている。交通騒音が低い周波数を
        占める場所では、より高い声で鳴く鳥がおり、その変化は世代をまたいで都市の鳥に残る。

        こうした変化を適応と呼ぶべきかは論争があり、その論争には意味がある。都市が単なる
        フィルターにすぎないなら、都市の保全とは生き残ったものを守ることである。しかし都市が
        変化を生む装置でもあるなら、都市は農村の同種とは異なる個体群を作り出していることになり、
        問いは「私たちは意図せずに何を選択しているのか」に変わる。
        """,
    )

    val automation = passage(
        "The cost of a faster answer",
        """
        When a task is handed to a machine, the gain is usually described in time saved.
        The cost is harder to see, because it is not paid at once. A system that sorts
        applications, flags transactions or suggests a diagnosis does not merely do the
        work faster; it changes what the people around it practise. Skills that go
        unexercised decay, and the ones that decay first are the ones the machine
        handles best.

        This matters most when the machine is right nearly always. If it failed often,
        its users would stay alert. Because it rarely fails, they learn to accept its
        output, and the rare case — the one the system was never built for — arrives
        when nobody is in a state to catch it. Pilots have known this for decades and
        have a name for it: automation dependency.

        None of this is an argument for doing things slowly by hand. It is an argument
        for deciding, deliberately, which judgements to keep. A tool that shows its
        reasoning, or that asks the user to commit to an answer before revealing its
        own, keeps that user in practice at the cost of a little speed — which is
        exactly the point.
        """,
        "Why does the author say that a system which is right nearly always is the most risky kind?",
        "めったに失敗しないために利用者が出力をそのまま受け入れるようになり、使われない技能が衰えるから。そして想定外の稀な事例が来たとき、誰もそれを捕まえられる状態にない。",
        "「省力化の代償は一度に払われない」という書き出しの主張をつかむ\n" +
            "automation dependency が何を指すかを説明できる\n" +
            "最終段落の処方（判断を選んで手元に残す・答えを先に決めさせる）を訳せる",
        """
        ある作業を機械に任せるとき、その利得はたいてい「短縮された時間」で語られる。
        代償のほうは見えにくい。一度に支払われるものではないからだ。応募書類を仕分けし、
        取引に印をつけ、診断を提案するシステムは、単に作業を速くするだけではない。
        周囲の人間が何を習練するかを変えてしまう。使われない技能は衰え、まっさきに衰えるのは、
        その機械がいちばんうまく扱う技能である。

        これがもっとも重大になるのは、機械がほとんど常に正しいときである。しばしば失敗するので
        あれば、使う側は警戒を解かない。めったに失敗しないからこそ、人はその出力を受け入れる
        ことを学び、そして稀な事例——そのシステムが想定していなかった事例——は、誰も
        それを捕まえられる状態にないときにやって来る。操縦士たちは何十年も前からこれを知っており、
        自動化依存という名前まで与えている。

        以上は、何でも手作業でゆっくりやれという議論ではない。どの判断を自分の手元に残すかを、
        意識して決めよという議論である。推論の過程を見せる道具、あるいは自分の答えを示す前に
        利用者に答えを確定させる道具は、わずかな速さと引き換えに使い手を習練の中に留める。
        そここそが要点なのである。
        """,
    )

    val language = passage(
        "Learning a language, twice",
        """
        A child learns a first language without being taught. Nobody explains the rule
        for word order, and yet by four the rule is in place, applied to sentences the
        child has never heard. A second language learned later is different in almost
        every respect: it is studied, corrected and practised, and the result is usually
        an accent that never quite goes and a grammar that has to be watched.

        The contrast has been used to argue that the capacity for language has a
        critical period, after which the mechanism is no longer available. The evidence
        is real but narrower than the claim. Later learners do reach very high levels,
        and those who do tend to be the ones who used the language for years for
        something they cared about, rather than the ones who studied it hardest for a
        term.

        For an examination the practical reading is modest. A learner cannot become a
        child again, and does not need to. What can be copied from the child is the
        volume and the purpose: language met often and in quantity, and used for
        something other than the language itself. A vocabulary list is a way in, not a
        substitute for that.
        """,
        "What, according to the author, can an older learner usefully copy from a child learning a first language?",
        "量と目的。すなわち、言語に頻繁かつ大量に触れること、そして言語そのもの以外の目的のために使うこと。子どもに戻ることはできないし、その必要もない。",
        "母語習得と第二言語学習の違いの列挙を追える\n" +
            "臨界期の主張に対する「証拠は主張より狭い」という留保を訳せる\n" +
            "最終文（単語リストは入口であって代わりではない）の含みを言える",
        """
        子どもは教えられずに母語を習得する。語順の規則を説明する者は誰もいないのに、4歳までに
        その規則は定着し、一度も聞いたことのない文にも適用される。後から学ぶ第二言語は、
        ほとんどあらゆる点で異なる。学習し、訂正され、練習する——そしてたいていの結果は、
        完全には消えない訛りと、注意し続けなければならない文法である。

        この対比は、言語の能力には臨界期があり、それを過ぎるとその仕組みはもう使えない、
        という主張の根拠に使われてきた。証拠は実在するが、主張よりも射程は狭い。後から学んだ
        者でも非常に高い水準に達することはあるし、そこに達するのは、一学期のあいだ最も熱心に
        勉強した者ではなく、自分が大切に思う何かのために何年もその言語を使った者であることが多い。

        試験に向けての実践的な読みは、ささやかなものだ。学習者は子どもに戻れないし、戻る必要も
        ない。子どもから写せるのは量と目的である。すなわち、頻繁に大量に触れること、そして
        言語そのもの以外の何かのために使うこと。単語リストはその入口であって、代わりではない。
        """,
    )

    s.link(forgetting, language, LinkType.SAME_GROUP, "どちらも「学び方」そのものを主題にした文章")
    s.link(intuition, model, LinkType.SAME_GROUP, "判断の型を扱う文章。直観の誤用 ↔ 模型の省略")
    s.link(intuition, automation, LinkType.RELATED, "人が判断を手放す場面。確率での誤用 ↔ 機械への依存")
    s.link(model, cities, LinkType.CONTRAST, "抽象の話 ↔ 観察の話。どちらも「何を切り捨てたか」を問う")
    s.link(automation, forgetting, LinkType.RELATED, "使わない能力は衰える、という同じ論理の別の現れ方")
}

internal const val READING_DECK = "速読（時間を測って読む）"
