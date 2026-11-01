#!/usr/bin/env python3
"""Compone y sintetiza la música original del juego (estilo chiptune).

Todo se genera por código: osciladores de onda cuadrada y triangular, ruido para la percusión.
No usa muestras ni material de terceros.

Pistas (app/src/main/res/raw):
  musica_menu       menús, créditos y "Cómo jugar"      Do mayor     88 bpm   tranquila
  musica_dia        partida, velocidad 1                Do mayor    126 bpm   alegre
  musica_atardecer  partida, velocidad 2                Sol mayor   108 bpm   cálida
  musica_noche      partida, velocidad 3                Re menor    140 bpm   intensa
  musica_fin        fin de partida                      La menor     76 bpm   melancólica

Cada pista dura 16 compases y se repite sin cortes (la cola de las notas se pliega al inicio).
Uso: python3 herramientas/gen_musica.py   (necesita numpy, scipy y ffmpeg con libvorbis)
"""
import os, subprocess, tempfile
import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, lfilter

SR = 44100
RAW = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res", "raw")

MAYOR = [0, 2, 4, 5, 7, 9, 11]
MENOR = [0, 2, 3, 5, 7, 8, 10]


def nota(raiz, escala, grado):
    """Nota MIDI del grado `grado` de la escala (0 = tónica; 7 = octava)."""
    return raiz + escala[grado % 7] + 12 * (grado // 7)


def hz(m):
    return 440.0 * 2 ** ((m - 69) / 12.0)


# ------------------------------------------------------------------ instrumentos
def onda(tipo, f, n, duty=0.5, vib=0.0):
    t = np.arange(n) / SR
    if vib:
        ft = f * (1 + vib * np.sin(2 * np.pi * 5.5 * t) * np.clip((t - 0.15) / 0.3, 0, 1))
        fase = np.cumsum(ft) / SR
    else:
        fase = f * t
    p = fase % 1.0
    if tipo == "tri":
        return 4 * np.abs(p - 0.5) - 1
    w = np.where(p < duty, 1.0, -1.0) - (2 * duty - 1)       # sin componente continua
    return (w + np.concatenate(([w[0]], w[:-1]))) / 2        # suaviza un poco el borde (menos aliasing)


def envolvente(n, ataque=0.004, tau=0.0, sost=1.0, rel=0.03):
    t = np.arange(n) / SR
    e = np.ones(n) * sost
    if tau:
        e = np.exp(-t / tau)
    a = max(int(ataque * SR), 1)
    e[:a] *= np.linspace(0, 1, a)
    r = min(int(rel * SR), n)
    e[n - r:] *= np.linspace(1, 0, r)
    return e


def kick(n=int(0.22 * SR)):
    t = np.arange(n) / SR
    f = 45 + 110 * np.exp(-t / 0.03)
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.07)


def snare(rng, n=int(0.2 * SR)):
    t = np.arange(n) / SR
    return (rng.uniform(-1, 1, n) * 0.8 + np.sin(2 * np.pi * 190 * t) * 0.5) * np.exp(-t / 0.05)


def hat(rng, n=int(0.06 * SR)):
    t = np.arange(n) / SR
    r = rng.uniform(-1, 1, n)
    return np.diff(r, prepend=0) * np.exp(-t / 0.014)


# ------------------------------------------------------------------ mezcla
class Mezcla:
    def __init__(self, compases, bpm):
        self.beat = 60.0 / bpm
        self.nb = int(round(compases * 4 * self.beat * SR))
        self.cola = int(1.5 * SR)
        self.buf = np.zeros((2, self.nb + self.cola))

    def poner(self, inicio_beats, senal, vol, pan=0.0):
        i = int(round(inicio_beats * self.beat * SR))
        fin = min(i + len(senal), self.buf.shape[1])
        s = senal[:fin - i] * vol
        izq, der = np.cos((pan + 1) * np.pi / 4), np.sin((pan + 1) * np.pi / 4)
        self.buf[0, i:fin] += s * izq
        self.buf[1, i:fin] += s * der

    def cerrar(self):
        b = self.buf
        # suaviza los agudos (bocinas de teléfono) y quita graves inaudibles; se filtra antes de plegar la cola
        bb, ab = butter(2, 5200 / (SR / 2))
        bh, ah = butter(1, 35 / (SR / 2), "high")
        b = lfilter(bh, ah, lfilter(bb, ab, b, axis=1), axis=1)
        b[:, :self.cola] += b[:, self.nb:self.nb + self.cola]     # pliega la cola para que el bucle sea continuo
        b = b[:, :self.nb]
        b = np.tanh(b * 1.1) / np.tanh(1.1)                         # limitador suave
        b = b / max(np.abs(b).max(), 1e-9) * 0.89
        return b


def segundos(m, beats):
    return int(beats * m.beat * SR)


def componer(p):
    rng = np.random.default_rng(p["semilla"])
    m = Mezcla(16, p["bpm"])
    raiz, esc = p["raiz"], p["escala"]
    acordes = p["acordes"] * (16 // len(p["acordes"]))
    melodia = p["melodia"]
    # si solo hay 8 compases de melodía se repite una octava más arriba la segunda vez
    if len(melodia) == 8:
        melodia = melodia + [[(None if g is None else g + p.get("sube", 7), d) for g, d in c] for c in melodia]

    for c in range(16):
        base = c * 4.0
        grado = acordes[c]
        triada = [grado, grado + 2, grado + 4, grado + 7]

        # melodía
        pos = 0.0
        for g, d in melodia[c]:
            if g is not None:
                f = hz(nota(raiz, esc, g))
                n = segundos(m, d * 0.96)
                vib = 0.003 if d >= 1.5 else 0.0
                s = onda("pulso", f, n, p["duty"], vib) * envolvente(n, 0.006, 0, 1.0, 0.04)
                if p.get("lead_pluck"):
                    s *= np.exp(-np.arange(n) / SR / (d * m.beat * 1.4 + 0.05))
                m.poner(base + pos, s, p["vol_lead"], -0.1)
            pos += d

        # bajo
        estilo = p["bajo"]
        rb = nota(raiz - 12, esc, grado)
        if estilo == "camina":
            for k, off in enumerate([0, 12, 7, 12, 0, 12, 7, 12]):
                n = segundos(m, 0.5 * 0.9)
                m.poner(base + k * 0.5, onda("tri", hz(rb + off), n) * envolvente(n, 0.004, 0, 1, 0.02), p["vol_bajo"])
        elif estilo == "empuja":
            for k, off in enumerate([0, 0, 12, 0, 0, 0, 12, 0]):
                n = segundos(m, 0.5 * 0.85)
                m.poner(base + k * 0.5, onda("tri", hz(rb + off), n) * envolvente(n, 0.003, 0.2, 1, 0.02), p["vol_bajo"])
        elif estilo == "mitad":
            for k, off in enumerate([0, 7]):
                n = segundos(m, 2 * 0.97)
                m.poner(base + k * 2, onda("tri", hz(rb + off), n) * envolvente(n, 0.01, 0, 1, 0.05), p["vol_bajo"])
        else:  # "larga"
            n = segundos(m, 4 * 0.98)
            m.poner(base, onda("tri", hz(rb), n) * envolvente(n, 0.03, 0, 1, 0.15), p["vol_bajo"])

        # arpegios
        arp = p["arp"]
        if arp:
            paso, pat, tau, vol, duty = arp
            for k, idx in enumerate(pat):
                f = hz(nota(raiz, esc, triada[idx]))
                n = segundos(m, paso * 1.0)
                s = onda("pulso", f, n, duty) * envolvente(n, 0.002, tau, 1, 0.01)
                m.poner(base + k * paso, s, vol, 0.45 if (k % 2 == 0) else 0.25)

        # colchón de acordes
        if p.get("pad"):
            n = segundos(m, 4.0)
            for t in triada[:3]:
                s = onda("tri", hz(nota(raiz, esc, t) ), n) * envolvente(n, 0.25, 0, 1, 0.3)
                m.poner(base, s, p["pad"], -0.4)

        # percusión (rejilla de 16 pasos por compás)
        d = p["bateria"]
        if d:
            for paso in range(16):
                t0 = base + paso * 0.25
                if paso in d.get("kick", ()):
                    m.poner(t0, kick(), d["vol"] * 1.0, 0)
                if paso in d.get("snare", ()):
                    m.poner(t0, snare(rng), d["vol"] * 0.7, 0.1)
                if paso in d.get("hat", ()):
                    m.poner(t0, hat(rng), d["vol"] * 0.45, -0.25)
    return m.cerrar()


# ------------------------------------------------------------------ las 5 piezas
# melodía: lista de compases; cada compás = [(grado, duración en tiempos)] y suma 4 tiempos.
R = None
PIEZAS = {
    "musica_dia": dict(
        semilla=1, bpm=126, raiz=60, escala=MAYOR, duty=0.5, vol_lead=0.30, vol_bajo=0.34,
        acordes=[0, 5, 3, 4, 0, 5, 3, 4, 3, 0, 4, 5, 3, 0, 4, 4],
        melodia=[
            [(4, .5), (4, .5), (2, .5), (4, .5), (7, 1), (4, 1)],
            [(5, 1), (4, .5), (2, .5), (4, 2)],
            [(5, .5), (7, .5), (5, .5), (3, .5), (5, 1), (7, 1)],
            [(6, 1), (5, .5), (4, .5), (4, 2)],
            [(7, .5), (9, .5), (7, .5), (4, .5), (7, 2)],
            [(5, .5), (7, .5), (9, 1), (7, 1), (5, 1)],
            [(7, .5), (5, .5), (3, .5), (5, .5), (7, 2)],
            [(6, .5), (8, .5), (6, .5), (4, .5), (4, 1), (R, 1)],
            [(7, 1), (5, .5), (7, .5), (5, 1), (3, 1)],
            [(4, 1), (2, .5), (4, .5), (7, 2)],
            [(6, 1), (4, .5), (6, .5), (8, 1), (6, 1)],
            [(7, 1), (5, .5), (7, .5), (9, 1), (7, 1)],
            [(7, .5), (8, .5), (9, 1), (7, 1), (5, 1)],
            [(4, .5), (5, .5), (4, 1), (2, 1), (0, 1)],
            [(1, 1), (2, 1), (4, 1), (6, 1)],
            [(8, 1), (6, 1), (4, 2)],
        ],
        bajo="camina", arp=(0.25, [0, 1, 2, 3, 2, 1, 2, 1] * 2, 0.09, 0.10, 0.125),
        bateria=dict(vol=0.30, kick=(0, 8), snare=(4, 12), hat=tuple(range(0, 16, 2))),
    ),
    "musica_atardecer": dict(
        semilla=2, bpm=108, raiz=67, escala=MAYOR, duty=0.25, vol_lead=0.26, vol_bajo=0.34, pad=0.05,
        acordes=[0, 4, 5, 2, 3, 0, 3, 4],
        melodia=[
            [(7, 1.5), (4, .5), (2, 1), (4, 1)],
            [(6, 1.5), (4, .5), (1, 1), (4, 1)],
            [(5, 1), (7, 1), (9, 1.5), (7, .5)],
            [(6, 1.5), (4, .5), (2, 2)],
            [(5, 1), (7, .5), (5, .5), (3, 1), (5, 1)],
            [(4, 1), (7, 1), (9, 2)],
            [(7, 1.5), (5, .5), (3, 1), (5, 1)],
            [(4, 1), (6, 1), (8, 2)],
        ],
        bajo="camina", arp=(0.5, [0, 1, 2, 3, 2, 1, 2, 3], 0.18, 0.09, 0.25),
        bateria=dict(vol=0.22, kick=(0, 8), snare=(4, 12), hat=tuple(range(0, 16, 2))),
    ),
    "musica_noche": dict(
        semilla=3, bpm=140, raiz=62, escala=MENOR, duty=0.25, vol_lead=0.28, vol_bajo=0.36,
        acordes=[0, 5, 2, 6, 0, 3, 6, 4],
        melodia=[
            [(7, .5), (9, .5), (7, .5), (4, .5), (2, 1), (4, 1)],
            [(5, .5), (7, .5), (9, 1), (7, .5), (5, .5), (7, 1)],
            [(6, .5), (4, .5), (2, .5), (4, .5), (6, 1), (4, 1)],
            [(6, .5), (8, .5), (10, 1), (8, 1), (6, 1)],
            [(7, .5), (9, .5), (7, .5), (4, .5), (2, .5), (4, .5), (7, 1)],
            [(7, 1), (5, .5), (3, .5), (5, 1), (7, 1)],
            [(6, .5), (8, .5), (6, .5), (4, .5), (6, 2)],
            [(4, 1), (6, 1), (8, 1), (6, 1)],
        ],
        bajo="empuja", arp=(0.25, [0, 2, 1, 3] * 4, 0.07, 0.09, 0.125),
        bateria=dict(vol=0.34, kick=(0, 4, 8, 12), snare=(4, 12), hat=tuple(range(0, 16, 2))),
    ),
    "musica_menu": dict(
        semilla=4, bpm=88, raiz=60, escala=MAYOR, duty=0.5, vol_lead=0.20, vol_bajo=0.30, pad=0.06,
        acordes=[0, 4, 5, 3, 0, 4, 3, 4],
        melodia=[
            [(4, 1), (7, 1), (4, 2)],
            [(6, 1), (4, 1), (8, 2)],
            [(7, 1), (5, 1), (9, 1), (7, 1)],
            [(5, 2), (3, 1), (5, 1)],
            [(7, 1.5), (9, .5), (7, 1), (4, 1)],
            [(8, 1.5), (6, .5), (4, 1), (6, 1)],
            [(7, 1), (5, 1), (3, 1), (5, 1)],
            [(4, 2), (6, 2)],
        ],
        bajo="mitad", lead_pluck=True, arp=(1.0, [0, 1, 2, 1], 0.5, 0.07, 0.25),
        bateria=dict(vol=0.12, hat=(4, 12)),
    ),
    "musica_fin": dict(
        semilla=5, bpm=76, raiz=57, escala=MENOR, duty=0.5, vol_lead=0.22, vol_bajo=0.30, pad=0.08,
        acordes=[0, 5, 3, 4, 0, 5, 3, 0],
        melodia=[
            [(4, 2), (2, 1), (0, 1)],
            [(5, 2), (4, 1), (2, 1)],
            [(3, 1.5), (5, .5), (7, 2)],
            [(6, 1), (4, 1), (2, 2)],
            [(4, 2), (7, 1), (4, 1)],
            [(5, 1), (7, 1), (9, 2)],
            [(7, 1), (5, 1), (3, 1), (2, 1)],
            [(0, 3), (R, 1)],
        ],
        bajo="larga", arp=None, bateria=dict(vol=0.20, kick=(0,)),
        sube=0,
    ),
}


def codificar(wav, ogg):
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "3", ogg], check=True)


def main():
    os.makedirs(RAW, exist_ok=True)
    for nombre, p in PIEZAS.items():
        audio = componer(p)
        with tempfile.TemporaryDirectory() as tmp:
            wav = os.path.join(tmp, nombre + ".wav")
            wavfile.write(wav, SR, (audio.T * 32767).astype(np.int16))
            codificar(wav, os.path.join(RAW, nombre + ".ogg"))
        print("%-18s %5.1f s" % (nombre, audio.shape[1] / SR))


if __name__ == "__main__":
    main()
