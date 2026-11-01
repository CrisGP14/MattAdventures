# Regenera el piso (pasto + tierra, 3 paletas) del escenario.
# Uso:  python3 herramientas/gen_piso.py   (necesita: pip install pillow numpy)
# Para cambiar colores edita el diccionario PAL; para mas/menos flores o piedras, los rangos de cada bucle.
# Genera el piso del juego en pixel art (500x24 "pixeles de arte", se amplia x4 a 2000x96) y los arboles recortados.
from PIL import Image
import numpy as np, math, random, os
import os
AQUI=os.path.dirname(os.path.abspath(__file__))
RES=os.path.join(AQUI,"..","app","src","main","res","drawable-nodpi")+os.sep
W,H=500,24

def hx(s): s=s.lstrip("#"); return (int(s[0:2],16),int(s[2:4],16),int(s[4:6],16),255)

PAL={
 "dia":   dict(D="#145A17",H="#8CF04F",M="#3DBE2B",S="#2A8F1E",G="#1F7A1B",
               E1="#8F541C",E2="#C47A2C",E3="#9A5B22",PL="#E2A15A",PD="#7A4517",ST="#A8651F",
               F=["#FFE14D","#FFFFFF","#FF7AA8"]),
 "tarde": dict(D="#234D10",H="#B6D83C",M="#5DAA24",S="#3E7E1A",G="#336B15",
               E1="#6E4A15",E2="#9C6A1F",E3="#7C5418",PL="#C69A4D",PD="#5A3A10",ST="#85581A",
               F=["#FFB347","#FFFFFF","#FFE14D"]),
 "noche": dict(D="#0E4A2D",H="#4FD08A",M="#1F9A5A",S="#166E44",G="#115C39",
               E1="#5E3B20",E2="#8A5A32",E3="#6B4527",PL="#B58A5F",PD="#46291A",ST="#774C2A",
               F=["#BFD8FF","#FFFFFF","#9AB8FF"]),
}

def periodica(x, fases, ks, amps):
    return sum(a*math.sin(2*math.pi*k*x/W+f) for f,k,a in zip(fases,ks,amps))

def generar(nombre, semilla):
    p={k:(hx(v) if isinstance(v,str) else [hx(c) for c in v]) for k,v in PAL[nombre].items()}
    rnd=random.Random(semilla)
    img=np.zeros((H,W,4),np.uint8)
    def put(x,y,c):
        if 0<=y<H: img[y,x%W]=c
    f1,f2,f3=[rnd.uniform(0,6.28) for _ in range(3)]
    # borde superior del pasto y borde inferior (ondulados, periodicos => sin costura)
    T=[int(round(3.6+periodica(x,(f1,f2),(5,13),(0.9,0.7)))) for x in range(W)]
    T=[min(5,max(2,t)) for t in T]
    B=[T[x]+8+int(round(1.2+periodica(x,(f3,f1),(9,23),(0.9,0.6)))) for x in range(W)]
    for x in range(W):
        t,b=T[x],B[x]
        put(x,t,p["D"])                                   # contorno superior
        for y in range(t+1,t+3): put(x,y,p["H"])          # brillo
        for y in range(t+3,t+7): put(x,y,p["M"])          # cuerpo
        for y in range(t+7,b+1): put(x,y,p["S"])          # sombra
        put(x,b+1,p["D"])                                 # contorno inferior del pasto
        # tierra
        for y in range(b+2,H):
            d=y-(b+2)
            c=p["E1"] if d<2 else (p["E3"] if y>=H-3 else p["E2"])
            put(x,y,c)
    # motas de luz y de sombra dentro del pasto
    for _ in range(70):
        x=rnd.randrange(W); y=T[x]+rnd.randrange(3,7)
        put(x,y,p["H"] if rnd.random()<0.55 else p["S"])
    # trazos de hierba (V pequenas) en el cuerpo del pasto
    for _ in range(26):
        x=rnd.randrange(W); y=T[x]+4
        put(x,y,p["G"]); put(x,y+1,p["G"]); put(x-1,y-1,p["H"]); put(x+1,y-1,p["H"])
    # puntas de pasto que sobresalen del borde
    x=0
    while x<W:
        x+=rnd.randint(4,11)
        alto=rnd.choice((1,1,2,2,3))
        for k in range(1,alto+1): put(x,T[x%W]-k,p["M"])
        put(x,T[x%W]-alto,p["H"])
        if rnd.random()<0.5: put(x+1,T[(x+1)%W]-1,p["M"])
    # flores
    for _ in range(11):
        x=rnd.randrange(3,W-3); col=rnd.choice(p["F"]); t=T[x]
        put(x,t-1,p["G"]); put(x,t-2,p["G"])
        for dx,dy in ((0,-4),(-1,-3),(1,-3),(0,-3),(0,-2)) if False else ((-1,-3),(0,-3),(1,-3),(0,-4),(0,-2)): put(x+dx,t+dy,col)
        put(x,t-3,hx("#FFD23A") if col!=p["F"][0] else hx("#FF9B1F"))   # centro
    # tierra: lineas de estrato interrumpidas
    for fila in (6,11):
        x=0
        while x<W:
            x+=rnd.randint(10,30); largo=rnd.randint(4,12)
            for k in range(largo): 
                y=B[(x+k)%W]+fila
                if y<H-1: put(x+k,y,p["ST"])
    # piedras chicas
    for _ in range(34):
        x=rnd.randrange(W); y=B[x]+rnd.randint(4,H-B[x]-2) if B[x]+4<H-2 else None
        if y is None or y>=H-1: continue
        a=rnd.choice((2,2,3))
        for k in range(a): put(x+k,y,p["PL"])
        for k in range(a): put(x+k,y+1,p["PD"])
    # piedras grandes con contorno
    for _ in range(7):
        x=rnd.randrange(W); y0=B[x]+4
        if y0>=H-4: continue
        w=rnd.choice((5,6)); h=3
        for dx in range(w):
            for dy in range(h):
                esq=(dx in (0,w-1) and dy in (0,h-1))
                if esq: continue
                borde=(dy==h-1) or dx in (0,w-1)
                put(x+dx,y0+dy,p["PD"] if borde else p["PL"])
        put(x+1,y0,p["PL"]); put(x+2,y0,p["PL"])
    return Image.fromarray(img,"RGBA")

for i,(nombre,sem) in enumerate((("dia",11),("tarde",23),("noche",37)),start=1):
    g=generar(nombre,sem)
    g.resize((W*4,H*4),Image.NEAREST).save(f"{RES}suelo_{i}.png")
print("suelo listo")

# (los arboles, arbustos y nubes los genera gen_capas.py)
