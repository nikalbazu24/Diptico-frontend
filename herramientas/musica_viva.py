"""Música viva de Díptico: cada canción en partes (A, B, C y puente) y en dos ánimos (día y noche).
El reproductor las va encadenando en otro orden cada vez, según la hora y la pantalla, con descansos.
Todo se compone y se sintetiza aquí (sin muestras ni melodías de terceros).

Uso: python3 musica_viva.py <carpeta res/raw> [pista ...]
"""
import subprocess
import sys
import wave
import zlib
from pathlib import Path

import numpy as np
from scipy.signal import butter, sosfilt

SR = 44100
OUT = Path(sys.argv[1] if len(sys.argv) > 1 else ".")
SOLO = set(sys.argv[2:])
COMPASES = 8
COLA = 1.2  # segundos de resonancia al final de cada parte


# ---------- Utilidades ----------

def hz(m):
    return 440.0 * 2 ** ((m - 69) / 12)


def tt(n):
    return np.arange(n) / SR


def lp(x, fc, orden=2):
    return sosfilt(butter(orden, min(fc, SR / 2 - 100), "low", fs=SR, output="sos"), x)


def hp(x, fc, orden=2):
    return sosfilt(butter(orden, fc, "high", fs=SR, output="sos"), x)


def bp(x, f0, f1, orden=2):
    return sosfilt(butter(orden, [f0, min(f1, SR / 2 - 100)], "band", fs=SR, output="sos"), x)


def adsr(n, a=0.005, d=0.12, s=0.6, r=0.08):
    t = tt(n)
    dur = n / SR
    e = np.full(n, s, dtype=float)
    e[t < a] = t[t < a] / a
    m = (t >= a) & (t < a + d)
    e[m] = 1 - (1 - s) * (t[m] - a) / d
    m = t >= dur - r
    e[m] *= np.clip((dur - t[m]) / r, 0, 1)
    return e


class Rng:
    def __init__(self, semilla):
        self.r = np.random.default_rng(semilla)

    def elegir(self, l, p=None):
        return l[self.r.choice(len(l), p=p)]

    def u(self, a=0.0, b=1.0):
        return float(self.r.uniform(a, b))

    def ruido(self, n):
        return self.r.uniform(-1, 1, n)


# ---------- Instrumentos (todos sintetizados) ----------

def seno(f, n, fase=0.0):
    return np.sin(2 * np.pi * f * tt(n) + fase)


def piano(f, n, r):
    t = tt(n)
    caida = 2.2 + f / 500
    x = sum(a * np.sin(2 * np.pi * f * k * (1 + 0.0004 * k * k) * t) * np.exp(-t * caida * (0.6 + 0.5 * k))
            for k, a in enumerate((1.0, 0.45, 0.22, 0.12, 0.06), start=1))
    golpe = lp(r.ruido(n), 3000) * np.exp(-t * 60) * 0.08
    return (x + golpe) * np.minimum(t / 0.002, 1) * adsr(n, 0.002, 0.05, 1.0, 0.12)


def pluck(f, n, r, brillo=3.0, caida=4.0):
    t = tt(n)
    x = seno(f, n) + 0.5 * seno(f * 2, n) * np.exp(-t * brillo * 2) + 0.3 * seno(f * 3, n) * np.exp(-t * brillo * 3)
    return x * np.exp(-t * caida) * np.minimum(t / 0.003, 1)


def koto(f, n, r):
    t = tt(n)
    curva = f * (1 + 0.012 * np.exp(-t * 30))
    fase = 2 * np.pi * np.cumsum(curva) / SR
    x = np.sin(fase) + 0.6 * np.sin(2 * fase) * np.exp(-t * 6) + 0.35 * np.sin(3 * fase) * np.exp(-t * 9) + 0.2 * np.sin(5 * fase) * np.exp(-t * 14)
    return x * np.exp(-t * 3.2) * np.minimum(t / 0.002, 1)


def campana(f, n, r, caida=1.8):
    t = tt(n)
    x = seno(f, n) + 0.45 * seno(f * 2.76, n) * np.exp(-t * 5) + 0.25 * seno(f * 5.4, n) * np.exp(-t * 9) + 0.1 * seno(f * 8.9, n) * np.exp(-t * 14)
    return x * np.exp(-t * caida) * np.minimum(t / 0.002, 1)


def marimba(f, n, r):
    t = tt(n)
    x = seno(f, n) * np.exp(-t * 5) + 0.35 * seno(f * 4, n) * np.exp(-t * 22) + 0.1 * seno(f * 9.9, n) * np.exp(-t * 40)
    return x * np.minimum(t / 0.001, 1)


def flauta(f, n, r):
    t = tt(n)
    vib = 1 + 0.006 * np.sin(2 * np.pi * 5.2 * t) * np.clip((t - 0.18) / 0.3, 0, 1)
    fase = 2 * np.pi * np.cumsum(f * vib) / SR
    x = np.sin(fase) + 0.18 * np.sin(2 * fase) + 0.06 * np.sin(3 * fase)
    aire = bp(r.ruido(n), f * 0.9, f * 3) * 0.12
    return (x + aire) * adsr(n, 0.06, 0.1, 0.85, 0.12)


def chip(f, n, r, ciclo=0.25):
    fase = (f * tt(n)) % 1.0
    x = np.where(fase < ciclo, 1.0, -1.0) * 0.6
    return lp(x, 7000) * adsr(n, 0.002, 0.06, 0.55, 0.04)


def triangular(f, n, r):
    fase = (f * tt(n)) % 1.0
    return (4 * np.abs(fase - 0.5) - 1) * adsr(n, 0.004, 0.05, 0.8, 0.05)


def sintetizador(f, n, r):
    t = tt(n)
    x = sum(((f * d * t) % 1.0) * 2 - 1 for d in (1.0, 1.006, 0.994)) / 3
    corte = 900 + 2600 * np.exp(-t * 5)
    # Filtro que se cierra: dos tramos (abierto al golpe, cerrado después).
    y = lp(x, 3200) * np.exp(-t * 5) + lp(x, 1100) * (1 - np.exp(-t * 5))
    return y * adsr(n, 0.01, 0.15, 0.6, 0.12) * (corte > 0)


def acordeon(f, n, r):
    t = tt(n)
    x = sum(((f * d * t) % 1.0) * 2 - 1 for d in (1.0, 1.004, 0.997)) / 3
    trem = 1 + 0.12 * np.sin(2 * np.pi * 5.8 * t)
    return lp(x, 2400) * trem * adsr(n, 0.03, 0.1, 0.8, 0.08)


def guitarra(f, n, r):
    t = tt(n)
    x = sum(a * np.sin(2 * np.pi * f * k * t) * np.exp(-t * (2.5 + 1.6 * k)) for k, a in enumerate((1.0, 0.7, 0.45, 0.3, 0.18, 0.1), start=1))
    return x * np.minimum(t / 0.002, 1) * adsr(n, 0.001, 0.05, 1.0, 0.05)


def colchon(f, n, r):
    t = tt(n)
    x = sum(((f * d * t) % 1.0) * 2 - 1 for d in (1.0, 1.007, 0.993, 2.001))
    return lp(x / 4, 1300) * adsr(n, 0.6, 0.3, 0.8, 0.7)


def organo(f, n, r):
    x = seno(f, n) + 0.5 * seno(2 * f, n) + 0.25 * seno(4 * f, n) + 0.12 * seno(3 * f, n)
    return x * adsr(n, 0.08, 0.1, 0.9, 0.2) * (1 + 0.04 * np.sin(2 * np.pi * 6 * tt(n)))


def bajo_suave(f, n, r):
    return (seno(f, n) + 0.2 * seno(2 * f, n)) * adsr(n, 0.008, 0.15, 0.7, 0.08)


def bajo_sierra(f, n, r):
    x = ((f * tt(n)) % 1.0) * 2 - 1
    return lp(x, 700) * adsr(n, 0.005, 0.1, 0.7, 0.06)


INSTRUMENTOS = {
    "piano": piano, "pluck": pluck, "koto": koto, "campana": campana, "marimba": marimba, "flauta": flauta,
    "chip": chip, "chip12": lambda f, n, r: chip(f, n, r, 0.125), "triangular": triangular, "sinte": sintetizador,
    "acordeon": acordeon, "guitarra": guitarra, "colchon": colchon, "organo": organo,
    "bajo": bajo_suave, "bajo_sierra": bajo_sierra, "bajo_tri": triangular, "bajo_chip": lambda f, n, r: chip(f, n, r, 0.5),
}


# ---------- Percusión ----------

def bombo(r, vol=1.0):
    n = int(0.3 * SR)
    t = tt(n)
    f = 48 + 95 * np.exp(-t * 30)
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 10) * vol


def caja(r, vol=1.0, tono=190):
    n = int(0.22 * SR)
    t = tt(n)
    return (hp(r.ruido(n), 1500) * np.exp(-t * 18) * 0.7 + seno(tono, n) * np.exp(-t * 30) * 0.5) * vol


def platillo(r, vol=1.0, largo=0.06):
    n = int(largo * SR)
    t = tt(n)
    return hp(r.ruido(n), 7000, 4) * np.exp(-t * (4 / largo)) * vol


def palmas(r, vol=1.0):
    n = int(0.12 * SR)
    t = tt(n)
    x = np.zeros(n)
    for k, d in enumerate((0, 0.008, 0.017)):
        i = int(d * SR)
        x[i:] += bp(r.ruido(n - i), 900, 2600) * np.exp(-t[: n - i] * 35) * (1 - 0.2 * k)
    return x * vol


def cascabel(r, vol=1.0):
    n = int(0.16 * SR)
    t = tt(n)
    return (hp(r.ruido(n), 6000) * np.exp(-t * 20) + 0.3 * seno(5200, n) * np.exp(-t * 25)) * vol


def madera(r, vol=1.0, tono=820):
    n = int(0.08 * SR)
    t = tt(n)
    return (seno(tono, n) + 0.4 * seno(tono * 2.3, n)) * np.exp(-t * 45) * vol


def shaker(r, vol=1.0):
    n = int(0.09 * SR)
    t = tt(n)
    return bp(r.ruido(n), 4000, 11000) * np.minimum(t / 0.02, 1) * np.exp(-t * 30) * vol


def cencerro(r, vol=1.0):
    n = int(0.18 * SR)
    t = tt(n)
    return ((((560 * t) % 1) < 0.5) * 2.0 - 1 + (((845 * t) % 1) < 0.5) * 2.0 - 1) * 0.3 * np.exp(-t * 14) * vol


# ---------- La mezcla ----------

class Mezcla:
    def __init__(self, segundos):
        self.n = int((segundos + COLA + 0.5) * SR)
        self.i = np.zeros(self.n)
        self.d = np.zeros(self.n)

    def poner(self, x, t0, vol=1.0, pan=0.0):
        a = int(t0 * SR)
        if a >= self.n:
            return
        b = min(a + len(x), self.n)
        x = x[: b - a] * vol
        self.i[a:b] += x * (1 - max(pan, 0))
        self.d[a:b] += x * (1 + min(pan, 0))

    def eco(self, retardo, fb=0.3, mezcla=0.22):
        k = int(retardo * SR)
        for canal, otro in ((self.i, self.d), (self.d, self.i)):
            y = canal.copy()
            for p in range(1, 5):
                g = mezcla * fb ** (p - 1)
                y[k * p:] += (otro if p % 2 else canal)[: -k * p] * g
            canal[:] = y

    def sala(self, cantidad=0.18):
        # Una sala chica: varios ecos cortos y apagados (sin convolución, para que sea liviano).
        for canal in (self.i, self.d):
            y = canal.copy()
            for ms, g in ((23, 0.5), (37, 0.42), (53, 0.33), (71, 0.26), (97, 0.2), (131, 0.14)):
                k = int(ms / 1000 * SR)
                y[k:] += lp(canal[:-k], 3500, 1) * g * cantidad
            canal[:] = y

    def guardar(self, nombre, segundos, volumen):
        n = int((segundos + COLA) * SR)
        est = np.stack([self.i[:n], self.d[:n]], 1)
        f = int(0.6 * SR)
        est[-f:] *= np.linspace(1, 0, f)[:, None]
        est = np.tanh(est * 1.05)
        est /= np.max(np.abs(est)) / 0.9
        wav = OUT / f"{nombre}.wav"
        with wave.open(str(wav), "wb") as w:
            w.setnchannels(2)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes((est * 32767).astype("<i2").tobytes())
        ogg = OUT / f"{nombre}.ogg"
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav),
                        "-af", f"loudnorm=I={volumen}:TP=-1.5:LRA=9", "-ar", "44100",
                        "-c:a", "libvorbis", "-q:a", "1", "-metadata", "artist=Díptico", str(ogg)], check=True)
        wav.unlink()
        return ogg.stat().st_size


# ---------- Armonía y melodía ----------

ESCALAS = {
    "mayor": [0, 2, 4, 5, 7, 9, 11], "menor": [0, 2, 3, 5, 7, 8, 10], "dorica": [0, 2, 3, 5, 7, 9, 10],
    "mixolidia": [0, 2, 4, 5, 7, 9, 10], "lidia": [0, 2, 4, 6, 7, 9, 11], "armonica": [0, 2, 3, 5, 7, 8, 11],
    "penta": [0, 2, 4, 7, 9], "pentamenor": [0, 3, 5, 7, 10], "in": [0, 1, 5, 7, 8],
}

RITMOS = {
    # Motivos de dos compases: (inicio, largo) en pulsos.
    "tranquilo": [[(0, 1.5), (1.5, 0.5), (2, 2), (4, 1), (5, 1), (6, 2)],
                  [(0, 1), (1, 1), (2, 1.5), (3.5, 0.5), (4, 3), (7, 1)],
                  [(0.5, 0.5), (1, 1), (2, 1), (3, 1), (4, 2), (6, 1), (7, 1)]],
    "saltarin": [[(0, 0.5), (0.5, 0.5), (1, 1), (2, 0.5), (2.5, 0.5), (3, 1), (4, 0.75), (4.75, 0.25), (5, 1), (6, 1), (7, 1)],
                 [(0, 1), (1, 0.5), (1.5, 0.5), (2, 1), (3, 1), (4, 0.5), (4.5, 0.5), (5, 0.5), (5.5, 0.5), (6, 2)],
                 [(0, 0.75), (0.75, 0.25), (1, 0.5), (1.5, 0.5), (2, 1), (3.5, 0.5), (4, 1), (5, 1), (6, 0.5), (6.5, 1.5)]],
    "sincopado": [[(0, 0.75), (0.75, 0.75), (1.5, 1), (2.5, 0.5), (3, 1), (4, 0.5), (4.5, 1), (5.5, 0.5), (6, 2)],
                  [(0.5, 1), (1.5, 0.5), (2, 0.75), (2.75, 1.25), (4.5, 0.5), (5, 1), (6, 1.5), (7.5, 0.5)]],
    "largo": [[(0, 2), (2, 1), (3, 1), (4, 3), (7, 1)], [(0, 3), (3, 1), (4, 2), (6, 2)], [(0, 1), (1, 3), (4, 2), (6, 2)]],
    "tres": [[(0, 1), (1, 0.5), (1.5, 0.5), (2, 1), (3, 1.5), (4.5, 0.5), (5, 1)],
             [(0, 0.5), (0.5, 0.5), (1, 1), (2, 1), (3, 1), (4, 1), (5, 1)],
             [(0, 1.5), (1.5, 0.5), (2, 1), (3, 0.5), (3.5, 0.5), (4, 2)]],
}


class Cancion:
    def __init__(self, receta, parte, animo):
        self.c = receta
        self.parte = parte
        self.animo = animo
        self.r = Rng(zlib.crc32(f"{receta['id']}-{parte}-{animo}".encode()))
        # La semilla del motivo no depende del ánimo: el día y la noche cantan la misma idea.
        self.rm = Rng(zlib.crc32(f"{receta['id']}-motivo".encode()))
        self.bpm = receta["bpm"] * (0.94 if animo == "n" else 1.0)
        self.b = 60 / self.bpm
        self.pc = receta.get("pulsos", 4)
        self.esc = ESCALAS[receta["escala"]]
        self.raiz = receta["raiz"]
        prog = receta["progresiones"]
        self.acordes = {"a": prog[0], "b": prog[1 % len(prog)], "c": prog[0], "p": receta.get("puente", prog[-1])}[parte]

    def nota_de(self, i):
        L = len(self.esc)
        return self.raiz + self.esc[i % L] + 12 * (i // L)

    def acorde(self, compas):
        return self.acordes[compas % len(self.acordes)]

    def cercana(self, i, acorde, rango):
        pcs = {a % 12 for a in acorde}
        cand = [k for k in range(rango[0], rango[1] + 1) if self.nota_de(k) % 12 in pcs]
        return min(cand, key=lambda k: abs(k - i)) if cand else i

    def motivo(self, r, ritmo):
        """Una idea de dos compases (grados de la escala), que después se repite, se mueve y se contesta."""
        L = len(self.esc)
        rango = (L, 2 * L + 2)
        i = L + r.elegir([0, 2, 4]) % L
        notas = []
        sube = r.u() < 0.6
        for k, (ini, largo) in enumerate(ritmo):
            fuerte = abs(ini - round(ini)) < 1e-6 and int(round(ini)) % 2 == 0
            if k == 0 or fuerte:
                i = self.cercana(i + (1 if sube else -1), self.acorde(int(ini // self.pc)), rango)
            else:
                paso = r.elegir([1, 1, 2, -1, -1, 0, 3, -2], None)
                i = max(rango[0], min(rango[1], i + (paso if sube else -paso)))
            if k == len(ritmo) // 2:
                sube = not sube
            notas.append((ini, largo, i))
        return notas

    def melodia(self):
        c = self.c
        ritmos = RITMOS[c["ritmo"]]
        motA = self.motivo(self.rm, self.rm.elegir(ritmos))
        motB = self.motivo(self.rm, self.rm.elegir(ritmos))
        dos = 2 * self.pc
        frase = []
        if self.parte == "p":
            # Puente: notas largas sobre el acorde, con aire.
            for k in range(COMPASES // 2):
                ac = self.acorde(2 * k)
                i = self.cercana(len(self.esc) + 2 * (k % 3), ac, (len(self.esc), 2 * len(self.esc) + 2))
                frase.append((k * dos, dos * 0.75, i))
                if self.r.u() < 0.5:
                    frase.append((k * dos + dos * 0.75, dos * 0.25, i + 1))
            return frase
        base = motA if self.parte in ("a", "c") else motB
        respuesta = motB if self.parte in ("a", "c") else motA
        for bloque, (mot, despl) in enumerate(((base, 0), (base, 2 if self.parte != "c" else 1), (respuesta, 0), (base, -1))):
            for ini, largo, i in mot:
                t = bloque * dos + ini
                if bloque == 3 and ini >= dos * 0.5:
                    break
                frase.append((t, largo, i + despl))
        # Cierre en la tónica, largo.
        frase.append((3 * dos + dos * 0.5, dos * 0.5, len(self.esc)))
        if self.parte == "c":
            # C: la misma idea, más arriba y con adornos rápidos.
            nueva = []
            for t, largo, i in frase:
                if largo >= 1 and self.r.u() < 0.4:
                    nueva += [(t, 0.25, i + 1), (t + 0.25, largo - 0.25, i + len(self.esc) // 2)]
                else:
                    nueva.append((t, largo, i + len(self.esc) // 2))
            frase = nueva
        return frase

    def componer(self, nombre):
        c = self.c
        dia = self.animo == "d"
        total = COMPASES * self.pc * self.b
        m = Mezcla(total)
        b = self.b
        r = self.r
        # 1) Acompañamiento: acordes (o colchón de noche).
        for comp in range(COMPASES):
            t0 = comp * self.pc * b
            ac = self.acorde(comp)
            estilo = c["acomp_dia"] if dia else c["acomp_noche"]
            if estilo == "colchon" and comp % 2 == 0:
                n = int(2 * self.pc * b * SR)
                for k, nota in enumerate(ac):
                    m.poner(colchon(hz(nota), n, r), t0, 0.05, pan=-0.3 + 0.3 * k)
            elif estilo == "organo" and comp % 2 == 0:
                n = int(2 * self.pc * b * SR)
                for k, nota in enumerate(ac):
                    m.poner(organo(hz(nota), n, r), t0, 0.045, pan=-0.2 + 0.2 * k)
            elif estilo == "rasgueo":
                golpes = [0, 1, 1.5, 2, 2.5] if self.pc == 3 else [0, 1, 1.5, 2.5, 3]
                for g in golpes:
                    n = int(0.9 * b * SR)
                    for k, nota in enumerate(ac + [ac[0] + 12]):
                        m.poner(guitarra(hz(nota), n, r), t0 + g * b + k * 0.012, 0.05 * (1.0 if g in (0, 2) else 0.7), pan=-0.25)
            elif estilo in ("arpegio", "arpegio16", "arpegio_lento"):
                paso = {"arpegio": 0.5, "arpegio16": 0.25, "arpegio_lento": 1.0}[estilo]
                inst = INSTRUMENTOS[c.get("arpegio_inst", "pluck")]
                orden = [0, 1, 2, 1] if len(ac) < 4 else [0, 1, 2, 3, 2, 1]
                k = 0
                g = 0.0
                while g < self.pc - 1e-6:
                    nota = ac[orden[k % len(orden)]] + 12
                    n = int(min(paso * 2.2, 1.6) * b * SR)
                    m.poner(inst(hz(nota), n, r), t0 + g * b, 0.06 if dia else 0.045, pan=0.3 if k % 2 else -0.3)
                    g += paso
                    k += 1
            elif estilo == "acordes":
                inst = INSTRUMENTOS[c.get("acordes_inst", "piano")]
                for g, largo in ((0, 1.5), (self.pc / 2 + 0.5 if self.pc == 4 else 2, 1.5)):
                    n = int(largo * b * SR)
                    for k, nota in enumerate(ac):
                        m.poner(inst(hz(nota), n, r), t0 + g * b + k * 0.006, 0.045, pan=-0.2 + 0.15 * k)
        # 2) Bajo.
        bajo = INSTRUMENTOS[c.get("bajo", "bajo")]
        estilo_bajo = c["bajo_dia"] if dia else c.get("bajo_noche", "largo")
        for comp in range(COMPASES):
            t0 = comp * self.pc * b
            raiz = self.acorde(comp)[0] - 24
            while raiz < 33:
                raiz += 12
            golpes = {
                "raiz": [(0, 1.8, 0), (2, 1.8, 7)] if self.pc == 4 else [(0, 1.8, 0), (2, 0.9, 7)],
                "octavas": [(k * 0.5, 0.45, 12 if k % 2 else 0) for k in range(self.pc * 2)],
                "caminante": [(0, 0.9, 0), (1, 0.9, 4), (2, 0.9, 7), (3, 0.9, 9)][: self.pc],
                "largo": [(0, self.pc * 0.95, 0)],
                "saltado": [(0, 0.7, 0), (1.5, 0.5, 0), (2, 0.7, 7), (3.5, 0.4, 12)][: max(2, self.pc)],
                "cueca": [(0, 0.9, 0), (1.5, 0.5, 7), (2, 0.9, 0)],
            }[estilo_bajo]
            for g, largo, sem in golpes:
                n = int(largo * b * SR)
                m.poner(bajo(hz(raiz + sem), n, r), t0 + g * b, 0.24 if dia else 0.18)
        # 3) Batería (solo de día, salvo que la receta pida algo suave de noche).
        bat = c["bateria"] if dia else c.get("bateria_noche", "nada")
        if self.parte == "p" and bat not in ("nada", "latido"):
            bat = "suave"
        for comp in range(COMPASES):
            t0 = comp * self.pc * b
            if bat == "lofi":
                m.poner(bombo(r), t0, 0.5)
                m.poner(bombo(r, 0.7), t0 + 2.58 * b, 0.42)
                m.poner(caja(r, 0.6), t0 + b, 0.18)
                m.poner(caja(r, 0.6), t0 + 3 * b, 0.18)
                for k in range(8):
                    m.poner(platillo(r, 0.8 if k % 2 == 0 else 0.5), t0 + (k * 0.5 + (0.08 if k % 2 else 0)) * b, 0.07, pan=0.3)
            elif bat == "chip":
                for k in range(self.pc):
                    m.poner(bombo(r, 0.8) if k % 2 == 0 else caja(r, 0.7, 260), t0 + k * b, 0.35 if k % 2 == 0 else 0.16)
                for k in range(self.pc * 2):
                    m.poner(platillo(r, 0.7, 0.03), t0 + k * 0.5 * b, 0.06, pan=0.25)
            elif bat == "synth":
                m.poner(bombo(r), t0, 0.55)
                m.poner(bombo(r), t0 + 2 * b, 0.5)
                m.poner(caja(r, 1.0, 170), t0 + b, 0.22)
                m.poner(caja(r, 1.0, 170), t0 + 3 * b, 0.22)
                for k in range(8):
                    m.poner(platillo(r, 0.6, 0.05), t0 + k * 0.5 * b, 0.05, pan=-0.2)
            elif bat == "folk":
                m.poner(bombo(r, 0.6), t0, 0.35)
                for k in range(self.pc):
                    m.poner(shaker(r), t0 + (k + 0.5) * b, 0.12, pan=0.3)
                if self.pc == 4:
                    m.poner(palmas(r), t0 + 3 * b, 0.12, pan=-0.2)
            elif bat == "fiesta":
                for k in range(self.pc):
                    m.poner(bombo(r, 0.8), t0 + k * b, 0.38)
                    m.poner(shaker(r), t0 + (k + 0.5) * b, 0.14, pan=0.3)
                m.poner(caja(r, 0.8), t0 + 1.5 * b, 0.16)
                m.poner(caja(r, 0.8), t0 + 3 * b, 0.16)
                m.poner(cencerro(r), t0 + 0.5 * b, 0.08, pan=-0.35)
                m.poner(cencerro(r, 0.7), t0 + 2.5 * b, 0.07, pan=-0.35)
            elif bat == "cueca":
                # Palmas que marcan el 6/8 (1 y 3 del compás de tres, con el «y» del 2).
                for g in (0, 1.5, 2):
                    m.poner(palmas(r), t0 + g * b, 0.16 if g == 0 else 0.11, pan=0.2)
                m.poner(bombo(r, 0.6), t0, 0.3)
            elif bat == "suave":
                for k in range(self.pc):
                    m.poner(shaker(r, 0.6), t0 + (k + 0.5) * b, 0.06, pan=0.3)
            elif bat == "latido":
                m.poner(bombo(r, 0.5), t0, 0.32)
                m.poner(bombo(r, 0.35), t0 + 0.45 * b, 0.24)
            elif bat == "madera":
                for k in (0, 1.5, 2.5) if self.pc == 4 else (0, 2):
                    m.poner(madera(r), t0 + k * b, 0.1, pan=0.3)
            elif bat == "cascabeles":
                for k in range(self.pc * 4):
                    m.poner(cascabel(r, 0.9 if k % 4 == 0 else 0.5), t0 + k * 0.25 * b, 0.05, pan=0.35)
                m.poner(bombo(r, 0.6), t0, 0.3)
                m.poner(bombo(r, 0.6), t0 + 2 * b, 0.28)
        # 4) Melodía: de día el instrumento principal; de noche, el suave, más bajo y con notas más largas.
        inst = INSTRUMENTOS[c["melodia_dia"] if dia else c["melodia_noche"]]
        octava = c.get("octava", 0) + (0 if dia else c.get("octava_noche", -12))
        frase = self.melodia()
        if not dia and self.parte != "p":
            # De noche se juntan las notas cortas: la misma idea, más despacio.
            frase = [(t, max(largo, 1.0), i) for (t, largo, i) in frase if abs(t * 2 - round(t * 2)) < 1e-6 and (largo >= 0.75 or int(t * 2) % 2 == 0)]
        vol = c.get("vol_melodia", 0.1) * (0.85 if not dia else 1.0)
        for t, largo, i in frase:
            n = int(min(largo, 4) * b * SR * (1.25 if c["melodia_dia"] in ("campana", "koto", "pluck", "marimba", "piano") else 1.0))
            m.poner(inst(hz(self.nota_de(i) + octava), max(n, 400), r), t * b, vol, pan=0.08)
        # 5) Adornos y ambiente.
        if dia and c.get("adorno") and self.parte in ("b", "c"):
            inst = INSTRUMENTOS[c["adorno"]]
            for comp in range(1, COMPASES, 2):
                ac = self.acorde(comp)
                for k in range(3):
                    n = int(1.2 * b * SR)
                    m.poner(inst(hz(ac[k % len(ac)] + 24), n, r), (comp * self.pc + 2 + k * 0.33) * b, 0.035, pan=0.4)
        amb = c.get("ambiente", "")
        if amb == "olas" or (amb == "olas_noche" and not dia):
            n = int(total * SR)
            t = tt(n)
            olas = lp(r.ruido(n), 700) * (0.5 + 0.5 * np.sin(2 * np.pi * t / 7.5)) ** 2
            m.poner(olas, 0, 0.12)
        if amb == "grillos" and not dia:
            for k in range(int(total / 0.9)):
                if r.u() < 0.6:
                    n = int(0.05 * SR)
                    x = seno(4600 + 300 * r.u(), n) * np.exp(-tt(n) * 60)
                    for rep in range(3):
                        m.poner(x, k * 0.9 + rep * 0.06, 0.02, pan=r.u(-0.6, 0.6))
        if amb == "viento":
            n = int(total * SR)
            t = tt(n)
            m.poner(bp(r.ruido(n), 300, 1400) * (0.4 + 0.6 * np.sin(2 * np.pi * t / 11) ** 2), 0, 0.045)
        m.sala(c.get("sala", 0.18))
        if c.get("eco"):
            m.eco(b * c["eco"], 0.3, 0.18)
        return m.guardar(nombre, total, -18 if dia else -21)


# ---------- Las recetas: cada canción con su carácter ----------

def ac(*notas):
    return list(notas)


RECETAS = [
    dict(id="bisagra", bpm=82, escala="mayor", raiz=48, ritmo="sincopado",
         progresiones=[[ac(53, 57, 60, 64), ac(52, 55, 59, 62), ac(50, 53, 57, 60), ac(48, 52, 55, 59)],
                       [ac(50, 53, 57, 60), ac(55, 59, 62, 65), ac(48, 52, 55, 59), ac(45, 48, 52, 55)]],
         puente=[ac(53, 57, 60, 64), ac(48, 52, 55, 59)],
         acomp_dia="acordes", acordes_inst="piano", acomp_noche="colchon", bajo="bajo", bajo_dia="raiz", bajo_noche="largo",
         bateria="lofi", melodia_dia="piano", melodia_noche="campana", octava=0, octava_noche=0, adorno="chip", vol_melodia=0.09, eco=0.75),
    dict(id="cartucho", bpm=126, escala="mayor", raiz=60, ritmo="saltarin",
         progresiones=[[ac(48, 52, 55), ac(43, 47, 50), ac(45, 48, 52), ac(41, 45, 48)],
                       [ac(41, 45, 48), ac(43, 47, 50), ac(40, 43, 47), ac(45, 48, 52)]],
         acomp_dia="arpegio16", arpegio_inst="chip12", acomp_noche="arpegio", bajo="bajo_chip", bajo_dia="octavas", bajo_noche="raiz",
         bateria="chip", bateria_noche="suave", melodia_dia="chip", melodia_noche="triangular", octava=12, octava_noche=0, vol_melodia=0.07),
    dict(id="medianoche", bpm=68, escala="dorica", raiz=62, ritmo="largo",
         progresiones=[[ac(50, 53, 57, 60), ac(55, 59, 62, 65)], [ac(48, 52, 55, 59), ac(53, 57, 60, 64)]],
         acomp_dia="colchon", acomp_noche="colchon", bajo="bajo", bajo_dia="largo", bateria="suave", melodia_dia="campana",
         melodia_noche="campana", octava=0, octava_noche=-12, adorno="campana", vol_melodia=0.08, eco=1.0, sala=0.3, ambiente="viento"),
    dict(id="fantasma", bpm=92, escala="armonica", raiz=62, ritmo="tranquilo",
         progresiones=[[ac(50, 53, 57), ac(46, 50, 53), ac(43, 46, 50), ac(45, 49, 52)],
                       [ac(43, 46, 50), ac(50, 53, 57), ac(46, 50, 53), ac(45, 49, 52)]],
         acomp_dia="arpegio", arpegio_inst="campana", acomp_noche="organo", bajo="bajo", bajo_dia="largo", bateria="latido",
         bateria_noche="latido", melodia_dia="flauta", melodia_noche="organo", octava=0, octava_noche=0, vol_melodia=0.08, eco=0.5),
    dict(id="nieve", bpm=100, escala="mayor", raiz=67, ritmo="tranquilo",
         progresiones=[[ac(55, 59, 62), ac(52, 55, 59), ac(48, 52, 55), ac(50, 54, 57)],
                       [ac(48, 52, 55), ac(50, 54, 57), ac(55, 59, 62), ac(52, 55, 59)]],
         acomp_dia="arpegio", arpegio_inst="pluck", acomp_noche="colchon", bajo="bajo_tri", bajo_dia="raiz", bateria="cascabeles",
         bateria_noche="nada", melodia_dia="campana", melodia_noche="campana", octava=0, octava_noche=-12, adorno="campana", vol_melodia=0.08, eco=0.5),
    dict(id="sakura", bpm=70, escala="in", raiz=62, ritmo="largo",
         progresiones=[[ac(50, 57, 62), ac(51, 55, 58)], [ac(55, 62, 67), ac(50, 57, 62)]],
         acomp_dia="arpegio", arpegio_inst="koto", acomp_noche="arpegio_lento", bajo="bajo", bajo_dia="largo", bateria="madera",
         melodia_dia="flauta", melodia_noche="flauta", octava=0, octava_noche=-12, adorno="koto", vol_melodia=0.09, sala=0.3, eco=0.75),
    dict(id="neon", bpm=100, escala="menor", raiz=57, ritmo="sincopado",
         progresiones=[[ac(45, 48, 52), ac(41, 45, 48), ac(48, 52, 55), ac(43, 47, 50)],
                       [ac(41, 45, 48), ac(43, 47, 50), ac(45, 48, 52), ac(45, 48, 52)]],
         acomp_dia="arpegio16", arpegio_inst="chip", acomp_noche="colchon", bajo="bajo_sierra", bajo_dia="octavas", bajo_noche="largo",
         bateria="synth", melodia_dia="sinte", melodia_noche="colchon", octava=12, octava_noche=0, vol_melodia=0.09, eco=0.75),
    dict(id="oceano", bpm=76, escala="penta", raiz=64, ritmo="tranquilo",
         progresiones=[[ac(52, 56, 59), ac(49, 52, 56), ac(45, 49, 52), ac(47, 51, 54)],
                       [ac(45, 49, 52), ac(47, 51, 54), ac(52, 56, 59), ac(52, 56, 59)]],
         acomp_dia="arpegio", arpegio_inst="marimba", acomp_noche="colchon", bajo="bajo", bajo_dia="raiz", bateria="suave",
         melodia_dia="marimba", melodia_noche="campana", octava=0, octava_noche=-12, vol_melodia=0.1, ambiente="olas", sala=0.25),
    dict(id="bosque", bpm=96, escala="mixolidia", raiz=67, ritmo="saltarin",
         progresiones=[[ac(55, 59, 62), ac(53, 57, 60), ac(48, 52, 55), ac(55, 59, 62)],
                       [ac(48, 52, 55), ac(50, 53, 57), ac(53, 57, 60), ac(55, 59, 62)]],
         acomp_dia="rasgueo", acomp_noche="arpegio", arpegio_inst="guitarra", bajo="bajo", bajo_dia="caminante", bajo_noche="largo",
         bateria="folk", melodia_dia="flauta", melodia_noche="flauta", octava=0, octava_noche=-12, vol_melodia=0.09, ambiente="grillos"),
    dict(id="portatil", bpm=138, escala="menor", raiz=64, ritmo="saltarin",
         progresiones=[[ac(52, 55, 59), ac(48, 52, 55), ac(50, 54, 57), ac(47, 50, 54)],
                       [ac(48, 52, 55), ac(50, 54, 57), ac(52, 55, 59), ac(52, 55, 59)]],
         acomp_dia="arpegio16", arpegio_inst="chip", acomp_noche="arpegio", bajo="bajo_tri", bajo_dia="octavas", bajo_noche="raiz",
         bateria="chip", melodia_dia="chip12", melodia_noche="triangular", octava=12, octava_noche=0, vol_melodia=0.07),
    dict(id="fiesta", bpm=116, escala="mayor", raiz=65, ritmo="saltarin",
         progresiones=[[ac(53, 57, 60), ac(46, 50, 53), ac(48, 52, 55), ac(53, 57, 60)],
                       [ac(50, 53, 57), ac(46, 50, 53), ac(48, 52, 55), ac(48, 52, 55)]],
         acomp_dia="acordes", acordes_inst="marimba", acomp_noche="arpegio", arpegio_inst="marimba", bajo="bajo", bajo_dia="saltado",
         bateria="fiesta", bateria_noche="suave", melodia_dia="acordeon", melodia_noche="marimba", octava=0, octava_noche=0, vol_melodia=0.08),
    dict(id="cueca", bpm=104, pulsos=3, escala="mayor", raiz=62, ritmo="tres",
         progresiones=[[ac(50, 54, 57), ac(45, 49, 52), ac(45, 49, 52), ac(50, 54, 57)],
                       [ac(55, 59, 62), ac(50, 54, 57), ac(45, 49, 52), ac(50, 54, 57)]],
         acomp_dia="rasgueo", acomp_noche="arpegio", arpegio_inst="guitarra", bajo="bajo", bajo_dia="cueca", bajo_noche="largo",
         bateria="cueca", melodia_dia="acordeon", melodia_noche="guitarra", octava=0, octava_noche=0, vol_melodia=0.08),
    dict(id="bloques", bpm=72, escala="lidia", raiz=60, ritmo="tranquilo",
         progresiones=[[ac(48, 52, 55, 59), ac(50, 54, 57, 60)], [ac(45, 48, 52, 55), ac(50, 54, 57, 60)]],
         acomp_dia="acordes", acordes_inst="piano", acomp_noche="arpegio_lento", arpegio_inst="piano", bajo="bajo", bajo_dia="largo",
         bateria="madera", melodia_dia="piano", melodia_noche="piano", octava=12, octava_noche=0, vol_melodia=0.09, sala=0.3),
]


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    total = 0
    for receta in RECETAS:
        if SOLO and receta["id"] not in SOLO:
            continue
        for parte in "abcp":
            for animo in "dn":
                nombre = f"viva_{receta['id']}_{parte}_{animo}"
                total += Cancion(receta, parte, animo).componer(nombre)
        print(receta["id"], "listo", total // 1024, "KB en total")
