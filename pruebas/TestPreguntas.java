package cristiangiovannigonzalezplascencia.mattadventures;
import java.util.*;
public class TestPreguntas {
    public static void main(String[] a) {
        Random r = new Random(7); int mal = 0;
        for (int c = 0; c <= 3000; c++) for (int k = 0; k < 20; k++) {
            int[] o = BolsaPreguntas.opciones(c, r);
            boolean tiene = false; for (int v : o) { if (v == c) tiene = true; if (v < 0) mal++; }
            if (!tiene || o[0]==o[1] || o[0]==o[2] || o[1]==o[2]) { mal++; System.out.println("MAL c="+c+" "+Arrays.toString(o)); }
        }
        System.out.println("opciones: 60,000 combinaciones, errores = " + mal);
        // ejemplos
        for (int c : new int[]{2, 6, 15, 95, 375, 1646}) System.out.println("  correcta " + c + " -> " + Arrays.toString(BolsaPreguntas.opciones(c, r)));
        // bolsa: sin repetir hasta agotar y sin repetir al cambiar de bolsa
        List<EPreguntas> l = new ArrayList<>(); for (int i = 0; i < 30; i++) l.add(new EPreguntas("P"+i, String.valueOf(i*3+1), "ayuda"));
        l.add(new EPreguntas("rota","abc","x"));   // respuesta no numerica se ignora
        BolsaPreguntas b = new BolsaPreguntas(l, new Random(3)); String ant = null; boolean ok = true; Set<String> vistas = new HashSet<>();
        for (int i = 0; i < 300; i++) { PreguntaJuego p = b.siguiente(); if (p.texto.equals(ant)) ok = false; ant = p.texto; if (i < 30) { if (!vistas.add(p.texto)) ok = false; } if (p.texto.equals("rota")) ok = false; if (!p.opciones[p.correcta].equals(String.valueOf(Integer.parseInt(p.opciones[p.correcta])))) ok = false; }
        System.out.println("bolsa: 30 distintas antes de repetir, sin repetidas seguidas, ignora rotas: " + ok + " (distintas en las primeras 30: " + vistas.size() + ")");
        BolsaPreguntas vacia = new BolsaPreguntas(new ArrayList<EPreguntas>(), new Random(1)); PreguntaJuego g = vacia.siguiente();
        System.out.println("base vacia -> pregunta generada: " + g.texto + " opciones=" + Arrays.toString(g.opciones) + " correcta=" + g.opciones[g.correcta]);
        System.exit(mal == 0 && ok ? 0 : 1);
    }
}
