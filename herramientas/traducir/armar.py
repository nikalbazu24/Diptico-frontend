"""Arma assets/idiomas/<código>.json a partir de las traducciones.

t1..t3.tsv: número (de base1.json) <tab> en <tab> pt <tab> zh <tab> ja
extras.tsv: texto en español <tab> en <tab> pt <tab> zh <tab> ja
"""
import glob, json, os, re

AQUI = os.path.dirname(os.path.abspath(__file__))
DESTINO = os.path.join(AQUI, "..", "..", "app", "src", "main", "assets", "idiomas")
IDIOMAS = ["en", "pt", "zh", "ja"]

base = json.load(open(os.path.join(AQUI, "base1.json"), encoding="utf-8"))
dic = {c: {} for c in IDIOMAS}
problemas = []

def agregar(es, cols, origen):
    if len(cols) != 4:
        problemas.append(f"{origen}: {len(cols)} columnas"); return
    marcas = sorted(re.findall(r"\{\d+\}", es))
    for c, t in zip(IDIOMAS, cols):
        t = t.replace("\\n", "\n")
        if sorted(re.findall(r"\{\d+\}", t)) != marcas:
            problemas.append(f"{origen} [{c}]: marcas distintas: {es!r} -> {t!r}")
            continue
        if t != es:
            dic[c][es] = t

for f in sorted(glob.glob(os.path.join(AQUI, "t*.tsv"))):
    for n, linea in enumerate(open(f, encoding="utf-8").read().split("\n"), 1):
        if not linea.strip(): continue
        partes = linea.split("\t")
        i = int(partes[0])
        agregar(base[i], partes[1:], f"{os.path.basename(f)}:{n}")

for ex in sorted(glob.glob(os.path.join(AQUI, "extras*.tsv"))):
    for n, linea in enumerate(open(ex, encoding="utf-8").read().split("\n"), 1):
        if not linea.strip(): continue
        partes = linea.split("\t")
        agregar(partes[0], partes[1:], f"{os.path.basename(ex)}:{n}")

os.makedirs(DESTINO, exist_ok=True)
for c in IDIOMAS:
    json.dump(dic[c], open(os.path.join(DESTINO, c + ".json"), "w", encoding="utf-8"), ensure_ascii=False, indent=0, sort_keys=True)
    print(c, len(dic[c]), "textos")
for p in problemas: print("PROBLEMA", p)
