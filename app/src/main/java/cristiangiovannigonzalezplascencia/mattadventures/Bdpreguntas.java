package cristiangiovannigonzalezplascencia.mattadventures;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class Bdpreguntas extends SQLiteOpenHelper
{

    private static final String BD_Nombre = "Problemas";
    private static  final int VERSION = 2;
    private SQLiteDatabase db;

    public Bdpreguntas(Context context)
    {
        super(context, BD_Nombre, null, VERSION);
        db = this.getWritableDatabase();
    }
    private ContentValues generarValores(EPreguntas preguntas)
    {
        ContentValues valores = new ContentValues();
        valores.put(EPreguntas.CAMPO_Pregunta,preguntas.getPregunta());
        valores.put(EPreguntas.CAMPO_Respuesta,preguntas.getRespuesta());
        valores.put(EPreguntas.CAMPO_Ayuda,preguntas.getAyuda());
        return valores;
    }

    public void insertarPregunta (EPreguntas preguntas)
    {
        db.insert(EPreguntas.TABLE_NAME,null,generarValores(preguntas));
    }

    /** Inserta todas las preguntas en una sola transacción (más rápido y atómico). */
    public void insertarPreguntas (List<EPreguntas> lista)
    {
        db.beginTransaction();
        try
        {
            for (EPreguntas p : lista) insertarPregunta(p);
            db.setTransactionSuccessful();
        }
        finally
        {
            db.endTransaction();
        }
    }

    public ArrayList<EPreguntas> getPreguntas()
    {
        ArrayList<EPreguntas> preguntas = new ArrayList<>();
        String columnas[] ={EPreguntas.CAMPO_ID,EPreguntas.CAMPO_Pregunta,EPreguntas.CAMPO_Respuesta,EPreguntas.CAMPO_Ayuda};
        Cursor c = db.query(EPreguntas.TABLE_NAME,columnas,null,null,null,null,null);
        try
        {
            if(c.moveToFirst())
            {
                do
                {
                    EPreguntas p = new EPreguntas();
                    p.setId(c.getInt(0));
                    p.setPregunta(c.getString(1));
                    p.setRespuesta(c.getString(2));
                    p.setAyuda(c.getString(3));
                    preguntas.add(p);

                }while(c.moveToNext());
            }
        }
        finally
        {
            c.close();
        }
        return preguntas;
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(EPreguntas.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        if (oldVersion < 2)
        {
            // La pregunta del producto 162 tenia la respuesta 150; la correcta es 6 (162 / (9 x 3))
            ContentValues v = new ContentValues();
            v.put(EPreguntas.CAMPO_Respuesta, "6");
            v.put(EPreguntas.CAMPO_Ayuda, "Multiplica los dos numeros que ya tienes (9 x 3) y divide 162 entre ese resultado");
            db.update(EPreguntas.TABLE_NAME, v,
                    EPreguntas.CAMPO_Pregunta + " LIKE ? AND " + EPreguntas.CAMPO_Respuesta + " = ?",
                    new String[]{"El producto de 3 numeros es 162%", "150"});
        }
    }
}
