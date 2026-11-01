# Rellena el logo original (letras solo con contorno amarillo) conservando la misma tipografia.
# Uso: python3 herramientas/gen_logo.py      (necesita: pillow numpy scipy)
# Entrada : herramientas/fuentes/ml_original.png     Salida: app/src/main/res/drawable-nodpi/ml.png
import os
import numpy as np
from PIL import Image
from scipy import ndimage as ndi

AQUI = os.path.dirname(os.path.abspath(__file__))
ENTRADA = os.path.join(AQUI, "fuentes", "ml_original.png")
SALIDA = os.path.join(AQUI, "..", "app", "src", "main", "res", "drawable-nodpi", "ml.png")
S = 4          # ampliacion (el original es chico: 339x126)
PAD = 34       # margen para el borde exterior y la sombra

def suavizar(mascara, sigma):
    return ndi.gaussian_filter(mascara.astype(float), sigma)

src = Image.open(ENTRADA).convert("RGBA")
a = np.array(src.getchannel("A").resize((src.width * S, src.height * S), Image.BICUBIC)).astype(float) / 255
a = ndi.gaussian_filter(a, 1.2)
O = ndi.binary_closing(a > 0.40, iterations=2)            # contorno amarillo original, ya limpio

# ---- interior de cada letra y huecos (A, D, R) ----
lab, n = ndi.label(~O)
H, W = O.shape
fuera = set(np.unique(np.concatenate([lab[0], lab[-1], lab[:, 0], lab[:, -1]]))) - {0}
objs = ndi.find_objects(lab)
interiores = [i for i in range(1, n + 1) if i not in fuera]
caja = {i: (objs[i - 1][1].start, objs[i - 1][0].start, objs[i - 1][1].stop, objs[i - 1][0].stop) for i in interiores}
def dentro(a_, b_):   # caja a_ completamente dentro de la caja b_
    return a_[0] >= b_[0] and a_[1] >= b_[1] and a_[2] <= b_[2] and a_[3] <= b_[3]
huecos = {i for i in interiores for j in interiores if i != j and dentro(caja[i], caja[j]) and caja[i] != caja[j]}
cuerpo = np.isin(lab, [i for i in interiores if i not in huecos])
forma = O | cuerpo                                         # todo lo que es "letra" (contorno + relleno)

# ---- lienzo con margen ----
def pad(m): return np.pad(m, PAD, mode="constant")
forma, cuerpo, O = pad(forma), pad(cuerpo), pad(O)
HH, WW = forma.shape
yy = np.arange(HH)[:, None] * np.ones((1, WW))

# degradado vertical por renglon (el logo tiene 2 renglones)
mitad = (np.where(forma.any(axis=1))[0])
filas = np.where(forma.any(axis=1))[0]
cortes = np.where(np.diff(filas) > 8)[0]
corte = filas[cortes[0]] + 4 if len(cortes) else HH // 2
def t_renglon(y):
    t = np.zeros_like(y)
    r1 = y < corte
    y0, y1 = filas[0], corte
    t[r1] = (y[r1] - y0) / max(1, (y1 - y0))
    y0b, y1b = corte, filas[-1]
    t[~r1] = (y[~r1] - y0b) / max(1, (y1b - y0b))
    return np.clip(t, 0, 1)
t = t_renglon(yy)

def mezcla(c0, c1, f):
    c0, c1 = np.array(c0, float), np.array(c1, float)
    return c0[None, None, :] * (1 - f[..., None]) + c1[None, None, :] * f[..., None]

def capa(color_rgb, alfa):
    return np.dstack([np.broadcast_to(np.array(color_rgb, float), (HH, WW, 3)), alfa * 255])

def sobre(base, nueva):
    a0, a1 = base[..., 3:4] / 255, nueva[..., 3:4] / 255
    sal_a = a1 + a0 * (1 - a1)
    rgb = (nueva[..., :3] * a1 + base[..., :3] * a0 * (1 - a1)) / np.maximum(sal_a, 1e-6)
    return np.dstack([rgb, sal_a * 255])

lienzo = np.zeros((HH, WW, 4))

# 1) sombra suave debajo
sombra = np.roll(suavizar(ndi.binary_dilation(forma, iterations=10), 7), 12, axis=0)
lienzo = sobre(lienzo, capa((20, 10, 0), sombra * 0.40))

# 2) borde exterior oscuro (redondeado)
borde = suavizar(ndi.binary_dilation(forma, iterations=11), 2.2)
borde = np.clip((borde - 0.35) * 3.0, 0, 1)
lienzo = sobre(lienzo, capa((74, 31, 0), borde))

# 3) aro claro (el contorno amarillo original) con degradado suave
aro = np.clip(suavizar(O | cuerpo, 1.3) * 1.0, 0, 1)
lienzo = sobre(lienzo, np.dstack([mezcla((255, 250, 190), (255, 214, 64), t), aro * 255]))

# 4) relleno de las letras: amarillo arriba -> naranja abajo
cuerpo_s = np.clip(suavizar(cuerpo, 1.3), 0, 1)
relleno = mezcla((255, 235, 40), (255, 128, 0), np.clip(t * 1.1, 0, 1))
lienzo = sobre(lienzo, np.dstack([relleno, cuerpo_s * 255]))

# 5) bisel: luz arriba-izquierda, sombra abajo-derecha, dentro del relleno
desp_ul = np.roll(np.roll(cuerpo, 7, axis=0), 7, axis=1)
luz = cuerpo & ~desp_ul
desp_dr = np.roll(np.roll(cuerpo, -7, axis=0), -7, axis=1)
som = cuerpo & ~desp_dr
lienzo = sobre(lienzo, capa((255, 255, 255), suavizar(luz, 1.2) * 0.50))
lienzo = sobre(lienzo, capa((160, 60, 0), suavizar(som, 1.2) * 0.35))

# 6) brillo: franja clara en el tercio superior de cada renglon
brillo = cuerpo * np.clip(1 - t / 0.42, 0, 1) * 0.30
lienzo = sobre(lienzo, capa((255, 255, 255), suavizar(brillo, 1.0)))

img = Image.fromarray(np.clip(lienzo, 0, 255).astype(np.uint8), "RGBA")
img.save(SALIDA)
print("logo", img.size, "huecos dejados vacios:", sorted(huecos))
