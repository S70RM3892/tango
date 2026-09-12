package com.tango.recall.data

/**
 * 熟語.
 *
 * A phrasal verb is not an arbitrary pair. The particle carries a meaning of its own —
 * off separates, up completes, out exhausts, through goes all the way — and once that
 * is visible, a family of ten stops being ten separate things to remember. So they are
 * grouped by that particle and each note says, in one line, why the pair means what it
 * means.
 *
 * Every entry also carries the one-word equivalent. Recognising an idiom is enough for
 * reading; having the single word is what makes it usable when writing, and it ties
 * the idiom to a word already in the collection.
 *
 * Columns: 熟語 | 意味 | 芯 | なぜその意味か | 例文 | 例文訳 | 1語で言い換えると
 */
internal fun seedIdioms(s: Seeder) {
    val deckId = s.deck(
        name = IDIOM_DECK,
        type = NoteType.IDIOM,
        templates = setOf("idiom_ja", "ja_idiom", "idiom_cloze"),
        newPerDay = 10,
        relationQuiz = true,
    )

    val byFamily = LinkedHashMap<String, MutableList<Long>>()
    for (line in IDIOM_TABLE.lines()) {
        val row = line.trim()
        if (row.isEmpty() || row.startsWith("#")) continue
        val cells = row.split("|").map { it.trim() }
        require(cells.size == 7) { "列が 7 つではありません: $row" }
        val id = s.note(
            deckId, NoteType.IDIOM,
            mapOf(
                "phrase" to cells[0], "meaning" to cells[1], "family" to cells[2],
                "core" to cells[3], "example" to cells[4], "exampleJa" to cells[5],
                "synonym" to cells[6], "memo" to "",
            ),
            listOf("熟語", cells[2].substringBefore("（").trim()),
        )
        byFamily.getOrPut(cells[2]) { mutableListOf() } += id
    }

    // Within a family, everything is linked: the particle is the thing being learned.
    for ((family, ids) in byFamily) {
        for (i in ids.indices) for (j in i + 1 until ids.size) {
            s.link(ids[i], ids[j], LinkType.SAME_GROUP, family)
        }
    }

    // And across to the single words, which is what makes an idiom writable.
    val notes = s.repo.listNotes(null, "", Int.MAX_VALUE).associateBy { it.title() }
    fun link(idiom: String, word: String, type: LinkType, memo: String) {
        val a = notes[idiom]?.id ?: return
        val b = notes[word]?.id ?: return
        s.link(a, b, type, memo)
    }
    link("put off", "defer", LinkType.SYNONYM, "1語なら defer。どちらも「先へ送る」")
    link("put up with", "endure", LinkType.SYNONYM, "耐える。put up with は口語寄り")
    link("do away with", "abolish", LinkType.SYNONYM, "廃止する")
    link("make up for", "compensate", LinkType.SYNONYM, "埋め合わせる。compensate for と同じ形")
    link("account for", "explain", LinkType.SYNONYM, "説明する／占める")
    link("get over", "conquer", LinkType.SYNONYM, "克服する")
    link("run out of", "deplete", LinkType.RELATED, "使い果たす側と、使い果たされる側")
    link("comply with", "comply", LinkType.DERIVED, "同じ動詞。前置詞まで含めて覚える")
    link("look forward to", "anticipate", LinkType.SYNONYM, "期待して待つ。anticipate は中立、look forward to は好意的")
    link("take A into account", "consider", LinkType.SYNONYM, "考慮に入れる")
    link("give rise to", "generate", LinkType.SYNONYM, "引き起こす")
    link("bring about", "trigger", LinkType.SYNONYM, "もたらす・引き起こす")
    link("get rid of", "eliminate", LinkType.SYNONYM, "取り除く")
    link("be aware of", "recognize", LinkType.RELATED, "気づいている状態と、気づく動作")
    link("be inclined to", "tend", LinkType.SYNONYM, "〜する傾向がある")
    link("be subject to", "subject", LinkType.DERIVED, "同じ語。受け身の形で「〜を受けやすい」")
    link("in spite of", "nonetheless", LinkType.RELATED, "譲歩をつくる型。前置詞句と副詞")
    link("far from", "anything but", LinkType.SYNONYM, "「決して〜ない」。どちらも強い否定")
    link("by no means", "far from", LinkType.SYNONYM, "決して〜ない")
    link("turn down", "reject", LinkType.SYNONYM, "断る・却下する")
    link("carry out", "conduct", LinkType.SYNONYM, "実行する。carry out an experiment / conduct an experiment")
    link("point out", "indicate", LinkType.SYNONYM, "指摘する")
    link("break out", "occur", LinkType.RELATED, "戦争・火事など、突発的に起こる場合は break out")
    link("stick to", "persist", LinkType.SYNONYM, "やり通す・固執する")
    link("cope with", "resist", LinkType.CONTRAST, "うまく対処する ↔ 抗う")
}

private val IDIOM_TABLE = """
# 熟語 | 意味 | 芯 | なぜその意味か | 例文 | 例文訳 | 1語で言い換えると
give up|あきらめる、やめる|up（上へ・すっかり）|差し出して（give）手放しきる（up）|He refused to give up after the first failure.|彼は最初の失敗であきらめようとしなかった。|abandon, quit
take up|始める、（時間・場所を）取る|up（上へ・すっかり）|手に取って（take）自分のものにする|She took up running last spring.|彼女は昨春からランニングを始めた。|begin, occupy
bring up|育てる、話題に出す|up（上へ・すっかり）|下から上へ引き上げる。人なら育てる、話なら持ち出す|He brought up the question of cost.|彼は費用の問題を持ち出した。|raise, rear
make up|作り上げる、構成する、埋め合わせる|up（上へ・すっかり）|部品から上へ組み上げる|Women make up half of the workforce.|女性が労働力の半分を占めている。|constitute, invent
put up with|我慢する|up（上へ・すっかり）|上へ積んだまま（put up）いっしょに居続ける（with）|I cannot put up with the noise any longer.|もうその騒音には我慢できない。|tolerate, endure
come up with|思いつく|up（上へ・すっかり）|考えが水面まで上がってきて手元に来る|She came up with a simple solution.|彼女は単純な解決策を思いついた。|devise, conceive
end up|結局〜になる|up（追いつく・まとめる）|終わり（end）まで行き着く|They ended up taking the train.|彼らは結局電車で行くことになった。|finish, result
keep up with|遅れずについていく|up（追いつく・まとめる）|同じ高さを保ったまま一緒に進む|It is hard to keep up with the reading.|読む量についていくのは大変だ。|follow, match
catch up with|追いつく|up（追いつく・まとめる）|追いかけて同じ高さに並ぶ|He soon caught up with the others.|彼はすぐに他の者に追いついた。|overtake, reach
sum up|要約する|up（追いつく・まとめる）|全部を足し（sum）きる|Let me sum up the argument in one sentence.|議論を1文にまとめさせてください。|summarize
put off|延期する|off（分離）|予定から切り離して先へ送る|They put off the meeting until Friday.|彼らは会議を金曜まで延期した。|defer, postpone
call off|中止する|off（分離）|呼んで（call）その場から離脱させる|The match was called off because of rain.|試合は雨で中止になった。|cancel
take off|離陸する、脱ぐ、急に売れ出す|off（離れて動く）|接している面から離れる|The plane took off on time.|飛行機は定刻に離陸した。|depart, remove
set off|出発する、引き起こす|off（離れて動く）|その場を離れて動き出す|We set off before dawn.|私たちは夜明け前に出発した。|depart, trigger
show off|見せびらかす|off（離れて動く）|周りから切り離して目立たせる|He likes to show off his knowledge.|彼は知識をひけらかしたがる。|flaunt
be well off|裕福である|off（離れて動く）|暮らし向き（off）が良い|Her family was fairly well off.|彼女の家はかなり裕福だった。|wealthy
cut off|断つ、遮断する|off（分離）|切って（cut）離す|The village was cut off by the snow.|村は雪で孤立した。|isolate, sever
carry out|実行する|out（外へ・尽きる）|計画を外へ運び出して形にする|The team carried out three experiments.|チームは3つの実験を行った。|conduct, perform
figure out|理解する、解き明かす|out（外へ・尽きる）|形（figure）を外へ引き出す|I cannot figure out why it failed.|なぜ失敗したのか分からない。|understand, solve
turn out|結局〜と分かる、〜になる|out（尽きる・突発）|裏返して（turn）中身が外に出る|The rumour turned out to be false.|その噂は結局うそだと分かった。|prove
run out of|使い果たす|out（尽きる・突発）|走り続けて中身が外に尽きる|We ran out of time before the last question.|最後の問題の前に時間が尽きた。|exhaust, deplete
point out|指摘する|out（外へ・尽きる）|指し示して外へ出す|She pointed out an error in the proof.|彼女は証明の誤りを指摘した。|indicate, note
leave out|省く|out（尽きる・突発）|外に置き去りにする|You may leave out the details.|細部は省いてよい。|omit, exclude
break out|突発する|out（尽きる・突発）|殻を破って外へ出る|War broke out the following year.|翌年に戦争が起こった。|erupt
work out|うまくいく、算出する、鍛える|out（外へ・尽きる）|働かせて答えを外へ出す|Things worked out better than expected.|事態は予想よりうまくいった。|calculate, succeed
hand out|配る|out（外へ・尽きる）|手（hand）から外へ渡す|The teacher handed out the papers.|先生は用紙を配った。|distribute
carry on|続ける|on（続ける・みなす）|上に載せたまま（on）運び続ける|They carried on despite the weather.|天候にもかかわらず彼らは続けた。|continue
count on|当てにする|on（接触・継続）|寄りかかって数に入れる|You can count on her to be honest.|彼女が正直であることは当てにしてよい。|rely on
take on|引き受ける、帯びる|on（接触・継続）|身に載せる|He took on more work than he could manage.|彼は手に負えないほどの仕事を引き受けた。|assume, undertake
get on with|うまくやっていく、続ける|on（続ける・みなす）|接したまま前へ進む|She gets on with her colleagues and with the work.|彼女は同僚ともうまくやり、仕事も進めている。|proceed
look on A as B|A を B とみなす|on（続ける・みなす）|A に視線を載せて B として見る|Many look on the change as inevitable.|多くの人がその変化を避けられないものとみなしている。|regard, consider
insist on|強く求める、主張する|on（接触・継続）|その一点の上に立ち続ける|He insisted on paying for everyone.|彼は全員分を払うと言って譲らなかった。|demand
hold on|持ちこたえる、待つ|on（接触・継続）|つかんだまま離さない|Hold on until help arrives.|助けが来るまで持ちこたえなさい。|endure, wait
take over|引き継ぐ、乗っ取る|over（越えて・移す）|越えてこちらへ受け取る|A younger team took over the project.|若いチームがその計画を引き継いだ。|assume
get over|克服する、立ち直る|over（越えて・移す）|山を越えて向こう側へ出る|It took him a year to get over the loss.|その喪失から立ち直るのに1年かかった。|overcome, recover
look over|ざっと目を通す|over（越えて・移す）|上を一通りなでるように見る|Please look over the draft before Friday.|金曜までに草稿に目を通してください。|review, scan
think over|よく考える|over（越えて・移す）|頭の中で何度も上を行き来させる|Think it over before you decide.|決める前によく考えなさい。|consider
hand over|引き渡す|over（越えて・移す）|手を越えて相手に渡す|He handed over the keys without a word.|彼は黙って鍵を引き渡した。|surrender, transfer
give in|屈する、提出する|in（屈する・帰着する）|押されて内側へ折れる|The government finally gave in to pressure.|政府はついに圧力に屈した。|yield, submit
take in|理解する、取り込む、だます|in（中へ）|中へ取り込む|There was too much to take in at once.|一度に理解するには多すぎた。|absorb, comprehend
result in|〜という結果になる|in（屈する・帰着する）|結果がその中に落ち着く|The error resulted in a complete failure.|その誤りは全面的な失敗という結果を招いた。|cause
engage in|従事する|in（中へ）|自分をその中に結びつける|She engages in research on memory.|彼女は記憶の研究に従事している。|participate
specialize in|専門にする|in（中へ）|その中に特化して入り込む|This laboratory specializes in organic synthesis.|この研究室は有機合成を専門にしている。|major
believe in|（存在・価値を）信じる|in（屈する・帰着する）|その中に信頼を置く|He does not believe in luck.|彼は運というものを信じていない。|trust
fill in|記入する、埋める|in（中へ）|空所の中を満たす|Fill in the blanks with one word each.|各空所に1語ずつ記入しなさい。|complete
go through|経験する、通読する|through（貫く）|端から端まで通り抜ける|The country went through a long recession.|その国は長い不況を経験した。|experience, endure
get through|やり遂げる、切り抜ける|through（貫く）|通り抜けて向こうへ出る|We got through the work in two days.|私たちは2日で仕事をやり遂げた。|complete, survive
see through|見抜く|through（貫く）|表面を貫いて中を見る|She saw through his excuse at once.|彼女はすぐに彼の言い訳を見抜いた。|detect
look through|ざっと調べる、目を通す|through（貫く）|端から端まで視線を通す|He looked through the file for the date.|彼は日付を探してファイルに目を通した。|examine
break down|故障する、分解する、取り乱す|down（下へ・記録）|下へ崩れる、または細かく崩す|The machine broke down during the test.|その機械は試験中に故障した。|collapse, analyse
turn down|断る、音量を下げる|down（下へ・記録）|下へ向ける＝退ける|They turned down the proposal.|彼らはその提案を断った。|reject, refuse
put down|書き留める、鎮圧する|down（下へ・記録）|下（紙の上）に置く|Put down your name and the date.|名前と日付を書き留めなさい。|record, suppress
hand down|言い伝える、代々伝える|down（下へ・記録）|上の世代から下へ手渡す|The craft was handed down for centuries.|その技術は何世紀も伝えられてきた。|transmit
cut down on|減らす|down（下へ・記録）|量を切って下げる|You should cut down on salt.|塩分を減らしたほうがよい。|reduce
settle down|落ち着く|down（下へ・記録）|沈んで下に定まる|The class settled down once the bell rang.|ベルが鳴るとクラスは落ち着いた。|calm
lead to|〜につながる|to（到達・方向）|その先へ導いて到達する|Poor sleep leads to poor concentration.|睡眠不足は集中力の低下につながる。|cause
refer to|言及する、参照する|to（向ける・言及する）|その方向へ差し向ける|The author refers to three earlier studies.|著者は3つの先行研究に言及している。|mention, consult
stick to|やり通す、固執する|to（到達・方向）|くっついたまま離れない|Stick to the plan even if it is slow.|遅くても計画をやり通しなさい。|persist
look forward to|楽しみに待つ|to（向ける・言及する）|前方のその一点に視線を向け続ける|I look forward to hearing from you.|ご連絡を楽しみにしています。|anticipate
amount to|総計〜になる、結局〜に等しい|to（到達・方向）|積み上がってその量に届く|The losses amount to half the budget.|損失は予算の半分に達する。|total, equal
object to|反対する|to（向ける・言及する）|その方向へ異を投げる|Several members objected to the change.|数名の委員がその変更に反対した。|oppose
adapt to|順応する|to（到達・方向）|その環境に合わせて形を変える|Plants adapt to the local climate.|植物はその土地の気候に順応する。|adjust
account for|説明する、占める|for（求めて・代わりに）|その分を差し出して説明する|Three factors account for most of the variation.|3つの要因がばらつきの大半を占める。|explain, constitute
call for|必要とする、要求する|for（求めて・代わりに）|それを求めて呼ぶ|The situation calls for caution.|その状況は慎重さを必要とする。|require, demand
stand for|表す、支持する|for（求めて・代わりに）|それの代わりに立つ|The symbol stands for resistance.|その記号は抵抗を表す。|represent
make up for|埋め合わせる|for（求めて・代わりに）|不足の代わりに作って補う|Extra practice makes up for a late start.|余分な練習が出遅れを埋め合わせる。|compensate
apply for|応募する、申し込む|for（求めて・代わりに）|それを求めて自分を差し出す|She applied for a scholarship.|彼女は奨学金に応募した。|request
long for|切望する|for（求めて・代わりに）|それを求めて長く手を伸ばす|They longed for news from home.|彼らは故郷からの知らせを待ちわびた。|desire
do away with|廃止する、取り除く|with（伴う）|一緒にあったものを遠ざけて終わらせる|The school did away with the old rule.|学校はその古い規則を廃止した。|abolish
cope with|うまく対処する|with（伴う）|それと組み合って処理する|He could not cope with the workload.|彼はその仕事量に対処できなかった。|manage, handle
comply with|従う|with（伴う）|相手に合わせて動く|All members must comply with the rules.|全員が規則に従わなければならない。|obey, conform
part with|手放す|with（伴う）|一緒にあったものと別れる|She could not part with the old letters.|彼女は古い手紙を手放せなかった。|surrender
in terms of|〜の観点から|前置詞句（観点・関係）|term（言い方）の枠に入れて言えば|In terms of cost, the plan is unrealistic.|費用の点から見れば、その計画は非現実的だ。|regarding
in the face of|〜に直面して|前置詞句（逆接・対比）|それと顔を突き合わせた位置で|She stayed calm in the face of criticism.|彼女は批判に直面しても冷静だった。|despite, facing
at the expense of|〜を犠牲にして|前置詞句（逆接・対比）|その費用（expense）を支払って|Speed was gained at the expense of accuracy.|正確さを犠牲にして速さが得られた。|sacrificing
by virtue of|〜のおかげで|前置詞句（名詞をはさむ型）|その長所（virtue）を通して|He was chosen by virtue of his experience.|彼は経験ゆえに選ばれた。|because of
in spite of|〜にもかかわらず|前置詞句（逆接・対比）|それに逆らう位置に立って|In spite of the rain, the match went on.|雨にもかかわらず試合は続いた。|despite
on behalf of|〜を代表して|前置詞句（観点・関係）|その側（behalf）に立って|I speak on behalf of the whole team.|チーム全体を代表して申し上げます。|representing
with regard to|〜に関して|前置詞句（観点・関係）|その方向へ視線（regard）を向けて|With regard to the cost, nothing is decided.|費用に関しては何も決まっていない。|concerning
as a result of|〜の結果として|前置詞句（名詞をはさむ型）|それを原因とする結果の位置で|As a result of the delay, we missed the train.|遅れの結果、電車に乗り遅れた。|because of
for the sake of|〜のために|前置詞句（名詞をはさむ型）|その利益（sake）を目的として|He gave up the post for the sake of his health.|彼は健康のためその職を辞した。|for
in accordance with|〜に従って|前置詞句（観点・関係）|それと調和（accordance）した状態で|The test was done in accordance with the manual.|試験は手引きに従って行われた。|following
at the mercy of|〜のなすがままに|前置詞句（状態・時）|相手の情け（mercy）の位置に置かれて|The boat was at the mercy of the waves.|船は波のなすがままだった。|helpless before
in the long run|長い目で見れば|前置詞句（状態・時）|長い道のり（run）の中で|In the long run, the cheaper method costs more.|長い目で見れば、安い方法のほうが高くつく。|eventually
in the wake of|〜の後を受けて|前置詞句（名詞をはさむ型）|船の航跡（wake）の後ろに|New rules came in the wake of the accident.|事故を受けて新しい規則ができた。|following
on the verge of|今にも〜しようとして|前置詞句（状態・時）|縁（verge）の上に立って|The species is on the verge of extinction.|その種は絶滅寸前だ。|about to
by no means|決して〜ない|否定・限定の慣用|どの手段（means）によっても〜ない|The result is by no means certain.|その結果は決して確実ではない。|not at all
anything but|決して〜ない|否定・限定の慣用|それ以外（but）なら何でも、の裏返し|His explanation was anything but clear.|彼の説明は決して明快ではなかった。|far from
far from|〜どころではない|否定・限定の慣用|そこから遠い位置にある|Far from being simple, the proof runs ten pages.|単純どころか、その証明は10ページに及ぶ。|not at all
let alone|まして〜ない|限定・比較の慣用|それは放っておくとして（そこまで至らない）|He cannot read French, let alone write it.|彼はフランス語を読めない、まして書けない。|much less
to say nothing of|〜は言うまでもなく|限定・比較の慣用|それについては何も言わないとしても|The cost is huge, to say nothing of the time.|時間は言うまでもなく、費用が莫大だ。|not to mention
all but|ほとんど〜|限定・比較の慣用|あと一歩を除いて全部|The old custom has all but disappeared.|その古い習慣はほとんど消えてしまった。|almost
no more than|たった〜にすぎない|限定・比較の慣用|それ以上ではないと押さえる|The whole journey took no more than an hour.|その道のりは1時間しかかからなかった。|only
not so much A as B|A というよりむしろ B|限定・比較の慣用|A の量ほどではなく B だ、と比べる|It is not so much a theory as a guess.|それは理論というよりむしろ推測だ。|rather B than A
be aware of|気づいている|be + 形容詞（状態・資格）|意識（aware）がそれに向いている状態|Students should be aware of the deadline.|学生は締切を意識しておくべきだ。|know
be capable of|〜する能力がある|be + 形容詞（状態・資格）|それを収める容量（capable）がある|The device is capable of far higher speeds.|その装置ははるかに高い速度を出せる。|able
be indifferent to|無関心である|be + 形容詞（状態・資格）|どちらでも差（difference）がない|He was indifferent to the outcome.|彼は結果に無関心だった。|uninterested
be inclined to|〜する傾向がある|be + 形容詞 + 前置詞|その方向へ傾いて（incline）いる|People are inclined to believe what they hope.|人は望むことを信じたがるものだ。|tend
be bound to|必ず〜する|be + 形容詞 + 前置詞|そうする方向に縛られて（bound）いる|A change of this size is bound to be resisted.|この規模の変化は必ず抵抗にあう。|certain
be liable to|〜しがちだ、責任がある|be + 形容詞 + 前置詞|その責めを負う位置にある|Metal parts are liable to rust in damp air.|金属部品は湿った空気でさびやすい。|likely
be entitled to|〜する資格がある|be + 形容詞（状態・資格）|その権利の名（title）を与えられている|Every citizen is entitled to an explanation.|市民は誰でも説明を受ける資格がある。|eligible
be prone to|〜しやすい|be + 形容詞 + 前置詞|その方向へ前のめり（prone）になっている|Young plants are prone to frost damage.|若い苗は霜の害を受けやすい。|susceptible
be devoid of|〜を全く欠いている|be + 形容詞（状態・資格）|中が空（void）になっている|The report is devoid of any evidence.|その報告には証拠がまるでない。|lacking
be subject to|〜を受けやすい、〜に従う|be + 形容詞 + 前置詞|その下に置かれている|Prices are subject to change without notice.|価格は予告なく変更されることがある。|liable
take A into account|A を考慮に入れる|動詞 + 名詞の慣用|勘定（account）の中に入れる|We must take the delay into account.|遅れを考慮に入れなければならない。|consider
take advantage of|利用する、つけ込む|動詞＋名詞（生かす・保つ）|有利さ（advantage）を手に取る|He took advantage of the free hour to revise.|彼は空き時間を利用して復習した。|exploit, use
make sense of|理解する|動詞＋名詞（理解する・受け入れる）|意味（sense）を作り出す|I cannot make sense of this diagram.|この図の意味が分からない。|understand
give rise to|引き起こす|動詞＋名詞（引き起こす・終える）|立ち上がり（rise）を与える|The policy gave rise to unexpected problems.|その政策は予期せぬ問題を引き起こした。|cause
bring about|もたらす|動詞＋名詞（引き起こす・終える）|周囲（about）の状況を運んでくる|The invention brought about a social change.|その発明は社会の変化をもたらした。|cause
put an end to|終止符を打つ|動詞＋名詞（引き起こす・終える）|終わり（end）をそこに置く|The treaty put an end to the conflict.|その条約は紛争に終止符を打った。|stop
take A for granted|A を当然と思う|動詞 + 名詞の慣用|与えられたもの（granted）として受け取る|We take clean water for granted.|私たちはきれいな水を当然のものと思っている。|assume
come to terms with|折り合いをつける、受け入れる|動詞＋名詞（理解する・受け入れる）|条件（terms）のところまで歩み寄る|It took years to come to terms with the loss.|その喪失を受け入れるのに何年もかかった。|accept
get rid of|取り除く|動詞＋名詞（引き起こす・終える）|それから解放された（rid）状態になる|It is hard to get rid of a bad habit.|悪い習慣を取り除くのは難しい。|eliminate, remove
keep track of|把握し続ける|動詞＋名詞（生かす・保つ）|足跡（track）を追い続ける|Keep track of how long each section takes.|各節にかかる時間を記録しておきなさい。|monitor
lose sight of|見失う|動詞＋名詞（生かす・保つ）|視界（sight）から外す|Do not lose sight of the main question.|主題を見失ってはいけない。|forget
make the most of|最大限に生かす|動詞＋名詞（生かす・保つ）|得られる最大（the most）を作り出す|Make the most of the time you have.|あるだけの時間を最大限に生かしなさい。|maximize
pay attention to|注意を払う|動詞 + 名詞の慣用|注意（attention）を支払う|Pay attention to the units in each step.|各段階で単位に注意しなさい。|attend
bear in mind|心に留めておく|動詞 + 名詞の慣用|心（mind）の中に抱えたままにする|Bear in mind that the data are limited.|データが限られていることを心に留めておきなさい。|remember
""".trimIndent()

internal const val IDIOM_DECK = "英熟語（前置詞の芯でつなぐ）"
