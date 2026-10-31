package cristiangiovannigonzalezplascencia.mattadventures;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class Menu extends BaseActivity {

    private SQLiteDatabase db2;
    private ImageView[] nubes;
    private ImageView[] hojas;
    private TextView lbliniciar, lblnivel, lblsalir, lblusuario,lblhow;
    private int width;

    // Animación del fondo (nubes y hojas): se ejecuta en el hilo principal cada 50 ms
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable animar = new Runnable() {
        @Override
        public void run() {
            mover(nubes);
            mover(hojas);
            handler.postDelayed(this, 50);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_l);

        nubes = new ImageView[] {
                (ImageView) findViewById(R.id.imgnubes1),
                (ImageView) findViewById(R.id.imgnube2),
                (ImageView) findViewById(R.id.imgnube3),
                (ImageView) findViewById(R.id.imgnubes4)
        };
        hojas = new ImageView[] {
                (ImageView) findViewById(R.id.imghoja1),
                (ImageView) findViewById(R.id.imghoja2),
                (ImageView) findViewById(R.id.imghoja3),
                (ImageView) findViewById(R.id.imghoja4),
                (ImageView) findViewById(R.id.imghoja5),
                (ImageView) findViewById(R.id.imghoja6)
        };
        lbliniciar = (TextView) findViewById(R.id.lbluno);
        lblnivel = (TextView) findViewById(R.id.lblnivel);
        lblsalir = (TextView) findViewById(R.id.lblsalir);
        lblhow = (TextView) findViewById(R.id.lblhowto);
        lblusuario = (TextView) findViewById(R.id.lblusuarios);

        width = anchoPantalla();

        iniciarMusica(R.raw.starrynight);

        BdUsuarios bd_usu = new BdUsuarios(this, "Usuarios", null, 1);
        db2 = bd_usu.getWritableDatabase();

        Cursor c = db2.rawQuery("SELECT nombre FROM Usuarios where id =1", null);
        if (c.moveToFirst()) {
            lblusuario.setText(c.getString(0));
        }
        c.close();

        lblusuario.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view)
                    {
                        AlertDialog.Builder builder = new AlertDialog.Builder(Menu.this);
                        builder.setTitle("Ingrese su nombre de jugador");
                        final EditText input = new EditText(Menu.this);
                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                        builder.setView(input);
                        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String nombre = input.getText().toString().trim();
                                if (nombre.isEmpty()) return;
                                lblusuario.setText(nombre);

                                // Con ContentValues el nombre ya no rompe el SQL (apóstrofes, comillas...)
                                ContentValues valores = new ContentValues();
                                valores.put("nombre", nombre);
                                valores.put("puntaje1", 0);
                                valores.put("puntaje2", 0);
                                valores.put("puntaje3", 0);
                                db2.update("Usuarios", valores, "id = 1", null);
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        builder.show();
                    }
                });

        lbliniciar.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        irA(Tablero.class);
                    }
                });

        lblhow.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        irA(ComoJugar.class);
                    }
                });

        lblsalir.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        finishAffinity();
                    }
                });

        lblnivel.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        irA(MenuNiveles.class);
                    }
                });

    }

    private void irA(Class<?> destino) {
        detenerMusica();
        startActivity(new Intent(Menu.this, destino));
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(animar);
        handler.post(animar);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(animar);
        super.onPause();
    }

    /** Mueve los elementos 5 px a la izquierda y los regresa a la derecha al salir de pantalla. */
    private void mover(ImageView[] vistas) {
        for (ImageView v : vistas) {
            v.setX(v.getX() - 5);
            if (v.getX() < -100) v.setX(width);
        }
    }
}
