package cristiangiovannigonzalezplascencia.mattadventures;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View.OnTouchListener;
import java.util.ArrayList;

public class Actividades extends BaseActivity implements SensorEventListener, OnTouchListener {

    private ImageView imgayuda,imgndiez,imgnuno,imgmcien,imgmdiez;
    private TextView lblpregunta,lblrespuesta,lblayuda;
    private Button btnaceptar;
    private SQLiteDatabase db2;
    private int num = 0,respuesta,punt1,punt2,punt3,aux=0;
    private static int incorrecto=3,puntos=0;
    private String ayuda="s";
    private Sensor s;
    private SensorManager sensorM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividades);

        sensorM = (SensorManager) getSystemService(SENSOR_SERVICE);
        s = sensorM.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        iniciarMusica(R.raw.breakdown);

        BdUsuarios bd_usu = new BdUsuarios(this, "Usuarios", null, 1);
        db2 = bd_usu.getWritableDatabase();

        imgayuda = (ImageView) findViewById(R.id.imgayuda);
        lblpregunta = (TextView) findViewById(R.id.lblpregunta);
        lblrespuesta = (TextView) findViewById(R.id.lblrespuesta);
        lblayuda = (TextView) findViewById(R.id.lblayuda);
        btnaceptar = (Button)findViewById(R.id.btnresponder);
        imgmcien = (ImageView)findViewById(R.id.imgmcien);
        imgmdiez = (ImageView)findViewById(R.id.imgmdiez);
        imgndiez = (ImageView)findViewById(R.id.imgndiez);
        imgnuno = (ImageView)findViewById(R.id.imgnuno);

        lblrespuesta.setText(String.valueOf(num));

        imgayuda.setOnTouchListener(this);
        imgnuno.setOnTouchListener(this);
        imgndiez.setOnTouchListener(this);
        imgmdiez.setOnTouchListener(this);
        imgmcien.setOnTouchListener(this);

        Bdpreguntas x = new Bdpreguntas(this);
        ArrayList<EPreguntas> preg = x.getPreguntas();

        if(!preg.isEmpty())
        {
            int aleatorio = generarAleatorio(preg.size());
            EPreguntas p =  preg.get(aleatorio);
            lblpregunta.setText(p.getPregunta());
            respuesta = (Integer.parseInt(p.getRespuesta()));
            ayuda=p.getAyuda();

        }

        Cursor c = db2.rawQuery("SELECT* FROM Usuarios where id =1", null);
        if (c.moveToFirst()) {
            punt1 = c.getInt(2);
            punt2 = c.getInt(3);
            punt3 = c.getInt(4);
        }
        c.close();


        btnaceptar.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        if(num==respuesta)
                        {
                            puntos++;
                            Toast.makeText(getBaseContext(), "Respuesta Correcta puntos = "+puntos, Toast.LENGTH_SHORT).show();
                            incorrecto=3;
                            if (Tablero.next==2)
                            {
                                if(puntos > punt1) guardarPuntaje("puntaje1", puntos);
                                if(Tablero.flag)
                                {puntos=0; incorrecto=3; Tablero.flag=false;}
                                MenuNiveles.nivel=2;
                                irA(Tablero.class);
                            }
                            else if (Tablero.next==3)
                            {
                                if(puntos>punt2) guardarPuntaje("puntaje2", puntos);
                                if(Tablero.flag)
                                {puntos=0; incorrecto=3; Tablero.flag=false;}
                                MenuNiveles.nivel=3;
                                irA(Tablero.class);
                            }

                            else if (Tablero.next==4)
                            {
                                if(puntos>punt3) guardarPuntaje("puntaje3", puntos);
                                if(Tablero.flag)
                                {puntos=0; incorrecto=3; Tablero.flag=false;}
                                irA(Menu.class);
                            }
                            else
                            {
                                irA(Tablero.class);
                            }

                        }
                        else
                        {
                            incorrecto--;
                            Toast.makeText(getBaseContext(), "Respuesta Incorrecta oportunidades restantes = " + incorrecto, Toast.LENGTH_SHORT).show();
                        }

                        if(incorrecto<0)
                        {
                            Toast.makeText(getBaseContext(), "Has perdido", Toast.LENGTH_SHORT).show();
                            puntos=0; incorrecto=3;
                            irA(Menu.class);
                        }

                    }
                });

    }

    private void guardarPuntaje(String columna, int valor)
    {
        // La columna viene siempre de este archivo (nunca del usuario), el valor va como parámetro
        db2.execSQL("UPDATE Usuarios SET " + columna + " = ? WHERE id = 1", new Object[]{valor});
    }

    private void irA(Class<?> destino)
    {
        startActivity(new Intent(Actividades.this, destino));
        finish();
    }

    @Override
    protected void onResume ()
    {
        super.onResume();
        if (s != null) sensorM.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause ()
    {
        sensorM.unregisterListener(this);
        super.onPause();
    }

    private int generarAleatorio(int cantidad)
    {
        return (int)(Math.random()*cantidad);
    }

    @Override
    public void onSensorChanged(SensorEvent evento)
    {
        // "Cerca" = lectura menor al alcance máximo del sensor. Antes se comparaba contra 7 fijo,
        // pero muchos teléfonos reportan 5 cuando NO hay nada cerca y eso contaba doble.
        if (evento.values[0] < evento.sensor.getMaximumRange())
        {
            num++;
            lblrespuesta.setText(String.valueOf(num));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }

    @Override
    public boolean onTouch(View v, MotionEvent evento) {
        if (evento.getAction() == MotionEvent.ACTION_DOWN)
        {
            if (v.getId() == R.id.imgayuda)
            {
                aux++;
                lblayuda.setText(ayuda);
                if(aux>10)
                {
                    num=respuesta;
                    lblrespuesta.setText(String.valueOf(num));
                }
            }
            else if(v.getId() == R.id.imgnuno)
            {
                num-=1;
                lblrespuesta.setText(String.valueOf(num));
            }
            else if(v.getId() == R.id.imgndiez)
            {
                num-=10;
                lblrespuesta.setText(String.valueOf(num));
            }
            else if(v.getId() == R.id.imgmdiez)
            {
                num+=10;
                lblrespuesta.setText(String.valueOf(num));
            }
            else if(v.getId() == R.id.imgmcien)
            {
                num+=100;
                lblrespuesta.setText(String.valueOf(num));
            }
        }
        return false;
    }

}
