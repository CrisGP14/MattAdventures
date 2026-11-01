package cristiangiovannigonzalezplascencia.mattadventures;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Random;

/**
 * Splash: es el propio juego en modo demostración. Matt corre y salta los libros solo,
 * amanece mientras carga la barra, y el logo entra con rebote.
 */
public class MainActivity extends BaseActivity
{
    private static final int DURACION_MS = 4500;
    private static final String[] CONSEJOS = {
            "Toca la pantalla para saltar los libros",
            "Cada 30 segundos el juego se acelera",
            "Acierta las preguntas para seguir corriendo",
            "Esquiva libros para ganar más puntos",
            "La pista cuesta 15 puntos: úsala con cuidado"
    };

    private JuegoView juego;
    private ProgressBar pbprogres;
    private ImageView logo;
    private TextView lblcargando, lblconsejo;

    private ValueAnimator progresoAnim;
    private ObjectAnimator flotarAnim;
    private boolean cancelado = false;
    private int puntos = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable puntitos = new Runnable() {
        @Override
        public void run() {
            puntos = (puntos + 1) % 4;
            StringBuilder sb = new StringBuilder("Cargando");
            for (int i = 0; i < puntos; i++) sb.append('.');
            lblcargando.setText(sb.toString());
            handler.postDelayed(this, 350);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Crea la base de preguntas (vacía) si no existía
        new Bdpreguntas(this);

        juego = (JuegoView) findViewById(R.id.juegosplash);
        pbprogres = (ProgressBar) findViewById(R.id.pbprogres);
        logo = (ImageView) findViewById(R.id.imgsplashlogo);
        lblcargando = (TextView) findViewById(R.id.lblcargando);
        lblconsejo = (TextView) findViewById(R.id.lblconsejo);

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

        juego.setMotor(new MotorJuego());
        juego.setDemo(true);
        juego.setHoraManual(2f);                      // empieza de noche y amanece al cargar

        lblconsejo.setText(CONSEJOS[new Random().nextInt(CONSEJOS.length)]);
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
        cancelado = true;
        if (progresoAnim != null) progresoAnim.cancel();
        if (flotarAnim != null) flotarAnim.cancel();
        handler.removeCallbacks(puntitos);
        super.onPause();
    }

    private void iniciar()
    {
        cancelado = false;
        pbprogres.setProgress(0);
        handler.removeCallbacks(puntitos);
        handler.post(puntitos);

        // Entrada del logo: cae con rebote y luego flota suavemente
        float d = getResources().getDisplayMetrics().density;
        logo.setAlpha(0f);
        logo.setScaleX(0.6f);
        logo.setScaleY(0.6f);
        logo.setTranslationY(-60f * d);
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                .setDuration(650).setInterpolator(new OvershootInterpolator(1.6f))
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (cancelado) return;
                        float dd = getResources().getDisplayMetrics().density;
                        flotarAnim = ObjectAnimator.ofFloat(logo, "translationY", 0f, -5f * dd);
                        flotarAnim.setDuration(900);
                        flotarAnim.setRepeatCount(ValueAnimator.INFINITE);
                        flotarAnim.setRepeatMode(ValueAnimator.REVERSE);
                        flotarAnim.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
                        flotarAnim.start();
                    }
                }).start();

        // Carga: avanza la barra y amanece
        progresoAnim = ValueAnimator.ofFloat(0f, 1f);
        progresoAnim.setDuration(DURACION_MS);
        progresoAnim.setInterpolator(new LinearInterpolator());
        progresoAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                float v = (Float) a.getAnimatedValue();
                float suave = new DecelerateInterpolator(1.2f).getInterpolation(v);
                pbprogres.setProgress((int) (suave * 1000));
                juego.setHoraManual(2f * (1f - v));   // noche -> atardecer -> día
            }
        });
        progresoAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (cancelado) return;
                startActivity(new Intent(MainActivity.this, Menu.class));
                finish();
            }
        });
        progresoAnim.start();
    }

    @Override
    protected void onDestroy()
    {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
