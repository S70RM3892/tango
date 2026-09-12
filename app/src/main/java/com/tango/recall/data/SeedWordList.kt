package com.tango.recall.data

import com.tango.recall.data.Seed.Word

/**
 * The bulk of the word list.
 *
 * Written as a table rather than as Kotlin objects: at this size the punctuation of
 * the language gets in the way of reading the words, and a row per word is what makes
 * it possible to check the whole list at a glance. Columns are
 * `語 | 意味 | 品詞 | 語根 | 例文 | 例文訳 | コロケーション`, separated by `|`.
 *
 * Words are grouped by root and every group is linked into a clique, so answering one
 * brings up the rest. Groups are kept to four or five: a relation question that lists
 * nine partners is not a question anyone can answer.
 *
 * These are written for this app. A published word list is someone's compilation and
 * copying one into an app would be theft; if you already own one, the deck's TSV
 * import is the way to bring it in.
 */
internal fun seedWordList(s: Seeder) {
    val deckId = s.deck(
        name = Seed.ENGLISH_DECK,
        type = NoteType.ENGLISH,
        templates = setOf("en_ja", "ja_en", "cloze"),
        newPerDay = 15,
        relationQuiz = true,
    )
    Seed.addWords(s, deckId, parseWordTable(WORD_TABLE))

    val byWord = s.repo.listNotes(deckId, "", Int.MAX_VALUE).associateBy { it.title() }
    fun link(a: String, b: String, t: LinkType, memo: String) {
        val x = byWord[a]?.id ?: return
        val y = byWord[b]?.id ?: return
        s.link(x, y, t, memo)
    }
    for ((a, b, rest) in CROSS_LINKS) {
        val (type, memo) = rest
        link(a, b, type, memo)
    }
}

/** Turn the table into root groups, keeping the order the rows are written in. */
internal fun parseWordTable(table: String): List<Pair<String, List<Word>>> {
    val groups = LinkedHashMap<String, MutableList<Word>>()
    for (line in table.lines()) {
        val row = line.trim()
        if (row.isEmpty() || row.startsWith("#")) continue
        val cells = row.split("|").map { it.trim() }
        require(cells.size == 7) { "列が 7 つではありません: $row" }
        val root = cells[3]
        groups.getOrPut(root) { mutableListOf() } += Word(
            word = cells[0],
            meaning = cells[1],
            pos = cells[2],
            root = root,
            example = cells[4],
            exampleJa = cells[5],
            collocation = cells[6],
        )
    }
    return groups.map { (root, words) -> root to words.toList() }
}

/** Pairs worth noticing across the root groups: look-alikes, opposites, near-synonyms. */
private val CROSS_LINKS: List<Triple<String, String, Pair<LinkType, String>>> = listOf(
    Triple("affect", "effect", LinkType.CONFUSABLE to "affect は動詞「影響を与える」、effect は名詞「効果」。動詞の effect は「もたらす」で別物"),
    Triple("sensible", "sensitive", LinkType.CONFUSABLE to "sensible は「分別のある」、sensitive は「敏感な」。人を褒める意味が違う"),
    Triple("credible", "credulous", LinkType.CONFUSABLE to "credible は「信用できる（もの）」、credulous は「すぐ信じる（人）」"),
    Triple("explicit", "implicit", LinkType.ANTONYM to "はっきり述べられた ↔ 言外に含まれた"),
    Triple("finite", "infinite", LinkType.ANTONYM to "有限 ↔ 無限"),
    Triple("prevail", "prevalent", LinkType.DERIVED to "動詞と形容詞。「広く行きわたる」→「広く見られる」"),
    Triple("evade", "invade", LinkType.CONFUSABLE to "同じ vad（行く）。外へ逃げる ↔ 中へ攻め込む"),
    Triple("compel", "impel", LinkType.CONFUSABLE to "どちらも「押して〜させる」。compel は強制、impel は内から駆り立てる"),
    Triple("moderate", "modest", LinkType.CONFUSABLE to "moderate は程度が中くらい、modest は控えめ・慎ましい"),
    Triple("intervene", "interfere", LinkType.CONFUSABLE to "intervene は中立的に「介入する」、interfere は邪魔をする含み"),
    Triple("obvious", "trivial", LinkType.CONTRAST to "明白だ ↔ 取るに足らない。数学の証明ではどちらもよく出る"),
    Triple("hypothesis", "empirical", LinkType.CONTRAST to "仮説（頭で立てる）↔ 経験に基づく（データで確かめる）"),
    Triple("arbitrary", "rigorous", LinkType.ANTONYM to "恣意的 ↔ 厳密"),
    Triple("attribute", "contribute", LinkType.CONFUSABLE to "attribute A to B は「原因を B に帰す」、contribute to は「貢献する」"),
    Triple("terminate", "determine", LinkType.CONFUSABLE to "同じ term（境）。終わらせる / 境を定める＝決定する"),
    Triple("resolve", "dissolve", LinkType.CONFUSABLE to "同じ solv（解く）。問題を解決する / 固体が溶ける"),
    Triple("legitimate", "legislation", LinkType.RELATED to "同じ leg（法）。合法の / 立法"),
    Triple("oblige", "obligation", LinkType.DERIVED to "動詞と名詞"),
    Triple("vital", "survive", LinkType.RELATED to "同じ viv / vit（生きる）"),
    Triple("notion", "notorious", LinkType.CONFUSABLE to "同じ not（知る）。考え・概念 / 悪い意味で知れ渡った"),
    Triple("deliberate", "arbitrary", LinkType.CONTRAST to "意図的に選ぶ ↔ 理由なく決める"),
    Triple("plausible", "credible", LinkType.SYNONYM to "もっともらしい / 信用できる。どちらも根拠の弱さを問題にする文脈で出る"),
    Triple("sustainable", "extinct", LinkType.CONTRAST to "続けられる ↔ 絶えてしまった"),
    Triple("prejudice", "discrimination", LinkType.RELATED to "偏見（心のなか）→ 差別（行為）"),
    Triple("intuition", "empirical", LinkType.CONTRAST to "直観による ↔ 観察・実験による"),
    Triple("substantial", "negligible", LinkType.ANTONYM to "かなりの ↔ 無視できるほどの。量を評価する語の両端"),
    Triple("abundant", "scarce", LinkType.ANTONYM to "豊富 ↔ 乏しい"),
    Triple("coherent", "ambiguous", LinkType.CONTRAST to "筋が通っている ↔ どちらにも読める"),
    Triple("inevitable", "inherent", LinkType.CONFUSABLE to "避けられない（結果）/ もともと備わっている（性質）"),
    Triple("assume", "presume", LinkType.CONFUSABLE to "assume は議論の出発点として置く、presume は根拠から推定する"),
    Triple("immigrant", "emigrate", LinkType.CONTRAST to "im-（中へ）入ってくる人 ↔ e-（外へ）出ていく"),
    Triple("preserve", "conserve", LinkType.CONFUSABLE to "そのまま保存する / 減らさないように保つ。物理の「保存則」は conserve"),
    Triple("respond", "correspond", LinkType.CONFUSABLE to "反応する / 一致する。cor-（共に）がつくと「対応」になる"),
    Triple("reluctant", "enthusiastic", LinkType.ANTONYM to "気が進まない ↔ 乗り気だ"),
    Triple("skeptical", "credulous", LinkType.ANTONYM to "疑ってかかる ↔ すぐ信じる"),
    Triple("consequence", "trigger", LinkType.CONTRAST to "結果の側 ↔ 原因の側。因果を書くときに対で使う"),
    Triple("stem", "attribute", LinkType.RELATED to "stem from は原因から書き、attribute A to B は結果から書く"),
    Triple("precise", "concise", LinkType.CONFUSABLE to "正確 / 簡潔。つづりが近く意味が違う"),
    Triple("fundamental", "profound", LinkType.SYNONYM to "どちらも「根の深い」。profound は影響の深さに使う"),
    Triple("conclude", "exclude", LinkType.CONTRAST to "同じ clud（閉じる）。締めくくる / 締め出す"),
)

private val WORD_TABLE = """
# 語 | 意味 | 品詞 | 語根 | 例文 | 例文訳 | コロケーション
dictate|指示する、書き取らせる|動|dict（言う）|The teacher dictated a short passage to the class.|先生はクラスに短い文章を書き取らせた。|dictate terms
predict|予測する|動|dict（言う）|No one could predict how the experiment would end.|実験がどう終わるかは誰にも予測できなかった。|predict the outcome
contradict|矛盾する、反論する|動|dict（言う）|His account contradicts what the witness said.|彼の説明は目撃者の話と矛盾する。|contradict oneself
verdict|評決、判断|名|dict（言う）|The jury returned a verdict of not guilty.|陪審は無罪の評決を下した。|reach a verdict
facilitate|容易にする、促進する|動|fac / fect（作る）|A good diagram facilitates understanding.|よい図は理解を助ける。|facilitate learning
deficient|欠けている、不十分な|形|fac / fect（作る）|The diet was deficient in vitamins.|その食事はビタミンが不足していた。|be deficient in
affect|影響を与える|動|fac / fect（作る）|The drought affected the whole region.|干ばつは地域全体に影響した。|affect the outcome
effect|効果、結果|名|fac / fect（作る）|The new rule had little effect on behaviour.|新しい規則は行動にほとんど効果がなかった。|have an effect on
fluent|流暢な|形|flu（流れる）|She is fluent in three languages.|彼女は3か国語を流暢に話す。|fluent in
influence|影響、影響を与える|名・動|flu（流れる）|His writing influenced a whole generation.|彼の著作は一世代全体に影響を与えた。|a strong influence on
fluctuate|変動する|動|flu（流れる）|Prices fluctuate with the season.|価格は季節によって変動する。|fluctuate wildly
superfluous|余分な、不要な|形|flu（流れる）|Cut any superfluous detail from the report.|報告書から余分な細部は削りなさい。|superfluous information
generate|生み出す|動|gen（生む）|The scheme generated more problems than it solved.|その計画は解決した以上の問題を生んだ。|generate income / interest
genuine|本物の、真摯な|形|gen（生む）|Her surprise seemed genuine.|彼女の驚きは本物のようだった。|a genuine interest in
indigenous|その土地固有の|形|gen（生む）|The plant is indigenous to this island.|その植物はこの島に固有のものだ。|indigenous to
degenerate|悪化する、退化する|動|gen（生む）|The debate soon degenerated into an argument.|議論はすぐに口論へと堕した。|degenerate into
neglect|怠る、放置する|動|lect / leg（選ぶ・集める）|He neglected his health for years.|彼は何年も健康をおろそかにした。|neglect one's duty
eligible|資格のある|形|lect / leg（選ぶ・集める）|Only residents are eligible to apply.|住民だけが応募する資格がある。|be eligible for
intellect|知性|名|lect / leg（選ぶ・集める）|The problem is one of judgement rather than intellect.|それは知性というより判断の問題だ。|a sharp intellect
collective|集団の、共同の|形|lect / leg（選ぶ・集める）|The decision was a collective one.|その決定は全員によるものだった。|collective responsibility
eloquent|雄弁な|形|loqu / log（話す）|Her silence was more eloquent than any speech.|彼女の沈黙はどんな演説よりも雄弁だった。|an eloquent speaker
dialogue|対話|名|loqu / log（話す）|The two sides finally entered into dialogue.|両者はついに対話に入った。|open a dialogue
apology|謝罪、弁明|名|loqu / log（話す）|He offered an apology without being asked.|彼は求められる前に謝罪した。|owe someone an apology
logic|論理|名|loqu / log（話す）|I cannot follow the logic of that argument.|その議論の論理についていけない。|the logic behind
manipulate|巧みに操る|動|man（手）|The figures had been manipulated to look better.|数字はよく見えるように操作されていた。|manipulate data
manual|手作業の、手引き|形・名|man（手）|The work is still largely manual.|その仕事はいまだ大半が手作業だ。|a manual task
manifest|明らかにする、明白な|動・形|man（手）|His anxiety manifested itself as anger.|彼の不安は怒りとなって現れた。|manifest itself in
mandate|命令する、権限|動・名|man（手）|The law mandates regular inspections.|その法律は定期的な検査を義務づけている。|a clear mandate
moderate|穏やかな、和らげる|形・動|mod（型・尺度）|The climate here is moderate all year.|ここの気候は一年中穏やかだ。|a moderate increase
modify|修正する|動|mod（型・尺度）|We modified the design after the first test.|最初の試験のあと設計を修正した。|modify a plan
accommodate|収容する、応じる|動|mod（型・尺度）|The hall can accommodate five hundred people.|そのホールは500人を収容できる。|accommodate a request
modest|控えめな、ささやかな|形|mod（型・尺度）|He was modest about his achievements.|彼は自分の業績について控えめだった。|a modest improvement
innovate|革新する|動|nov（新しい）|Companies must innovate to survive.|企業は生き残るために革新しなければならない。|innovate rapidly
novel|新奇な、小説|形・名|nov（新しい）|The team took a novel approach to the problem.|チームはその問題に新奇な手法をとった。|a novel idea
renovate|改修する|動|nov（新しい）|They renovated the building without changing its front.|正面を変えずに建物を改修した。|renovate a house
novice|初心者|名|nov（新しい）|Even a novice can see the mistake.|初心者でもその誤りは分かる。|a complete novice
impede|妨げる|動|ped（足）|Heavy snow impeded the rescue.|大雪が救助を妨げた。|impede progress
pedestrian|歩行者、平凡な|名・形|ped（足）|The street is closed to all but pedestrians.|その通りは歩行者以外通行止めだ。|a pedestrian crossing
expedite|迅速に進める|動|ped（足）|We hired staff to expedite the process.|手続きを早めるため人を雇った。|expedite a request
expedition|遠征、探検|名|ped（足）|The expedition set out before dawn.|遠征隊は夜明け前に出発した。|lead an expedition
compel|強いる|動|pel / puls（押す）|Illness compelled him to give up the race.|病気のため彼はレースを断念せざるをえなかった。|compel someone to do
repel|はねつける、撃退する|動|pel / puls（押す）|Like charges repel each other.|同じ符号の電荷は互いに反発する。|repel an attack
impulse|衝動|名|pel / puls（押す）|He bought it on impulse.|彼は衝動でそれを買った。|on impulse
impel|駆り立てる|動|pel / puls（押す）|Curiosity impelled her to open the letter.|好奇心が彼女に手紙を開けさせた。|impel someone to do
implicit|暗黙の|形|plic / ply（折る）|There was an implicit threat in his tone.|彼の口調には暗黙の脅しがあった。|an implicit assumption
explicit|明示的な|形|plic / ply（折る）|The instructions were explicit about the order.|指示は順序について明示的だった。|be explicit about
complicated|複雑な|形|plic / ply（折る）|The rules are more complicated than they look.|規則は見た目より複雑だ。|a complicated process
comply|従う|動|plic / ply（折る）|All schools must comply with the new rule.|すべての学校が新しい規則に従わねばならない。|comply with
disrupt|混乱させる、中断させる|動|rupt（破る）|The storm disrupted the whole timetable.|嵐が予定全体を乱した。|disrupt a service
corrupt|腐敗した、堕落させる|形・動|rupt（破る）|Power corrupts even the honest.|権力は誠実な人間さえ堕落させる。|a corrupt official
abrupt|突然の、ぶっきらぼうな|形|rupt（破る）|The meeting came to an abrupt end.|会議は突然終わった。|an abrupt change
bankrupt|破産した|形|rupt（破る）|The firm went bankrupt within a year.|その会社は1年で破産した。|go bankrupt
consent|同意する、同意|動・名|sens / sent（感じる）|She consented to the operation.|彼女は手術に同意した。|give consent
resent|憤慨する|動|sens / sent（感じる）|He resented being treated as a beginner.|彼は初心者扱いされることに腹を立てた。|resent being treated
sentiment|感情、意見|名|sens / sent（感じる）|Public sentiment turned against the plan.|世論はその計画に反対へ傾いた。|public sentiment
sensible|分別のある|形|sens / sent（感じる）|It was a sensible decision under the circumstances.|状況を考えれば分別ある決定だった。|a sensible choice
sensitive|敏感な、微妙な|形|sens / sent（感じる）|The instrument is sensitive to small changes.|その装置はわずかな変化にも敏感だ。|sensitive to
designate|指定する、任命する|動|sign（しるし）|The area was designated a national park.|その地域は国立公園に指定された。|designate someone as
significant|重要な、著しい|形|sign（しるし）|There was no significant difference between the groups.|グループ間に有意な差はなかった。|a significant increase
assign|割り当てる|動|sign（しるし）|Each student was assigned a different topic.|学生には別々の主題が割り当てられた。|assign a task to
resign|辞職する、あきらめる|動|sign（しるし）|He resigned rather than accept the terms.|条件を受け入れるくらいならと彼は辞職した。|resign oneself to
assimilate|同化する、吸収する|動|simil（似た）|Children assimilate new words quickly.|子どもは新しい語をすばやく吸収する。|assimilate information
resemble|似ている|動|simil（似た）|The copy resembles the original closely.|その複製は原本によく似ている。|closely resemble
simultaneous|同時の|形|simil（似た）|The two events were simultaneous.|2つの出来事は同時だった。|simultaneous translation
assemble|集める、組み立てる|動|simil（似た）|A crowd assembled outside the hall.|群衆がホールの外に集まった。|assemble the parts
isolate|孤立させる|動|sol（一人）|The village was isolated by the flood.|その村は洪水で孤立した。|isolate a variable
sole|唯一の|形|sol（一人）|That was the sole reason for the delay.|それが遅れの唯一の理由だった。|the sole purpose
solitude|孤独、ひとりでいること|名|sol（一人）|He worked best in solitude.|彼はひとりでいるときに最もよく働いた。|in solitude
desolate|荒涼とした|形|sol（一人）|The plain looked desolate in winter.|その平原は冬には荒涼として見えた。|a desolate landscape
dissolve|溶かす、解散する|動|solv（解く）|Salt dissolves readily in water.|塩は水によく溶ける。|dissolve in water
resolve|解決する、決意する|動|solv（解く）|They resolved the dispute without a court.|彼らは裁判なしで紛争を解決した。|resolve a conflict
absolute|絶対的な|形|solv（解く）|There is no absolute rule about this.|これについて絶対的な規則はない。|absolute certainty
solution|解決策、溶液|名|solv（解く）|We are still looking for a practical solution.|まだ実用的な解決策を探している。|find a solution
desperate|絶望的な、必死の|形|sper（希望）|They made a desperate attempt to save the crop.|彼らは作物を救おうと必死の試みをした。|a desperate attempt
prosper|繁栄する|動|sper（希望）|The town prospered after the railway came.|鉄道が通ってから町は栄えた。|prosper under
despair|絶望、絶望する|名・動|sper（希望）|He never gave in to despair.|彼は決して絶望に屈しなかった。|in despair
tend|傾向がある、世話をする|動|tend / tens（伸ばす）|People tend to remember the first item best.|人は最初の項目を最もよく覚えている傾向がある。|tend to do
extend|延ばす、広げる|動|tend / tens（伸ばす）|They extended the deadline by a week.|締切を1週間延ばした。|extend a deadline
intense|激しい、強烈な|形|tend / tens（伸ばす）|The heat became intense by noon.|昼までに暑さは強烈になった。|intense pressure
contend|主張する、争う|動|tend / tens（伸ばす）|Some contend that the effect is exaggerated.|その効果は誇張だと主張する者もいる。|contend that
determine|決定する、突き止める|動|term（境・終わり）|We could not determine the cause.|原因を突き止められなかった。|determine whether
terminate|終わらせる|動|term（境・終わり）|The contract was terminated early.|契約は早期に打ち切られた。|terminate a contract
terminal|終点の、末期の|形・名|term（境・終わり）|The illness was already terminal.|その病気はすでに末期だった。|a bus terminal
exterminate|根絶する|動|term（境・終わり）|The disease has been almost exterminated.|その病気はほぼ根絶された。|exterminate pests
testify|証言する|動|test（証言）|Two witnesses testified in court.|2人の証人が法廷で証言した。|testify to
protest|抗議する|動|test（証言）|Students protested against the fee increase.|学生は値上げに抗議した。|protest against
contest|争う、異議を唱える|動・名|test（証言）|They contested the result of the election.|彼らは選挙結果に異議を唱えた。|contest a claim
testimony|証言、証拠|名|test（証言）|His testimony changed the case entirely.|彼の証言は事件を一変させた。|give testimony
contribute|貢献する、寄与する|動|trib（与える）|Several factors contributed to the failure.|いくつかの要因が失敗の一因となった。|contribute to
attribute|〜に帰する、特性|動・名|trib（与える）|She attributed her success to luck.|彼女は成功を運のおかげだとした。|attribute A to B
distribute|分配する|動|trib（与える）|The aid was distributed evenly.|支援は均等に分配された。|distribute among
tribute|賛辞、貢ぎ物|名|trib（与える）|The prize is a tribute to years of work.|その賞は長年の仕事への賛辞だ。|pay tribute to
invade|侵入する|動|vad（行く）|Weeds invaded the whole garden.|雑草が庭全体に侵入した。|invade privacy
evade|逃れる、回避する|動|vad（行く）|He evaded every question about the cost.|彼は費用についての質問をすべてかわした。|evade a question
pervade|行きわたる|動|vad（行く）|A sense of unease pervaded the room.|不安な空気が部屋中に広がっていた。|pervade the atmosphere
evasive|言い逃れの|形|vad（行く）|Her answer was deliberately evasive.|彼女の答えはわざと言い逃れだった。|an evasive answer
evaluate|評価する|動|val（価値）|We need data to evaluate the method.|その手法を評価するにはデータが要る。|evaluate the effect
prevail|広く行きわたる、打ち勝つ|動|val（価値）|The older custom still prevails in the north.|北部では古い習慣がなお行われている。|prevail over
valid|妥当な、有効な|形|val（価値）|The argument is valid but the premise is not.|議論は妥当だが前提がそうではない。|a valid point
equivalent|同等の|形・名|val（価値）|One mole is equivalent to that number of particles.|1 mol はその数の粒子に相当する。|be equivalent to
prevalent|広く見られる|形|val（価値）|The belief is still prevalent among students.|その考えは学生の間でなお広く見られる。|be prevalent in
intervene|介入する|動|ven / vent（来る）|The government intervened to stop the strike.|政府はストを止めるため介入した。|intervene in
convention|慣習、大会|名|ven / vent（来る）|By convention, the answer is given in metres.|慣習として答えはメートルで示す。|by convention
venture|思い切ってやる、冒険|動・名|ven / vent（来る）|No one ventured to contradict him.|あえて彼に反論する者はいなかった。|venture to do
circumvent|回避する、出し抜く|動|ven / vent（来る）|They circumvented the rule with a loophole.|彼らは抜け道で規則を回避した。|circumvent a rule
obvious|明らかな|形|vi / via（道）|The reason is obvious once you see the figure.|図を見れば理由は明らかだ。|for obvious reasons
deviate|逸脱する|動|vi / via（道）|The results deviate from the prediction.|結果は予測から外れている。|deviate from
previous|以前の|形|vi / via（道）|The previous attempt had failed for the same reason.|以前の試みも同じ理由で失敗していた。|the previous year
trivial|取るに足らない|形|vi / via（道）|The difference is trivial in practice.|その差は実際には取るに足らない。|a trivial matter
convince|納得させる|動|vict / vinc（勝つ）|Nothing I said could convince him.|私が何を言っても彼を納得させられなかった。|convince someone of
convict|有罪を宣告する|動|vict / vinc（勝つ）|He was convicted on weak evidence.|彼は弱い証拠で有罪とされた。|convict of
invincible|無敵の|形|vict / vinc（勝つ）|The team seemed invincible that season.|そのシーズン、チームは無敵に見えた。|an invincible defence
vital|きわめて重要な、生命の|形|viv / vit（生きる）|Water is vital to every living thing.|水はあらゆる生物にとって不可欠だ。|be vital to
revive|生き返らせる、復活させる|動|viv / vit（生きる）|The custom was revived after fifty years.|その習慣は50年ぶりに復活した。|revive interest
vivid|鮮やかな、生き生きした|形|viv / vit（生きる）|She has a vivid memory of that morning.|彼女はその朝を鮮明に覚えている。|a vivid description
survive|生き延びる|動|viv / vit（生きる）|Only two of the seedlings survived the winter.|苗のうち2本だけが冬を越した。|survive on
recognize|認識する、認める|動|cogn / not（知る）|I barely recognized the place.|その場所がほとんど分からなかった。|recognize the need for
cognitive|認知の|形|cogn / not（知る）|The test measures cognitive ability.|そのテストは認知能力を測る。|cognitive function
notion|考え、概念|名|cogn / not（知る）|He had no notion of what was coming.|彼は何が来るのか全く分かっていなかった。|have no notion of
notorious|悪名高い|形|cogn / not（知る）|The road is notorious for accidents.|その道路は事故で悪名高い。|notorious for
credible|信用できる|形|cred（信じる）|There is no credible evidence for the claim.|その主張に信用できる証拠はない。|a credible explanation
credentials|資格、信任状|名|cred（信じる）|She has excellent credentials for the post.|彼女はその職にふさわしい資格をもつ。|academic credentials
credulous|すぐ信じる|形|cred（信じる）|The scheme preyed on credulous investors.|その計画はすぐ信じる投資家を食い物にした。|credulous readers
adequate|十分な、適切な|形|equ（等しい）|The supply was barely adequate for a week.|供給は1週間分にかろうじて足りた。|adequate for
equate|同一視する|動|equ（等しい）|Do not equate wealth with success.|富を成功と同一視してはいけない。|equate A with B
equilibrium|平衡|名|equ（等しい）|The reaction reached equilibrium in ten minutes.|反応は10分で平衡に達した。|reach equilibrium
define|定義する|動|fin（終わり・境）|The term is never clearly defined in the paper.|その語は論文中で明確に定義されていない。|define A as B
infinite|無限の|形|fin（終わり・境）|The series has infinite terms but a finite sum.|その級数は項が無限だが和は有限だ。|an infinite number of
finite|有限の|形|fin（終わり・境）|We have a finite amount of time.|使える時間は有限だ。|a finite set
confine|限る、閉じ込める|動|fin（終わり・境）|The discussion was confined to one point.|議論は1点に限られた。|be confined to
gratitude|感謝|名|grat（喜ぶ）|She expressed her gratitude in a short note.|彼女は短い手紙で感謝を伝えた。|express gratitude
grateful|感謝している|形|grat（喜ぶ）|I am grateful for the chance to explain.|説明する機会をいただき感謝しています。|be grateful for
gratify|満足させる|動|grat（喜ぶ）|The result gratified everyone involved.|その結果は関係者全員を満足させた。|gratify a desire
legitimate|正当な、合法の|形|leg / lig（法・結ぶ）|That is a legitimate question to ask.|それは尋ねるに値する正当な問いだ。|a legitimate concern
obligation|義務|名|leg / lig（法・結ぶ）|You are under no obligation to answer.|答える義務はありません。|have an obligation to
oblige|義務づける、余儀なくさせる|動|leg / lig（法・結ぶ）|The rule obliges us to record every change.|規則により変更をすべて記録せねばならない。|be obliged to do
delegate|委任する、代表|動・名|leg / lig（法・結ぶ）|He delegates the routine work to others.|彼は日常業務を他人に任せる。|delegate authority
hypothesis|仮説|名|論証の語彙|The hypothesis was rejected after three trials.|その仮説は3回の試行のあと棄却された。|test a hypothesis
empirical|経験的な、実証的な|形|論証の語彙|There is little empirical support for the theory.|その理論を裏づける実証的根拠は乏しい。|empirical evidence
criterion|基準|名|論証の語彙|Cost was not the only criterion.|費用が唯一の基準ではなかった。|meet a criterion
plausible|もっともらしい|形|論証の語彙|That is a plausible but unproven explanation.|それはもっともらしいが証明されていない説明だ。|a plausible account
arbitrary|恣意的な|形|論証の語彙|The cut-off point looks arbitrary.|その区切りは恣意的に見える。|an arbitrary decision
rigorous|厳密な、厳しい|形|論証の語彙|The proof is not rigorous enough.|その証明は十分に厳密ではない。|a rigorous test
perceive|知覚する、とらえる|動|認知の語彙|We perceive depth with two eyes.|我々は両眼で奥行きを知覚する。|perceive A as B
intuition|直観|名|認知の語彙|Intuition is a poor guide in probability.|確率では直観はあてにならない。|trust one's intuition
bias|偏り、先入観|名|認知の語彙|The sample has an obvious bias.|その標本には明らかな偏りがある。|a bias towards
deliberate|意図的な、熟考する|形・動|認知の語彙|The omission was deliberate, not careless.|その省略は不注意ではなく意図的だった。|a deliberate choice
subconscious|潜在意識の|形|認知の語彙|Much of the process is subconscious.|その過程の多くは意識下で起きる。|a subconscious desire
sustainable|持続可能な|形|社会の語彙|The current rate of use is not sustainable.|現在の使用量は持続可能ではない。|sustainable development
emission|排出|名|社会の語彙|Emission of the gas has fallen since 2010.|その気体の排出は2010年以降減っている。|reduce emissions
habitat|生息地|名|社会の語彙|The forest is the habitat of several rare birds.|その森は数種の希少な鳥の生息地だ。|natural habitat
extinct|絶滅した|形|社会の語彙|The species became extinct within a century.|その種は一世紀のうちに絶滅した。|become extinct
welfare|福祉、幸福|名|社会の語彙|The policy is aimed at the welfare of children.|その政策は子どもの福祉を目的としている。|social welfare
legislation|法律、立法|名|社会の語彙|New legislation came into force in April.|新しい法律が4月に施行された。|pass legislation
discrimination|差別、識別|名|社会の語彙|The law forbids discrimination of any kind.|その法律はいかなる差別も禁じている。|discrimination against
prejudice|偏見|名|社会の語彙|Prejudice is hard to argue away.|偏見は議論では取り除きにくい。|racial prejudice
interfere|干渉する、邪魔をする|動|fer（運ぶ）|Do not interfere with the experiment while it runs.|実験中に手を出してはいけない。|interfere with
conclude|結論づける、締めくくる|動|clud / clus（閉じる）|From this we conclude that the effect is real.|ここから効果は本物だと結論づける。|conclude that
exclude|除外する|動|clud / clus（閉じる）|We cannot exclude the possibility of error.|誤りの可能性を排除できない。|exclude A from B
inclusive|包括的な、含めて|形|clud / clus（閉じる）|The fee is inclusive of all materials.|料金には材料費がすべて含まれる。|inclusive of
seclude|引き離す、隔離する|動|clud / clus（閉じる）|He secluded himself to finish the work.|彼は仕事を終えるため人を避けて閉じこもった。|seclude oneself
acquire|獲得する、習得する|動|quir / quest（求める）|Children acquire grammar without being taught.|子どもは教わらずに文法を習得する。|acquire a skill
inquire|尋ねる、調査する|動|quir / quest（求める）|She inquired about the cause of the delay.|彼女は遅れの原因を尋ねた。|inquire into
require|必要とする、要求する|動|quir / quest（求める）|The proof requires only two lemmas.|その証明に必要な補題は2つだけだ。|require that
conquer|征服する、克服する|動|quir / quest（求める）|He finally conquered his fear of speaking.|彼はついに話すことへの恐れを克服した。|conquer a fear
decisive|決定的な、断固とした|形|cid / cis（切る）|Her evidence proved decisive.|彼女の証拠が決定的となった。|a decisive factor
concise|簡潔な|形|cid / cis（切る）|Keep the summary concise.|要約は簡潔に保ちなさい。|a concise account
precise|正確な、まさにその|形|cid / cis（切る）|We need a precise measurement, not an estimate.|必要なのは見積もりではなく正確な測定だ。|to be precise
incident|出来事、事件|名|cid / cis（切る）|The incident was reported the following day.|その出来事は翌日報じられた。|an isolated incident
respond|反応する、答える|動|spond / spons（答える）|The cells respond to light within seconds.|細胞は数秒で光に反応する。|respond to
correspond|一致する、文通する|動|spond / spons（答える）|The results correspond closely to the model.|結果はモデルとよく一致する。|correspond to
responsible|責任がある、原因である|形|spond / spons（答える）|A single gene is responsible for the trait.|1つの遺伝子がその形質の原因である。|be responsible for
sponsor|後援する、後援者|動・名|spond / spons（答える）|The research was sponsored by the university.|その研究は大学の後援を受けた。|sponsor an event
assume|想定する、引き受ける|動|sume / sumpt（取る）|Let us assume the sequence converges.|数列が収束すると仮定しよう。|assume that
presume|推定する、決めてかかる|動|sume / sumpt（取る）|I presume you have read the instructions.|説明はお読みになったものと存じます。|presume to do
consume|消費する|動|sume / sumpt（取る）|The engine consumes less fuel at low speed.|そのエンジンは低速では燃料消費が少ない。|consume energy
resume|再開する|動|sume / sumpt（取る）|Work resumed after a short break.|短い休憩のあと作業が再開した。|resume work
suspend|一時停止する、吊るす|動|pend / pens（吊す）|The trial was suspended for a week.|裁判は1週間中断された。|suspend a service
compensate|補う、償う|動|pend / pens（吊す）|Extra practice compensates for a late start.|余分な練習が出遅れを埋め合わせる。|compensate for
indispensable|不可欠な|形|pend / pens（吊す）|Careful notes are indispensable in this work.|この仕事では丁寧な記録が欠かせない。|indispensable to
pending|未決の、〜まで|形・前|pend / pens（吊す）|The decision is still pending.|決定はまだ保留のままだ。|pending approval
preserve|保存する、保つ|動|serv（保つ）|Salt was once used to preserve meat.|塩はかつて肉の保存に使われた。|preserve the environment
reserve|取っておく、控えめさ|動・名|serv（保つ）|He reserves judgement until the data are in.|彼はデータが揃うまで判断を保留する。|reserve the right to
conserve|保存する、節約する|動|serv（保つ）|Energy is conserved in a closed system.|閉じた系ではエネルギーは保存される。|conserve energy
observe|観察する、遵守する|動|serv（保つ）|We observed the same pattern in every trial.|どの試行でも同じ傾向が観察された。|observe a rule
conform|従う、適合する|動|form（形）|The result conforms to the theory.|その結果は理論と一致する。|conform to
transform|変える、変換する|動|form（形）|The discovery transformed the whole field.|その発見は分野全体を一変させた。|transform A into B
formulate|定式化する、まとめる|動|form（形）|He formulated the rule as a single equation.|彼はその規則を1本の式にまとめた。|formulate a theory
reform|改革する、改革|動・名|form（形）|The system was reformed after the scandal.|不祥事の後、制度は改革された。|reform a system
fundamental|根本的な|形|fund / found（底）|There is a fundamental difference between the two.|両者には根本的な違いがある。|fundamental to
profound|深遠な、深刻な|形|fund / found（底）|The change had a profound effect on the region.|その変化は地域に深刻な影響を与えた。|a profound influence
foundation|基礎、土台|名|fund / found（底）|The argument rests on a weak foundation.|その議論は弱い土台の上に立っている。|lay the foundation
founder|創設者、沈没する|名・動|fund / found（底）|The founder of the school left no records.|その学校の創設者は記録を残さなかった。|the founder of
construct|構築する、組み立てる|動|struct（建てる）|We constructed a model from the data.|データからモデルを組み立てた。|construct an argument
obstruct|妨げる|動|struct（建てる）|Fallen trees obstructed the road.|倒木が道をふさいでいた。|obstruct the view
infrastructure|社会基盤|名|struct（建てる）|The region lacks basic infrastructure.|その地域には基本的な社会基盤がない。|build infrastructure
structural|構造上の|形|struct（建てる）|The problem is structural, not personal.|その問題は個人ではなく構造の問題だ。|a structural change
announce|発表する|動|nounce（告げる）|The results will be announced on Monday.|結果は月曜に発表される。|announce a decision
denounce|非難する、告発する|動|nounce（告げる）|The paper denounced the policy as unjust.|その新聞は政策を不当だと非難した。|denounce A as B
pronounce|発音する、宣言する|動|nounce（告げる）|The word is easier to read than to pronounce.|その語は発音するより読むほうがやさしい。|pronounce a word
renounce|放棄する、捨てる|動|nounce（告げる）|He renounced his claim to the land.|彼はその土地への権利を放棄した。|renounce a right
endure|耐える、持続する|動|dur（続く）|The custom endured for three centuries.|その習慣は3世紀にわたり続いた。|endure hardship
durable|耐久性のある|形|dur（続く）|The material is light but durable.|その素材は軽いが丈夫だ。|a durable solution
duration|継続時間|名|dur（続く）|The duration of the reaction was measured twice.|反応の継続時間を2度測定した。|for the duration of
impartial|公平な|形|part（分ける）|A judge must remain impartial.|裁判官は公平でなければならない。|an impartial observer
particle|粒子、小片|名|part（分ける）|Each particle carries a small charge.|各粒子はわずかな電荷を帯びている。|a charged particle
partial|部分的な、偏った|形|part（分ける）|We have only a partial record of the period.|その時代の記録は部分的にしかない。|be partial to
confident|自信のある、確信して|形|fid（信じる）|I am confident that the method works.|その手法が有効だと確信している。|be confident of
confide|打ち明ける|動|fid（信じる）|She confided her doubts to a colleague.|彼女は疑念を同僚に打ち明けた。|confide in
fidelity|忠実さ、正確さ|名|fid（信じる）|The translation keeps a high fidelity to the original.|その翻訳は原文に高い忠実さを保っている。|fidelity to
migrate|移動する、移住する|動|migr（移る）|The birds migrate south before winter.|その鳥は冬の前に南へ渡る。|migrate to
immigrant|移民（入ってくる人）|名|migr（移る）|The city grew with each wave of immigrants.|移民の波のたびに都市は成長した。|an immigrant family
emigrate|移住する（出ていく）|動|migr（移る）|Her grandparents emigrated in the 1920s.|彼女の祖父母は1920年代に国を出た。|emigrate from
complement|補う、補完するもの|動・名|plet / plen（満たす）|The two methods complement each other.|2つの手法は互いを補い合う。|complement each other
deplete|使い果たす|動|plet / plen（満たす）|Repeated cropping depleted the soil.|繰り返しの作付けが土地をやせさせた。|deplete resources
replenish|補充する|動|plet / plen（満たす）|Rain replenished the reservoir.|雨が貯水池を満たし直した。|replenish supplies
coherent|首尾一貫した|形|評論の語彙|The essay is detailed but not coherent.|その小論は詳しいが一貫していない。|a coherent argument
ambiguous|あいまいな|形|評論の語彙|The wording is ambiguous and should be revised.|その言い回しはあいまいで直すべきだ。|ambiguous about
comprehensive|包括的な|形|評論の語彙|We need a comprehensive survey, not samples.|標本ではなく包括的な調査が必要だ。|a comprehensive account
inherent|本来備わっている|形|評論の語彙|Risk is inherent in any experiment.|危険はどんな実験にも本来つきものだ。|inherent in
inevitable|避けられない|形|評論の語彙|Some loss of accuracy is inevitable.|精度の低下はある程度避けられない。|an inevitable consequence
subtle|微妙な、繊細な|形|評論の語彙|The difference is subtle but real.|その違いは微妙だが確かにある。|a subtle distinction
paradox|逆説|名|評論の語彙|It is a paradox that saving time creates none.|時間を節約しても時間ができないのは逆説だ。|an apparent paradox
explicitly|明示的に|副|評論の語彙|The paper explicitly rejects that reading.|その論文はその読み方を明確に退けている。|state explicitly
substantial|かなりの、実質的な|形|数量の語彙|There was a substantial rise in temperature.|気温のかなりの上昇が見られた。|a substantial amount
considerable|かなりの|形|数量の語彙|The method saves a considerable amount of time.|その方法はかなりの時間を節約する。|a considerable number of
negligible|無視できるほどの|形|数量の語彙|The error is negligible at this scale.|この尺度では誤差は無視できる。|a negligible effect
marginal|わずかな、周辺の|形|数量の語彙|The improvement was marginal at best.|改善はせいぜいわずかだった。|a marginal difference
abundant|豊富な|形|数量の語彙|Evidence for the first claim is abundant.|最初の主張の証拠は豊富にある。|abundant in
scarce|乏しい、不足した|形|数量の語彙|Water became scarce by August.|8月までに水が不足した。|scarce resources
reluctant|気が進まない|形|態度の語彙|He was reluctant to change the method.|彼はその方法を変えたがらなかった。|be reluctant to do
indifferent|無関心な|形|態度の語彙|The public remained indifferent to the warning.|世間はその警告に無関心なままだった。|indifferent to
skeptical|懐疑的な|形|態度の語彙|Most researchers were skeptical of the result.|大半の研究者はその結果に懐疑的だった。|skeptical about
obstinate|頑固な|形|態度の語彙|He was obstinate in refusing help.|彼は頑なに助けを断った。|an obstinate refusal
enthusiastic|熱心な|形|態度の語彙|She was enthusiastic about the new approach.|彼女は新しい手法に熱心だった。|enthusiastic about
consequence|結果、重大さ|名|因果の語彙|The consequence of that choice was immediate.|その選択の結果はすぐに現れた。|as a consequence of
trigger|引き起こす、引き金|動・名|因果の語彙|A small change can trigger a large effect.|小さな変化が大きな影響を引き起こしうる。|trigger a reaction
stem|由来する、茎|動・名|因果の語彙|The disagreement stems from a single word.|その不一致は一語に由来する。|stem from
underlie|根底にある|動|因果の語彙|A simple rule underlies the whole pattern.|単純な規則がその全体を支えている。|underlie a theory
""".trimIndent()
