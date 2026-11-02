"""Saca los textos visibles de Díptico (en español) para traducirlos.

Uso: python3 herramientas/textos.py  -> escribe herramientas/textos_es.json con la lista de textos.
Los textos con ${...} o $nombre quedan como plantillas con {0}, {1}...
"""
import json, os, re, sys

RAIZ = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "java", "cl", "nikalbazu", "diptico")
# Archivos que solo hablan con internet o con la IA: sus textos no se muestran.
SALTAR = {"Almacen.kt", "Idioma.kt", "Biblioteca.kt"}

def literales(codigo):
    """Recorre el código y devuelve los literales "..." (no los de triple comilla), respetando ${...} anidados."""
    i, n = 0, len(codigo)
    salida = []
    pendiente = []
    salida_pend = None
    while i < n:
        c = codigo[i]
        if codigo.startswith("//", i):
            i = codigo.find("\n", i)
            if i < 0: break
            continue
        if codigo.startswith("/*", i):
            prof, i = 1, i + 2
            while i < n and prof:
                if codigo.startswith("/*", i): prof += 1; i += 2
                elif codigo.startswith("*/", i): prof -= 1; i += 2
                else: i += 1
            continue
        if codigo.startswith('"""', i):
            j = codigo.find('"""', i + 3)
            i = j + 3 if j >= 0 else n
            continue
        if c == "'":
            j = i + 1
            while j < n and codigo[j] != "'":
                j += 2 if codigo[j] == "\\" else 1
            i = j + 1
            continue
        if c == '"':
            j = i + 1
            partes = []  # (texto, es_expresion)
            buf = ""
            while j < n and codigo[j] != '"':
                ch = codigo[j]
                if ch == "\\":
                    sig = codigo[j + 1]
                    buf += {"n": "\n", "t": "\t", '"': '"', "\\": "\\", "$": "$", "'": "'"}.get(sig, sig)
                    j += 2
                    continue
                if ch == "$" and j + 1 < n and codigo[j + 1] == "{":
                    prof, k = 1, j + 2
                    while k < n and prof:
                        if codigo[k] == "{": prof += 1
                        elif codigo[k] == "}": prof -= 1
                        elif codigo[k] == '"':
                            # string dentro de la expresión
                            k += 1
                            while k < n and codigo[k] != '"':
                                k += 2 if codigo[k] == "\\" else 1
                        k += 1
                    partes.append((buf, False)); buf = ""
                    partes.append((codigo[j + 2:k - 1], True))
                    j = k
                    continue
                if ch == "$" and j + 1 < n and (codigo[j + 1].isalpha() or codigo[j + 1] == "_"):
                    m = re.match(r"[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)*", codigo[j + 1:])
                    partes.append((buf, False)); buf = ""
                    partes.append((m.group(0), True))
                    j += 1 + len(m.group(0))
                    continue
                buf += ch
                j += 1
            partes.append((buf, False))
            # "texto " +\n "más texto": se une en un solo texto (así se ve en pantalla).
            m = re.match(r'\s*\+\s*"(?!"")', codigo[j + 1:])
            if m and salida_pend is not None:
                pass
            if m:
                pendiente.extend(partes)
                i = j + 1 + m.end() - 1
                continue
            salida.append(pendiente + partes)
            pendiente = []
            i = j + 1
            continue
        i += 1
    return salida

def plantilla(partes):
    t, k = "", 0
    for texto, expr in partes:
        if expr:
            t += "{%d}" % k; k += 1
        else:
            t += texto.replace("{", "{{").replace("}", "}}") if False else texto
    return t, k

# Palabras sueltas en minúscula que sí se ven en pantalla (el filtro de abajo las confunde con identificadores).
VISIBLES = {"jugadas", "partidas", "puntos", "jugado"}

def visible(t):
    s = re.sub(r"\{\d+\}", "", t).strip()
    if t in VISIBLES: return True
    # "{0} juegos", "{0} jugadas": número + palabra en minúscula = texto para la pantalla.
    if re.fullmatch(r"\{\d+\} [a-záéíóúñ]+", t): return True
    if len(s) < 2 or len(t) > 420: return False
    if not re.search(r"[A-Za-zÁÉÍÓÚáéíóúÑñ]", s): return False
    if re.match(r"^(https?:|content:|app:|file:|android\.|image/|audio/|video/|vnd\.|application/|primary:|[a-z]+/[a-z*])", s): return False
    if re.search(r"[\\^\[\]]|\(\?|\.\*", t): return False   # regex
    # identificadores: todo minúscula sin espacios ni tildes (ids, claves, extensiones)
    if re.fullmatch(r"[a-z0-9_\-.:/#%]+", s): return False
    if re.fullmatch(r"[A-Z0-9_]+", s) and len(s) > 1 and s not in ("A", "B", "X", "Y"): return False  # CONSTANTES
    if re.fullmatch(r"[A-Za-z]+\.[A-Za-z.]+", s): return False  # paquetes
    if re.fullmatch(r"[a-z]+[A-Z][A-Za-z0-9]*", s): return False  # camelCase
    if s.startswith("%") or "%0" in s or "%s" in s or "%d" in s: return False
    return True

def main():
    vistos = {}
    for raiz, _, archivos in os.walk(RAIZ):
        for a in sorted(archivos):
            if not a.endswith(".kt") or a in SALTAR: continue
            codigo = open(os.path.join(raiz, a), encoding="utf-8").read()
            for partes in literales(codigo):
                t, k = plantilla(partes)
                if visible(t):
                    vistos.setdefault(t, a)
    salida = sorted(vistos)
    destino = os.path.join(os.path.dirname(__file__), "textos_es.json")
    json.dump(salida, open(destino, "w", encoding="utf-8"), ensure_ascii=False, indent=0)
    print(len(salida), "textos ->", destino, file=sys.stderr)

if __name__ == "__main__":
    main()
