package cristiangiovannigonzalezplascencia.mattadventures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Reparte las preguntas sin repetir hasta haber salido todas ("bolsa"), y arma las tres
 * opciones de respuesta. No usa nada de Android.
 */
public class BolsaPreguntas {

    private final List<EPreguntas> todas = new ArrayList<EPreguntas>();
    private final List<EPreguntas> bolsa = new ArrayList<EPreguntas>();
    private final Random azar;
    private EPreguntas ultima;

    public BolsaPreguntas(List<EPreguntas> lista, Random azar) {
        this.azar = azar;
        for (EPreguntas e : lista) {
            if (e.getPregunta() == null || e.getRespuesta() == null) continue;
            try {
                Integer.parseInt(e.getRespuesta().trim());
                todas.add(e);
            } catch (NumberFormatException ignorada) {
                // pregunta con respuesta no numérica: se omite
            }
        }
    }

    public PreguntaJuego siguiente() {
        if (todas.isEmpty()) return generarSuma();

        if (bolsa.isEmpty()) {
            bolsa.addAll(todas);
            Collections.shuffle(bolsa, azar);
            // que la primera de la bolsa nueva no sea igual a la última de la anterior
            int n = bolsa.size();
            if (n > 1 && bolsa.get(n - 1) == ultima) Collections.swap(bolsa, n - 1, 0);
        }
        EPreguntas e = bolsa.remove(bolsa.size() - 1);
        ultima = e;

        int correcta = Integer.parseInt(e.getRespuesta().trim());
        return armar(e.getPregunta(), e.getAyuda(), correcta);
    }

    /** Si la base de datos estuviera vacía, el juego no se queda sin preguntas. */
    private PreguntaJuego generarSuma() {
        int a = 10 + azar.nextInt(90);
        int b = 10 + azar.nextInt(90);
        return armar("¿Cuánto es " + a + " + " + b + "?", "Suma las unidades, luego las decenas.", a + b);
    }

    private PreguntaJuego armar(String texto, String ayuda, int correcta) {
        int[] valores = opciones(correcta, azar);
        String[] textos = new String[3];
        int pos = 0;
        for (int i = 0; i < 3; i++) {
            textos[i] = String.valueOf(valores[i]);
            if (valores[i] == correcta) pos = i;
        }
        return new PreguntaJuego(texto, ayuda, textos, pos);
    }

    /** Devuelve 3 valores distintos: la respuesta correcta y dos distractores creíbles, en orden aleatorio. */
    static int[] opciones(int correcta, Random azar) {
        List<Integer> cerca = new ArrayList<Integer>();
        List<Integer> lejos = new ArrayList<Integer>();

        int[] deltasCerca = {1, -1, 2, -2, 10, -10};
        for (int d : deltasCerca) {
            int v = correcta + d;
            if (v >= 0 && v != correcta && !cerca.contains(v)) cerca.add(v);
        }
        // distractores "lejanos": solo si el número es lo bastante grande para que sean creíbles
        List<Integer> deltasLejos = new ArrayList<Integer>();
        deltasLejos.add(5);
        deltasLejos.add(-5);
        if (correcta >= 20) { deltasLejos.add(20); deltasLejos.add(-20); }
        if (correcta >= 200) { deltasLejos.add(100); deltasLejos.add(-100); }
        for (int d : deltasLejos) {
            int v = correcta + d;
            if (v >= 0 && v != correcta && !lejos.contains(v)) lejos.add(v);
        }
        // errores típicos: dígitos invertidos, doble y mitad
        if (correcta >= 10) {
            int inv = invertir(correcta);
            if (inv != correcta && !lejos.contains(inv)) lejos.add(inv);
        }
        if (correcta >= 2 && correcta * 2 != correcta && !lejos.contains(correcta * 2)) lejos.add(correcta * 2);
        if (correcta >= 4 && correcta % 2 == 0 && !lejos.contains(correcta / 2)) lejos.add(correcta / 2);

        Collections.shuffle(cerca, azar);
        Collections.shuffle(lejos, azar);

        int[] r = new int[3];
        r[0] = correcta;
        r[1] = cerca.get(0);                      // siempre hay al menos 3 candidatos cercanos (c+1, c+2, c+10)
        int segundo = 0;
        boolean listo = false;
        // el segundo distractor sale de "lejos" (si hay) para dar variedad; si no, de "cerca"
        for (int v : lejos) { if (v != r[1]) { segundo = v; listo = true; break; } }
        if (!listo) { for (int v : cerca) { if (v != r[1]) { segundo = v; break; } } }
        r[2] = segundo;

        // mezclar posiciones
        for (int i = 2; i > 0; i--) {
            int j = azar.nextInt(i + 1);
            int t = r[i]; r[i] = r[j]; r[j] = t;
        }
        return r;
    }

    private static int invertir(int n) {
        int r = 0;
        while (n > 0) { r = r * 10 + n % 10; n /= 10; }
        return r;
    }
}
