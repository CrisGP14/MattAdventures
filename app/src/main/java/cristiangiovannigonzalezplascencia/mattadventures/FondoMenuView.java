package cristiangiovannigonzalezplascencia.mattadventures;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

/**
 * Fondo de los menús: degradado azul con números y símbolos de matemáticas que flotan.
 * Es distinto a propósito del escenario del juego (cielo, árboles y suelo).
 */
public class FondoMenuView extends View {

    private static final String[] SIMBOLOS = {"+", "−", "×", "÷", "=", "%", "π", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "²", "√"};
    private static final int N = 30;

    private final float[] x = new float[N], y = new float[N], tam = new float[N], vel = new float[N];
    private final float[] giro = new float[N], giroVel = new float[N], alfa = new float[N];
    private final int[] simbolo = new int[N];

    private final Paint pFondo = new Paint();
    private final Paint pBrillo = new Paint();
    private final Paint pTexto = new Paint(Paint.ANTI_ALIAS_FLAG);

    private long ultimoNs = 0;
    private float ancho, alto;

    public FondoMenuView(Context context) {
        super(context);
        iniciar();
    }

    public FondoMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        iniciar();
    }

    private void iniciar() {
        pTexto.setTypeface(Typeface.DEFAULT_BOLD);
        pTexto.setTextAlign(Paint.Align.CENTER);
        Random r = new Random(7);
        for (int i = 0; i < N; i++) {
            x[i] = r.nextFloat();
            y[i] = r.nextFloat();
            tam[i] = 22f + r.nextFloat() * 44f;
            vel[i] = 6f + r.nextFloat() * 16f;
            giro[i] = r.nextFloat() * 360f;
            giroVel[i] = (r.nextFloat() - 0.5f) * 24f;
            alfa[i] = 0.10f + r.nextFloat() * 0.14f;
            simbolo[i] = r.nextInt(SIMBOLOS.length);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        ancho = w;
        alto = h;
        pFondo.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0xFF111C66, 0xFF2456C4, 0xFF2FA3D6},
                new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
        // resplandor suave detrás del personaje (lado izquierdo)
        pBrillo.setShader(new RadialGradient(w * 0.25f, h * 0.55f, Math.max(w, h) * 0.45f,
                0x55FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas c) {
        long ahora = System.nanoTime();
        float dt = ultimoNs == 0 ? 0f : (ahora - ultimoNs) / 1e9f;
        ultimoNs = ahora;
        if (dt > 0.05f) dt = 0.05f;

        float d = getResources().getDisplayMetrics().density;
        c.drawRect(0, 0, ancho, alto, pFondo);
        c.drawRect(0, 0, ancho, alto, pBrillo);

        for (int i = 0; i < N; i++) {
            y[i] -= vel[i] * dt / (alto / d);          // sube lentamente
            if (y[i] < -0.15f) {
                y[i] = 1.15f;
            }
            giro[i] += giroVel[i] * dt;

            pTexto.setTextSize(tam[i] * d);
            pTexto.setColor(0xFFFFFFFF);
            pTexto.setAlpha((int) (255 * alfa[i]));
            c.save();
            c.rotate(giro[i], x[i] * ancho, y[i] * alto);
            c.drawText(SIMBOLOS[simbolo[i]], x[i] * ancho, y[i] * alto, pTexto);
            c.restore();
        }
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ultimoNs = 0;
    }
}
