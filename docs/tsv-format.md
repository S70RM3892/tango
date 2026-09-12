# Tango 取り込みTSV仕様（APK v1.4 から復元）

`com.tango.recall.data.ImportExport` を逆コンパイルして確定させた仕様。
アプリ本体を変更せずに、この形式のファイルを「デッキ設定 → ファイルから取り込む」で流し込める。

## 全体ルール

| 項目 | 仕様 |
|---|---|
| 区切り | 1行目にタブがあればTSV、無ければCSV |
| ヘッダ | 必須。フィールドID（英字）でも日本語ラベルでも可（大小文字無視） |
| タグ列 | `tags` または `タグ`。値はスペースまたはカンマ区切り |
| 改行 | セル内改行は `\n`（バックスラッシュ + n の2文字）で書く |
| 空セル | そのフィールドは保存されない（欠損扱い。エラーにはならない） |
| 空行 | スキップ |
| 重複判定 | 型ごとの「タイトル欄」が一致する既存ノートにマージ（上書き更新） |
| クォート | TSVでは解釈しない。CSVのみ `"` を解釈 |

デッキの型と取り込むTSVの型が一致している必要がある（デッキ作成後に型は変更不可）。

## タイトル欄（重複判定キー）

| 型 | キー |
|---|---|
| english | `word` |
| chem_substance | `name` |
| chem_reaction | `title` |
| eisakubun | `ja` |
| chem_calc | `question` |
| basic | `front` |

## 型ごとの列（この順番で並べる）

### english（英単語）
`word` `meaning` `pos` `root` `example` `exampleJa` `collocation` `memo` `tags`

カード: `en_ja` 英→和(REVEAL, 既定ON) / `ja_en` 和→英・入力(TYPE, 既定ON) /
`cloze` 例文穴埋め(CLOZE, example中のwordを空所化, 既定ON) /
`root_word` 語源→語(REVEAL, 既定OFF) / `collo` コロケーション→語(TYPE, 既定OFF) /
`ja_en_sentence` 例文の和訳→英訳(SELF_CHECK, 既定OFF)

`cloze` を効かせるには `example` の中に `word` の文字列が現れている必要がある。

### chem_substance（化学・物質）
`name` `formula` `category` `props` `uses` `memo` `tags`

カード: `name_formula` 名称→化学式・入力(TYPE) / `formula_name` 化学式→名称(REVEAL) / `name_props` 名称→性質(REVEAL)

### chem_reaction（化学・反応）
`title` `equation` `condition` `point` `memo` `tags`

カード: `title_eq` 反応名→反応式・入力(TYPE) / `eq_title` 反応式→反応名(REVEAL) / `title_cond` 反応名→条件・触媒(REVEAL)

### eisakubun（和文英訳）
`ja` `en` `structures` `traps` `memo` `tags`

カード: `ja_en_write` 和文英訳を書く(SELF_CHECK, チェックリスト = `structures`) /
`trap_only` 言い換えのポイントだけ確認(REVEAL, 既定OFF)

`structures` は1行に1項目。自己採点のチェックリストになるので、`\n` で複数行にする。

### chem_calc（化学・計算）
`question` `answer` `unit` `tolerance` `solution` `memo` `tags`

カード: `calc` 計算して答える(NUMERIC, 単位=`unit`, 許容誤差=`tolerance`) / `method` 解き方を思い出す(REVEAL, 既定OFF)

`answer` は数値のみ。`tolerance` は％（未指定なら1）。指数は `2.7e-5` / `2.7×10^-5` の両方が読める。

### basic（自由形式）
`front` `back` `hint` `memo` `tags`

カード: `fb` 表→裏(REVEAL, 既定ON) / `bf` 裏→表(REVEAL, 既定OFF)

物理・数学・古文など専用の型がまだ無い教科は、当面この型で入れる。

## タグ運用（過去問との接続）

`notes.tags` は自由文字列なので、規約で構造を持たせる。

```
kyodai-2015-1     京大 2015年度 第1問
kyodai-freq       京大頻出
common-test       共通テスト範囲
weak              自分が落としやすい
```

デッキをまたいだ検索（ホームの「検索（どのデッキからでも）」）が `tags LIKE ?` で効くので、
`kyodai-` 接頭辞を付けておくと過去問由来のノートだけ横断で拾える。
