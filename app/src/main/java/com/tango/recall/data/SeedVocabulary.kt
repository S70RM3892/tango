package com.tango.recall.data

import com.tango.recall.data.Seed.Word

/**
 * More vocabulary, still organised by root.
 *
 * A word met on its own has nothing to hang from; a word met as one of four sharing a
 * root ("send": omit, submit, transmit, dismiss) arrives with three hooks already in
 * place, and the prefix does the rest of the work. Every entry carries a sentence,
 * because the exam asks for the word inside one, not the word by itself.
 */
internal fun seedMoreVocabulary(s: Seeder) {
    val deckId = s.deck(
        name = Seed.ENGLISH_DECK,
        type = NoteType.ENGLISH,
        templates = setOf("en_ja", "ja_en", "cloze"),
        newPerDay = 15,
        relationQuiz = true,
    )

    Seed.addWords(s, deckId, MORE_ROOTS)

    val byWord = s.repo.listNotes(deckId, "", Int.MAX_VALUE).associateBy { it.title() }
    fun link(a: String, b: String, t: LinkType, memo: String) {
        val x = byWord[a]?.id ?: return
        val y = byWord[b]?.id ?: return
        s.link(x, y, t, memo)
    }

    // Across the root groups: the prefixes are the other half of the system.
    link("infer", "deduce", LinkType.SYNONYM, "どちらも「推論する」。deduce は前提から必然的に、infer は手がかりから")
    link("persist", "resist", LinkType.CONFUSABLE, "同じ sist（立つ）。per-（通して）やり通す / re-（逆らって）抗う")
    link("submit", "subject", LinkType.RELATED, "sub-（下へ）。下に差し出す → 提出する / 下に置く → 服従させる")
    link("suppress", "oppress", LinkType.CONFUSABLE, "sup-（下へ）押さえ込む / op-（に対して）押しつけて虐げる")
    link("dismiss", "reject", LinkType.SYNONYM, "どちらも「退ける」。dismiss は取り合わない含み")
    link("aspire", "inspire", LinkType.CONFUSABLE, "a-（〜へ）息を向ける＝志す / in-（中へ）息を吹き込む＝奮い立たせる")
    link("occur", "incur", LinkType.CONFUSABLE, "起こる / 自ら招く。incur は損失・費用を目的語にとる")
    link("anticipate", "project", LinkType.SYNONYM, "先を見込む。project は数値の予測に使うことが多い")
    link("abstract", "extract", LinkType.CONTRAST, "同じ tract（引く）。抜き出して一般化する / そのまま抜き出す")
    link("evoke", "provoke", LinkType.CONTRAST, "呼び起こす（記憶・感情）/ 引き起こす（反発・批判）")
    link("compress", "abstract", LinkType.RELATED, "どちらも「縮めて要点にする」方向の語")
    link("confer", "concur", LinkType.RELATED, "con-（共に）。持ち寄って協議する / 意見が重なる")
    link("transmit", "conduct", LinkType.RELATED, "伝える・導く。物理では熱や電気の伝導で並んで出てくる")
    link("gradual", "progress", LinkType.RELATED, "grad / gress（歩む）。一歩ずつ ↔ 前へ進む")
    link("susceptible", "resist", LinkType.ANTONYM, "影響を受けやすい ↔ 抗う")
    link("prescribe", "describe", LinkType.CONFUSABLE, "pre-（前もって）指示する / de-（下へ）書き写す＝描写する")
}

private val MORE_ROOTS: List<Pair<String, List<Word>>> = listOf(
    "fer（運ぶ）" to listOf(
        Word("confer", "協議する、授与する", "動", "fer（運ぶ）",
            "The committee conferred for hours before reaching a decision.",
            "委員会は結論に達するまで何時間も協議した。", "confer with / confer a degree on"),
        Word("infer", "推論する、推察する", "動", "fer（運ぶ）",
            "From her expression I inferred that the news was bad.",
            "彼女の表情から、知らせは悪いものだと推察した。", "infer A from B"),
        Word("defer", "延期する、（意見に）従う", "動", "fer（運ぶ）",
            "They deferred the decision until the next meeting.",
            "彼らは決定を次の会議まで先送りした。", "defer to someone's judgement"),
        Word("fertile", "肥沃な、（発想が）豊かな", "形", "fer（運ぶ）",
            "The soil here is fertile enough to grow almost anything.",
            "ここの土はたいていのものが育つほど肥沃だ。", "fertile soil / imagination"),
    ),
    "mit / miss（送る）" to listOf(
        Word("omit", "省く、抜かす", "動", "mit / miss（送る）",
            "You may omit the details if time is short.",
            "時間がなければ細部は省いてよい。", "omit to mention"),
        Word("submit", "提出する、服従する", "動", "mit / miss（送る）",
            "Applications must be submitted by Friday.",
            "申請書は金曜日までに提出しなければならない。", "submit an application / submit to"),
        Word("transmit", "伝える、送信する", "動", "mit / miss（送る）",
            "The disease is transmitted by mosquitoes.",
            "その病気は蚊によって媒介される。", "transmit a signal / a disease"),
        Word("dismiss", "退ける、解雇する", "動", "mit / miss（送る）",
            "He dismissed the idea as unrealistic.",
            "彼はその考えを非現実的だとして退けた。", "dismiss an idea / a claim"),
    ),
    "scrib / script（書く）" to listOf(
        Word("describe", "描写する、述べる", "動", "scrib / script（書く）",
            "Words cannot describe how grateful I am.",
            "どれほど感謝しているかは言葉では言い表せない。", "describe A as B"),
        Word("prescribe", "処方する、指示する", "動", "scrib / script（書く）",
            "The doctor prescribed a week of complete rest.",
            "医者は1週間の完全な休養を指示した。", "prescribe medicine / rules"),
        Word("subscribe", "定期購読する、賛同する", "動", "scrib / script（書く）",
            "I do not subscribe to that view.",
            "私はその見方には賛同しない。", "subscribe to"),
        Word("manuscript", "原稿、写本", "名", "scrib / script（書く）",
            "The manuscript was turned down by three publishers.",
            "その原稿は3つの出版社に断られた。", "submit a manuscript"),
    ),
    "spir（息をする）" to listOf(
        Word("aspire", "熱望する、志す", "動", "spir（息をする）",
            "She aspires to become a researcher.",
            "彼女は研究者になることを志している。", "aspire to do"),
        Word("conspire", "共謀する", "動", "spir（息をする）",
            "They conspired to keep the results secret.",
            "彼らは結果を隠しておこうと共謀した。", "conspire to do"),
        Word("inspire", "奮い立たせる、着想を与える", "動", "spir（息をする）",
            "His teacher inspired him to study physics.",
            "恩師の影響で彼は物理を学ぶ気になった。", "inspire someone to do"),
        Word("expire", "期限が切れる", "動", "spir（息をする）",
            "My passport expires at the end of next month.",
            "私のパスポートは来月末で期限が切れる。", "expire / expiry date"),
    ),
    "tract（引く）" to listOf(
        Word("abstract", "抽象的な、要約", "形・名", "tract（引く）",
            "The argument is too abstract to be of any use.",
            "その議論は抽象的すぎて役に立たない。", "an abstract concept"),
        Word("distract", "気をそらす", "動", "tract（引く）",
            "Noise from the street distracted me from my work.",
            "通りの騒音で仕事から気がそれた。", "distract A from B"),
        Word("extract", "抜き出す、抽出する", "動", "tract（引く）",
            "It is hard to extract any meaning from these figures.",
            "これらの数字から意味を読み取るのは難しい。", "extract information from"),
        Word("retract", "撤回する、引っ込める", "動", "tract（引く）",
            "The newspaper retracted the statement the next day.",
            "新聞は翌日その記述を撤回した。", "retract a statement"),
    ),
    "press（押す）" to listOf(
        Word("compress", "圧縮する", "動", "press（押す）",
            "The report was compressed into a single page.",
            "その報告書は1ページに圧縮された。", "compress a file / a gas"),
        Word("suppress", "抑える、鎮圧する", "動", "press（押す）",
            "She could not suppress a smile.",
            "彼女は笑みを抑えきれなかった。", "suppress laughter / a revolt"),
        Word("oppress", "圧迫する、虐げる", "動", "press（押す）",
            "The regime oppressed its own people for decades.",
            "その体制は何十年も自国民を虐げた。", "oppress a minority"),
        Word("impress", "印象づける", "動", "press（押す）",
            "What impressed me most was his patience.",
            "最も印象に残ったのは彼の忍耐強さだった。", "impress A with B"),
    ),
    "cur / cours（走る・流れる）" to listOf(
        Word("occur", "起こる、ふと思い浮かぶ", "動", "cur / cours（走る・流れる）",
            "It never occurred to me that he might refuse.",
            "彼が断るかもしれないとは思いもしなかった。", "it occurs to someone that"),
        Word("incur", "（損失などを）招く", "動", "cur / cours（走る・流れる）",
            "The company incurred heavy losses last year.",
            "その会社は昨年多額の損失を被った。", "incur costs / debts"),
        Word("concur", "意見が一致する", "動", "cur / cours（走る・流れる）",
            "Most experts concur that the policy has failed.",
            "その政策は失敗だという点で大半の専門家が一致している。", "concur with"),
        Word("current", "現在の、流れ", "形・名", "cur / cours（走る・流れる）",
            "The current system cannot handle this much traffic.",
            "現行の仕組みではこれほどの通信量をさばけない。", "the current situation"),
    ),
    "sist（立つ）" to listOf(
        Word("persist", "続く、やり通す", "動", "sist（立つ）",
            "If the symptoms persist, see a doctor.",
            "症状が続くようなら医者にかかりなさい。", "persist in doing"),
        Word("resist", "抵抗する、こらえる", "動", "sist（立つ）",
            "I could not resist the temptation to look.",
            "見たいという誘惑に抗えなかった。", "resist temptation / change"),
        Word("consist", "（〜から）成る", "動", "sist（立つ）",
            "The committee consists of twelve members.",
            "その委員会は12名から成る。", "consist of / consist in"),
        Word("insist", "強く主張する", "動", "sist（立つ）",
            "He insisted that he had done nothing wrong.",
            "彼は何も悪いことはしていないと言い張った。", "insist on doing"),
    ),
    "gress / grad（歩む）" to listOf(
        Word("progress", "進歩、進む", "名・動", "gress / grad（歩む）",
            "Progress has been slower than we expected.",
            "進展は予想より遅い。", "make progress"),
        Word("regress", "後退する", "動", "gress / grad（歩む）",
            "Without practice, skills quickly regress.",
            "練習しなければ技能はすぐに後退する。", "regress to"),
        Word("gradual", "段階的な、ゆるやかな", "形", "gress / grad（歩む）",
            "The change was gradual enough to go unnoticed.",
            "その変化は気づかれないほどゆるやかだった。", "a gradual increase"),
        Word("aggressive", "攻撃的な、積極的な", "形", "gress / grad（歩む）",
            "He took an aggressive approach to the negotiation.",
            "彼は交渉で強気の姿勢をとった。", "an aggressive strategy"),
    ),
    "ject（投げる）" to listOf(
        Word("reject", "拒絶する、却下する", "動", "ject（投げる）",
            "The journal rejected the paper without review.",
            "その学術誌は審査もせずに論文を却下した。", "reject a proposal"),
        Word("inject", "注入する", "動", "ject（投げる）",
            "The nurse injected the vaccine into his arm.",
            "看護師は彼の腕にワクチンを注射した。", "inject A into B"),
        Word("project", "見込む、投影する", "動", "ject（投げる）",
            "Sales are projected to double next year.",
            "売上は来年倍増すると見込まれている。", "be projected to do"),
        Word("subject", "さらす、服従させる", "動", "ject（投げる）",
            "The samples were subjected to extreme heat.",
            "試料は極度の高温にさらされた。", "be subject to / subject A to B"),
    ),
    "cept / cip（取る）" to listOf(
        Word("anticipate", "予期する、見越す", "動", "cept / cip（取る）",
            "We anticipate a sharp rise in demand.",
            "需要の急激な増加を見込んでいる。", "anticipate a problem"),
        Word("susceptible", "影響を受けやすい", "形", "cept / cip（取る）",
            "Young plants are susceptible to frost.",
            "若い苗は霜の害を受けやすい。", "be susceptible to"),
        Word("conceive", "思いつく、心に抱く", "動", "cept / cip（取る）",
            "It is hard to conceive of a world without electricity.",
            "電気のない世界は想像しがたい。", "conceive of"),
        Word("intercept", "途中で捕らえる、傍受する", "動", "cept / cip（取る）",
            "The message was intercepted before it arrived.",
            "その通信は届く前に傍受された。", "intercept a signal"),
    ),
    "voc / vok（呼ぶ・声）" to listOf(
        Word("advocate", "主張する、擁護者", "動・名", "voc / vok（呼ぶ・声）",
            "She advocates a complete ban on the practice.",
            "彼女はその慣行の全面禁止を主張している。", "advocate for"),
        Word("evoke", "呼び起こす", "動", "voc / vok（呼ぶ・声）",
            "The smell evoked memories of my childhood.",
            "その匂いは子ども時代の記憶を呼び起こした。", "evoke a memory / a response"),
        Word("provoke", "引き起こす、怒らせる", "動", "voc / vok（呼ぶ・声）",
            "The remark provoked an angry response.",
            "その発言は怒りの反応を招いた。", "provoke criticism"),
        Word("vocation", "天職、使命感", "名", "voc / vok（呼ぶ・声）",
            "He regards teaching as a vocation rather than a job.",
            "彼は教えることを職業というより天職と考えている。", "a sense of vocation"),
    ),
)
