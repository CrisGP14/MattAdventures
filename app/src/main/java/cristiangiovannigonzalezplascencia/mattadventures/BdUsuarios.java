package cristiangiovannigonzalezplascencia.mattadventures;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BdUsuarios extends SQLiteOpenHelper
{
    String sqlCreate = "CREATE TABLE Usuarios (id integer primary key autoincrement, nombre TEXT, puntaje1 integer" +
                                                                              ", puntaje2 integer, puntaje3 integer)";

    public BdUsuarios(Context contexto, String nombre, SQLiteDatabase.CursorFactory factory, int version) {
        super(contexto, nombre, factory, version);
        //super(context, BD_Nombre, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db2) {
        db2.execSQL(sqlCreate);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int versionAnterior, int versionNueva) {}
}