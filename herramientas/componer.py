"""Compone y sintetiza los tres temas originales de Díptico (bucles sin cortes)."""
import subprocess
import sys
from pathlib import Path

import numpy as np
from scipy.signal import butter, sosfilt

SR = 44100
OUT = Path(sys.argv[1] if len(sys.argv) > 1 else ".")
rng = np.random.default_rng(7)


def hz(m):
    return 440.0 * 2 ** ((m - 69) / 12)


def env(n, a=0.005, d=0.15, s=0.6, r=0.08, dur=None):
    """Envolvente ADSR de n muestras."""
    t = np.arange(n) / SR
    dur = dur if dur is not None else n / SR
    e = np.ones(n) * s
    ai = t < a
    e[ai] = t[ai] / a
    di = (t >= a) & (t < a + d)
    e[di] = 1 - (1 - s) * (t[di] - a) / d
    ri = t >= dur - r
    e[ri] *= np.clip((dur - t[ri]) / r, 0, 1)
    return e


def pulso(f, n, duty=0.5, vib=0.0):
    t = np.arange(n) / SR
    fase = np.cumsum(f * (1 + vib * np.sin(2 * np.pi * 5.5 * t)) / SR)
    return np.where((fase % 1.0) < duty, 1.0, -1.0)


def triangulo(f, n):
    t = np.arange(n) / SR
    fase = (f * t) % 1.0
    return 4 * np.abs(fase - 0.5) - 1


def seno(f, n):
    return np.sin(2 * np.pi * f * np.arange(n) / SR)


def sierra(f, n):
    fase = (f * np.arange(n) / SR) % 1.0
    return 2 * fase - 1


def pasabajos(x, fc, orden=2):
    sos = butter(orden, fc, "low", fs=SR, output="sos")
    return sosfilt(sos, x)


def pasaaltos(x, fc, orden=2):
    sos = butter(orden, fc, "high", fs=SR, output="sos")
    return sosfilt(sos, x)


def ruido(n):
    return rng.uniform(-1, 1, n)


class Mezcla:
    def __init__(self, segundos):
        self.n = int(segundos * SR) + SR * 4  # cola para el bucle
        self.izq = np.zeros(self.n)
        self.der = np.zeros(self.n)

    def poner(self, x, t0, vol=1.0, pan=0.0):
        i = int(t0 * SR)
        j = min(i + len(x), self.n)
        if i >= self.n:
            return
        x = x[: j - i] * vol
        self.izq[i:j] += x * (1 - max(pan, 0))
        self.der[i:j] += x * (1 + min(pan, 0))

    def eco(self, retardo, fb=0.35, mezcla=0.25):
        d = int(retardo * SR)
        for canal, otro in ((self.izq, self.der), (self.der, self.izq)):
            y = canal.copy()
            for k in range(1, 5):
                g = mezcla * fb ** (k - 1)
                y[d * k:] += otro[: -d * k] * g if k % 2 else canal[: -d * k] * g
            canal[:] = y

    def terminar(self, segundos, nombre, titulo):
        largo = int(segundos * SR)
        izq, der = self.izq.copy(), self.der.copy()
        # La cola se suma al comienzo: el bucle no se corta.
        cola = self.n - largo
        izq[:cola] += izq[largo:]
        der[:cola] += der[largo:]
        izq, der = izq[:largo], der[:largo]
        est = np.stack([izq, der], 1)
        est = np.tanh(est * 1.1)  # limitador suave
        est /= np.max(np.abs(est)) / 0.89
        wav = OUT / f"{nombre}.wav"
        import wave

        with wave.open(str(wav), "wb") as w:
            w.setnchannels(2)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes((est * 32767).astype("<i2").tobytes())
        ogg = OUT / f"{nombre}.ogg"
        subprocess.run(
            ["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav), "-af", "loudnorm=I=-17:TP=-1.5:LRA=7", "-ar", "44100", "-c:a", "libvorbis", "-q:a", "4",
             "-metadata", f"title={titulo}", "-metadata", "artist=Díptico", str(ogg)],
            check=True,
        )
        wav.unlink()
        print(nombre, f"{segundos:.1f}s", ogg.stat().st_size // 1024, "KB")


# ---------- Percusión ----------

def bombo(vol=1.0):
    n = int(0.28 * SR)
    t = np.arange(n) / SR
    f = 50 + 90 * np.exp(-t * 28)
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 11)
    return x * vol


def caja(vol=1.0, tono=190):
    n = int(0.22 * SR)
    t = np.arange(n) / SR
    x = pasaaltos(ruido(n), 1500) * np.exp(-t * 18) * 0.7 + seno(tono, n) * np.exp(-t * 30) * 0.5
    return x * vol


def platillo(vol=1.0, largo=0.06):
    n = int(largo * SR)
    t = np.arange(n) / SR
    return pasaaltos(ruido(n), 7000, 4) * np.exp(-t * (4 / largo)) * vol


# ---------- Tema 1: Bisagra (lofi chiptune tranquila) ----------

def bisagra():
    bpm = 82
    b = 60 / bpm
    compases = 24
    total = compases * 4 * b
    m = Mezcla(total)
    # Fmaj7 · Em7 · Dm7 · Cmaj7  (descenso clásico)
    acordes = [[53, 57, 60, 64], [52, 55, 59, 62], [50, 53, 57, 60], [48, 52, 55, 59]]
    bajos = [41, 40, 38, 36]

    def swing(beat):
        entero = int(beat * 2)
        return beat + (0.08 if entero % 2 == 1 else 0)

    for c in range(compases):
        t0 = c * 4 * b
        ac = acordes[c % 4]
        # acordes suaves (pulso 25 %) en el 1 y en el "y" del 2
        for golpe, largo in ((0, 1.4), (1.5, 2.2)):
            n = int(largo * b * SR)
            for k, nota in enumerate(ac):
                x = pulso(hz(nota + 12), n, 0.25) * env(n, 0.01, 0.4, 0.35, 0.3)
                m.poner(pasabajos(x, 1800), t0 + golpe * b, 0.05, pan=-0.3 + 0.2 * k)
        # bajo triangular
        for golpe, nota in ((0, bajos[c % 4]), (2, bajos[c % 4] + 7), (3.5, bajos[c % 4] + 12)):
            n = int(0.9 * b * SR)
            m.poner(triangulo(hz(nota), n) * env(n, 0.005, 0.2, 0.7, 0.1), t0 + golpe * b, 0.32)
        # batería con swing
        if c >= 2:
            m.poner(bombo(), t0, 0.55)
            m.poner(bombo(0.7), t0 + swing(2.5) * b, 0.5)
            m.poner(caja(0.6), t0 + 1 * b, 0.32)
            m.poner(caja(0.6), t0 + 3 * b, 0.32)
            for k in range(8):
                m.poner(platillo(0.6 if k % 2 == 0 else 0.35), t0 + swing(k / 2) * b, 0.16, pan=0.35)

    # melodía (pentatónica de Do) en frases de 4 compases: A · A' · B · A''
    frase_a = [(0, 1, 76), (1, 0.5, 74), (1.5, 1.5, 72), (4, 1, 69), (5, 0.5, 72), (5.5, 2, 74),
               (8, 1, 72), (9, 0.5, 69), (9.5, 1.5, 67), (12, 2.5, 64), (14.5, 1, 67)]
    frase_a2 = [(0, 1, 76), (1, 0.5, 79), (1.5, 1.5, 76), (4, 1, 74), (5, 0.5, 72), (5.5, 2, 74),
                (8, 1, 76), (9, 0.5, 74), (9.5, 1, 72), (10.5, 1, 69), (12, 3, 72)]
    frase_b = [(0, 2, 81), (2, 1, 79), (3, 1, 76), (4, 2, 79), (6, 1, 76), (7, 1, 74),
               (8, 2, 76), (10, 1, 74), (11, 1, 72), (12, 3, 74)]
    plan = [None, frase_a, frase_a2, frase_b, frase_a, frase_a2]
    for bloque, frase in enumerate(plan):
        if frase is None:
            continue
        base = bloque * 4 * 4 * b
        for (beat, largo, nota) in frase:
            n = int(largo * b * SR)
            x = pulso(hz(nota), n, 0.5, vib=0.004) * env(n, 0.01, 0.2, 0.55, 0.12)
            m.poner(pasabajos(x, 3200), base + swing(beat) * b, 0.075, pan=0.15)
    # ruido de vinilo
    n = m.n
    crujido = pasabajos(ruido(n) * (rng.uniform(0, 1, n) > 0.9993), 5000) * 0.4 + pasabajos(ruido(n), 900) * 0.02
    m.izq += crujido
    m.der += crujido
    m.eco(b * 0.75, 0.4, 0.22)
    m.terminar(total, "diptico_bisagra", "Bisagra")


# ---------- Tema 2: Cartucho (arcade alegre) ----------

def cartucho():
    bpm = 126
    b = 60 / bpm
    compases = 32
    total = compases * 4 * b
    m = Mezcla(total)
    acordes = [[60, 64, 67], [55, 59, 62], [57, 60, 64], [53, 57, 60]]  # C G Am F
    for c in range(compases):
        t0 = c * 4 * b
        ac = acordes[c % 4]
        # arpegio en semicorcheas (pulso 12,5 %)
        orden = [0, 1, 2, 1] if c % 8 < 4 else [0, 2, 1, 2]
        for k in range(16):
            nota = ac[orden[k % 4]] + 12 + (12 if k % 8 == 7 else 0)
            n = int(0.24 * b * SR)
            x = pulso(hz(nota), n, 0.125) * env(n, 0.002, 0.08, 0.3, 0.05)
            m.poner(x, t0 + k * b / 4, 0.045, pan=-0.35)
        # bajo en corcheas a la octava
        for k in range(8):
            nota = ac[0] - 24 + (12 if k % 2 else 0)
            n = int(0.45 * b * SR)
            m.poner(triangulo(hz(nota), n) * env(n, 0.003, 0.1, 0.8, 0.05), t0 + k * b / 2, 0.3)
        # batería
        if c >= 4:
            for k in range(4):
                m.poner(bombo(), t0 + k * b, 0.5)
            m.poner(caja(0.8, 220), t0 + 1 * b, 0.32)
            m.poner(caja(0.8, 220), t0 + 3 * b, 0.32)
            for k in range(8):
                m.poner(platillo(0.7), t0 + (k + 0.5) / 2 * b, 0.13, pan=0.3)
    # melodía pegadiza
    motivo = [(0, 0.5, 72), (0.5, 0.5, 74), (1, 1, 76), (2, 0.5, 79), (2.5, 1.5, 76),
              (4, 0.5, 74), (4.5, 0.5, 76), (5, 1, 74), (6, 2, 71),
              (8, 0.5, 69), (8.5, 0.5, 72), (9, 1, 76), (10, 0.5, 74), (10.5, 1.5, 72),
              (12, 0.5, 69), (12.5, 0.5, 72), (13, 1, 77), (14, 2, 76)]
    respuesta = [(0, 1, 79), (1, 0.5, 77), (1.5, 0.5, 76), (2, 2, 74),
                 (4, 1, 74), (5, 0.5, 76), (5.5, 0.5, 74), (6, 2, 71),
                 (8, 1, 72), (9, 0.5, 74), (9.5, 0.5, 76), (10, 2, 81),
                 (12, 1, 77), (13, 1, 76), (14, 2, 72)]
    for bloque in range(8):
        if bloque < 1:
            continue
        frase = motivo if bloque % 2 == 1 else respuesta
        base = bloque * 4 * 4 * b
        for (beat, largo, nota) in frase:
            n = int(largo * b * SR * 0.95)
            x = pulso(hz(nota), n, 0.5 if bloque < 5 else 0.25, vib=0.003) * env(n, 0.003, 0.1, 0.6, 0.06)
            m.poner(x, base + beat * b, 0.07, pan=0.2)
    m.eco(b * 0.5, 0.3, 0.15)
    m.terminar(total, "diptico_cartucho", "Cartucho")


# ---------- Tema 3: Medianoche (ambiente, calma) ----------

def medianoche():
    bpm = 68
    b = 60 / bpm
    compases = 16
    total = compases * 4 * b
    m = Mezcla(total)
    acordes = [[57, 60, 64, 67], [53, 57, 60, 64], [48, 52, 55, 59], [55, 59, 62, 66]]  # Am7 Fmaj7 Cmaj7 G(add#11)
    for c in range(compases):
        t0 = c * 4 * b
        n = int(4.6 * b * SR)
        pad = np.zeros(n)
        for nota in acordes[c % 4]:
            f = hz(nota)
            pad += sierra(f * 0.997, n) + sierra(f * 1.004, n)
        pad = pasabajos(pad, 1100, 2) * env(n, 1.2, 0.5, 0.8, 1.4)
        m.poner(pad, t0, 0.035, pan=-0.2)
        m.poner(pad, t0 + 0.012, 0.035, pan=0.2)
        raiz = acordes[c % 4][0] - 24
        n2 = int(3.8 * b * SR)
        m.poner(seno(hz(raiz), n2) * env(n2, 0.3, 0.5, 0.8, 1.0), t0, 0.22)
        for k in range(8):
            if c >= 4:
                m.poner(platillo(0.4, 0.09), t0 + (k / 2 + 0.02 * (k % 2)) * b, 0.07, pan=0.4 if k % 2 else -0.4)
    # campanitas
    campanas = [(0, 76), (1.5, 79), (3, 72), (6, 74), (8, 76), (9.5, 72), (11, 69), (14, 67),
                (16, 76), (17.5, 81), (19, 79), (22, 76), (24, 74), (25.5, 72), (27, 71), (30, 72)]
    for vuelta in (1, 2, 3):
        base = vuelta * 4 * 4 * b - (8 * b if vuelta == 3 else 0)
        for (beat, nota) in campanas:
            if vuelta == 3 and beat >= 16:
                break
            n = int(2.5 * SR)
            t = np.arange(n) / SR
            f = hz(nota)
            x = (seno(f, n) + 0.4 * seno(f * 2.76, n) * np.exp(-t * 6) + 0.2 * seno(f * 5.4, n) * np.exp(-t * 10))
            x *= np.exp(-t * 1.6) * np.minimum(t / 0.004, 1)
            m.poner(x, base + beat * b, 0.06, pan=0.3 if beat % 2 else -0.3)
    m.eco(b * 0.75, 0.5, 0.3)
    m.terminar(total, "diptico_medianoche", "Medianoche")


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    bisagra()
    cartucho()
    medianoche()
