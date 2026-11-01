package cristiangiovannigonzalezplascencia.mattadventures;
import java.util.Random;
public class TestDemo {
    // misma logica que JuegoView.jugarSolo()
    static void jugarSolo(MotorJuego m) {
        float px1 = m.getJugadorX() + MotorJuego.HIT_X; MotorJuego.Obstaculo prox = null;
        for (MotorJuego.Obstaculo o : m.getObstaculos())
            if (o.x + o.ancho * (1f - MotorJuego.HIT_OBST_LADOS) > px1 && (prox == null || o.x < prox.x)) prox = o;
        if (prox != null) { float dist = prox.x + prox.ancho / 2f - (px1 + MotorJuego.HIT_ANCHO / 2f);
            if (dist <= m.getVelocidad() * (MotorJuego.VELOCIDAD_SALTO / MotorJuego.GRAVEDAD)) m.saltar(); }
    }
    public static void main(String[] a) {
        int choques = 0, corridas = 0, esquives = 0;
        for (int fps : new int[]{24, 30, 60, 120}) for (int ancho : new int[]{640, 915, 1280}) for (int sem = 0; sem < 60; sem++) {
            MotorJuego m = new MotorJuego(new Random(sem * 31 + fps)); m.setAncho(ancho); m.modoDemo(1);
            for (int i = 0; i < 3.5 * 60; i++) { jugarSolo(m); m.actualizar(1f / 60f); }      // precalentamiento
            for (int i = 0; i < 4.5 * fps; i++) { jugarSolo(m); m.actualizar(1f / fps);
                if (m.getEstado() == MotorJuego.Estado.PREGUNTA) { choques++; break; } }
            corridas++; esquives += m.getEsquivados();
        }
        System.out.println("splash demo: " + corridas + " corridas (24/30/60/120 fps, 3 anchos de pantalla), choques = " + choques + ", libros esquivados en total = " + esquives);
        System.exit(choques == 0 ? 0 : 1);
    }
}
