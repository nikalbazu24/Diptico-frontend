"""Cueca de Díptico, para las Fiestas Patrias de Chile (6/8, guitarra, acordeón, pandero y palmas)."""
import numpy as np
import componer as C
from componer import SR, hz, env, pulso, triangulo, seno, sierra, pasabajos, pasaaltos, ruido, Mezcla, bombo
from componer2 import pluck


def palma(vol=1.0):
    n = int(0.09 * SR)
    t = np.arange(n) / SR
    return pasaaltos(pasabajos(ruido(n), 4500), 900) * np.exp(-t * 45) * vol


def pandero(vol=1.0):
    n = int(0.12 * SR)
    t = np.arange(n) / SR
    return pasaaltos(ruido(n), 6000, 4) * np.exp(-t * 30) * vol


def cueca():
    corchea = 60 / 300  # 6/8 rápido
    compas = 6 * corchea
    compases = 40
    total = compases * compas
    m = Mezcla(total)
    # D G A D (I IV V I) con la hemiola típica de la cueca.
    acordes = [[50, 54, 57], [55, 59, 62], [57, 61, 64], [50, 54, 57]]
    for c in range(compases):
        t0 = c * compas
        ac = acordes[(c // 2) % 4]
        # Rasgueo de guitarra: acentos en 1, 3, 5 (y 4 en compases pares: hemiola).
        acentos = (0, 2, 4) if c % 2 == 0 else (0, 3)
        for k in range(6):
            fuerte = k in acentos
            for i, nota in enumerate(ac + [ac[0] + 12]):
                n = int(0.5 * SR)
                m.poner(pluck(hz(nota), n, 5, 6), t0 + k * corchea + i * 0.008, 0.06 if fuerte else 0.025, pan=-0.3)
        # Bajo
        nb = int(2.5 * corchea * SR)
        m.poner(triangulo(hz(ac[0] - 12), nb) * env(nb, 0.005, 0.1, 0.7, 0.05), t0, 0.3)
        m.poner(triangulo(hz(ac[2] - 12), nb) * env(nb, 0.005, 0.1, 0.7, 0.05), t0 + 3 * corchea, 0.25)
        if c >= 4:
            m.poner(bombo(0.7), t0, 0.35)
            m.poner(bombo(0.5), t0 + 3 * corchea, 0.25)
            for k in range(6):
                m.poner(pandero(1.0 if k in (0, 3) else 0.5), t0 + k * corchea, 0.08, pan=0.35)
            m.poner(palma(), t0 + 2 * corchea, 0.18, pan=-0.15)
            m.poner(palma(), t0 + 5 * corchea, 0.18, pan=0.15)

    def acordeon(f, n):
        t = np.arange(n) / SR
        trem = 1 + 0.004 * np.sin(2 * np.pi * 6 * t)
        x = pulso(f * trem, n, 0.35) + 0.6 * sierra(f * 1.003 * trem, n)
        return pasabajos(x, 3200) * env(n, 0.02, 0.1, 0.8, 0.06)

    frase = [(0, 1, 74), (1, 1, 76), (2, 1, 78), (3, 2, 79), (5, 1, 78), (6, 1, 76), (7, 1, 74), (8, 1, 73), (9, 3, 74),
             (12, 1, 78), (13, 1, 79), (14, 1, 81), (15, 2, 83), (17, 1, 81), (18, 1, 79), (19, 1, 78), (20, 1, 76), (21, 3, 78)]
    for bloque in range(1, 10):
        base = bloque * 4 * compas
        for (beat, largo, nota) in frase:
            if beat >= 24:
                break
            n = int(largo * corchea * SR * 0.95)
            m.poner(acordeon(hz(nota), n), base + beat * corchea, 0.05, pan=0.2)
    m.eco(corchea * 2, 0.2, 0.1)
    m.terminar(total, "diptico_cueca", "Cueca")


if __name__ == "__main__":
    C.OUT.mkdir(parents=True, exist_ok=True)
    cueca()
