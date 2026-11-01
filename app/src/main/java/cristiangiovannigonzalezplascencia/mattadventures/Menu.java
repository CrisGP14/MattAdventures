package cristiangiovannigonzalezplascencia.mattadventures;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.animation.ValueAnimator;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class Menu extends BaseActivity {

    private static final String NOMBRE_POR_DEFECTO = "Ingre Usuario";

    private SQLiteDatabase db2;
    private TextView lblusuario;
    private ImageView imgpersonaje, imgsombra;
    private ValueAnimator animacion;

    private static final float CICLO_SEG = 2.6f;      // cada ciclo: respira y al final da un saltito
    private static final float SALTO_INICIO = 1.95f;  // segundo del ciclo en que empieza el salto
    private static final float SALTO_DURACION = 0.55f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_l);

        imgpersonaje = (ImageView) findViewById(R.id.imgpersonaje);
        imgsombra = (ImageView) findViewById(R.id.imgsombra);
        lblusuario = (TextView) findViewById(R.id.lblusuarios);
        TextView lbliniciar = (TextView) findViewById(R.id.lbluno);
        TextView lblpuntajes = (TextView) findViewById(R.id.lblnivel);
        TextView lblhow = (TextView) findViewById(R.id.lblhowto);
        TextView lblcreditos = (TextView) findViewById(R.id.lblcreditos);
        TextView lblsalir = (TextView) findViewById(R.id.lblsalir);

        imgpersonaje.setImageDrawable(sprite(R.drawable.aa));   // Matt de frente

        iniciarMusica(R.raw.musica_menu);

        BdUsuarios bd_usu = new BdUsuarios(this, "Usuarios", null, 1);
        db2 = bd_usu.getWritableDatabase();
        lblusuario.setText(textoNombre(leerNombre()));

        lblusuario.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                pedirNombre();
            }
        });
        lbliniciar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                irA(CargarDatos.class);   // la primera vez carga las preguntas y abre el juego
            }
        });
        lblpuntajes.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mostrarPuntajes();
            }
        });
        lblhow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                irA(ComoJugar.class);
            }
        });
        lblcreditos.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                irA(Credito.class);
            }
        });
        lblsalir.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                finishAffinity();
            }
        });
    }

    /** Sprite sin suavizado para que el pixel art se vea nítido al agrandarlo. */
    private Drawable sprite(int id) {
        Drawable d = new BitmapDrawable(getResources(), Escenario.cargar(getResources(), id));
        d.setFilterBitmap(false);
        return d;
    }

    private String leerNombre() {
        String nombre = "";
        Cursor c = db2.rawQuery("SELECT nombre FROM Usuarios where id =1", null);
        if (c.moveToFirst()) nombre = c.getString(0);
        c.close();
        return nombre == null ? "" : nombre.trim();
    }

    private boolean esNombrePorDefecto(String nombre) {
        return nombre.isEmpty() || nombre.equals(NOMBRE_POR_DEFECTO);
    }

    private String textoNombre(String nombre) {
        return esNombrePorDefecto(nombre) ? "Toca para poner tu nombre" : "Jugador: " + nombre;
    }

    private void pedirNombre() {
        AlertDialog.Builder builder = new AlertDialog.Builder(Menu.this);
        builder.setTitle("Ingresa tu nombre de jugador");
        final EditText input = new EditText(Menu.this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        String actual = leerNombre();
        if (!esNombrePorDefecto(actual)) {
            input.setText(actual);
            input.setSelection(actual.length());
        }
        builder.setView(input);
        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String nombre = input.getText().toString().trim();
                if (nombre.isEmpty()) return;
                lblusuario.setText(textoNombre(nombre));

                // Con ContentValues el nombre ya no rompe el SQL (apóstrofes, comillas...)
                ContentValues valores = new ContentValues();
                valores.put("nombre", nombre);
                valores.put("puntaje1", 0);
                valores.put("puntaje2", 0);
                valores.put("puntaje3", 0);
                db2.update("Usuarios", valores, "id = 1", null);
            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    /** Muestra los récords del jugador (se guardan al terminar o salir de una partida). */
    private void mostrarPuntajes() {
        String nombre = leerNombre();
        int mejor = 0, racha = 0, aciertos = 0;
        Cursor c = db2.rawQuery("SELECT puntaje1, puntaje2, puntaje3 FROM Usuarios WHERE id = 1", null);
        if (c.moveToFirst()) {
            mejor = c.getInt(0);
            racha = c.getInt(1);
            aciertos = c.getInt(2);
        }
        c.close();

        new AlertDialog.Builder(Menu.this)
                .setTitle(esNombrePorDefecto(nombre) ? "Puntajes" : "Puntajes de " + nombre)
                .setMessage("Mejor puntaje: " + mejor
                        + "\nMejor racha de libros esquivados: " + racha
                        + "\nRespuestas correctas en total: " + aciertos)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void irA(Class<?> destino) {
        detenerMusica();
        startActivity(new Intent(Menu.this, destino));
        finish();
    }

    /** Matt respira (sube y baja un poco) y cada tanto da un saltito; la sombra se achica al saltar. */
    private void animarPersonaje() {
        final float d = getResources().getDisplayMetrics().density;
        imgpersonaje.setPivotX(62f * d);
        imgpersonaje.setPivotY(124f * d);      // se estira desde los pies
        animacion = ValueAnimator.ofFloat(0f, CICLO_SEG);
        animacion.setDuration((long) (CICLO_SEG * 1000));
        animacion.setInterpolator(new android.view.animation.LinearInterpolator());
        animacion.setRepeatCount(ValueAnimator.INFINITE);
        animacion.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                float t = (Float) a.getAnimatedValue();
                float respira = (float) Math.sin(t * 2f * Math.PI / CICLO_SEG * 2f);     // dos respiraciones por ciclo
                float alto = 0f, estira = 1f + 0.015f * respira;
                float u = (t - SALTO_INICIO) / SALTO_DURACION;
                if (u >= 0f && u <= 1f) {
                    alto = 4f * u * (1f - u) * 26f;                    // parábola: sube y baja 26 dp
                    estira = 1f + 0.06f * (float) Math.sin(u * Math.PI);
                } else if (u > 1f && u < 1.25f) {
                    estira = 1f - 0.07f * (float) Math.sin((u - 1f) / 0.25f * Math.PI);   // se aplasta al caer
                }
                imgpersonaje.setTranslationY(-alto * d);
                imgpersonaje.setScaleY(estira);
                imgpersonaje.setScaleX(2f - estira);
                float k = 1f - alto / 60f;
                imgsombra.setScaleX(k);
                imgsombra.setAlpha(0.5f + 0.5f * k);
            }
        });
        animacion.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (animacion == null) animarPersonaje(); else animacion.resume();
    }

    @Override
    protected void onPause() {
        if (animacion != null) animacion.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (animacion != null) animacion.cancel();
        super.onDestroy();
    }
}
