#!/usr/bin/env python3
"""docs/ の外を指す相対リンクを検出する。

Zensical がビルドするのは docs/ 配下だけです。docs/ の外を指す相対リンクは、
GitHub 上で Markdown を直接読むときは動きますが、公開サイトでは 404 になります。
Zensical のビルド警告は .md を指すものしか報告しないため、このスクリプトで補います。

docs/ の外を参照するときは、GitHub の絶対URLを使ってください。
    ファイル:       https://github.com/ht-0328/crypto-autotrading-lab/blob/main/<パス>
    ディレクトリ:   https://github.com/ht-0328/crypto-autotrading-lab/tree/main/<パス>

使い方（リポジトリのルートで実行）:
    python3 scripts/check-doc-links.py

検出が1件でもあれば終了コード 1 を返します。
"""
import re
import pathlib
import sys

root = pathlib.Path(__file__).resolve().parent.parent
docs = root / "docs"
link_re = re.compile(r'(?<!\!)\[([^\]\[]*)\]\(([^)\s]+)(?:\s+"[^"]*")?\)')
code_re = re.compile(r"`+[^`]*`+")

found = 0
for md in sorted(docs.rglob("*.md")):
    in_fence = False
    for i, line in enumerate(md.read_text(encoding="utf-8").split("\n"), 1):
        # コードフェンスの中は対象外
        if line.lstrip().startswith("```"):
            in_fence = not in_fence
            continue
        if in_fence:
            continue
        # インラインコードの中も対象外（書き方の例としてリンクの形を載せている箇所があるため）
        line = code_re.sub(lambda m: " " * len(m.group(0)), line)
        for m in link_re.finditer(line):
            target = m.group(2)
            if target.startswith(("http", "#", "mailto:")):
                continue
            resolved = (md.parent / target.split("#")[0]).resolve()
            try:
                resolved.relative_to(docs)
                if resolved.exists():
                    continue
            except ValueError:
                pass
            print(f"{md.relative_to(root)}:{i}  {m.group(0)}")
            found += 1

print(f"{found} 件")
sys.exit(1 if found else 0)
