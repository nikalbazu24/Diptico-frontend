"""Más sonidos de navegación de Díptico (1.0 beta.2) y dos juegos nuevos: "retro" (8 bits) y "burbuja" (suaves).
Cada juego tiene los mismos 15 sonidos, con la misma línea: mover, consola, aceptar, atrás, abrir, jugar,
borde (llegaste al final), página (cambiar de pestaña), deslizar (carrusel), tecla, sí / no (interruptores),
levantar / soltar (mover widgets) y aviso.
Uso: python3 componer5.py <carpeta res/raw>"""
import subprocess, sys, wave
from pathlib import Path
import numpy as np

SR = 44100
OUT = Path(sys.argv[1] if len(sys.argv) > 1 else ".")
rng = np.random.default_rng(5)


def guardar(nombre, x, vol=0.5):
    x = np.asarray(x, dtype=float)
    x = x / (np.max(np.abs(x)) + 1e-9) * vol
    n = min(int(0.01 * SR), len(x))
    x[-n:] *= np.linspace(1, 0, n)
    wav = OUT / f"{nombre}.wav"
    with wave.open(str(wav), "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((x * 32767).astype("<i2").tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav), "-c:a", "libvorbis", "-q:a", "3", str(OUT / f"{nombre}.ogg")], check=True)
    wav.unlink()


def mezclar(*partes):
    largo = max(int(t * SR) + len(x) for t, x in partes)
    y = np.zeros(largo)
    for t, x in partes:
        i = int(t * SR)
        y[i:i + len(x)] += x
    return y


def tt(dur):
    return np.arange(int(dur * SR)) / SR


def nota(f, dur, caida=18.0, arm=(1.0, 0.25, 0.08), ataque=0.003):
    t = tt(dur)
    x = sum(a * np.sin(2 * np.pi * f * (k + 1) * t) for k, a in enumerate(arm))
    return x * np.exp(-t * caida) * np.minimum(t / ataque, 1)


def barrido(f0, f1, dur, caida=12.0, forma="seno"):
    t = tt(dur)
    fr = f0 * (f1 / f0) ** (t / dur)
    fase = 2 * np.pi * np.cumsum(fr) / SR
    x = np.sin(fase) if forma == "seno" else np.sign(np.sin(fase)) * 0.4
    return x * np.exp(-t * caida) * np.minimum(t / 0.002, 1)


def ruido_filtrado(dur, f0, f1, caida=8.0):
    """Un "fsst" de aire que sube o baja (para deslizar)."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    r = rng.standard_normal(n)
    y = np.zeros(n)
    a = 0.0
    for i in range(n):
        f = f0 + (f1 - f0) * i / n
        k = min(1.0, 2 * np.pi * f / SR)
        a += k * (r[i] - a)
        y[i] = a
    env = np.sin(np.pi * np.minimum(t / dur, 1)) ** 1.5
    return y * env * np.exp(-t * caida * 0.2)


def cuadrada(f, dur, caida=10.0, ciclo=0.5):
    t = tt(dur)
    fase = (f * t) % 1.0
    return np.where(fase < ciclo, 0.35, -0.35) * np.exp(-t * caida)


# ---------- Clásicos (los de siempre, más los nuevos) ----------
def clasicos():
    guardar("sfx_borde", mezclar((0, nota(392, 0.09, 40, (1.0, 0.3))), (0.04, nota(370, 0.1, 40, (1.0, 0.3)))), 0.3)
    guardar("sfx_pagina", mezclar((0, nota(1175, 0.07, 50)), (0.03, nota(1568, 0.09, 45)), (0.06, ruido_filtrado(0.08, 3000, 6000) * 0.15)), 0.34)
    guardar("sfx_deslizar", ruido_filtrado(0.18, 900, 3800) * 0.7 + np.pad(nota(1318, 0.06, 60), (int(0.12 * SR), 0))[:int(0.18 * SR)] * 0.3, 0.3)
    guardar("sfx_tecla", nota(2093, 0.035, 120, (1.0, 0.1)), 0.24)
    guardar("sfx_si", mezclar((0, nota(880, 0.08, 40)), (0.05, nota(1319, 0.12, 30))), 0.38)
    guardar("sfx_no", mezclar((0, nota(1319, 0.08, 40)), (0.05, nota(880, 0.12, 30))), 0.34)
    guardar("sfx_levantar", barrido(500, 1100, 0.14, 14), 0.36)
    guardar("sfx_soltar", mezclar((0, barrido(900, 420, 0.1, 20)), (0.07, nota(330, 0.12, 30, (1.0, 0.5)))), 0.4)
    guardar("sfx_aviso", mezclar((0, nota(1047, 0.25, 10)), (0.09, nota(1568, 0.35, 8))), 0.4)


# ---------- Retro (8 bits) ----------
def retro():
    c = cuadrada
    guardar("sfx_r_mover", c(1320, 0.04, 60, 0.25), 0.26)
    guardar("sfx_r_consola", mezclar((0, c(988, 0.05, 40)), (0.045, c(1319, 0.06, 40))), 0.3)
    guardar("sfx_r_aceptar", mezclar((0, c(784, 0.05, 30)), (0.05, c(1175, 0.09, 25))), 0.34)
    guardar("sfx_r_atras", mezclar((0, c(1175, 0.05, 30)), (0.05, c(659, 0.09, 25))), 0.32)
    guardar("sfx_r_abrir", mezclar(*[(i * 0.045, c(f, 0.07, 30)) for i, f in enumerate([523, 659, 784, 1047])]), 0.32)
    guardar("sfx_r_jugar", mezclar(*[(i * 0.07, c(f, 0.12, 16)) for i, f in enumerate([523, 659, 784, 1047, 1319, 1568])]), 0.34)
    guardar("sfx_r_borde", c(196, 0.09, 30, 0.5), 0.26)
    guardar("sfx_r_pagina", barrido(600, 1500, 0.08, 20, "cuadrada"), 0.28)
    guardar("sfx_r_deslizar", barrido(300, 1200, 0.15, 10, "cuadrada"), 0.24)
    guardar("sfx_r_tecla", c(1760, 0.025, 120, 0.125), 0.2)
    guardar("sfx_r_si", mezclar((0, c(988, 0.05, 35)), (0.05, c(1976, 0.07, 30))), 0.3)
    guardar("sfx_r_no", mezclar((0, c(494, 0.05, 35)), (0.05, c(247, 0.08, 30))), 0.3)
    guardar("sfx_r_levantar", barrido(400, 1600, 0.12, 12, "cuadrada"), 0.28)
    guardar("sfx_r_soltar", barrido(1400, 300, 0.12, 14, "cuadrada"), 0.3)
    guardar("sfx_r_aviso", mezclar((0, c(1319, 0.08, 20)), (0.09, c(1319, 0.08, 20)), (0.18, c(1760, 0.16, 12))), 0.32)


# ---------- Burbuja (suaves, como gotitas) ----------
def burbuja():
    def gota(f, dur=0.09, sube=1.6, caida=28):
        return barrido(f, f * sube, dur, caida)
    guardar("sfx_u_mover", gota(900, 0.05, 1.35, 60), 0.26)
    guardar("sfx_u_consola", mezclar((0, gota(700, 0.07)), (0.04, gota(1000, 0.07))), 0.3)
    guardar("sfx_u_aceptar", mezclar((0, gota(600, 0.1, 1.8)), (0.06, gota(1100, 0.12, 1.5))), 0.36)
    guardar("sfx_u_atras", barrido(1100, 520, 0.12, 22), 0.32)
    guardar("sfx_u_abrir", mezclar((0, gota(500, 0.1, 2.0)), (0.05, gota(800, 0.1, 1.8)), (0.1, gota(1200, 0.14, 1.5))), 0.34)
    guardar("sfx_u_jugar", mezclar(*[(i * 0.06, gota(f, 0.16, 1.4, 14)) for i, f in enumerate([440, 554, 659, 880, 1109])]), 0.38)
    guardar("sfx_u_borde", barrido(300, 240, 0.1, 30), 0.24)
    guardar("sfx_u_pagina", mezclar((0, gota(800, 0.06, 1.5, 40)), (0.03, gota(1300, 0.07, 1.4, 40))), 0.28)
    guardar("sfx_u_deslizar", ruido_filtrado(0.16, 500, 2200) * 0.6 + np.pad(gota(1000, 0.06), (int(0.1 * SR), 0))[:int(0.16 * SR)] * 0.4, 0.26)
    guardar("sfx_u_tecla", gota(1400, 0.03, 1.3, 120), 0.2)
    guardar("sfx_u_si", gota(700, 0.14, 2.0, 18), 0.32)
    guardar("sfx_u_no", barrido(900, 450, 0.14, 18), 0.3)
    guardar("sfx_u_levantar", gota(400, 0.16, 2.6, 12), 0.32)
    guardar("sfx_u_soltar", mezclar((0, barrido(1000, 400, 0.08, 25)), (0.06, gota(350, 0.1, 1.3))), 0.34)
    guardar("sfx_u_aviso", mezclar((0, gota(880, 0.2, 1.2, 10)), (0.1, gota(1320, 0.3, 1.2, 8))), 0.34)


# ---------- Bloques (madera y pops): los nuevos ----------
def bloques():
    def madera(f, dur, caida=60.0):
        t = tt(dur)
        r = rng.standard_normal(len(t))
        return (r * 0.25 + np.sin(2 * np.pi * f * t)) * np.exp(-t * caida)

    def pop(f0, f1, dur):
        return barrido(f0, f1, dur, 18)
    guardar("sfx_b_borde", madera(220, 0.1, 50), 0.3)
    guardar("sfx_b_pagina", mezclar((0, madera(600, 0.06, 70)), (0.04, madera(800, 0.06, 70))), 0.32)
    guardar("sfx_b_deslizar", mezclar(*[(i * 0.035, madera(400 + i * 90, 0.05, 80)) for i in range(4)]), 0.3)
    guardar("sfx_b_tecla", madera(900, 0.035, 110), 0.22)
    guardar("sfx_b_si", pop(400, 1000, 0.12), 0.36)
    guardar("sfx_b_no", pop(800, 300, 0.12), 0.34)
    guardar("sfx_b_levantar", pop(300, 900, 0.16), 0.34)
    guardar("sfx_b_soltar", mezclar((0, madera(260, 0.1, 40)), (0.03, madera(180, 0.12, 35))), 0.4)
    guardar("sfx_b_aviso", mezclar((0, madera(660, 0.12, 30)), (0.09, madera(990, 0.18, 22))), 0.36)


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    clasicos(); retro(); burbuja(); bloques()
    print("listo")
