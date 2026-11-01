package cristiangiovannigonzalezplascencia.mattadventures;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;
import java.util.Random;

/**
 * Dibuja la partida y hace avanzar al motor en cada fotograma.
 * Toda la lógica del juego vive en MotorJuego; aquí solo se pinta y se recibe el toque.
 */
public class JuegoView extends View {

    /** Poner en true para ver las cajas de choque (útil para ajustar la dificultad). */
    private static final boolean DEBUG_CAJAS = false;
    /** Si es false, el escenario se queda siempre de día. */
    private static final boolean CAMBIA_LA_HORA = true;
    /** Hora del día para cada nivel de velocidad: día, atardecer, noche, noche. */
    private static final float[] HORA_POR_NIVEL = {0f, 1f, 2f, 2f};

    private static final String[] NOMBRE_NIVEL = {"", "¡Más rápido!", "¡Más rápido!", "¡Velocidad máxima!"};

    private MotorJuego motor;
    private Escenario escenario;
    private Bitmap[] correr;          // dc, da, db, da
    private Bitmap spriteSalto, spriteGolpe;
    private Bitmap[] libros;          // verde, azul, rojo (46 x 34, pixel art)
    private Bitmap corazonLleno, corazonVacio;

    private final Paint pBmp = new Paint();                            // sprites, libros y corazones: pixel art, sin suavizar
    private final Paint pTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pContorno = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pForma = new Paint();                          // rectángulos: sin suavizar (bordes nítidos)
    private final RectF rf = new RectF();

    private float densidad = 1f, anchoDp, altoDp;
    private long ultimoNs = 0;
    private float anim = 0f, hora = 0f, destello = 0f, aviso = 0f;
    private int nivelVisto = 0;
    private String textoAviso = "";
    private int mejor = 0;
    private Runnable alToque;

    // Modo demostración (splash): el personaje juega solo y no se muestra el marcador
    private boolean demo = false, demoLista = false;
    private float horaManual = -1f;

    // Polvo que levanta Matt al correr, saltar y aterrizar
    private static final int MAX_POLVO = 32;
    private final float[] pvX = new float[MAX_POLVO], pvY = new float[MAX_POLVO];
    private final float[] pvVx = new float[MAX_POLVO], pvVy = new float[MAX_POLVO];
    private final float[] pvVida = new float[MAX_POLVO], pvMax = new float[MAX_POLVO], pvTam = new float[MAX_POLVO];
    private int pvSiguiente = 0;
    private float relojPolvo = 0f;
    private boolean enSueloAntes = true;
    private final Random azar = new Random();

    public JuegoView(Context context) {
        super(context);
        iniciar();
    }

    public JuegoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        iniciar();
    }

    private void iniciar() {
        pBmp.setFilterBitmap(false);
        pBmp.setAntiAlias(false);
        pTexto.setTypeface(Typeface.DEFAULT_BOLD);
        pContorno.setTypeface(Typeface.DEFAULT_BOLD);
        pContorno.setStyle(Paint.Style.STROKE);
        pContorno.setStrokeJoin(Paint.Join.ROUND);
        pContorno.setColor(0xCC000000);
    }

    public void setMotor(MotorJuego m) {
        motor = m;
        if (anchoDp > 0) motor.setAncho(anchoDp);
    }

    public void setMejor(int mejor) { this.mejor = mejor; }

    /** En modo demostración Matt juega solo (sin marcador ni avisos); sirve para el splash. */
    public void setDemo(boolean demo) {
        this.demo = demo;
        demoLista = false;
    }

    /** Fija la hora del día (0 día, 1 atardecer, 2 noche). Un valor negativo vuelve al cambio automático por nivel. */
    public void setHoraManual(float h) { horaManual = h; }

    /** Matt salta solo cuando el siguiente libro llega al punto justo (el mismo que usan las pruebas). */
    private void jugarSolo() {
        float px1 = motor.getJugadorX() + MotorJuego.HIT_X;
        MotorJuego.Obstaculo prox = null;
        for (MotorJuego.Obstaculo o : motor.getObstaculos()) {
            if (o.x + o.ancho * (1f - MotorJuego.HIT_OBST_LADOS) > px1 && (prox == null || o.x < prox.x)) prox = o;
        }
        if (prox != null) {
            float centro = prox.x + prox.ancho / 2f;
            float dist = centro - (px1 + MotorJuego.HIT_ANCHO / 2f);
            if (dist <= motor.getVelocidad() * (MotorJuego.VELOCIDAD_SALTO / MotorJuego.GRAVEDAD)) motor.saltar();
        }
        if (motor.getEstado() == MotorJuego.Estado.PREGUNTA) motor.resolverPregunta(true);
    }

    /** Avanza el juego unos segundos antes de mostrarse, para que ya haya libros en pantalla. */
    private void precalentar(float segundos) {
        motor.modoDemo(1);
        for (int i = 0; i < (int) (segundos * 60); i++) {
            jugarSolo();
            motor.actualizar(1f / 60f);
        }
        demoLista = true;
    }

    public void setOnToque(Runnable r) { alToque = r; }

    /** Destello blanco breve al chocar. */
    public void efectoGolpe() { destello = 0.30f; }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        densidad = getResources().getDisplayMetrics().density;
        anchoDp = w / densidad;
        altoDp = h / densidad;
        if (motor != null) motor.setAncho(anchoDp);
    }

    private void cargarRecursos() {
        escenario = new Escenario(getResources());
        Bitmap da = Escenario.cargar(getResources(), R.drawable.da);
        correr = new Bitmap[]{
                Escenario.cargar(getResources(), R.drawable.dc), da,
                Escenario.cargar(getResources(), R.drawable.db), da};
        spriteSalto = correr[0];
        spriteGolpe = Escenario.cargar(getResources(), R.drawable.aa);
        libros = new Bitmap[]{
                Escenario.cargar(getResources(), R.drawable.libro_verde),
                Escenario.cargar(getResources(), R.drawable.libro_azul),
                Escenario.cargar(getResources(), R.drawable.libro_rojo)};
        corazonLleno = Escenario.cargar(getResources(), R.drawable.corazon_lleno);
        corazonVacio = Escenario.cargar(getResources(), R.drawable.corazon_vacio);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (alToque != null) alToque.run();
            return true;
        }
        return super.onTouchEvent(e);
    }

    // ------------------------------------------------------------------
    // Dibujo
    // ------------------------------------------------------------------

    @Override
    protected void onDraw(Canvas canvas) {
        if (escenario == null) cargarRecursos();
        if (motor == null || anchoDp <= 0) return;

        long ahora = System.nanoTime();
        float dt = ultimoNs == 0 ? 0f : (ahora - ultimoNs) / 1e9f;
        ultimoNs = ahora;
        if (dt > 0.05f) dt = 0.05f;
        anim += dt;

        if (demo) {
            if (!demoLista) precalentar(3.5f);
            jugarSolo();
        }
        motor.actualizar(dt);
        float pies = Escenario.yPies(altoDp);
        actualizarPolvo(dt, pies);

        // hora del día según el nivel de velocidad (cambio suave)
        if (horaManual >= 0f) {
            hora = horaManual;
        } else {
            float objetivoHora = CAMBIA_LA_HORA ? HORA_POR_NIVEL[motor.getNivel()] : 0f;
            float paso = dt * 0.25f;
            if (hora < objetivoHora) hora = Math.min(objetivoHora, hora + paso);
            else if (hora > objetivoHora) hora = Math.max(objetivoHora, hora - paso);
        }

        if (!demo && motor.getNivel() != nivelVisto) {
            nivelVisto = motor.getNivel();
            aviso = 2.0f;
            textoAviso = NOMBRE_NIVEL[Math.min(nivelVisto, NOMBRE_NIVEL.length - 1)];
        }
        if (aviso > 0f) aviso -= dt;
        if (destello > 0f) destello -= dt;

        canvas.save();
        canvas.scale(densidad, densidad);

        escenario.dibujar(canvas, anchoDp, altoDp, motor.getDistancia(), hora, anim);

        dibujarObstaculos(canvas, pies);
        dibujarPolvo(canvas);
        dibujarJugador(canvas, pies);
        if (!demo) {
            dibujarHud(canvas);
            dibujarAvisos(canvas);
        }

        if (destello > 0f) {
            pForma.setColor(Color.WHITE);
            pForma.setAlpha((int) (255 * Math.min(1f, destello / 0.30f) * 0.7f));
            canvas.drawRect(0, 0, anchoDp, altoDp, pForma);
        }

        canvas.restore();
        postInvalidateOnAnimation();
    }

    // ------------------------------------------------------------------
    // Polvo
    // ------------------------------------------------------------------

    private void polvo(float x, float y, float vx, float vy, float vida, float tam) {
        int i = pvSiguiente;
        pvSiguiente = (pvSiguiente + 1) % MAX_POLVO;
        pvX[i] = x; pvY[i] = y; pvVx[i] = vx; pvVy[i] = vy;
        pvVida[i] = vida; pvMax[i] = vida; pvTam[i] = tam;
    }

    private void actualizarPolvo(float dt, float pies) {
        if (motor.getEstado() != MotorJuego.Estado.CORRIENDO) { enSueloAntes = motor.estaEnSuelo(); return; }

        float x = motor.getJugadorX() + 20f;
        float v = motor.getVelocidad();
        boolean suelo = motor.estaEnSuelo();

        if (enSueloAntes && !suelo) {                 // despegue
            for (int k = 0; k < 4; k++) polvo(x + azar.nextFloat() * 10f, pies, -v * 0.7f + azar.nextFloat() * 40f, -(20f + azar.nextFloat() * 40f), 0.4f, 3f + azar.nextFloat() * 2f);
        } else if (!enSueloAntes && suelo) {          // aterrizaje
            for (int k = 0; k < 7; k++) polvo(x + azar.nextFloat() * 24f, pies, -v * 0.8f + (azar.nextFloat() - 0.5f) * 120f, -(15f + azar.nextFloat() * 45f), 0.5f, 3f + azar.nextFloat() * 3f);
        }
        enSueloAntes = suelo;

        relojPolvo += dt;
        if (suelo && relojPolvo > 0.09f) {            // al correr
            relojPolvo = 0f;
            polvo(x, pies, -v * 0.9f + azar.nextFloat() * 20f, -(25f + azar.nextFloat() * 35f), 0.35f + azar.nextFloat() * 0.15f, 2.5f + azar.nextFloat() * 2f);
        }

        for (int i = 0; i < MAX_POLVO; i++) {
            if (pvVida[i] <= 0f) continue;
            pvVida[i] -= dt;
            pvX[i] += pvVx[i] * dt;
            pvY[i] += pvVy[i] * dt;
            pvVy[i] *= 0.94f;
        }
    }

    /** Polvo en pixel art: cuadritos opacos de 6, 4 y 2 dp que se encogen por escalones (sin transparencias suaves). */
    private void dibujarPolvo(Canvas c) {
        int color = hora < 0.5f ? 0xFFD9B98A : (hora < 1.5f ? 0xFFC49A66 : 0xFF9A93AD);
        pForma.setStyle(Paint.Style.FILL);
        pForma.setColor(color);
        for (int i = 0; i < MAX_POLVO; i++) {
            if (pvVida[i] <= 0f) continue;
            float f = pvVida[i] / pvMax[i];
            float base = pvTam[i] >= 4.5f ? 6f : 4f;
            float lado = f > 0.55f ? base : (f > 0.25f ? base - 2f : 2f);
            float x = Math.round(pvX[i] / 2f) * 2f, y = Math.round(pvY[i] / 2f) * 2f;
            c.drawRect(x, y - lado, x + lado, y, pForma);
        }
    }

    /** Sombra en pixel art: tres franjas que forman una elipse "de escalones". */
    private void sombra(Canvas c, float centroX, float y, float ancho, int alfa) {
        pForma.setStyle(Paint.Style.FILL);
        pForma.setColor(0xFF000000);
        pForma.setAlpha(alfa);
        float cx = Math.round(centroX / 2f) * 2f, w = Math.round(ancho / 4f) * 4f;
        c.drawRect(cx - w * 0.30f, y - 4f, cx + w * 0.30f, y - 2f, pForma);
        c.drawRect(cx - w * 0.50f, y - 2f, cx + w * 0.50f, y + 1f, pForma);
        c.drawRect(cx - w * 0.30f, y + 1f, cx + w * 0.30f, y + 3f, pForma);
        pForma.setAlpha(255);
    }

    private void dibujarJugador(Canvas c, float pies) {
        float x = motor.getJugadorX();
        float altura = motor.getAltura();
        MotorJuego.Estado est = motor.getEstado();

        // sombra: se achica y se aclara al subir
        float k = Math.max(0.35f, 1f - altura / 220f);
        sombra(c, x + 32f, pies, 44f * k, (int) (95 * k));

        // parpadeo mientras es invulnerable
        if (motor.esInvulnerable() && ((int) (anim * 14f) & 1) == 0) return;

        Bitmap b;
        if (est == MotorJuego.Estado.PREGUNTA || est == MotorJuego.Estado.FIN) b = spriteGolpe;
        else if (!motor.estaEnSuelo()) b = spriteSalto;
        else if (est == MotorJuego.Estado.LISTO) b = correr[1];
        else b = correr[((int) (motor.getDistancia() / 24f)) & 3];

        float s = MotorJuego.JUGADOR_SPRITE;
        rf.set(x, pies - altura - s + 1f, x + s, pies - altura + 1f);
        c.drawBitmap(b, null, rf, pBmp);

        if (DEBUG_CAJAS) {
            pForma.setStyle(Paint.Style.STROKE);
            pForma.setColor(Color.RED);
            pForma.setStrokeWidth(1.5f);
            c.drawRect(x + MotorJuego.HIT_X, pies - altura - 50f, x + MotorJuego.HIT_X + MotorJuego.HIT_ANCHO, pies - altura - MotorJuego.HIT_PIES, pForma);
            pForma.setStyle(Paint.Style.FILL);
        }
    }

    private void dibujarObstaculos(Canvas c, float pies) {
        List<MotorJuego.Obstaculo> lista = motor.getObstaculos();
        for (int i = 0; i < lista.size(); i++) {
            MotorJuego.Obstaculo o = lista.get(i);

            sombra(c, o.x + o.ancho / 2f, pies, o.ancho + 6f, 80);

            final float alturaLibro = 34f, anchoLibro = 46f;
            if (o.tipo == 1) {
                // dos libros lado a lado
                libroEn(c, o.x, pies, 0f, o.variante);
                libroEn(c, o.x + anchoLibro, pies, 0f, o.variante + 1);
            } else if (o.tipo == 0) {
                libroEn(c, o.x, pies, 0f, o.variante);
            } else {
                // torre de libros: cada libro mide ~11 dp de grueso, así que 4 libros = 64 dp y 6 libros = 92 dp
                // (la misma altura que usa el motor para los choques)
                int n = o.tipo == 2 ? 4 : 6;
                float salto = (o.alto - alturaLibro) / (n - 1);
                for (int k = 0; k < n; k++) {
                    float desvio = (k % 3 == 1) ? 4f : ((k % 3 == 2) ? -2f : 0f);   // torre un poco desordenada
                    libroEn(c, o.x + desvio, pies, Math.round(k * salto), o.variante + k);
                }
            }

            if (DEBUG_CAJAS) {
                pForma.setStyle(Paint.Style.STROKE);
                pForma.setColor(Color.RED);
                pForma.setStrokeWidth(1.5f);
                c.drawRect(o.x + o.ancho * MotorJuego.HIT_OBST_LADOS, pies - o.alto * MotorJuego.HIT_OBST_ARRIBA,
                        o.x + o.ancho * (1f - MotorJuego.HIT_OBST_LADOS), pies, pForma);
                pForma.setStyle(Paint.Style.FILL);
            }
        }
    }

    private void libroEn(Canvas c, float x, float pies, float elevacion, int color) {
        Bitmap b = libros[color % 3];
        float izq = Math.round(x), arriba = Math.round(pies - elevacion - b.getHeight());
        c.drawBitmap(b, izq, arriba, pBmp);
    }

    // ------------------------------------------------------------------
    // Marcador, corazones y mensajes
    // ------------------------------------------------------------------

    private void texto(Canvas c, String t, float x, float y, float tam, int color, Paint.Align alin) {
        pTexto.setTextSize(tam);
        pTexto.setTextAlign(alin);
        pTexto.setColor(color);
        pContorno.setTextSize(tam);
        pContorno.setTextAlign(alin);
        pContorno.setStrokeWidth(Math.max(2f, tam * 0.16f));
        pContorno.setAlpha((int) (Color.alpha(color) * 0.8f));
        c.drawText(t, x, y, pContorno);
        c.drawText(t, x, y, pTexto);
    }

    private void dibujarHud(Canvas c) {
        // corazones en pixel art (arriba a la izquierda)
        float ancho = corazonLleno.getWidth(), alto = corazonLleno.getHeight();
        for (int i = 0; i < MotorJuego.VIDAS_INICIALES; i++) {
            Bitmap h = i < motor.getVidas() ? corazonLleno : corazonVacio;
            c.drawBitmap(h, 16f + i * (ancho + 6f), 12f, pBmp);
        }

        // indicador de velocidad: 4 barritas cuadradas, con borde oscuro
        float bx = 16f, by = 12f + alto + 8f;
        pForma.setStyle(Paint.Style.FILL);
        for (int i = 0; i < MotorJuego.VELOCIDADES.length; i++) {
            float x0 = bx + i * 24f;
            pForma.setColor(0xFF1B2A4A);
            c.drawRect(x0 - 2f, by - 2f, x0 + 20f, by + 8f, pForma);
            pForma.setColor(i <= motor.getNivel() ? 0xFFFFD83A : 0xFF55648C);
            c.drawRect(x0, by, x0 + 18f, by + 6f, pForma);
        }

        // puntos (arriba a la derecha)
        int puntos = motor.getPuntos();
        texto(c, "Puntos " + puntos, anchoDp - 16f, 36f, 24f, Color.WHITE, Paint.Align.RIGHT);
        int rec = Math.max(mejor, puntos);
        if (rec > 0) texto(c, "Mejor " + rec, anchoDp - 16f, 58f, 15f, 0xFFFFE680, Paint.Align.RIGHT);
        if (motor.getRacha() >= 3) texto(c, "Racha x" + motor.getRacha(), anchoDp - 16f, 78f, 15f, 0xFFB9F6CA, Paint.Align.RIGHT);
    }

    private void dibujarAvisos(Canvas c) {
        if (motor.getEstado() == MotorJuego.Estado.LISTO) {
            float pulso = 0.65f + 0.35f * (float) Math.sin(anim * 4f);
            int a = (int) (255 * pulso);
            texto(c, "Toca para empezar", anchoDp / 2f, altoDp * 0.36f, 32f, Color.argb(a, 255, 255, 255), Paint.Align.CENTER);
            texto(c, "Toca la pantalla para saltar los libros", anchoDp / 2f, altoDp * 0.36f + 30f, 17f, 0xFFFFFFFF, Paint.Align.CENTER);
        }
        if (aviso > 0f && motor.getEstado() != MotorJuego.Estado.LISTO && !textoAviso.isEmpty()) {
            float vida = Math.min(1f, aviso / 0.5f);               // se desvanece al final
            float entrada = Math.min(1f, (2.0f - aviso) / 0.25f);  // "salta" al aparecer
            float tam = 30f + 8f * (1f - entrada);
            texto(c, textoAviso, anchoDp / 2f, altoDp * 0.30f, tam, Color.argb((int) (255 * vida), 255, 216, 58), Paint.Align.CENTER);
        }
    }
}
