package cristiangiovannigonzalezplascencia.mattadventures;

/** Una pregunta lista para mostrar: texto, pista y tres opciones (una correcta). */
public class PreguntaJuego {
    public final String texto;
    public final String ayuda;
    public final String[] opciones;
    /** Posición (0, 1 o 2) de la opción correcta. */
    public final int correcta;

    public PreguntaJuego(String texto, String ayuda, String[] opciones, int correcta) {
        this.texto = texto;
        this.ayuda = ayuda;
        this.opciones = opciones;
        this.correcta = correcta;
    }
}
