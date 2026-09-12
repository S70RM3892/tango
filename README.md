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

| 型 | ファイル | 件数 |
|---|---|---|
| english | kyodai_core_01.tsv | 99 |
| eisakubun | kyodai_01.tsv | 31 |
| chem_substance | muki_01.tsv | 55 |
| chem_reaction | kogyo_jikken_01.tsv | 30 |
| chem_calc | keisan_01.tsv | 20 |
| basic | physics_01.tsv | 57 |
| basic | math_teiseki_01.tsv | 40 |
| basic | kobun_01.tsv | 74 |
| | **合計** | **406** |

アプリ内蔵のシードは約70件なので、取り込むと約6.8倍になる。
