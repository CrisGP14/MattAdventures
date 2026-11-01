#!/usr/bin/env python3
"""Genera el icono de Matt Adventures: el logo del juego sobre un fondo de amanecer.

Lienzo adaptativo de 108 dp (432 px). Zona visible bajo cualquier mascara: 72 dp centrales;
zona segura: 66 dp. El logo se ajusta al ancho de la zona segura.
Salidas (en app/src/main/res):
  drawable-nodpi/ic_launcher_{background,foreground,monochrome}.png  (432x432)
  mipmap-*/ic_launcher.png y ic_launcher_round.png                   (iconos clasicos)
  mipmap-anydpi-v26/ic_launcher.xml y ic_launcher_round.xml
Uso: python3 herramientas/gen_icono.py   (necesita pillow y numpy)
"""
import math, os
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

AQUI = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(AQUI, "..", "app", "src", "main", "res")
LOGO = os.path.join(RES, "drawable-nodpi", "ml.png")
S = 432
SS = 2                      # supermuestreo del fondo para bordes suaves

def fondo():
    n = S * SS
    y, x = np.mgrid[0:n, 0:n].astype(np.float32)
    cx, cy = n * 0.5, n * 0.52
    # cielo: azul profundo arriba -> celeste calido en el horizonte
    t = np.clip(y / (n * 0.80), 0, 1)[..., None]
    c0, c1, c2 = np.array([28, 84, 214]), np.array([86, 178, 250]), np.array([255, 222, 140])
    img = np.where(t < 0.55, c0 + (c1 - c0) * (t / 0.55), c1 + (c2 - c1) * ((t - 0.55) / 0.45))
    # rayos de sol alternados, desde detras del logo
    ang = np.arctan2(y - cy, x - cx)
    rayos = (np.sin(ang * 12) > 0).astype(np.float32)[..., None]
    dist = np.sqrt((x - cx) ** 2 + (y - cy) ** 2) / (n * 0.72)
    fuerza = np.clip(1 - dist, 0, 1)[..., None]
    img = img + rayos * fuerza * 26 * np.array([1.0, 1.0, 0.75])
    # resplandor central calido para que el logo destaque
    glow = np.clip(1 - np.sqrt((x - cx) ** 2 + ((y - cy) * 1.5) ** 2) / (n * 0.42), 0, 1)[..., None] ** 1.6
    img = img * (1 - glow * 0.45) + np.array([255, 233, 160]) * glow * 0.45
    im = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    d = ImageDraw.Draw(im)
    # nubes suaves
    def nube(px, py, w):
        for dx, dy, r in ((0, 0, .5), (-.32, .12, .36), (.34, .1, .4), (.05, .16, .45)):
            d.ellipse([px + dx * w - r * w * .5, py + dy * w - r * w * .5,
                       px + dx * w + r * w * .5, py + dy * w + r * w * .5], fill=(255, 255, 255, 235))
    nube(n * 0.18, n * 0.2, n * 0.2)
    nube(n * 0.82, n * 0.15, n * 0.16)
    nube(n * 0.84, n * 0.62, n * 0.12)
    # colinas con pasto
    def colina(base, amp, fase, c1, c2):
        pts = [(0, n)]
        for i in range(0, n + 1, 8):
            pts.append((i, base + amp * math.sin(i / n * math.pi * 2 + fase)))
        pts.append((n, n))
        d.polygon(pts, fill=c1)
        d.line(pts[1:-1], fill=c2, width=int(n * 0.012))
    colina(n * 0.73, n * 0.03, 1.0, (60, 160, 78, 255), (120, 214, 98, 255))
    colina(n * 0.80, n * 0.025, 3.6, (36, 126, 62, 255), (84, 182, 84, 255))
    return im.resize((S, S), Image.LANCZOS)

def logo_ajustado():
    lg = Image.open(LOGO).convert("RGBA")
    lg = lg.crop(lg.getchannel("A").point(lambda v: 255 if v > 8 else 0).getbbox())
    ancho = int(S * 72 / 108)
    lg = lg.resize((ancho, round(lg.height * ancho / lg.width)), Image.LANCZOS)
    return lg

def primer_plano(lg):
    fg = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    pos = ((S - lg.width) // 2, (S - lg.height) // 2 - 14)
    # sombra suave bajo el logo para despegarlo del fondo
    sh = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    sh.paste((20, 30, 70, 150), (pos[0], pos[1] + 7), lg.getchannel("A"))
    fg.alpha_composite(sh.filter(ImageFilter.GaussianBlur(5)))
    fg.alpha_composite(lg, pos)
    return fg, pos

def monocromo(lg, pos):
    """Letras en blanco; el contorno oscuro del logo queda transparente para separarlas."""
    a = np.array(lg).astype(np.float32)
    lum = a[..., :3].mean(axis=2)
    alfa = a[..., 3] * np.clip((lum - 90) / 60, 0, 1)
    m = np.zeros((S, S, 4), np.uint8)
    h, w = alfa.shape
    m[pos[1]:pos[1] + h, pos[0]:pos[0] + w, :3] = 255
    m[pos[1]:pos[1] + h, pos[0]:pos[0] + w, 3] = alfa.astype(np.uint8)
    return Image.fromarray(m, "RGBA")

def guardar(im, ruta):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    im.save(ruta)

def main():
    bg = fondo()
    lg = logo_ajustado()
    fg, pos = primer_plano(lg)
    mono = monocromo(lg, pos)
    nodpi = os.path.join(RES, "drawable-nodpi")
    guardar(bg, os.path.join(nodpi, "ic_launcher_background.png"))
    guardar(fg, os.path.join(nodpi, "ic_launcher_foreground.png"))
    guardar(mono, os.path.join(nodpi, "ic_launcher_monochrome.png"))

    comp = bg.copy(); comp.alpha_composite(fg)
    vis = comp.crop((S // 6, S // 6, S - S // 6, S - S // 6))      # los 72 dp visibles
    for carpeta, lado in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)):
        base = vis.resize((lado, lado), Image.LANCZOS)
        def con_mascara(forma):
            m = Image.new("L", (lado * 4, lado * 4), 0)
            dm = ImageDraw.Draw(m)
            if forma == "circulo": dm.ellipse([0, 0, lado * 4 - 1, lado * 4 - 1], fill=255)
            else: dm.rounded_rectangle([0, 0, lado * 4 - 1, lado * 4 - 1], radius=lado * 4 // 6, fill=255)
            r = base.copy(); r.putalpha(m.resize((lado, lado), Image.LANCZOS)); return r
        guardar(con_mascara("cuadrado"), os.path.join(RES, "mipmap-" + carpeta, "ic_launcher.png"))
        guardar(con_mascara("circulo"), os.path.join(RES, "mipmap-" + carpeta, "ic_launcher_round.png"))

    xml = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
"""
    for nombre in ("ic_launcher.xml", "ic_launcher_round.xml"):
        ruta = os.path.join(RES, "mipmap-anydpi-v26", nombre)
        os.makedirs(os.path.dirname(ruta), exist_ok=True)
        open(ruta, "w").write(xml)

    # previsualizacion bajo mascaras (no va en el APK) + version tematica
    prev = Image.new("RGBA", (5 * 232, 250), (40, 44, 60, 255))
    for k, forma in enumerate(("circulo", "squircle", "cuadrado", "completo", "mono")):
        t = (comp if forma != "mono" else Image.new("RGBA", (S, S), (70, 80, 120, 255))).resize((216, 216), Image.LANCZOS)
        if forma == "mono":
            mm = mono.resize((216, 216), Image.LANCZOS); t.alpha_composite(mm)
        ms = Image.new("L", (216, 216), 0); dm = ImageDraw.Draw(ms)
        a, b = 36, 180
        if forma in ("circulo", "mono"): dm.ellipse([a, a, b, b], fill=255)
        elif forma == "squircle": dm.rounded_rectangle([a, a, b, b], radius=40, fill=255)
        elif forma == "cuadrado": dm.rounded_rectangle([a, a, b, b], radius=12, fill=255)
        else: ms = Image.new("L", (216, 216), 255)
        t.putalpha(ms)
        prev.alpha_composite(t, (k * 232 + 8, 16))
    prev.save("/tmp/icono_previa.png")

if __name__ == "__main__":
    main()
