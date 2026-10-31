package cristiangiovannigonzalezplascencia.mattadventures;


public class EPreguntas
{
    public static final String TABLE_NAME = "Preguntas";
    public static final String CAMPO_ID = "Id_pregunta";
    public static final String CAMPO_Pregunta = "Pregunta";
    public static final String CAMPO_Respuesta = "Respuesta";
    public static final String CAMPO_Ayuda = "Leccion";
    public static final String CREATE_TABLE = "create table " + TABLE_NAME + "( " +
            CAMPO_ID +" integer primary key autoincrement, "+
            CAMPO_Pregunta + " text, "+
            CAMPO_Respuesta + " text, " +
            CAMPO_Ayuda + " text   );";


    private int id;
    private String pregunta,respuesta,ayuda;


    public EPreguntas(String pregunta, String respuesta, String ayuda )//,String puntaje1, String puntaje2, String puntaje3, String usuario)
    {
        this.pregunta = pregunta;
        this.respuesta = respuesta;
        this.ayuda = ayuda;

    }

    public EPreguntas ()
    {

    }

    public String getRespuesta()
    {
        return respuesta;
    }

    public void setRespuesta(String respuesta) {this.respuesta = respuesta;}

    public String getPregunta()
    {
        return pregunta;
    }

    public void setPregunta(String pregunta)
    {
        this.pregunta = pregunta;
    }

    public String getAyuda()
    {
        return ayuda;
    }

    public void setAyuda(String ayuda)
    {
        this.ayuda = ayuda;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }
}
