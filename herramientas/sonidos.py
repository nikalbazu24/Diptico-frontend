"""Sonidos cortos de navegación de Díptico (suaves, para no cansar)."""
import subprocess, sys, wave
from pathlib import Path
import numpy as np

SR = 44100
OUT = Path(sys.argv[1] if len(sys.argv) > 1 else ".")


def nota(f, dur, caida=18.0, armonicos=(1.0, 0.25, 0.08), ataque=0.003):
    n = int(dur * SR)
    t = np.arange(n) / SR
    x = sum(a * np.sin(2 * np.pi * f * (k + 1) * t) for k, a in enumerate(armonicos))
    return x * np.exp(-t * caida) * np.minimum(t / ataque, 1)


def guardar(nombre, x, vol=0.5):
    x = x / (np.max(np.abs(x)) + 1e-9) * vol
    n = int(0.01 * SR)
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


OUT.mkdir(parents=True, exist_ok=True)
guardar("sfx_mover", nota(1760, 0.06, 70, (1.0, 0.15)), 0.32)
guardar("sfx_consola", mezclar((0, nota(1318, 0.08, 45)), (0.035, nota(1760, 0.09, 40))), 0.38)
guardar("sfx_aceptar", mezclar((0, nota(988, 0.12, 30)), (0.05, nota(1480, 0.16, 22))), 0.45)
guardar("sfx_atras", mezclar((0, nota(1175, 0.1, 32)), (0.05, nota(784, 0.14, 26))), 0.4)
guardar("sfx_abrir", mezclar((0, nota(659, 0.2, 14)), (0.04, nota(988, 0.2, 14)), (0.08, nota(1319, 0.22, 12))), 0.42)
guardar("sfx_jugar", mezclar((0, nota(523, 0.3, 9)), (0.07, nota(784, 0.3, 9)), (0.14, nota(1047, 0.35, 8)), (0.21, nota(1568, 0.45, 7))), 0.5)
print("listo")
