#!/usr/bin/env python3
"""Tango 取り込みTSVの検証。仕様は docs/tsv-format.md（APK v1.4 から復元）。

  python3 tools/validate_tsv.py data/**/*.tsv
  python3 tools/validate_tsv.py            # data/ 以下を全部
"""
import sys, glob, os

SPEC = {
    "english":        dict(cols=["word","meaning","pos","root","example","exampleJa","collocation","memo"], key="word",     required=["word","meaning"]),
    "chem_substance": dict(cols=["name","formula","category","props","uses","memo"],                        key="name",     required=["name","formula"]),
    "chem_reaction":  dict(cols=["title","equation","condition","point","memo"],                            key="title",    required=["title","equation"]),
    "eisakubun":      dict(cols=["ja","en","structures","traps","memo"],                                    key="ja",       required=["ja","en"]),
    "chem_calc":      dict(cols=["question","answer","unit","tolerance","solution","memo"],                 key="question", required=["question","answer"]),
    "basic":          dict(cols=["front","back","hint","memo"],                                             key="front",    required=["front","back"]),
}

def detect_type(path, header):
    d = os.path.basename(os.path.dirname(path))
    if d in SPEC:
        return d
    for name, s in SPEC.items():
        if header[:len(s["cols"])] == s["cols"]:
            return name
    return None

def check(path):
    errs, warns = [], []
    with open(path, encoding="utf-8") as f:
        raw = f.read()
    if "\r" in raw:
        warns.append("CRLF が混ざっている（アプリ側で trimEnd されるので致命的ではない）")
    lines = [l for l in raw.split("\n") if l.strip()]
    if not lines:
        return ["空ファイル"], [], 0
    header = lines[0].split("\t")
    t = detect_type(path, header)
    if t is None:
        return [f"型を判定できない。ヘッダ: {header[:3]}"], [], 0
    spec = SPEC[t]
    want = spec["cols"] + ["tags"]
    if header != want:
        errs.append(f"ヘッダ不一致\n    期待: {want}\n    実際: {header}")
        return errs, warns, 0
    ncol = len(want)
    seen = {}
    for i, line in enumerate(lines[1:], start=2):
        cells = line.split("\t")
        if len(cells) != ncol:
            errs.append(f"{i}行目: 列数 {len(cells)} (期待 {ncol})")
            continue
        row = dict(zip(want, cells))
        for r in spec["required"]:
            if not row[r].strip():
                errs.append(f"{i}行目: 必須欄 '{r}' が空")
        k = row[spec["key"]].strip()
        if k in seen:
            errs.append(f"{i}行目: タイトル欄 '{k}' が {seen[k]}行目と重複（取り込み時にマージされて片方消える）")
        else:
            seen[k] = i
        # 型ごとの追加チェック
        if t == "english" and row["example"].strip():
            if row["word"].strip().lower() not in row["example"].lower():
                warns.append(f"{i}行目: example に word '{row['word']}' が現れない → cloze カードが作れない")
        if t == "chem_calc":
            a = row["answer"].strip().replace("×10^", "e").replace("^", "")
            try:
                float(a.replace("e+", "e"))
            except ValueError:
                errs.append(f"{i}行目: answer '{row['answer']}' が数値として読めない")
            if row["tolerance"].strip():
                try:
                    float(row["tolerance"])
                except ValueError:
                    errs.append(f"{i}行目: tolerance '{row['tolerance']}' が数値でない")
        if t == "eisakubun" and row["structures"].strip() and "\\n" not in row["structures"]:
            warns.append(f"{i}行目: structures が1項目のみ（チェックリストは複数行推奨）")
        for name, v in row.items():
            if "\n" in v:
                errs.append(f"{i}行目: 欄 '{name}' に生の改行（'\\n' の2文字で書くこと）")
    return errs, warns, len(lines) - 1

def main():
    paths = sys.argv[1:] or sorted(glob.glob("data/**/*.tsv", recursive=True))
    if not paths:
        print("TSV が見つからない"); return 1
    bad = total = 0
    for p in paths:
        errs, warns, n = check(p)
        status = "OK  " if not errs else "NG  "
        print(f"{status}{p}  ({n}件)")
        for w in warns[:10]:
            print(f"    warn: {w}")
        if len(warns) > 10:
            print(f"    warn: ほか {len(warns)-10} 件")
        for e in errs[:20]:
            print(f"    ERR : {e}")
        if len(errs) > 20:
            print(f"    ERR : ほか {len(errs)-20} 件")
        bad += bool(errs); total += n
    print(f"\n{len(paths)} ファイル / {total} ノート / NG {bad} ファイル")
    return 1 if bad else 0

if __name__ == "__main__":
    sys.exit(main())
