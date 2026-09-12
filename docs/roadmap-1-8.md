# 不足項目 1〜8 の実装計画

APK v1.4（`com.tango.recall`）の解析結果にもとづく。
**データだけで解決する項目（1・4・5）はこのリポジトリで完了済み。残りはアプリのソースが要る。**

| # | 項目 | ソース要否 | 状態 |
|---|---|---|---|
| 1 | データ量 | 不要（TSV取り込み） | 着手済み・406件 |
| 2 | FSRSパラメータ最適化 | **要** | 設計のみ |
| 3 | 通知・リマインダー | **要** | 設計のみ |
| 4 | 教科カバレッジ（物理・数学・古文） | 不要（basic型） | 着手済み |
| 5 | 過去問タグ運用 | 不要（tags列） | 規約を策定済み |
| 6 | 志望校データに配点を追加 | **要**（assets差し替え） | 列設計のみ |
| 7 | 自動バックアップ | **要** | 設計のみ |
| 8 | leech処理 / TTS | **要** | 設計のみ |

---

## 2. FSRS パラメータ最適化

現状 `com.tango.recall.srs.Fsrs` は固定重みで動いている（`optimize` 系のメソッドが存在しない）。
一方 `reviews` テーブルは最適化に必要な列を全部持っている:

```sql
reviews(cardId, noteId, deckId, rating, ts, phase, stability, difficulty, tookMs)
```

**方針**: 端末内で全件最適化を回すのは重いので、2段構えにする。

1. `settings` に `fsrs_w`（カンマ区切り19個）を持たせ、`Fsrs` が起動時に読む。無ければ既定値。
2. 「レビュー履歴を書き出す」を追加し、`reviews` を Anki 互換の revlog CSV で出す。
   PC 側で公式オプティマイザを回して得た w を、設定画面に貼り付けて保存。

**注意**: レビュー件数が概ね1000件を超えるまで最適化の効果は出ない。項目1が先。

## 3. 通知・リマインダー

マニフェストの権限は `android.permission.DUMP`（profileinstaller由来）のみ。
`POST_NOTIFICATIONS` も WorkManager も AlarmManager も使っていない＝リマインダーが存在しない。

**必要な変更**:
- `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>`（API 33+ は実行時許可）
- `androidx.work:work-runtime-ktx` を追加し、日次 `PeriodicWorkRequest`
- Worker で `SELECT COUNT(*) FROM cards WHERE suspended=0 AND phase!='NEW' AND due<=?` を引き、
  0件なら通知しない
- 通知時刻は `settings` に `notify_hour` として保存
- 試験日が設定されているときは残り日数を本文に入れる（`examDate` が既にある）

**INTERNET 権限は追加しない**。オフライン設計は集中の観点で維持する価値がある。

## 6. 志望校データに配点を追加

`assets/universities.tsv` の現在の列:

```
university  kind  prefecture  region  city  faculty  department  field  years  capacity
```

**追加する列**（末尾に足せば既存パーサを壊さない可能性が高いが、`Universities` の実装確認が必要）:

```
kyotsu_total  niji_total  niji_kokugo  niji_sugaku  niji_rika  niji_gaikokugo
```

検証済みの値（京都大学 工学部・2026年度入試）:

| 項目 | 配点 |
|---|---|
| 共通テスト合計 | 225 |
| 二次合計 | 800 |
| 二次 国語 | 100 |
| 二次 数学 | 250 |
| 二次 理科 | 250 |
| 二次 外国語 | 200 |

出典: 旺文社パスナビ 京都大学 工学部 入試科目
https://passnavi.obunsha.co.jp/univ/0560/subject/?facultyID=045

**1318学科ぶんは1件ずつ出典確認が要る**ので、まず志望校候補（京大＋併願先）だけ埋めるのが現実的。
配点が入れば `ExamOutlook`（試験日逆算）と結びつけて「配点あたりの伸びしろ」が出せる。

なお現在のデータは**国公立のみ**（国立870・公立448）。私立が無いので併願検討には使えない。

## 7. 自動バックアップ

現状は手動 JSON エクスポートのみ。`exportJson` は `format: "tango-backup", version: 1` を出す。

**必要な変更**:
- 初回に `ACTION_OPEN_DOCUMENT_TREE` で保存先フォルダを選ばせ、永続URI許可を取る（権限不要）
- WorkManager の週次ジョブで `exportJson` の結果を `tango-backup-YYYYMMDD.json` として書き出し
- 直近8世代だけ残して古いものを削除
- マニフェストの `android:allowBackup` の値を確認し、必要なら `fullBackupContent` に DB を含める

## 8. leech処理 / TTS

**leech**: `cards.lapses` は既にある。`FsrsScheduler` の rating=AGAIN 処理の直後に

```
lapses >= N（既定8）→ 自動で suspended=1 にして「取り違え」画面に送る
```

を入れる。閾値は `settings` の `leech_threshold`。
現状の「何度も間違えているもの」は一覧表示だけで、キューから外す導線が無い。

**TTS**: `android.speech.tts.TextToSpeech` は未使用。`english` 型の `word` / `example` を読み上げる。
ただし京大二次にリスニングは無いので、優先度は 2・3 より低い。
