package cristiangiovannigonzalezplascencia.mattadventures;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class Tablero extends BaseActivity implements OnTouchListener, SensorEventListener {
    private ImageView imgpersonaje,imgder,imgizq,imgpuerta,imgpuertai,imgnubem,imgnubem2,imgnubem3,imgprueba,imgdefensa;
    public static int letf, right,frame=0,move=0,time,next;
    private boolean def=false,mod_cami=false,jugando=false;
    private boolean tocandoDer=false,tocandoIzq=false,miraDerecha=true;
    public static  boolean flag=false;
    private int width,height;
    private Sensor sensorAcelerometro = null;
    private RelativeLayout frmtablero;
    private SensorManager sensorM;
    private boolean avisoSensor=false;
    private long ultimoPaso=0;

    /** Milisegundos mínimos entre dos pasos (antes un Thread.sleep(70) que congelaba la pantalla). */
    private static final int MS_ENTRE_PASOS = 70;

    private final Handler handler = new Handler(Looper.getMainLooper());

    // Movimiento del obstáculo (el libro). Antes era un Thread aparte que movía vistas desde fuera del hilo principal.
    private final Runnable bucleObstaculo = new Runnable() {
        @Override
        public void run() {
            if (!jugando) return;
            moverObstaculo();
            if (jugando) handler.postDelayed(this, time);
        }
    };

    // Mientras se mantiene presionado un botón, repite el paso cada MS_ENTRE_PASOS
    private final Runnable caminarConBoton = new Runnable() {
        @Override
        public void run() {
            if (!mod_cami) return;
            darPaso();
            handler.postDelayed(this, MS_ENTRE_PASOS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tablero);
        // En el modo acelerómetro no se toca la pantalla: que no se apague a mitad del nivel
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        frmtablero = (RelativeLayout) findViewById(R.id.frmtablero);
        imgpersonaje = (ImageView) findViewById(R.id.imgpersonaje);
        imgder = (ImageView) findViewById(R.id.imgder);
        imgizq = (ImageView) findViewById(R.id.imgizq);
        imgpuerta = (ImageView)findViewById(R.id.imgpuerta);
        imgpuertai = (ImageView)findViewById(R.id.imgpuertai);
        imgnubem = (ImageView)findViewById(R.id.imgnubee);
        imgnubem2 = (ImageView)findViewById(R.id.imgnubee2);
        imgnubem3 = (ImageView)findViewById(R.id.imgnubee3);
        imgprueba = (ImageView)findViewById(R.id.imgprueba);
        imgdefensa = (ImageView)findViewById(R.id.imgdefensa);
        sensorM = (SensorManager) getSystemService(SENSOR_SERVICE);

        width = anchoPantalla();
        height = altoPantalla();

        sensorAcelerometro = sensorM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        imgder.setOnTouchListener(this);
        imgizq.setOnTouchListener(this);
        imgdefensa.setOnTouchListener(this);

        imgpersonaje.setX(width/2);
        imgpuerta.setX(width);

        // Estado limpio al empezar el nivel (los campos estáticos conservaban valores del intento anterior)
        right = 0;
        letf = 0;
        frame = 0;

        move = 0;
        int nivel = MenuNiveles.nivel;
        if(nivel==1)
        {
            frmtablero.setBackgroundResource(R.drawable.mundo1);
            iniciarMusica(R.raw.ambler);
            time=20;
            next=1;
            imgprueba.setX(100);
        }
        else if(nivel==2)
        {
            frmtablero.setBackgroundResource(R.drawable.mundo2);
            iniciarMusica(R.raw.l);
            time=17;
            next=2;
            imgprueba.setX(100);
        }
        else if(nivel==3)
        {
            frmtablero.setBackgroundResource(R.drawable.mundo3);
            iniciarMusica(R.raw.adventure);
            time=15;
            next=3;
            imgprueba.setX(200);
        }

    }

    @Override
    protected void onResume ()
    {
        super.onResume();
        comprobar();
        jugando = true;
        handler.removeCallbacks(bucleObstaculo);
        handler.postDelayed(bucleObstaculo, time);
    }

    @Override
    protected void onPause ()
    {
        // Antes, al volver de segundo plano se reiniciaba toda la pantalla. Ahora solo se pausa.
        jugando = false;
        handler.removeCallbacks(bucleObstaculo);
        handler.removeCallbacks(caminarConBoton);
        sensorM.unregisterListener(this);
        tocandoDer = false;
        tocandoIzq = false;
        mod_cami = false;
        right = 0;
        letf = 0;
        super.onPause();
    }

    public void defensa ()
    {
        def=true;
        imgpersonaje.setImageResource(R.drawable.mdefensa);
    }

    public void comprobar ()
    {
        if (sensorAcelerometro == null)
        {
            if (!avisoSensor)
            {
                Toast.makeText(getApplicationContext(), "No hay Sensor movimiento", Toast.LENGTH_SHORT).show();
                avisoSensor = true;
            }
        }
        else
        {
            // GAME en lugar de NORMAL: con NORMAL el teléfono manda solo ~5 lecturas por segundo
            sensorM.registerListener(this, sensorAcelerometro, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent evento) {
        int accion = evento.getActionMasked();
        int id = v.getId();

        if (id == R.id.imgder || id == R.id.imgizq)
        {
            if (accion == MotionEvent.ACTION_DOWN)
            {
                if (id == R.id.imgder) tocandoDer = true; else tocandoIzq = true;
                actualizarDireccionBotones();
                handler.removeCallbacks(caminarConBoton);
                handler.post(caminarConBoton);
            }
            else if (accion == MotionEvent.ACTION_UP || accion == MotionEvent.ACTION_CANCEL)
            {
                if (id == R.id.imgder) tocandoDer = false; else tocandoIzq = false;
                actualizarDireccionBotones();
                if (!mod_cami) handler.removeCallbacks(caminarConBoton);
                if (id == R.id.imgder) { imgpersonaje.setImageResource(R.drawable.da); miraDerecha = true; }
                else { imgpersonaje.setImageResource(R.drawable.ia); miraDerecha = false; }
            }
        }
        else if (id == R.id.imgdefensa)
        {
            if (accion == MotionEvent.ACTION_DOWN || accion == MotionEvent.ACTION_MOVE)
            {
                defensa();
            }
            else if (accion == MotionEvent.ACTION_UP || accion == MotionEvent.ACTION_CANCEL)
            {
                def=false;
                imgpersonaje.setImageResource(miraDerecha ? R.drawable.da : R.drawable.ia);
            }
        }

        return true;
    }

    private void actualizarDireccionBotones()
    {
        right = tocandoDer ? 1 : 0;
        letf = (tocandoIzq && !tocandoDer) ? 1 : 0;
        mod_cami = tocandoDer || tocandoIzq;
    }

    /** True si ya pasó el tiempo mínimo desde el último paso. */
    private boolean tocaPaso()
    {
        long ahora = SystemClock.uptimeMillis();
        if (ahora - ultimoPaso < MS_ENTRE_PASOS) return false;
        ultimoPaso = ahora;
        return true;
    }

    private void darPaso()
    {
        if (!tocaPaso()) return;
        iniciar();
        movimiento_escenario();
    }

    public void iniciar()
    {
        frame++;
        if (right == 1)
        {
            move++;
            switch (frame) {
                case 1:
                    imgpersonaje.setImageResource(R.drawable.dc);
                    break;
                case 2:
                    imgpersonaje.setImageResource(R.drawable.da);
                    break;
                case 3:
                    imgpersonaje.setImageResource(R.drawable.db);
                    break;
                case 4:
                    imgpersonaje.setImageResource(R.drawable.da);
                    frame = 0;
                    break;
            }
            miraDerecha = true;

            int ancho = imgpuerta.getWidth(), xobj = (int) imgpuerta.getX(), xpers = (int) imgpersonaje.getX();
            if (move>=50 && imgpuerta.getX()!=width-80)  imgpuerta.setX(imgpuerta.getX()-10);
            if(imgpersonaje.getX()!=width/2 && move!= 60) imgpersonaje.setX(imgpersonaje.getX()+10);
            if(move==60)
            {
                move=59;
                if((imgpersonaje.getX()+65)!=imgpuerta.getX()) imgpersonaje.setX(imgpersonaje.getX()+10);
                if (colision(xobj, ancho, xpers))
                {
                    jugando = false;
                    startActivity(new Intent(Tablero.this,CargarDatos.class));
                    flag=true;
                    if(MenuNiveles.nivel==1) next =2;
                    else if (MenuNiveles.nivel==2) next =3;
                    else if (MenuNiveles.nivel==3) next =4;
                    finish();
                }

            }

        }
        else if (letf == 1)
        {
            move--;
            switch (frame)
            {
                case 1:
                    imgpersonaje.setImageResource(R.drawable.ic);
                    break;
                case 2:
                    imgpersonaje.setImageResource(R.drawable.ia);
                    break;
                case 3:
                    imgpersonaje.setImageResource(R.drawable.ib);
                    break;
                case 4:
                    imgpersonaje.setImageResource(R.drawable.ia);
                    frame=0;
                    break;
            }
            miraDerecha = false;

            if (move>=-50 && imgpuerta.getX()>=width-80 && imgpuerta.getX()!=width)  imgpuerta.setX(imgpuerta.getX()+10);
            if(imgpersonaje.getX()!=width/2 && move !=-60) imgpersonaje.setX(imgpersonaje.getX()-10);
            if(move==-60) {
                move = -59;
                if ((imgpersonaje.getX() - 65) != imgpuertai.getX())
                    imgpersonaje.setX(imgpersonaje.getX() - 10);

                int ancho = imgpuertai.getWidth(), xobj = (int) imgpuertai.getX(), xpers = (int) imgpersonaje.getX();
                if (colision(xobj, ancho, xpers))
                {
                    jugando = false;
                    int nivel = MenuNiveles.nivel;
                    if(nivel==1)
                    {
                        startActivity(new Intent(Tablero.this, ComoJugar.class));
                        finish();
                    }

                    else if(nivel==2)
                    {
                        startActivity(new Intent(Tablero.this, Menu.class));
                        MenuNiveles.nivel=1;
                        finish();
                    }

                    else if(nivel==3)
                    {
                        startActivity(new Intent(Tablero.this, Credito.class));
                        finish();
                    }
                }
            }
        }
    }

    public boolean colision (int xobj, int anchoObj, int xper)
    {
        return xper>xobj && xper<xobj+anchoObj;
    }

    /** Inclinación lateral del teléfono, ya corregida según cómo esté girada la pantalla. Positivo = derecha. */
    @SuppressWarnings("deprecation")
    private float inclinacion(SensorEvent evento)
    {
        float x = evento.values[0];
        float y = evento.values[1];
        int rotacion = getWindowManager().getDefaultDisplay().getRotation();
        switch (rotacion)
        {
            case Surface.ROTATION_0:   return -x;
            case Surface.ROTATION_180: return x;
            case Surface.ROTATION_270: return -y;
            case Surface.ROTATION_90:
            default:                   return y;   // el caso del juego original (horizontal normal)
        }
    }

    @Override
    public void onSensorChanged(SensorEvent evento)
    {
        if (mod_cami) return;      // si hay un botón presionado, manda el botón
        float y = inclinacion(evento);

        if(y >2 && !def)
        {
            right = 1;
            darPaso();
        }
        else if(y<2 && y>-2)
        {
            right=0;
            imgpersonaje.setImageResource(R.drawable.da);
        }
        if(y < -2 && !def)
        {
            letf = 1;
            darPaso();
        }
        else if(y>-2 &&y<0)
        {
            letf=0;
            imgpersonaje.setImageResource(R.drawable.ia);
        }
    }

    public void movimiento_escenario ()
    {
        if(right==1)
        {
            imgnubem.setX(imgnubem.getX()-10);
            if(imgnubem.getX() <-100) imgnubem.setX(width);

            imgnubem2.setX(imgnubem2.getX()-10);
            if(imgnubem2.getX() <-100) imgnubem2.setX(width);

            imgnubem3.setX(imgnubem3.getX()-10);
            if(imgnubem3.getX() <-100) imgnubem3.setX(width);
        }
        else if(letf==1)
        {
            imgnubem.setX(imgnubem.getX()+10);
            if(imgnubem.getX() >width) imgnubem.setX(1-15);

            imgnubem2.setX(imgnubem2.getX()+10);
            if(imgnubem2.getX() >width) imgnubem2.setX(1-15);

            imgnubem3.setX(imgnubem3.getX()+10);
            if(imgnubem3.getX() >width) imgnubem3.setX(1-15);
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void moverObstaculo()
    {
        int ancho = imgprueba.getWidth(),xobj= (int) imgprueba.getX(), xpers = (int) imgpersonaje.getX();
        if(colision(xobj,ancho,xpers) && !def)
        {
            jugando = false;
            startActivity(new Intent(Tablero.this,CargarDatos.class));
            imgprueba.setX(width);
            finish();
            return;
        }
        if(imgprueba.getX() <-100) imgprueba.setX(width);
        imgprueba.setX(imgprueba.getX()-5);
    }
}
