"""Ocho temas más de Díptico, uno por cada estilo de tema (bucles sin cortes)."""
import sys
import numpy as np
import componer as C
from componer import SR, hz, env, pulso, triangulo, seno, sierra, pasabajos, pasaaltos, ruido, Mezcla, bombo, caja, platillo

rng = np.random.default_rng(11)


def pluck(f, n, brillo=3.0, caida=4.0):
    t = np.arange(n) / SR
    x = seno(f, n) + 0.5 * seno(f * 2, n) * np.exp(-t * brillo * 2) + 0.25 * seno(f * 3, n) * np.exp(-t * brillo * 3)
    return x * np.exp(-t * caida) * np.minimum(t / 0.003, 1)


def campana(f, n, caida=1.8):
    t = np.arange(n) / SR
    x = seno(f, n) + 0.45 * seno(f * 2.76, n) * np.exp(-t * 5) + 0.25 * seno(f * 5.4, n) * np.exp(-t * 9)
    return x * np.exp(-t * caida) * np.minimum(t / 0.003, 1)


def melodia(m, base, b, frase, fn, vol, pan=0.1):
    for (beat, largo, nota) in frase:
        n = int(largo * b * SR)
        m.poner(fn(hz(nota), n), base + beat * b, vol, pan=pan)


# ---------- Fantasma (Halloween, Día de Muertos) ----------
def fantasma():
    bpm = 96; b = 60 / bpm; compases = 24; total = compases * 4 * b
    m = Mezcla(total)
    acordes = [[50, 53, 57], [46, 50, 53], [43, 46, 50], [45, 49, 52]]  # Dm Bb Gm A
    for c in range(compases):
        t0 = c * 4 * b
        ac = acordes[c % 4]
        for k in range(8):  # celesta en corcheas
            nota = ac[[0, 1, 2, 1][k % 4]] + 24
            n = int(0.9 * b * SR)
            m.poner(campana(hz(nota), n, 3.5), t0 + k * b / 2, 0.05, pan=-0.3 if k % 2 else 0.3)
        n = int(3.9 * b * SR)  # órgano
        org = sum(pulso(hz(x + 12), n, 0.5) for x in ac)
        m.poner(pasabajos(org, 900) * env(n, 0.3, 0.4, 0.8, 0.6), t0, 0.03)
        nb = int(1.8 * b * SR)
        for g in (0, 2):
            m.poner(triangulo(hz(ac[0] - 12), nb) * env(nb, 0.01, 0.3, 0.6, 0.2), t0 + g * b, 0.3)
        if c >= 4:
            m.poner(bombo(0.8), t0, 0.45); m.poner(bombo(0.6), t0 + 2.5 * b, 0.4)
            m.poner(caja(0.5, 170), t0 + 3 * b, 0.25)
    def teremin(f, n):
        t = np.arange(n) / SR
        fase = 2 * np.pi * np.cumsum(f * (1 + 0.012 * np.sin(2 * np.pi * 5.5 * t))) / SR
        return np.sin(fase) * env(n, 0.12, 0.3, 0.8, 0.3)
    frase = [(0, 2, 74), (2, 1, 77), (3, 1, 76), (4, 3, 74), (8, 1.5, 70), (9.5, 0.5, 69), (10, 2, 67), (12, 4, 69)]
    for bloque in (1, 2, 4, 5):
        melodia(m, bloque * 16 * b, b, frase if bloque % 2 else [(x, y, z + (3 if i % 3 == 0 else 0)) for i, (x, y, z) in enumerate(frase)], teremin, 0.06)
    m.eco(b * 0.75, 0.45, 0.28)
    m.terminar(total, "diptico_fantasma", "Fantasma")


# ---------- Nieve (Navidad, Año Nuevo) ----------
def nieve():
    bpm = 104; b = 60 / bpm; compases = 24; total = compases * 4 * b
    m = Mezcla(total)
    acordes = [[60, 64, 67], [57, 60, 64], [53, 57, 60], [55, 59, 62]]
    for c in range(compases):
        t0 = c * 4 * b
        ac = acordes[c % 4]
        for k, g in enumerate((0, 1, 2, 3)):
            n = int(0.95 * b * SR)
            m.poner(pluck(hz(ac[k % 3] + 12), n, 2, 3), t0 + g * b, 0.08, pan=-0.25)
        nb = int(1.9 * b * SR)
        m.poner(triangulo(hz(ac[0] - 12), nb) * env(nb, 0.01, 0.2, 0.7, 0.2), t0, 0.28)
        m.poner(triangulo(hz(ac[2] - 12), nb) * env(nb, 0.01, 0.2, 0.7, 0.2), t0 + 2 * b, 0.25)
        if c >= 2:
            for k in range(16):  # cascabeles
                m.poner(platillo(0.9 if k % 4 == 0 else 0.5, 0.05), t0 + k * b / 4, 0.09, pan=0.35)
            m.poner(bombo(0.7), t0, 0.35); m.poner(bombo(0.7), t0 + 2 * b, 0.35)
    frase = [(0, 1, 76), (1, 1, 76), (2, 2, 76), (4, 1, 76), (5, 1, 76), (6, 2, 76), (8, 1, 76), (9, 1, 79), (10, 1.5, 72), (11.5, 0.5, 74), (12, 4, 76)]
    frase2 = [(0, 1, 77), (1, 1, 77), (2, 1.5, 77), (3.5, 0.5, 77), (4, 1, 77), (5, 1, 76), (6, 1, 76), (7, 1, 76), (8, 1, 74), (9, 1, 74), (10, 1, 76), (11, 1, 74), (12, 4, 79)]
    for bloque in range(1, 6):
        melodia(m, bloque * 16 * b, b, frase if bloque % 2 else frase2, lambda f, n: campana(f, n, 1.4), 0.075)
    m.eco(b * 0.5, 0.35, 0.2)
    m.terminar(total, "diptico_nieve", "Nieve")


# ---------- Sakura (primavera, Japón, Año Nuevo Lunar) ----------
def sakura():
    bpm = 72; b = 60 / bpm; compases = 16; total = compases * 4 * b
    m = Mezcla(total)
    escala = [62, 63, 67, 69, 70, 74, 75, 79, 81, 82]  # In: D Eb G A Bb
    for c in range(compases):
        t0 = c * 4 * b
        raiz = [50, 46, 43, 45][c % 4]
        n = int(4.4 * b * SR)
        pad = sum(sierra(hz(x) * d, n) for x in (raiz + 12, raiz + 19) for d in (0.997, 1.003))
        m.poner(pasabajos(pad, 700) * env(n, 1.0, 0.5, 0.8, 1.2), t0, 0.025)
        m.poner(seno(hz(raiz - 12), int(3.6 * b * SR)) * env(int(3.6 * b * SR), 0.2, 0.5, 0.8, 0.8), t0, 0.2)
        for k in range(6):  # koto
            if rng.uniform() < 0.75:
                nota = escala[int(rng.integers(0, 6))]
                nn = int(1.6 * SR)
                m.poner(pluck(hz(nota), nn, 4, 2.5), t0 + (k * 0.66 + rng.uniform(0, 0.1)) * b, 0.07, pan=float(rng.uniform(-0.5, 0.5)))
    frase = [(0, 2, 74), (2, 1, 75), (3, 1, 74), (4, 3, 70), (8, 1, 69), (9, 1, 70), (10, 2, 74), (12, 4, 67)]
    for bloque in (1, 3):
        def flauta(f, n):
            t = np.arange(n) / SR
            return (seno(f * (1 + 0.004 * np.sin(2 * np.pi * 5 * t)), n) + 0.08 * pasabajos(ruido(n), 3000)) * env(n, 0.08, 0.3, 0.75, 0.25)
        melodia(m, bloque * 16 * b, b, frase, flauta, 0.07)
    m.eco(b * 0.75, 0.5, 0.32)
    m.terminar(total, "diptico_sakura", "Sakura")


# ---------- Neón (arcade, synthwave) ----------
def neon():
    bpm = 108; b = 60 / bpm; compases = 32; total = compases * 4 * b
    m = Mezcla(total)
    acordes = [[57, 60, 64], [53, 57, 60], [48, 52, 55], [55, 59, 62]]
    for c in range(compases):
        t0 = c * 4 * b
        ac = acordes[c % 4]
        for k in range(16):  # bajo en semicorcheas
            n = int(0.23 * b * SR)
            m.poner(pasabajos(sierra(hz(ac[0] - 24 + (12 if k % 4 == 3 else 0)), n), 600) * env(n, 0.002, 0.08, 0.5, 0.03), t0 + k * b / 4, 0.22)
        n = int(4 * b * SR)
        pad = sum(sierra(hz(x) * d, n) for x in ac for d in (0.995, 1.005))
        puerta = (np.sin(2 * np.pi * (4 / b) * np.arange(n) / SR) > -0.3).astype(float)
        m.poner(pasabajos(pad * puerta, 2200) * env(n, 0.05, 0.4, 0.8, 0.3), t0, 0.022, pan=0.2)
        if c >= 4:
            for k in range(4):
                m.poner(bombo(), t0 + k * b, 0.45)
            m.poner(caja(0.9, 200), t0 + b, 0.3); m.poner(caja(0.9, 200), t0 + 3 * b, 0.3)
            for k in range(8):
                m.poner(platillo(0.6), t0 + (k + 0.5) * b / 2, 0.1, pan=0.4)
    frase = [(0, 1.5, 76), (1.5, 0.5, 74), (2, 2, 72), (4, 1, 69), (5, 1, 72), (6, 2, 74), (8, 1.5, 76), (9.5, 0.5, 79), (10, 2, 81), (12, 4, 79)]
    for bloque in range(2, 8):
        def lead(f, n):
            return pasabajos(sierra(f, n) + sierra(f * 1.006, n), 3500) * env(n, 0.01, 0.2, 0.7, 0.15)
        melodia(m, bloque * 16 * b, b, frase, lead, 0.04)
    m.eco(b * 0.75, 0.4, 0.22)
    m.terminar(total, "diptico_neon", "Neón")


# ---------- Océano (calma, agua) ----------
def oceano():
    bpm = 60; b = 60 / bpm; compases = 14; total = compases * 4 * b
    m = Mezcla(total)
    n = m.n
    t = np.arange(n) / SR
    olas = pasabajos(ruido(n), 700) * (0.5 + 0.5 * np.sin(2 * np.pi * t / 7.0)) ** 2
    m.izq += olas * 0.09
    m.der += np.roll(olas, SR) * 0.09
    acordes = [[50, 57, 62, 66], [47, 54, 59, 62], [45, 52, 57, 61], [43, 50, 55, 59]]
    for c in range(compases):
        t0 = c * 4 * b
        nn = int(4.5 * b * SR)
        pad = sum(seno(hz(x) * d, nn) for x in acordes[c % 4] for d in (0.998, 1.002))
        m.poner(pad * env(nn, 1.5, 0.5, 0.8, 1.5), t0, 0.03)
        for k in range(3):
            nota = acordes[c % 4][k + 1] + 12
            m.poner(campana(hz(nota), int(3 * SR), 1.2), t0 + (k * 1.3 + 0.2) * b, 0.04, pan=-0.4 + 0.4 * k)
    m.eco(b * 0.5, 0.55, 0.35)
    m.terminar(total, "diptico_oceano", "Océano")


# ---------- Bosque (folk, naturaleza) ----------
def bosque():
    bpm = 92; b = 60 / bpm; compases = 24; total = compases * 4 * b
    m = Mezcla(total)
    acordes = [[55, 59, 62], [52, 55, 59], [48, 52, 55], [50, 54, 57]]  # G Em C D
    for c in range(compases):
        t0 = c * 4 * b
        ac = acordes[c % 4]
        patron = [0, 2, 1, 2, 0, 2, 1, 2]
        for k in range(8):
            nota = ac[patron[k]] + (12 if k in (2, 6) else 0)
            m.poner(pluck(hz(nota), int(1.2 * SR), 3, 3), t0 + k * b / 2, 0.07, pan=-0.3)
        nb = int(1.8 * b * SR)
        m.poner(triangulo(hz(ac[0] - 12), nb) * env(nb, 0.01, 0.2, 0.7, 0.2), t0, 0.26)
        m.poner(triangulo(hz(ac[0] - 5), nb) * env(nb, 0.01, 0.2, 0.7, 0.2), t0 + 2 * b, 0.22)
        if c >= 4:
            m.poner(bombo(0.6), t0, 0.35); m.poner(caja(0.4, 260), t0 + 2 * b, 0.18)
            for k in range(4):
                m.poner(platillo(0.4, 0.04), t0 + (k + 0.5) * b, 0.08, pan=0.3)
    frase = [(0, 1, 74), (1, 0.5, 76), (1.5, 0.5, 74), (2, 2, 71), (4, 1, 72), (5, 1, 74), (6, 2, 76), (8, 1, 79), (9, 1, 78), (10, 1, 76), (11, 1, 74), (12, 4, 74)]
    def flauta(f, n):
        t = np.arange(n) / SR
        return (seno(f * (1 + 0.003 * np.sin(2 * np.pi * 5 * t)), n) + 0.2 * seno(f * 2, n) + 0.05 * pasabajos(ruido(n), 4000)) * env(n, 0.04, 0.2, 0.75, 0.15)
    for bloque in range(1, 6):
        melodia(m, bloque * 16 * b, b, frase, flauta, 0.06, pan=0.2)
    m.eco(b * 0.75, 0.3, 0.18)
    m.terminar(total, "diptico_bosque", "Bosque")


# ---------- Portátil (chiptune de 4 voces) ----------
def portatil():
    bpm = 140; b = 60 / bpm; compases = 32; total = compases * 4 * b
    m = Mezcla(total)
    acordes = [[64, 67, 71], [60, 64, 67], [62, 66, 69], [59, 62, 66]]  # Em C D Bm
    for c in range(compases):
        t0 = c * 4 * b
        ac = acordes[c % 4]
        for k in range(8):
            n = int(0.45 * b * SR)
            m.poner(pulso(hz(ac[0] - 24 + (12 if k % 2 else 0)), n, 0.5) * env(n, 0.002, 0.05, 0.8, 0.02), t0 + k * b / 2, 0.06)
        for k in range(16):
            n = int(0.22 * b * SR)
            m.poner(pulso(hz(ac[k % 3] + 12), n, 0.25) * env(n, 0.001, 0.05, 0.4, 0.02), t0 + k * b / 4, 0.03, pan=-0.4)
        if c >= 2:
            for k in range(8):
                nn = int(0.06 * SR)
                vol = 0.12 if k % 4 == 2 else 0.05
                m.poner(ruido(nn) * np.exp(-np.arange(nn) / SR * 60), t0 + k * b / 2, vol, pan=0.3)
    frase = [(0, 1, 76), (1, 1, 79), (2, 0.5, 83), (2.5, 0.5, 81), (3, 1, 79), (4, 2, 76), (6, 1, 74), (7, 1, 76),
             (8, 1, 78), (9, 1, 74), (10, 2, 71), (12, 1, 74), (13, 1, 76), (14, 2, 78)]
    for bloque in range(1, 8):
        melodia(m, bloque * 16 * b, b, frase, lambda f, n: pulso(f, n, 0.5 if bloque % 2 else 0.125) * env(n, 0.002, 0.1, 0.6, 0.05), 0.06, pan=0.15)
    m.terminar(total, "diptico_portatil", "Portátil")


# ---------- Fiesta (carnaval, fiestas patrias, celebraciones) ----------
def fiesta():
    bpm = 104; b = 60 / bpm; compases = 32; total = compases * 4 * b
    m = Mezcla(total)
    acordes = [[60, 64, 67], [65, 69, 72], [67, 71, 74], [65, 69, 72]]  # C F G F
    clave = [0, 0.75, 1.5, 2.5, 3]
    for c in range(compases):
        t0 = c * 4 * b
        ac = acordes[c % 4]
        for g in clave:
            nn = int(0.08 * SR)
            m.poner(seno(2500, nn) * np.exp(-np.arange(nn) / SR * 70), t0 + g * b, 0.12, pan=0.4)
        for g, d in ((0, 0), (1.5, 7), (2, 12), (3.5, 7)):
            nb = int(0.45 * b * SR)
            m.poner(triangulo(hz(ac[0] - 24 + d), nb) * env(nb, 0.003, 0.1, 0.7, 0.05), t0 + g * b, 0.3)
        for g in (0.5, 1.5, 2.5, 3.5):  # metales
            nn = int(0.3 * b * SR)
            st = sum(pasabajos(sierra(hz(x), nn), 2500) for x in ac)
            m.poner(st * env(nn, 0.005, 0.1, 0.5, 0.05), t0 + g * b, 0.03, pan=-0.2)
        if c >= 2:
            for k in range(4):
                m.poner(bombo(0.8), t0 + k * b, 0.38)
            for k in range(16):
                m.poner(platillo(0.8 if k % 4 == 2 else 0.4, 0.04), t0 + k * b / 4, 0.08, pan=-0.35)
    frase = [(0, 0.5, 72), (0.5, 0.5, 74), (1, 1, 76), (2, 0.5, 77), (2.5, 1.5, 79), (4, 1, 77), (5, 1, 76), (6, 2, 74),
             (8, 0.5, 74), (8.5, 0.5, 76), (9, 1, 77), (10, 0.5, 79), (10.5, 1.5, 81), (12, 1, 79), (13, 1, 77), (14, 2, 76)]
    for bloque in range(1, 8):
        def trompeta(f, n):
            return pasabajos(sierra(f, n) + 0.5 * pulso(f, n, 0.3), 2800) * env(n, 0.02, 0.15, 0.7, 0.08)
        melodia(m, bloque * 16 * b, b, frase, trompeta, 0.045, pan=0.15)
    m.eco(b * 0.5, 0.25, 0.12)
    m.terminar(total, "diptico_fiesta", "Fiesta")


if __name__ == "__main__":
    C.OUT.mkdir(parents=True, exist_ok=True)
    for f in (fantasma, nieve, sakura, neon, oceano, bosque, portatil, fiesta):
        f()
