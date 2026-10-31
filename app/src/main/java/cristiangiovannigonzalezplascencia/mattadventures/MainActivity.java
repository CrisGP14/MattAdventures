package cristiangiovannigonzalezplascencia.mattadventures;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

/** Splash animado: el personaje avanza mientras carga la barra de progreso. */
public class MainActivity extends BaseActivity
{
    private ImageView imgper;
    private ProgressBar pbprogres;
    private RelativeLayout frmsplash;
    private CountDownTimer timer;
    public static int seg=5,mili=seg*1000;
    private int x=4;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Crea la base de preguntas (vacía) si no existía
        new Bdpreguntas(this);

        pbprogres = (ProgressBar)findViewById(R.id.pbprogres);
        frmsplash = (RelativeLayout)findViewById(R.id.frmprincipal);
        imgper = (ImageView)findViewById(R.id.imgper);

        // Usuario por defecto: solo se crea si la tabla está vacía
        BdUsuarios bd_usu = new BdUsuarios(this, "Usuarios", null, 1);
        SQLiteDatabase db2 = bd_usu.getWritableDatabase();
        Cursor c = db2.rawQuery("SELECT COUNT(*) FROM Usuarios", null);
        int filas = 0;
        if (c.moveToFirst()) filas = c.getInt(0);
        c.close();
        if (filas == 0)
        {
            db2.execSQL("INSERT INTO Usuarios (id,nombre,puntaje1,puntaje2,puntaje3)" +
                    " VALUES (null,' Ingre Usuario','0','0','0')");
        }

        pbprogres.setProgressTintList(ColorStateList.valueOf(Color.CYAN));
        pbprogres.setMax(maximo());
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        iniciar();
    }

    @Override
    protected void onPause()
    {
        // Si la app pasa a segundo plano no se debe abrir el menú desde ahí
        // (Android lo bloquea y el juego se cerraría solo): se cancela y se reinicia al volver.
        if (timer != null) timer.cancel();
        super.onPause();
    }

    public void iniciar ()
    {
        if (timer != null) timer.cancel();
        x = 4;
        pbprogres.setProgress(0);
        timer = new CountDownTimer(mili,500)
        {

            @Override
            public void onTick(long millisUntilFinished)
            {
                pbprogres.setProgress(progreso(millisUntilFinished));
                x--;
                switch (x)
                {
                    case 4:
                        imgper.setImageResource(R.drawable.ca);
                        frmsplash.setBackgroundResource(R.drawable.mundo3);
                        break;
                    case 3:
                        imgper.setImageResource(R.drawable.ba);
                        frmsplash.setBackgroundResource(R.drawable.mundo1);
                        break;
                    case 2:
                        imgper.setImageResource(R.drawable.ca);
                        frmsplash.setBackgroundResource(R.drawable.mundo2);
                        break;
                    case 1:
                        imgper.setImageResource(R.drawable.ba);
                        x=5;
                        break;
                }
            }

            @Override
            public void onFinish()
            {
                startActivity(new Intent(MainActivity.this,Menu.class));
                finish();
            }
        }.start();

    }

    public int progreso(long miliseg)
    {
        return (int) ((mili-miliseg)/1000);
    }

    public int maximo()
    {
        return seg-2;
    }
}
