package cristiangiovannigonzalezplascencia.mattadventures;

import java.util.Random;

public class TestMotor {
    static final float DT = 1f/60f;

    // ¿Se puede saltar este obstáculo despegando a d dp de distancia? (devuelve true si pasa sin chocar)
    static boolean pasa(int nivel, int tipo, float d) {
        MotorJuego m = new MotorJuego(new Random(1));
        m.setAncho(900f);
        m.pruebaFijarNivel(nivel); m.pruebaSinGeneracion(); m.pruebaIniciar();
        m.pruebaAgregarObstaculo(tipo, m.getJugadorX() + d);
        m.saltar();
        for (int i = 0; i < 600; i++) {
            m.actualizar(DT);
            if (m.getEstado() == MotorJuego.Estado.PREGUNTA) return false;
            if (m.getEsquivados() > 0 && m.getObstaculos().isEmpty()) return true;
            if (m.getEsquivados() > 0) return true;
        }
        return false;
    }

    public static void main(String[] a) {
        int fallos = 0;

        // ---------- 1) Niveles de velocidad por tiempo ----------
        MotorJuego m = new MotorJuego(new Random(5));
        m.setAncho(900f); m.pruebaSinGeneracion(); m.pruebaIniciar();
        int[] subio = new int[4]; float[] cuando = new float[4];
        final int[] ult = {0};
        m.setEscucha(new MotorJuego.Escucha() {
            public void alSaltar() {} public void alGolpear() {} public void alEsquivar(int r) {}
            public void alSubirVelocidad(int n) { ult[0] = n; }
        });
        int prev = 0;
        for (int i = 0; i < 60 * 150; i++) {
            m.actualizar(DT);
            if (m.getNivel() != prev) { cuando[m.getNivel()] = m.getTiempo(); prev = m.getNivel(); }
        }
        System.out.printf("1) Subidas de velocidad a los %.1f s, %.1f s y %.1f s; nivel final=%d, velocidad final=%.0f dp/s tras 150 s%n",
                cuando[1], cuando[2], cuando[3], m.getNivel(), m.getVelocidad());
        if (Math.abs(cuando[1]-30)>0.1 || Math.abs(cuando[2]-60)>0.1 || Math.abs(cuando[3]-90)>0.1 || m.getNivel()!=3 || m.getVelocidad()!=420f) { System.out.println("   FALLO"); fallos++; }
        else System.out.println("   OK (maximo 3 subidas)");

        // ---------- 2) Ventana de salto por tipo y velocidad ----------
        System.out.println("\n2) Ventana para despegar (ms) que salva el obstaculo, a 60 fps:");
        String[] nombres = {"1 libro ", "2 libros", "pila de 2", "pila de 3"};
        float peorVentana = 9999;
        for (int t = 0; t < 4; t++) {
            StringBuilder sb = new StringBuilder(String.format("   %-9s", nombres[t]));
            for (int n = 0; n < 4; n++) {
                int ok = 0;
                for (float d = 0; d <= 900; d += 1f) if (pasa(n, t, d)) ok++;
                float ms = ok / MotorJuego.VELOCIDADES[n] * 1000f;
                sb.append(String.format("  v=%3.0f: %4.0f ms", MotorJuego.VELOCIDADES[n], ms));
                if (!(t == 3 && n == 0)) peorVentana = Math.min(peorVentana, ms);
            }
            System.out.println(sb);
        }
        System.out.printf("   Peor caso: %.0f ms%n", peorVentana);
        if (peorVentana < 150) { System.out.println("   FALLO: ventana demasiado estrecha"); fallos++; } else System.out.println("   OK (>= 150 ms)");

        // ---------- 3) Mismo resultado a 30, 60 y 144 fps ----------
        System.out.println("\n3) Consistencia entre frame rates (pila de 3 a 420 dp/s, mismo despegue):");
        for (int fps : new int[]{30, 60, 144}) {
            int ok = 0;
            for (float d = 0; d <= 900; d += 1f) {
                MotorJuego mm = new MotorJuego(new Random(1)); mm.setAncho(900f);
                mm.pruebaFijarNivel(3); mm.pruebaSinGeneracion(); mm.pruebaIniciar();
                mm.pruebaAgregarObstaculo(3, mm.getJugadorX() + d); mm.saltar();
                boolean paso = false;
                for (int i = 0; i < fps * 10; i++) { mm.actualizar(1f/fps); if (mm.getEstado()==MotorJuego.Estado.PREGUNTA) break; if (mm.getEsquivados()>0) { paso=true; break; } }
                if (paso) ok++;
            }
            System.out.printf("   %3d fps -> ventana %.0f ms%n", fps, ok / 420f * 1000f);
        }

        // ---------- 4) Bot con generacion aleatoria real ----------
        System.out.println("\n4) Bot jugando 40 partidas de 29 s por nivel (obstaculos aleatorios reales):");
        float[] sds = {0f, 0.03f, 0.06f, 0.10f};
        for (float sd : sds) {
            StringBuilder sb = new StringBuilder(String.format("   error de tiempo +-%3.0f ms:", sd*1000));
            for (int n = 0; n < 4; n++) {
                int obst = 0, golpes = 0;
                for (int partida = 0; partida < 40; partida++) {
                    Random r = new Random(100 + partida * 7 + n);
                    MotorJuego mm = new MotorJuego(new Random(partida * 13 + n));
                    mm.setAncho(900f); mm.pruebaFijarNivel(n); mm.pruebaIniciar();
                    float[] jit = {0f};
                    MotorJuego.Obstaculo objetivo = null;
                    for (int i = 0; i < 60 * 29; i++) {
                        // siguiente obstaculo aun no superado
                        MotorJuego.Obstaculo prox = null;
                        float px1 = mm.getJugadorX() + MotorJuego.HIT_X;
                        for (MotorJuego.Obstaculo o : mm.getObstaculos())
                            if (o.x + o.ancho * (1 - MotorJuego.HIT_OBST_LADOS) > px1 && (prox == null || o.x < prox.x)) prox = o;
                        if (prox != null) {
                            if (prox != objetivo) { objetivo = prox; jit[0] = (float) r.nextGaussian() * sd; }
                            float centro = prox.x + prox.ancho / 2f;
                            float dist = centro - (px1 + MotorJuego.HIT_ANCHO / 2f);
                            float v = mm.getVelocidad();
                            // despega cuando el centro del obstaculo esta a v*0.311 s (cima del salto), +- error
                            if (dist <= v * (MotorJuego.VELOCIDAD_SALTO / MotorJuego.GRAVEDAD + jit[0])) mm.saltar();
                        }
                        mm.actualizar(DT);
                        if (mm.getEstado() == MotorJuego.Estado.PREGUNTA) { golpes++; mm.resolverPregunta(true); }
                    }
                    obst += mm.getEsquivados() + 0;
                }
                sb.append(String.format("  v=%3.0f: %3d choques/%4d obst", MotorJuego.VELOCIDADES[n], golpes, obst));
                if (sd == 0f && golpes > 0) fallos++;  // perfecto: cero choques
                if (sd == 0.03f && golpes > 0.005 * (obst + golpes)) fallos++;  // +-30 ms: menos del 0.5 %
            }
            System.out.println(sb);
        }

        // ---------- 5) Reglas de vidas y puntos ----------
        MotorJuego p = new MotorJuego(new Random(2)); p.setAncho(900f); p.pruebaSinGeneracion(); p.pruebaIniciar();
        p.pruebaAgregarObstaculo(0, p.getJugadorX() + 20);
        for (int i = 0; i < 60 && p.getEstado()==MotorJuego.Estado.CORRIENDO; i++) p.actualizar(DT);
        boolean r1 = p.getEstado() == MotorJuego.Estado.PREGUNTA;
        p.resolverPregunta(true);
        boolean r2 = p.getEstado() == MotorJuego.Estado.CORRIENDO && p.esInvulnerable() && p.getVidas() == 4 && p.getAciertos() == 1;
        for (int k = 0; k < 4; k++) {
            for (int i = 0; i < 400 && p.esInvulnerable(); i++) p.actualizar(DT);
            p.pruebaAgregarObstaculo(0, p.getJugadorX() + 20);
            for (int i = 0; i < 400 && p.getEstado()==MotorJuego.Estado.CORRIENDO; i++) p.actualizar(DT);
            if (p.getEstado()==MotorJuego.Estado.PREGUNTA) p.resolverPregunta(false);
        }
        System.out.printf("%n5) Choque -> pregunta: %s | acertar mantiene vidas e invulnerable: %s | 4 errores -> FIN: %s (vidas=%d)%n",
                r1, r2, p.getEstado()==MotorJuego.Estado.FIN, p.getVidas());
        if (!r1 || !r2 || p.getEstado()!=MotorJuego.Estado.FIN) fallos++;

        System.out.println(fallos == 0 ? "\nTODAS LAS PRUEBAS OK" : "\nHAY " + fallos + " FALLOS");
        System.exit(fallos == 0 ? 0 : 1);
    }
}
