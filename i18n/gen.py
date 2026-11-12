"""Turn compact translations into Android strings.xml, with validation.
Usage: python i18n_gen.py <res_dir> <translations.txt>...
Translation files: '@<lang>' starts a language, then 'key=value' lines. Blank lines and '#' comments ignored.
Fails (writes nothing) if any language misses a key, has an unknown key, or changes the %1$s/%1$d placeholders."""
import io, re, sys
from xml.sax.saxutils import escape

QUALIFIER = {"id": "in", "zh-CN": "zh-rCN", "zh-TW": "zh-rTW", "pt-BR": "pt-rBR"}
NOT_TRANSLATED = {"app_name"}
PH = re.compile(r"%\d+\$[sd]")

res, files = sys.argv[1], sys.argv[2:]
entries = re.findall(r'<string name="([^"]+)"([^>]*)>(.*?)</string>', io.open(f"{res}/values/strings.xml", encoding="utf-8").read(), re.S)
english = {k: v for k, attrs, v in entries if 'translatable="false"' not in attrs}
keys = [k for k in english if k not in NOT_TRANSLATED]

langs, cur = {}, None
for f in files:
    for n, line in enumerate(io.open(f, encoding="utf-8").read().splitlines(), 1):
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("@"):
            cur = line[1:].strip(); langs.setdefault(cur, {}); continue
        k, sep, v = line.partition("=")
        if not sep or cur is None:
            sys.exit(f"{f}:{n}: bad line: {line!r}")
        langs[cur][k.strip()] = v.strip()

errors = []
for lang, t in langs.items():
    for k in keys:
        if k not in t:
            errors.append(f"{lang}: missing {k}")
        elif sorted(PH.findall(t[k])) != sorted(PH.findall(english[k])):
            errors.append(f"{lang}: placeholders differ in {k}: {t[k]!r}")
    errors += [f"{lang}: unknown key {k}" for k in t if k not in english]
if errors:
    sys.exit("\n".join(errors))

def android(v):
    v = escape(v).replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"')
    return "\\" + v if v[:1] in "@?" else v

import os
for lang, t in langs.items():
    d = f"{res}/values-{QUALIFIER.get(lang, lang)}"
    os.makedirs(d, exist_ok=True)
    # A literal % (e.g. "100 %") isn't a format code; tell Android so it isn't parsed as one.
    attr = lambda v: ' formatted="false"' if "%" in v and not PH.search(v) else ""
    body = "\n".join(f'    <string name="{k}"{attr(t[k])}>{android(t[k])}</string>' for k in keys)
    io.open(f"{d}/strings.xml", "w", encoding="utf-8", newline="\n").write(f"<resources>\n{body}\n</resources>\n")
print(f"wrote {len(langs)} languages x {len(keys)} strings: {', '.join(sorted(langs))}")
