# tango

Android 単語帳アプリ `com.tango.recall`（Tango v1.4）向けの学習データと運用ツール。

アプリ本体は FSRS スケジューラ・方向別カード・タイプ入力採点・取り違え検出・
知識グラフ・試験日逆算・TSV/JSON 入出力をすでに実装している。
このリポジトリは、そこに足りていない**中身（データ）**と、
アプリ側に必要な改修の**設計**を置く。

## 中身

```
data/
  english/          英単語（english 型）
  eisakubun/        和文英訳（eisakubun 型）
  chem_substance/   化学・物質
  chem_reaction/    化学・反応
  chem_calc/        化学・計算
  basic/            物理・数学・古文（basic 型。専用の型がまだ無いため）
docs/
  tsv-format.md     取り込みTSV仕様（APK v1.4 から逆コンパイルして復元）
  roadmap-1-8.md    不足項目の実装計画
tools/
  validate_tsv.py   取り込み前の検証
```

## 使い方

```bash
python3 tools/validate_tsv.py            # data/ 以下を全部検証
python3 tools/validate_tsv.py data/english/*.tsv
```

検証を通してから、アプリの「デッキ設定 → ファイルから取り込む」で読み込む。
デッキの型と TSV の型を一致させること（デッキ作成後に型は変更できない）。

## 現在の件数

| 型 | ファイル | 件数 | 用途 |
|---|---|---|---|
| basic | joho1_01.tsv | 95 | 共テ 情報Ⅰ |
| english | kyodai_core_01.tsv | 99 | 二次 英語 |
| basic | kobun_01.tsv | 74 | 二次 国語（古文） |
| basic | seikei_01.tsv | 70 | 共テ 政経 |
| basic | physics_01.tsv | 57 | 二次 理科 |
| chem_substance | muki_01.tsv | 55 | 二次 理科 |
| basic | math_teiseki_01.tsv | 40 | 二次 数学 |
| eisakubun | kyodai_01.tsv | 31 | 二次 英語（和文英訳） |
| chem_reaction | kogyo_jikken_01.tsv | 30 | 二次 理科 |
| chem_calc | keisan_01.tsv | 20 | 二次 理科 |
| | **合計** | **571** | |

アプリ内蔵のシードは約70件なので、取り込むと約8.2倍になる。

`data/universities/` は取り込みTSVではなく、アプリの `assets/universities.tsv` に
追加する配点列の参照データ（`tools/validate_tsv.py` の対象外）。

## 教科の優先順位

件数の配分は配点比ではなく、`S70RM3892/-` の設問単位の分析にもとづく。
全国上位2.3%相当の受験生が共通テストでまだ落としている点（1025点換算）:

| 科目 | 取りこぼし |
|---|---|
| 情報Ⅰ | 6.0 〜 8.4点 |
| 地歴公民（政経） | 3.0 〜 4.7点 |
| 英語リスニング | 2.5 〜 3.7点 |
| 数ⅠA・数ⅡBC・物理・化学の合計 | 4.7 〜 6.2点 |

情報Ⅰ＋地歴公民だけで全体の54〜55%を占める。
合計 16.6〜23.7点は情報学科の合格最低点の年変動σ（10.6点）の 1.6〜2.2 倍にあたる。
だから件数が最も多いのは英単語ではなく情報Ⅰになっている。
