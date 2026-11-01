package cristiangiovannigonzalezplascencia.mattadventures;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;

import java.util.Random;

/**
 * Dibuja el escenario del juego por capas (de atrás hacia adelante), todo en pixel art:
 *   cielo en bandas -> estrellas y luna -> nubes lejanas -> nubes cercanas -> pinos lejanos (con bruma)
 *   -> pinos cercanos -> arbusto -> piso.
 * Cada capa se mueve a distinta velocidad (parallax), lo que da sensación de profundidad.
 *
 * Todas las imágenes están guardadas con cada "pixel de arte" ampliado (x4), de modo que 1 pixel de imagen = 1 dp.
 * Se dibujan SIN suavizado y sus desplazamientos se ajustan a pixeles enteros de la pantalla para que no tiemblen.
 * Todo se dibuja en dp: quien lo use debe haber hecho canvas.scale(densidad, densidad).
 *
 * "hora" va de 0 a 2:  0 = día,  1 = atardecer,  2 = noche. Los valores intermedios mezclan dos horas.
 * Las imágenes salen de herramientas/gen_pixel.py (cielo, nubes, pinos, arbustos, luna) y herramientas/gen_piso.py (piso).
 */
public class Escenario {

    /** Alto del piso en dp (la imagen se dibuja a 1 dp por pixel). El piso siempre queda pegado al borde inferior. */
    public static final float SUELO_ALTO = 96f;
    /** Los pies del personaje quedan esta distancia (dp) por debajo del borde superior del piso, a media altura del pasto. */
    public static final float PIES_EN_SUELO = 26f;

    // ---- capas: dónde termina la imagen respecto al borde superior del piso (+ = hacia abajo) y velocidad de parallax ----
    private static final float ARBOL_ALTO = 112f, ARBOL_FONDO = 4f, ARBOL_VEL = 0.30f, ARBOL_TILE = 1000f;
    private static final float LEJOS_ALTO = 72f, LEJOS_FONDO = -4f, LEJOS_VEL = 0.15f;
    private static final float SETO_ALTO = 40f, SETO_FONDO = 20f, SETO_VEL = 0.60f, SETO_TILE = 2000f;
    private static final float SUELO_TILE = 2000f;

    /** El cielo es una imagen de 400 dp de alto cuyo borde inferior se apoya a esta distancia bajo el borde del piso. */
    private static final float CIELO_ALTO = 400f, CIELO_BAJO_HORIZONTE = 30f;

    // Nubes: {forma (0-2), x (fracción), y (fracción), escala (1 cercana, 0.5 lejana), opacidad, velocidad}.
    // Solo 1 y 0.5: así cada pixel de arte mide un número entero de dp.
    private static final float[][] NUBES = {
            {1, 0.06f, 0.35f, 0.5f, 0.75f, 0.04f},
            {2, 0.46f, 0.18f, 0.5f, 0.75f, 0.05f},
            {0, 0.84f, 0.48f, 0.5f, 0.75f, 0.03f},
            {0, 0.14f, 0.05f, 1f, 1.00f, 0.10f},
            {2, 0.57f, 0.42f, 1f, 1.00f, 0.08f},
            {1, 0.78f, 0.07f, 1f, 1.00f, 0.12f}};
    private static final float NUBE_W = 260f, NUBE_H = 132f;
    private static final float PIXEL = 4f;     // dp que mide cada pixel de arte (cielo, nubes, pinos, piso)

    private final float densidad;
    private final Bitmap luna;
    private final Bitmap[] nubes = new Bitmap[3];
    private final BitmapShader[] shCielo = new BitmapShader[3];
    private final BitmapShader[] shSuelo = new BitmapShader[3];
    private final BitmapShader[] shSeto = new BitmapShader[3];
    private final BitmapShader[] shArboles = new BitmapShader[3];
    private final BitmapShader[] shLejos = new BitmapShader[3];

    private final Paint pPixel = new Paint();                 // todo el escenario: pixel art, sin suavizado
    private final Paint pEstrella = new Paint();
    private final Matrix m = new Matrix();
    private final RectF dst = new RectF();
    private final Rect src = new Rect();

    private final float[] estX = new float[46], estY = new float[46], estFase = new float[46];

    public Escenario(Resources res) {
        densidad = res.getDisplayMetrics().density;
        int[] idCielo = {R.drawable.cielo_1, R.drawable.cielo_2, R.drawable.cielo_3};
        int[] idSuelo = {R.drawable.suelo_1, R.drawable.suelo_2, R.drawable.suelo_3};
        int[] idSeto = {R.drawable.arbustos_1, R.drawable.arbustos_2, R.drawable.arbustos_3};
        int[] idArboles = {R.drawable.arboles_1, R.drawable.arboles_2, R.drawable.arboles_3};
        int[] idLejos = {R.drawable.lejanos_1, R.drawable.lejanos_2, R.drawable.lejanos_3};
        int[] idNubes = {R.drawable.nubes_1, R.drawable.nubes_2, R.drawable.nubes_3};
        for (int i = 0; i < 3; i++) {
            shCielo[i] = new BitmapShader(cargar(res, idCielo[i]), Shader.TileMode.REPEAT, Shader.TileMode.CLAMP);
            shSuelo[i] = new BitmapShader(cargar(res, idSuelo[i]), Shader.TileMode.REPEAT, Shader.TileMode.CLAMP);
            shSeto[i] = new BitmapShader(cargar(res, idSeto[i]), Shader.TileMode.REPEAT, Shader.TileMode.CLAMP);
            shArboles[i] = new BitmapShader(cargar(res, idArboles[i]), Shader.TileMode.REPEAT, Shader.TileMode.CLAMP);
            shLejos[i] = new BitmapShader(cargar(res, idLejos[i]), Shader.TileMode.REPEAT, Shader.TileMode.CLAMP);
            nubes[i] = cargar(res, idNubes[i]);
        }
        luna = cargar(res, R.drawable.luna);

        Random r = new Random(42);
        for (int i = 0; i < estX.length; i++) {
            estX[i] = r.nextFloat();
            estY[i] = r.nextFloat();
            estFase[i] = r.nextFloat() * 6.28f;
        }
    }

    /** Carga una imagen sin que Android la reescale por densidad (1 pixel de imagen = 1 dp). */
    public static Bitmap cargar(Resources res, int id) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        return BitmapFactory.decodeResource(res, id, o);
    }

    /** Coordenada Y (dp) donde se apoyan los pies, para una pantalla de esa altura. */
    public static float yPies(float altoDp) {
        return altoDp - SUELO_ALTO + PIES_EN_SUELO;
    }

    /** Ajusta una medida en dp a un número entero de pixeles de pantalla (evita que el pixel art parpadee al moverse). */
    private float enteros(float dp) {
        return Math.round(dp * densidad) / densidad;
    }

    private void dibujarShader(Canvas c, BitmapShader sh, float tx, float ty, float esc, float izq, float arriba, float der, float abajo, int alfa) {
        m.setScale(esc, esc);
        m.postTranslate(enteros(tx), enteros(ty));
        sh.setLocalMatrix(m);
        pPixel.setShader(sh);
        pPixel.setAlpha(alfa);
        c.drawRect(izq, arriba, der, abajo, pPixel);
    }

    /** Franja que se repite horizontalmente. {@code abajo} es la coordenada donde termina la imagen. */
    private void franja(Canvas c, BitmapShader sh, float tile, float alto, float abajo, float desplazamiento, float anchoDp, int alfa) {
        float top = abajo - alto;
        dibujarShader(c, sh, -(desplazamiento % tile), top, 1f, 0, top, anchoDp, abajo, alfa);
    }

    /** La misma franja de dos horas distintas, mezclada por transparencia. */
    private void franjaHora(Canvas c, BitmapShader[] sh, int i0, float f, float tile, float alto, float abajo, float desplazamiento, float anchoDp) {
        franja(c, sh[i0], tile, alto, abajo, desplazamiento, anchoDp, 255);
        if (f > 0.004f) franja(c, sh[i0 + 1], tile, alto, abajo, desplazamiento, anchoDp, (int) (255 * f));
    }

    private void dibujarNubes(Canvas c, float anchoDp, float altoDp, float desplazamiento, int i0, float f, float t, int desde, int hasta) {
        float largo = anchoDp + 300f;
        float zona = Math.max(60f, altoDp - SUELO_ALTO - 130f);
        for (int i = desde; i < hasta; i++) {
            float[] n = NUBES[i];
            float x = n[1] * largo - (desplazamiento * n[5] + t * 5f * n[3]);
            x -= (float) Math.floor(x / largo) * largo;
            x -= 150f;
            x = Math.round(x / 2f) * 2f;                         // se mueve de 2 en 2 dp: escalones de pixel art
            float w = NUBE_W * n[3], h = NUBE_H * n[3];
            float y = Math.round((6f + n[2] * zona) / 4f) * 4f;
            dst.set(x, y, x + w, y + h);
            src.set((int) (n[0] * NUBE_W), 0, (int) (n[0] * NUBE_W + NUBE_W), (int) NUBE_H);
            pPixel.setShader(null);
            pPixel.setAlpha((int) (255 * n[4]));
            c.drawBitmap(nubes[i0], src, dst, pPixel);
            if (f > 0.004f) {
                pPixel.setAlpha((int) (255 * n[4] * f));
                c.drawBitmap(nubes[i0 + 1], src, dst, pPixel);
            }
        }
    }

    /**
     * @param desplazamiento cuánto ha avanzado el mundo (dp); mueve todas las capas, cada una a su velocidad
     * @param hora           0 = día, 1 = atardecer, 2 = noche
     * @param t              tiempo en segundos, solo para el parpadeo de estrellas y el movimiento lento de nubes
     */
    public void dibujar(Canvas c, float anchoDp, float altoDp, float desplazamiento, float hora, float t) {
        if (hora < 0f) hora = 0f;
        if (hora > 2f) hora = 2f;
        float noche = Math.max(0f, hora - 1f);
        int i0 = Math.min(1, (int) hora);
        float f = hora - i0;

        // cielo en bandas: la imagen se apoya en el horizonte; hacia arriba se repite su color superior
        float horizonte = altoDp - SUELO_ALTO + CIELO_BAJO_HORIZONTE;
        dibujarShader(c, shCielo[i0], 0, horizonte - CIELO_ALTO, 1f, 0, 0, anchoDp, altoDp, 255);
        if (f > 0.004f) dibujarShader(c, shCielo[i0 + 1], 0, horizonte - CIELO_ALTO, 1f, 0, 0, anchoDp, altoDp, (int) (255 * f));
        pPixel.setShader(null);

        // estrellas y luna (solo de noche)
        if (noche > 0.01f) {
            float cieloUtil = altoDp - SUELO_ALTO - 60f;
            pEstrella.setColor(Color.WHITE);
            for (int i = 0; i < estX.length; i++) {
                float brillo = 0.5f + 0.5f * (float) Math.sin(t * 1.6f + estFase[i]);
                int nivel = brillo > 0.66f ? 2 : (brillo > 0.33f ? 1 : 0);       // 3 niveles: sin degradado
                int alfa = (int) (noche * (nivel == 2 ? 255 : (nivel == 1 ? 150 : 70)));
                float x = Math.round(estX[i] * anchoDp / PIXEL) * PIXEL;
                float y = Math.round(estY[i] * cieloUtil / PIXEL) * PIXEL;
                pEstrella.setAlpha(alfa);
                c.drawRect(x, y, x + PIXEL, y + PIXEL, pEstrella);
                if (i % 6 == 0 && nivel == 2) {                                    // algunas brillan en cruz
                    c.drawRect(x - PIXEL, y, x, y + PIXEL, pEstrella);
                    c.drawRect(x + PIXEL, y, x + 2 * PIXEL, y + PIXEL, pEstrella);
                    c.drawRect(x, y - PIXEL, x + PIXEL, y, pEstrella);
                    c.drawRect(x, y + PIXEL, x + PIXEL, y + 2 * PIXEL, pEstrella);
                }
            }
            pPixel.setAlpha((int) (255 * noche));
            float lx = Math.round((anchoDp - luna.getWidth() - 250f) / PIXEL) * PIXEL;   // a la izquierda del marcador de puntos
            dst.set(lx, 16f, lx + luna.getWidth(), 16f + luna.getHeight());
            c.drawBitmap(luna, null, dst, pPixel);
        }

        // nubes: primero las lejanas (más chicas y tenues), luego las cercanas
        dibujarNubes(c, anchoDp, altoDp, desplazamiento, i0, f, t, 0, 3);
        dibujarNubes(c, anchoDp, altoDp, desplazamiento, i0, f, t, 3, NUBES.length);

        float sueloTop = altoDp - SUELO_ALTO;

        // pinos lejanos (con bruma) y cercanos, el arbusto que los "arraiga" y el piso
        franjaHora(c, shLejos, i0, f, ARBOL_TILE, LEJOS_ALTO, sueloTop + LEJOS_FONDO, desplazamiento * LEJOS_VEL, anchoDp);
        franjaHora(c, shArboles, i0, f, ARBOL_TILE, ARBOL_ALTO, sueloTop + ARBOL_FONDO, desplazamiento * ARBOL_VEL, anchoDp);
        franjaHora(c, shSeto, i0, f, SETO_TILE, SETO_ALTO, sueloTop + SETO_FONDO, desplazamiento * SETO_VEL, anchoDp);
        franja(c, shSuelo[i0], SUELO_TILE, SUELO_ALTO, altoDp, desplazamiento, anchoDp, 255);
        if (f > 0.004f) franja(c, shSuelo[i0 + 1], SUELO_TILE, SUELO_ALTO, altoDp, desplazamiento, anchoDp, (int) (255 * f));
        pPixel.setShader(null);
        pPixel.setAlpha(255);
    }
}
