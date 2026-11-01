package cristiangiovannigonzalezplascencia.mattadventures;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Lógica del corredor sin fin (estilo dinosaurio de Chrome). No usa nada de Android,
 * así que se puede probar con simulaciones.
 *
 * Unidades: dp (densidad independiente) y segundos. Por eso el juego se siente igual
 * en cualquier teléfono.
 *
 * TODOS LOS AJUSTES DE DIFICULTAD ESTÁN EN LA SECCIÓN "AJUSTES" DE ABAJO.
 */
public class MotorJuego {

    public enum Estado { LISTO, CORRIENDO, PREGUNTA, PAUSA, FIN }

    // ====================================================================
    // AJUSTES (aquí se afina cómo se siente el juego)
    // ====================================================================

    /** Velocidad a la que avanza el escenario y los obstáculos (dp por segundo), de menor a mayor. */
    public static final float[] VELOCIDADES = {240f, 300f, 360f, 420f};
    /** Cada cuántos segundos de juego sube al siguiente nivel de velocidad (3 subidas en total). */
    public static final float SEGUNDOS_POR_NIVEL = 30f;
    /** Qué tan rápido se alcanza la nueva velocidad (dp/s por segundo). Más alto = el cambio es más brusco. */
    public static final float ACELERACION = 90f;

    /** Fuerza del salto (dp/s) y gravedad (dp/s²). Con estos valores: sube ~147 dp y dura ~0.70 s. */
    public static final float VELOCIDAD_SALTO = 840f;
    public static final float GRAVEDAD = 2400f;
    /** Si se toca un poco ANTES de aterrizar, el salto se guarda este tiempo y sale al caer. */
    public static final float BUFFER_SALTO = 0.12f;

    public static final int VIDAS_INICIALES = 4;
    /** Segundos sin poder chocar después de contestar una pregunta. */
    public static final float SEGUNDOS_INVULNERABLE = 1.8f;

    /** Separación mínima entre obstáculos = velocidad * GAP_SEG + GAP_DP. Más bajo = más difícil. */
    public static final float GAP_SEG = 0.60f;
    public static final float GAP_DP = 50f;
    /** Variación aleatoria extra de la separación (0.9 = hasta 90 % más). */
    public static final float GAP_VARIACION = 0.9f;

    /** Puntos: 1 por cada 40 dp recorridos + 10 por cada obstáculo esquivado. */
    public static final float DP_POR_METRO = 40f;
    public static final int PUNTOS_POR_ESQUIVE = 10;
    public static final int COSTO_PISTA = 15;

    /** Tipos de obstáculo {ancho, alto} en dp: 1 libro, 2 libros, pila de 2, pila de 3. */
    public static final float[][] TIPOS = {{46f, 34f}, {92f, 34f}, {46f, 64f}, {46f, 92f}};
    /** Probabilidad de cada tipo (por nivel de velocidad). La pila de 3 solo aparece en los niveles altos. */
    private static final int[][] PESOS = {
            {60, 28, 12, 0},
            {45, 25, 20, 10},
            {35, 25, 25, 15},
            {30, 25, 25, 20}};

    // Jugador (sprite de 64 dp; la "caja de choque" es más pequeña para que sea justo)
    public static final float JUGADOR_SPRITE = 64f;
    public static final float HIT_X = 19f, HIT_ANCHO = 26f, HIT_PIES = 2f;
    /** Cuánto se achica la caja de choque del obstáculo (a cada lado / por arriba). */
    public static final float HIT_OBST_LADOS = 0.12f, HIT_OBST_ARRIBA = 0.92f;

    private static final float PASO = 1f / 120f;   // la física avanza en pasos fijos: igual a 30, 60 o 120 fps

    // ====================================================================

    public interface Escucha {
        void alSaltar();
        void alGolpear();
        void alSubirVelocidad(int nivel);
        void alEsquivar(int racha);
    }

    public static class Obstaculo {
        public float x;
        public final float ancho, alto;
        public final int tipo;
        /** Número que solo sirve para variar el color del dibujo (verde, azul, rojo); no afecta al juego. */
        public final int variante;
        public boolean contado;

        Obstaculo(int tipo, float x, int variante) {
            this.tipo = tipo;
            this.variante = variante;
            this.x = x;
            this.ancho = TIPOS[tipo][0];
            this.alto = TIPOS[tipo][1];
        }
    }

    private Estado estado = Estado.LISTO;
    private final Random azar;
    private Escucha escucha;

    private float anchoMundo = 800f;
    private float jugadorX = 112f;

    private float tiempo, velocidad, distancia;
    private int nivel;

    private float y, vy, bufferSalto, invulnerable;
    private boolean enSuelo = true;

    private final List<Obstaculo> obstaculos = new ArrayList<Obstaculo>();
    private float distanciaSiguiente;
    private boolean generar = true;
    private int generados;

    private int vidas = VIDAS_INICIALES;
    private int esquivados, racha, mejorRacha, aciertos, penalizacion;

    public MotorJuego() {
        this(new Random());
    }

    public MotorJuego(Random azar) {
        this.azar = azar;
        this.velocidad = VELOCIDADES[0] * 0.5f;
    }

    public void setEscucha(Escucha e) { escucha = e; }

    /** Ancho visible del juego en dp. */
    public void setAncho(float anchoDp) {
        anchoMundo = anchoDp;
        jugadorX = Math.max(64f, Math.min(160f, anchoDp * 0.14f));
    }

    // ------------------------------------------------------------------
    // Control
    // ------------------------------------------------------------------

    public void empezar() {
        if (estado == Estado.LISTO) estado = Estado.CORRIENDO;
    }

    public void saltar() {
        if (estado == Estado.CORRIENDO) bufferSalto = BUFFER_SALTO;
    }

    public void pausar() {
        if (estado == Estado.CORRIENDO) estado = Estado.PAUSA;
    }

    public void reanudar() {
        if (estado == Estado.PAUSA) estado = Estado.CORRIENDO;
    }

    /** Se llama cuando el jugador contesta la pregunta que apareció por un choque. */
    public void resolverPregunta(boolean correcta) {
        if (estado != Estado.PREGUNTA) return;
        if (correcta) {
            aciertos++;
        } else {
            vidas--;
        }
        if (vidas <= 0) {
            estado = Estado.FIN;
        } else {
            estado = Estado.CORRIENDO;
            invulnerable = SEGUNDOS_INVULNERABLE;
        }
    }

    public void cobrarPista() {
        penalizacion += COSTO_PISTA;
    }

    // ------------------------------------------------------------------
    // Simulación
    // ------------------------------------------------------------------

    public void actualizar(float dt) {
        if (estado != Estado.CORRIENDO) return;
        if (dt > 0.1f) dt = 0.1f;
        int pasos = (int) Math.ceil(dt / PASO);
        if (pasos < 1) return;
        float h = dt / pasos;
        for (int i = 0; i < pasos && estado == Estado.CORRIENDO; i++) paso(h);
    }

    private void paso(float h) {
        tiempo += h;

        // nivel de velocidad: sube cada SEGUNDOS_POR_NIVEL, máximo hasta el último valor de VELOCIDADES
        int n = Math.min(VELOCIDADES.length - 1, (int) (tiempo / SEGUNDOS_POR_NIVEL));
        if (n > nivel) {
            nivel = n;
            if (escucha != null) escucha.alSubirVelocidad(nivel);
        }
        float objetivo = VELOCIDADES[nivel];
        if (velocidad < objetivo) velocidad = Math.min(objetivo, velocidad + ACELERACION * h);
        distancia += velocidad * h;

        // salto
        if (bufferSalto > 0f) bufferSalto -= h;
        if (enSuelo && bufferSalto > 0f) {
            vy = VELOCIDAD_SALTO;
            enSuelo = false;
            bufferSalto = 0f;
            if (escucha != null) escucha.alSaltar();
        }
        if (!enSuelo) {
            vy -= GRAVEDAD * h;
            y += vy * h;
            if (y <= 0f) {
                y = 0f;
                vy = 0f;
                enSuelo = true;
            }
        }
        if (invulnerable > 0f) invulnerable -= h;

        // aparición de obstáculos
        distanciaSiguiente -= velocidad * h;
        if (generar && distanciaSiguiente <= 0f) {
            Obstaculo o = new Obstaculo(elegirTipo(), anchoMundo + 30f, generados++);
            obstaculos.add(o);
            float gapMin = objetivo * GAP_SEG + GAP_DP;
            distanciaSiguiente = o.ancho + gapMin + azar.nextFloat() * gapMin * GAP_VARIACION;
        }

        // mover, contar esquives y detectar choques
        float px1 = jugadorX + HIT_X;
        float px2 = px1 + HIT_ANCHO;
        for (int i = 0; i < obstaculos.size(); i++) {
            Obstaculo o = obstaculos.get(i);
            o.x -= velocidad * h;

            float ox1 = o.x + o.ancho * HIT_OBST_LADOS;
            float ox2 = o.x + o.ancho * (1f - HIT_OBST_LADOS);

            if (!o.contado && ox2 < px1) {
                o.contado = true;
                esquivados++;
                racha++;
                if (racha > mejorRacha) mejorRacha = racha;
                if (escucha != null) escucha.alEsquivar(racha);
            }

            if (invulnerable <= 0f && px2 > ox1 && px1 < ox2 && (y + HIT_PIES) < o.alto * HIT_OBST_ARRIBA) {
                obstaculos.remove(i);
                racha = 0;
                estado = Estado.PREGUNTA;
                if (escucha != null) escucha.alGolpear();
                return;
            }

            if (o.x + o.ancho < -60f) {
                obstaculos.remove(i);
                i--;
            }
        }
    }

    private int elegirTipo() {
        int[] pesos = PESOS[nivel];
        int total = 0;
        for (int p : pesos) total += p;
        int r = azar.nextInt(total);
        for (int i = 0; i < pesos.length; i++) {
            r -= pesos[i];
            if (r < 0) return i;
        }
        return 0;
    }

    // ------------------------------------------------------------------
    // Consulta
    // ------------------------------------------------------------------

    public Estado getEstado() { return estado; }
    public float getJugadorX() { return jugadorX; }
    /** Altura de los pies sobre el suelo, en dp. */
    public float getAltura() { return y; }
    public boolean estaEnSuelo() { return enSuelo; }
    public float getVelocidad() { return velocidad; }
    public float getDistancia() { return distancia; }
    public float getTiempo() { return tiempo; }
    public int getNivel() { return nivel; }
    public int getVidas() { return vidas; }
    public int getEsquivados() { return esquivados; }
    public int getRacha() { return racha; }
    public int getMejorRacha() { return mejorRacha; }
    public int getAciertos() { return aciertos; }
    public boolean esInvulnerable() { return invulnerable > 0f; }
    public float getInvulnerable() { return invulnerable; }
    public List<Obstaculo> getObstaculos() { return obstaculos; }

    public int getPuntos() {
        int p = (int) (distancia / DP_POR_METRO) + esquivados * PUNTOS_POR_ESQUIVE - penalizacion;
        return Math.max(0, p);
    }

    /** Arranca directamente en un nivel de velocidad (lo usa el splash para mostrar el juego en movimiento). */
    public void modoDemo(int nivelVelocidad) {
        nivel = Math.max(0, Math.min(VELOCIDADES.length - 1, nivelVelocidad));
        velocidad = VELOCIDADES[nivel];
        tiempo = nivel * SEGUNDOS_POR_NIVEL;
        estado = Estado.CORRIENDO;
    }

    // ------------------------------------------------------------------
    // Solo para pruebas
    // ------------------------------------------------------------------

    void pruebaFijarNivel(int n) {
        nivel = n;
        velocidad = VELOCIDADES[n];
        tiempo = n * SEGUNDOS_POR_NIVEL;
    }

    void pruebaSinGeneracion() {
        generar = false;
        distanciaSiguiente = Float.MAX_VALUE;
    }

    void pruebaAgregarObstaculo(int tipo, float x) {
        obstaculos.add(new Obstaculo(tipo, x, 0));
    }

    void pruebaIniciar() {
        estado = Estado.CORRIENDO;
    }
}
