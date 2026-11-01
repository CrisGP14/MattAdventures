# Genera el arte del escenario en pixel art de verdad (sin degradados suaves ni suavizado):
#   cielo_N     cielo en bandas con tramado (dithering)           luna       luna creciente
#   nubes_N     3 nubes con luz y sombra en bloques                arboles_N / lejanos_N   pinos cercanos y lejanos (con bruma)
#   arbustos_N  seto que "arraiga" los arboles                     libro_verde/azul/rojo   obstaculos
#   corazon_lleno / corazon_vacio  vidas del marcador
# N = 1 dia, 2 atardecer, 3 noche.   Uso: python3 herramientas/gen_pixel.py   (necesita pillow, numpy)
# Cada pixel de arte se guarda ampliado (x4 o x3) para que en la app 1 pixel de imagen = 1 dp.
import os, math, random
import numpy as np
from PIL import Image, ImageDraw

AQUI = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(AQUI, "..", "app", "src", "main", "res", "drawable-nodpi") + os.sep


def hx(s):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16))


def guardar(arr, nombre, k):
    """arr: array RGBA a resolucion de arte -> se amplia k veces SIN suavizar y se guarda."""
    im = Image.fromarray(arr.astype(np.uint8), "RGBA")
    im = im.resize((im.width * k, im.height * k), Image.NEAREST)
    im.save(RES + nombre)
    return im.size


BAYER4 = (np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]) + 0.5) / 16.0

# ------------------------------------------------------------------ CIELO (bandas + tramado)
CIELO = {   # los mismos colores del cielo original, ahora cuantizados en bandas
    1: [(0.0, "#19CAFC"), (0.33, "#43D3FB"), (0.66, "#76DFFA"), (1.0, "#9CE8F9")],
    2: [(0.0, "#FF7201"), (0.33, "#FD8A04"), (0.66, "#FCAA0A"), (1.0, "#FCC20E")],
    3: [(0.0, "#0C2BAF"), (0.33, "#1F41B6"), (0.66, "#3962C0"), (1.0, "#4E7BC9")],
}
BANDAS, ALTO_CIELO, ANCHO_CIELO = 8, 100, 4


def color_en(paradas, t):
    for (t0, c0), (t1, c1) in zip(paradas, paradas[1:]):
        if t0 <= t <= t1:
            f = (t - t0) / (t1 - t0)
            a, b = hx(c0), hx(c1)
            return tuple(a[i] + (b[i] - a[i]) * f for i in range(3))
    return hx(paradas[-1][1])


def cielo(n):
    cols = [color_en(CIELO[n], i / (BANDAS - 1)) for i in range(BANDAS)]
    img = np.zeros((ALTO_CIELO, ANCHO_CIELO, 4), np.uint8)
    img[..., 3] = 255
    for y in range(ALTO_CIELO):
        pos = y / (ALTO_CIELO - 1) * (BANDAS - 1)
        b = min(int(pos), BANDAS - 2)
        v = pos - b
        for x in range(ANCHO_CIELO):
            q = min(1.0, max(0.0, (v - 0.5) * 2.4 + 0.5))      # solo se tramado cerca del borde de la banda
            img[y, x, :3] = cols[b + 1] if q > BAYER4[y % 4, x % 4] else cols[b]
    return img


# ------------------------------------------------------------------ LUNA
def luna():
    S = 24
    yy, xx = np.mgrid[0:S, 0:S]
    d1 = np.hypot(xx - 11.5, yy - 12.5)
    d2 = np.hypot(xx - 17.0, yy - 9.5)
    forma = (d1 <= 10.4) & (d2 > 8.6)
    img = np.zeros((S, S, 4), np.uint8)
    claro, base, sombra, borde = hx("#FFFFFF"), hx("#DDF1FF"), hx("#A5D0F4"), hx("#6E9BD8")
    cerca_corte = d2 <= 10.6
    for y in range(S):
        for x in range(S):
            if not forma[y, x]:
                continue
            if d1[y, x] > 9.0:
                c = claro
            elif cerca_corte[y, x]:
                c = sombra
            else:
                c = base
            img[y, x] = c + (255,)
    for (cx, cy) in ((6, 14), (8, 8), (5, 10)):       # crateres
        for dx, dy in ((0, 0), (1, 0), (0, 1)):
            if forma[cy + dy, cx + dx]:
                img[cy + dy, cx + dx] = sombra + (255,)
    # contorno
    sal = img.copy()
    for y in range(S):
        for x in range(S):
            if img[y, x, 3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                xx2, yy2 = x + dx, y + dy
                if 0 <= xx2 < S and 0 <= yy2 < S and img[yy2, xx2, 3]:
                    sal[y, x] = borde + (255,)
                    break
    return sal


# ------------------------------------------------------------------ NUBES
FORMAS = [   # (cx, cy, r) a resolucion de arte, en una celda de 65 x 33
    [(10, 23, 5), (19, 18, 7), (30, 13, 9), (42, 17, 7), (52, 22, 6), (20, 24, 5), (43, 24, 5)],
    [(9, 23, 5), (18, 18, 7), (30, 17, 7), (41, 22, 5), (20, 24, 4), (31, 24, 5)],
    [(9, 23, 5), (18, 19, 6), (28, 22, 5), (19, 24, 4)],
]
BASE_NUBE = 28
PAL_NUBE = {   # (luz, cuerpo, sombra, contorno)
    1: ("#FFFFFF", "#E4F4FF", "#B6D8F5", "#7DB4E8"),
    2: ("#FFF3DC", "#FFD0A0", "#F29A78", "#D2644A"),
    3: ("#8FA6E8", "#6478C8", "#4A5AA8", "#2F3C86"),
}


def limpiar(m):
    """Quita solo los pixeles que sobresalen de a uno (pinchitos) y rellena huecos de 1 pixel; conserva los bultos."""
    m = m.copy()
    v = (np.roll(m, 1, 0).astype(int) + np.roll(m, -1, 0) + np.roll(m, 1, 1) + np.roll(m, -1, 1))
    m = np.where(m & (v <= 1), False, m)
    v = (np.roll(m, 1, 0).astype(int) + np.roll(m, -1, 0) + np.roll(m, 1, 1) + np.roll(m, -1, 1))
    m = np.where(~m & (v == 4), True, m)
    return m


def nube(forma, n):
    W, H = 65, 33
    luz, cuerpo, sombra, borde = (hx(c) for c in PAL_NUBE[n])
    yy, xx = np.mgrid[0:H, 0:W]
    m = np.zeros((H, W), bool)
    for cx, cy, r in forma:
        m |= np.hypot(xx - cx, yy - cy) <= r
    xmin, xmax = min(c[0] for c in forma), max(c[0] for c in forma)
    m[BASE_NUBE - 9:BASE_NUBE + 1, xmin:xmax + 1] = True      # base plana continua (sin huecos bajo los bultos)
    m[BASE_NUBE + 1:, :] = False
    m = limpiar(m)
    filas = np.where(m.any(axis=1))[0]
    top, bot = filas[0], filas[-1]
    img = np.zeros((H, W, 4), np.uint8)
    for y in range(H):
        for x in range(W):
            if not m[y, x]:
                continue
            t = (y - top) / max(1, bot - top)                  # 0 arriba, 1 abajo
            v = t * 2.0
            b = min(int(v), 1)
            f = v - b
            q = min(1.0, max(0.0, (f - 0.5) * 3.0 + 0.5))
            tonos = (luz, cuerpo, sombra)
            c = tonos[b + 1] if q > BAYER4[y % 4, x % 4] else tonos[b]
            img[y, x] = c + (255,)
    sal = img.copy()
    for y in range(H):
        for x in range(W):
            if m[y, x]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                x2, y2 = x + dx, y + dy
                if 0 <= x2 < W and 0 <= y2 < H and m[y2, x2]:
                    sal[y, x] = borde + (255,)
                    break
    return sal


# ------------------------------------------------------------------ PINOS
PAL_PINO = {   # (contorno, sombra, cuerpo, luz, tronco)
    1: ("#0A4A1C", "#168A2C", "#25B73A", "#5FE35C", "#6B3E1E"),
    2: ("#1B4A12", "#2A8A2C", "#3DBA46", "#7BE070", "#5C3A1A"),
    3: ("#03150E", "#0A3D27", "#0F5738", "#1B7A52", "#2A1A10"),
}
BRUMA = {1: (150, 226, 246), 2: (252, 176, 20), 3: (40, 70, 150)}


def pino(h, w, pal, bruma=None, mezcla=0.0):
    O, D, M, L, T = (hx(c) for c in pal)
    if bruma is not None:
        mix = lambda c: tuple(int(c[i] * (1 - mezcla) + bruma[i] * mezcla) for i in range(3))
        O, D, M, L, T = mix(O), mix(D), mix(M), mix(L), mix(T)
        O = mix(D)                                  # lejos no hay contorno oscuro: se funde con la bruma
    cx = w // 2
    ids = np.zeros((h, w), int)
    tronco = 2 if h >= 14 else 1
    cuerpo = h - tronco
    nt = 4 if h >= 20 else 3
    paso = cuerpo / (nt + 0.9)
    alto_tier = int(round(paso * 1.9))
    for t in range(nt):
        y0 = int(round(t * paso))
        wmax = (w / 2.0) * (0.5 + 0.5 * (t + 1) / nt)
        for r in range(alto_tier):
            y = y0 + r
            if y >= cuerpo:
                break
            mitad = int(round(wmax * (r + 1) / alto_tier))
            for x in range(cx - mitad, cx + mitad + 1):
                if 0 <= x < w:
                    ids[y, x] = t + 1
    img = np.zeros((h, w, 4), np.uint8)
    for y in range(cuerpo):
        xs = np.where(ids[y] > 0)[0]
        if len(xs) == 0:
            continue
        l, r = xs[0], xs[-1]
        for x in xs:
            u = (x - l) / max(1, r - l)
            c = L if u < 0.28 else (D if u > 0.70 else M)
            borde_abajo = y + 1 >= cuerpo or ids[y + 1, x] != ids[y, x]
            if borde_abajo and ids[y, x] > 1 or (borde_abajo and y + 1 >= cuerpo):
                c = D
            img[y, x] = c + (255,)
    for y in range(cuerpo, h):
        for x in (cx - 1, cx):
            if 0 <= x < w:
                img[y, x] = T + (255,)
    sal = img.copy()
    for y in range(h):
        for x in range(w):
            if img[y, x, 3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                x2, y2 = x + dx, y + dy
                if 0 <= x2 < w and 0 <= y2 < h and img[y2, x2, 3]:
                    sal[y, x] = O + (255,)
                    break
    return sal


def hilera(ancho, alto, n, semilla, hmin, hmax, wmin, wmax, sep_min, sep_max, bruma=None, mezcla=0.0):
    """Una franja de pinos periodica (sin costura). Mismo reparto para los tres momentos del dia."""
    rnd = random.Random(semilla)
    lienzo = np.zeros((alto, ancho, 4), np.uint8)
    x = rnd.randint(3, 8)
    arboles = []
    while x < ancho:
        h = rnd.randint(hmin, hmax)
        w = rnd.randint(wmin, wmax) | 1
        arboles.append((x, h, w))
        x += rnd.randint(sep_min, sep_max)
    arboles.sort(key=lambda t: t[1], reverse=True)          # los altos atras
    for (x, h, w) in arboles:
        sprite = pino(h, w, PAL_PINO[n], bruma, mezcla)
        for yy in range(h):
            for xx in range(w):
                if sprite[yy, xx, 3]:
                    lienzo[alto - h + yy, (x - w // 2 + xx) % ancho] = sprite[yy, xx]
    return lienzo


# ------------------------------------------------------------------ ARBUSTO (seto)
PAL_SETO = {
    1: dict(D="#0F5F1E", H="#5EDC4A", M="#2FA83A", S="#1E7F2C"),
    2: dict(D="#2A4A10", H="#A8CF3A", M="#4E9A22", S="#36701A"),
    3: dict(D="#0A3B2A", H="#3FBF80", M="#198A55", S="#116B42"),
}


def seto(n, semilla):
    W, H = 500, 10
    rnd = random.Random(semilla)
    p = {k: hx(v) for k, v in PAL_SETO[n].items()}
    masa = np.zeros((H, W), bool)
    x = 0
    while x < W:
        r = rnd.randint(4, 8)
        cx = x + r
        for xx in range(cx - r, cx + r + 1):
            alto = int(round(math.sqrt(max(0, r * r - (xx - cx) ** 2))))
            for y in range(H - alto, H):
                masa[y, xx % W] = True
        x += rnd.randint(5, 10)
    img = np.zeros((H, W, 4), np.uint8)
    arriba = ~np.roll(masa, 1, axis=0)
    izq = ~np.roll(masa, 1, axis=1)
    for y in range(H):
        for xx in range(W):
            if not masa[y, xx]:
                continue
            if arriba[y, xx]:
                c = p["D"]
            elif y > 0 and arriba[y - 1, xx] or izq[y, xx]:
                c = p["H"]
            elif y >= H - 3:
                c = p["S"]
            else:
                c = p["M"]
            img[y, xx] = c + (255,)
    for _ in range(90):
        xx = rnd.randrange(W)
        ys = np.where(masa[:, xx])[0]
        if len(ys) < 4:
            continue
        y = rnd.choice(list(ys[2:-1]))
        img[y, xx] = (p["H"] if rnd.random() < 0.5 else p["S"]) + (255,)
    return img


# ------------------------------------------------------------------ LIBROS (46 x 34, 1 pixel = 1 dp, como Matt)
LIBROS = {   # nombre: (cubierta, cubierta clara, cubierta oscura, contorno)
    "verde": ("#2E8B57", "#4FBF7F", "#1B5E3A", "#0E2F20"),
    "azul": ("#2F63C0", "#5C93F0", "#1F427F", "#0F2147"),
    "rojo": ("#C0392B", "#EE6A58", "#842318", "#431009"),
}
PAPEL, PAPEL_SOMBRA, ORO, ORO_OSCURO = hx("#F6EBC8"), hx("#D8C79C"), hx("#F4C430"), hx("#B8860B")


def libro(var):
    cub, claro, osc, borde = (hx(c) for c in LIBROS[var])
    W, H = 46, 34
    A, B, C, D = (1, 14), (31, 4), (45, 12), (15, 22)
    T = 11
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)

    def P(o, u, v, s, t):
        return (int(round(o[0] + u[0] * s + v[0] * t)), int(round(o[1] + u[1] * s + v[1] * t)))

    u = (B[0] - A[0], B[1] - A[1])            # largo de la tapa
    v = (D[0] - A[0], D[1] - A[1])            # ancho de la tapa
    dc = (C[0] - D[0], C[1] - D[1])           # lado largo, frente-derecha
    g = (0, T)                                 # grosor

    # lado largo (derecha): bloque de PAGINAS entre dos tapas
    d.polygon([D, C, (C[0], C[1] + T), (D[0], D[1] + T)], fill=PAPEL)
    for (t0, t1, col) in ((0.0, 0.20, cub), (0.80, 1.0, osc)):
        d.polygon([P(D, dc, g, 0, t0), P(D, dc, g, 1, t0), P(D, dc, g, 1, t1), P(D, dc, g, 0, t1)], fill=col)
    for t in (0.40, 0.55, 0.70):
        d.line([P(D, dc, g, 0.03, t), P(D, dc, g, 0.97, t)], fill=PAPEL_SOMBRA)

    # lado corto (izquierda): LOMO con una banda dorada
    d.polygon([A, D, (D[0], D[1] + T), (A[0], A[1] + T)], fill=osc)
    d.polygon([P(A, v, g, 0.25, 0.30), P(A, v, g, 0.75, 0.30), P(A, v, g, 0.75, 0.62), P(A, v, g, 0.25, 0.62)], fill=ORO)
    d.line([P(A, v, g, 0.04, 0.12), P(A, v, g, 0.96, 0.12)], fill=cub)

    # tapa: cubierta con marco claro y etiqueta
    d.polygon([A, B, C, D], fill=cub)
    d.line([P(A, u, v, 0.07, 0.12), P(A, u, v, 0.93, 0.12), P(A, u, v, 0.93, 0.88), P(A, u, v, 0.07, 0.88), P(A, u, v, 0.07, 0.12)], fill=claro)
    d.polygon([P(A, u, v, 0.30, 0.30), P(A, u, v, 0.70, 0.30), P(A, u, v, 0.70, 0.70), P(A, u, v, 0.30, 0.70)], fill=ORO)
    d.line([P(A, u, v, 0.36, 0.44), P(A, u, v, 0.64, 0.44)], fill=ORO_OSCURO)
    d.line([P(A, u, v, 0.36, 0.58), P(A, u, v, 0.58, 0.58)], fill=ORO_OSCURO)
    d.line([A, B], fill=claro)

    # contorno
    d.line([A, B, C, (C[0], C[1] + T), (D[0], D[1] + T), (A[0], A[1] + T), A], fill=borde)
    d.line([A, D, C], fill=borde)
    d.line([D, (D[0], D[1] + T)], fill=borde)
    return np.array(im)


# ------------------------------------------------------------------ CORAZONES (9 x 8)
CORAZON = [
    ".OO...OO.",
    "OLLO.OMMO",
    "OLMMOMMMO",
    "OMMMMMMMO",
    ".OMMMMMO.",
    "..OMMMO..",
    "...OMO...",
    "....O....",
]


def corazon(lleno):
    pal = ({"O": "#4A0F12", "L": "#FF9A9A", "M": "#E53935"} if lleno
           else {"O": "#1E2740", "L": "#7C8AAE", "M": "#55618A"})
    img = np.zeros((8, 9, 4), np.uint8)
    for y, fila in enumerate(CORAZON):
        for x, ch in enumerate(fila):
            if ch in pal:
                img[y, x] = hx(pal[ch]) + (255,)
    if lleno:                     # sombra a la derecha-abajo
        for y, fila in enumerate(CORAZON):
            for x, ch in enumerate(fila):
                if ch == "M" and (x >= 6 or y >= 5):
                    img[y, x] = hx("#B71C1C") + (255,)
    return img


# ------------------------------------------------------------------ SALIDA
if __name__ == "__main__":
    for n in (1, 2, 3):
        guardar(cielo(n), f"cielo_{n}.png", 4)
        hoja = np.zeros((33, 65 * 3, 4), np.uint8)
        for i, f in enumerate(FORMAS):
            hoja[:, i * 65:(i + 1) * 65] = nube(f, n)
        guardar(hoja, f"nubes_{n}.png", 4)
        guardar(hilera(250, 28, n, 7, 18, 26, 12, 17, 15, 23), f"arboles_{n}.png", 4)
        guardar(hilera(250, 18, n, 19, 11, 16, 8, 11, 11, 17, BRUMA[n], 0.62 if n != 3 else 0.55), f"lejanos_{n}.png", 4)
        guardar(seto(n, {1: 5, 2: 9, 3: 14}[n]), f"arbustos_{n}.png", 4)
    guardar(luna(), "luna.png", 4)
    for var in LIBROS:
        guardar(libro(var), f"libro_{var}.png", 1)
    guardar(corazon(True), "corazon_lleno.png", 3)
    guardar(corazon(False), "corazon_vacio.png", 3)
    print("arte pixel listo")
