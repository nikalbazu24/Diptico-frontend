"""Tema "Bloques" de Díptico: piano tranquilo y espaciado, con un colchón cálido (bucle sin cortes).
También genera los sonidos de navegación del tema de bloques (golpecitos de madera y "pops")."""
import sys
import numpy as np
import componer as C
from componer import SR, hz, env, seno, triangulo, pasabajos, pasaaltos, ruido, Mezcla
import sonidos as S

rng = np.random.default_rng(23)


def piano(f, n, vel=1.0):
    """Piano suave: armónicos un poco desafinados que se apagan a distinto ritmo."""
    t = np.arange(n) / SR
    x = np.zeros(n)
    for k, (a, caida) in enumerate([(1.0, 1.6), (0.42, 2.6), (0.2, 3.8), (0.09, 5.5), (0.05, 7.0)]):
        fk = f * (k + 1) * (1 + 0.0004 * (k + 1) ** 2)
        x += a * np.sin(2 * np.pi * fk * t + rng.random()) * np.exp(-t * caida)
    golpe = pasabajos(ruido(n), 2500) * np.exp(-t * 90) * 0.04
    ataque = np.minimum(t / 0.004, 1)
    return (x + golpe) * ataque * vel


def colchon(fs, n):
    t = np.arange(n) / SR
    x = sum(triangulo(f, n) * 0.5 + seno(f * 2.001, n) * 0.2 for f in fs)
    x = pasabajos(x, 900)
    return x * env(n, a=1.8, d=0.5, s=0.8, r=2.2)


def bloques():
    bpm = 68
    b = 60 / bpm
    compases = 16
    total = compases * 4 * b
    m = Mezcla(total)
    # Fmaj7 · Am7 · Dm9 · Cadd9/E  (dos vueltas, la segunda con otra melodía)
    acordes = [
        [53, 57, 60, 64], [57, 60, 64, 67], [50, 53, 57, 60, 64], [52, 55, 60, 62],
    ]
    for c in range(compases):
        t0 = c * 4 * b
        ac = acordes[c % 4]
        # Bajo y colchón
        m.poner(piano(hz(ac[0] - 12), int(4 * b * SR), 0.55), t0, 0.5, pan=-0.1)
        m.poner(colchon([hz(x) for x in ac[1:3]], int(4.4 * b * SR)), t0, 0.07)
        # Acorde quebrado, despacio
        for i, nota in enumerate(ac[1:]):
            m.poner(piano(hz(nota), int(3 * b * SR), 0.5), t0 + (0.5 + i * 0.75) * b, 0.28, pan=0.15 - i * 0.1)
    # Melodía: pocas notas, con aire entre ellas.
    frases = [
        [(0, 72), (1.5, 69), (3, 67)], [(0.5, 64), (2, 67), (3, 69)],
        [(0, 65), (1, 69), (2.5, 72)], [(0, 74), (2, 72)],
        [(0, 76), (1.5, 74), (3, 72)], [(0, 69), (2, 72), (3.5, 71)],
        [(0, 69), (1.5, 65), (2.5, 64)], [(0, 62), (2.5, 64)],
    ]
    for c in range(compases):
        if c % 8 in (3, 7) and c >= 8:
            continue
        frase = frases[(c + (0 if c < 8 else 4)) % 8]
        for beat, nota in frase:
            m.poner(piano(hz(nota), int(3.5 * b * SR), 0.9), c * 4 * b + beat * b, 0.34, pan=0.2)
            # Un eco más agudo, muy bajito, a veces.
            if rng.random() < 0.25:
                m.poner(piano(hz(nota + 12), int(2.5 * b * SR), 0.5), c * 4 * b + (beat + 0.5) * b, 0.08, pan=-0.3)
    m.eco(0.42, fb=0.4, mezcla=0.22)
    m.terminar(total, "diptico_bloques", "Bloques")


def sonidos_bloques():
    def madera(f, dur, caida=60.0):
        n = int(dur * SR)
        t = np.arange(n) / SR
        golpe = pasaaltos(pasabajos(ruido(n), f * 3), f * 0.6) * np.exp(-t * caida)
        tono = np.sin(2 * np.pi * f * t) * np.exp(-t * caida * 0.8)
        return golpe * 0.6 + tono

    def pop(f0, f1, dur):
        n = int(dur * SR)
        t = np.arange(n) / SR
        fr = f0 + (f1 - f0) * (t / dur)
        fase = 2 * np.pi * np.cumsum(fr) / SR
        return np.sin(fase) * np.exp(-t * 18) * np.minimum(t / 0.002, 1)

    def cuadrada(f, dur, caida=10.0):
        n = int(dur * SR)
        t = np.arange(n) / SR
        return np.sign(np.sin(2 * np.pi * f * t)) * 0.35 * np.exp(-t * caida)

    S.guardar("sfx_b_mover", madera(520, 0.07, 75), 0.34)
    S.guardar("sfx_b_consola", S.mezclar((0, madera(440, 0.08, 60)), (0.05, madera(620, 0.08, 60))), 0.38)
    S.guardar("sfx_b_aceptar", pop(380, 900, 0.14), 0.45)
    S.guardar("sfx_b_atras", pop(700, 260, 0.14), 0.4)
    S.guardar("sfx_b_abrir", S.mezclar((0, madera(330, 0.12, 40)), (0.06, pop(500, 1000, 0.12))), 0.42)
    S.guardar("sfx_b_jugar", S.mezclar(*[(i * 0.07, cuadrada(hz(n), 0.18)) for i, n in enumerate([60, 64, 67, 72, 76])]), 0.42)


if __name__ == "__main__":
    bloques()
    sonidos_bloques()
