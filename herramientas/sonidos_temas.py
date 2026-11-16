"""Sonidos propios de los temas Recortes, Acuario de DIPPO, Salón Recreativo y Constelación.
Todo se sintetiza aquí (sin muestras de terceros). Uso: python3 sonidos_temas.py <carpeta res/raw>"""
import subprocess, sys, wave
from pathlib import Path
import numpy as np

SR = 44100
OUT = Path(sys.argv[1] if len(sys.argv) > 1 else ".")
rng = np.random.default_rng(7)


def t_(dur):
    return np.arange(int(dur * SR)) / SR


def guardar(nombre, x, vol=0.45):
    x = x / (np.max(np.abs(x)) + 1e-9) * vol
    n = int(0.008 * SR)
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
        i = int(t * SR); y[i:i + len(x)] += x
    return y


def pasabanda(x, centro, ancho):
    f = np.fft.rfftfreq(len(x), 1 / SR)
    X = np.fft.rfft(x) * np.exp(-((f - centro) / ancho) ** 2)
    return np.fft.irfft(X, len(x))


# Recortes: papel que roza, tijera y un timbre de goma.
def roce(dur, centro, caida):
    t = t_(dur); return pasabanda(rng.standard_normal(len(t)), centro, centro * 0.6) * np.exp(-t * caida)

def sello(f, dur=0.12):
    t = t_(dur); return (np.sin(2 * np.pi * f * t) * 0.6 + rng.standard_normal(len(t)) * 0.25) * np.exp(-t * 45)

guardar("sfx_recortes_mover", roce(0.05, 3800, 80), 0.28)
guardar("sfx_recortes_aceptar", mezclar((0, roce(0.03, 5200, 120)), (0.045, roce(0.03, 4200, 120)), (0.08, sello(220))), 0.42)
guardar("sfx_recortes_atras", mezclar((0, roce(0.12, 1800, 25))), 0.32)
guardar("sfx_recortes_jugar", mezclar((0, roce(0.2, 1500, 12)), (0.12, sello(180)), (0.22, sello(260))), 0.46)

# Acuario de DIPPO: burbujas que suben de tono y una gota.
def burbuja(f0, f1, dur=0.08):
    t = t_(dur); fr = f0 + (f1 - f0) * (t / dur) ** 0.6
    return np.sin(2 * np.pi * np.cumsum(fr) / SR) * np.exp(-t * 30) * np.minimum(t / 0.004, 1)

guardar("sfx_acuario_mover", burbuja(600, 1100, 0.07), 0.3)
guardar("sfx_acuario_aceptar", mezclar((0, burbuja(500, 900)), (0.05, burbuja(700, 1300)), (0.1, burbuja(900, 1700, 0.1))), 0.42)
guardar("sfx_acuario_atras", mezclar((0, burbuja(1100, 600, 0.1)), (0.06, burbuja(800, 450, 0.12))), 0.38)
guardar("sfx_acuario_jugar", mezclar(*[(i * 0.045, burbuja(400 + i * 140, 800 + i * 220, 0.09)) for i in range(8)]), 0.46)

# Salón Recreativo: onda cuadrada de 8 bits y la ficha que cae.
def cuadrada(f, dur, caida=10):
    t = t_(dur); return np.sign(np.sin(2 * np.pi * f * t)) * 0.5 * np.exp(-t * caida)

guardar("sfx_salon_mover", cuadrada(1320, 0.035, 40), 0.22)
guardar("sfx_salon_aceptar", mezclar((0, cuadrada(988, 0.05, 20)), (0.05, cuadrada(1480, 0.08, 18))), 0.3)
guardar("sfx_salon_atras", mezclar((0, cuadrada(784, 0.05, 20)), (0.05, cuadrada(523, 0.08, 18))), 0.28)
guardar("sfx_salon_jugar", mezclar((0, cuadrada(1976, 0.06, 25)), (0.06, cuadrada(2637, 0.14, 12)),
                                   (0.2, cuadrada(523, 0.08, 15)), (0.28, cuadrada(659, 0.08, 15)), (0.36, cuadrada(784, 0.08, 15)), (0.44, cuadrada(1047, 0.2, 8))), 0.32)

# Constelación: campanitas de cristal.
def cristal(f, dur=0.5, caida=6):
    t = t_(dur)
    return (np.sin(2 * np.pi * f * t) + 0.4 * np.sin(2 * np.pi * f * 2.76 * t) * np.exp(-t * 8) + 0.2 * np.sin(2 * np.pi * f * 5.4 * t) * np.exp(-t * 18)) * np.exp(-t * caida) * np.minimum(t / 0.002, 1)

guardar("sfx_constelacion_mover", cristal(2093, 0.18, 22), 0.22)
guardar("sfx_constelacion_aceptar", mezclar((0, cristal(1568, 0.4, 9)), (0.06, cristal(2349, 0.5, 8))), 0.36)
guardar("sfx_constelacion_atras", mezclar((0, cristal(2349, 0.3, 12)), (0.06, cristal(1568, 0.4, 10))), 0.32)
guardar("sfx_constelacion_jugar", mezclar(*[(i * 0.08, cristal(f, 0.7, 5)) for i, f in enumerate([1047, 1319, 1568, 2093, 2637])]), 0.4)
print("listo")
