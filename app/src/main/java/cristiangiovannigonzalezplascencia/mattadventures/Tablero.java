package cristiangiovannigonzalezplascencia.mattadventures;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.util.Random;

/**
 * Pantalla del juego: corredor automático. Se toca para saltar los libros.
 * Si chocas con un libro aparece una pregunta encima de la partida (3 opciones):
 *   - acertar: sigues corriendo, con unos segundos sin poder chocar;
 *   - fallar: pierdes un corazón (con 0 corazones termina la partida).
 */
public class Tablero extends BaseActivity implements MotorJuego.Escucha {

    private static final int MS_ANTES_DE_PREGUNTAR = 450;
    private static final int MS_TRAS_ACIERTO = 900;
    private static final int MS_TRAS_ERROR = 1800;

    private MotorJuego motor;
    private JuegoView juego;
    private BolsaPreguntas bolsa;
    private PreguntaJuego actual;
    private boolean respondida, pistaUsada, guardado;

    private View panelPregunta, panelMensaje, tarjetaPregunta, tarjetaMensaje;
    private TextView lblpregunta, lblresultado, lblayuda, lbltitmsg, lbldetmsg;
    private Button[] opciones = new Button[3];
    private Button btnpista, btnmsg1, btnmsg2;

    private SQLiteDatabase db2;
    private int mejorPuntaje, mejorRachaGuardada, totalAciertos;

    private SoundPool sonidos;
    private int sSalto, sGolpe, sAcierto, sError;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tablero);
        // sin tocar la pantalla durante un rato no debe apagarse a mitad de la partida
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        juego = (JuegoView) findViewById(R.id.juego);
        panelPregunta = findViewById(R.id.panelPregunta);
        panelMensaje = findViewById(R.id.panelMensaje);
        tarjetaPregunta = findViewById(R.id.tarjetaPregunta);
        tarjetaMensaje = findViewById(R.id.tarjetaMensaje);
        lblpregunta = (TextView) findViewById(R.id.lblpregunta);
        lblresultado = (TextView) findViewById(R.id.lblresultado);
        lblayuda = (TextView) findViewById(R.id.lblayuda);
        lbltitmsg = (TextView) findViewById(R.id.lbltitmsg);
        lbldetmsg = (TextView) findViewById(R.id.lbldetmsg);
        btnpista = (Button) findViewById(R.id.btnpista);
        btnmsg1 = (Button) findViewById(R.id.btnmsg1);
        btnmsg2 = (Button) findViewById(R.id.btnmsg2);
        opciones[0] = (Button) findViewById(R.id.btnopcion1);
        opciones[1] = (Button) findViewById(R.id.btnopcion2);
        opciones[2] = (Button) findViewById(R.id.btnopcion3);

        ajustarTarjetas();

        BdUsuarios bd_usu = new BdUsuarios(this, "Usuarios", null, 1);
        db2 = bd_usu.getWritableDatabase();
        leerMarcas();

        motor = new MotorJuego();
        motor.setEscucha(this);
        juego.setMotor(motor);
        juego.setMejor(mejorPuntaje);
        juego.setOnToque(new Runnable() {
            @Override
            public void run() {
                alTocar();
            }
        });

        bolsa = new BolsaPreguntas(new Bdpreguntas(this).getPreguntas(), new Random());

        for (int i = 0; i < 3; i++) {
            final int pos = i;
            opciones[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    responder(pos);
                }
            });
        }
        btnpista.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pedirPista();
            }
        });

        iniciarSonidos();
        iniciarMusica(R.raw.musica_dia);
    }

    /** Las tarjetas miden 620 / 460 dp, pero nunca más que la pantalla. */
    private void ajustarTarjetas() {
        float d = getResources().getDisplayMetrics().density;
        int disponible = anchoPantalla() - (int) (32 * d);
        fijarAncho(tarjetaPregunta, Math.min((int) (620 * d), disponible));
        fijarAncho(tarjetaMensaje, Math.min((int) (460 * d), disponible));
    }

    private void fijarAncho(View v, int px) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        lp.width = px;
        v.setLayoutParams(lp);
    }

    // ------------------------------------------------------------------
    // Sonido
    // ------------------------------------------------------------------

    private void iniciarSonidos() {
        AudioAttributes atributos = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        sonidos = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(atributos).build();
        sSalto = sonidos.load(this, R.raw.sfx_salto, 1);
        sGolpe = sonidos.load(this, R.raw.sfx_golpe, 1);
        sAcierto = sonidos.load(this, R.raw.sfx_acierto, 1);
        sError = sonidos.load(this, R.raw.sfx_error, 1);
    }

    private void sonar(int id) {
        if (sonidos != null && id != 0) sonidos.play(id, 0.7f, 0.7f, 1, 0, 1f);
    }

    private void vibrar(int tipo) {
        juego.performHapticFeedback(tipo);
    }

    // ------------------------------------------------------------------
    // Eventos del motor (llegan en el hilo principal, durante el dibujo)
    // ------------------------------------------------------------------

    @Override
    public void alSaltar() {
        sonar(sSalto);
    }

    @Override
    public void alGolpear() {
        sonar(sGolpe);
        juego.efectoGolpe();
        vibrar(HapticFeedbackConstants.LONG_PRESS);
        // una pausa corta para que se vea el golpe antes de la pregunta
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) mostrarPregunta();
            }
        }, MS_ANTES_DE_PREGUNTAR);
    }

    /** Cada tramo del día tiene su propia pieza: día, atardecer y noche. */
    @Override
    public void alSubirVelocidad(final int nivel) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || motor.getEstado() == MotorJuego.Estado.FIN) return;
                if (nivel == 1) iniciarMusica(R.raw.musica_atardecer);
                else if (nivel == 2) iniciarMusica(R.raw.musica_noche);
            }
        });
    }

    @Override
    public void alEsquivar(int racha) {
    }

    // ------------------------------------------------------------------
    // Entrada
    // ------------------------------------------------------------------

    private void alTocar() {
        if (motor.getEstado() == MotorJuego.Estado.LISTO) {
            motor.empezar();
            motor.saltar();
        } else if (motor.getEstado() == MotorJuego.Estado.CORRIENDO) {
            motor.saltar();
        }
    }

    // ------------------------------------------------------------------
    // Preguntas
    // ------------------------------------------------------------------

    private void mostrarPregunta() {
        actual = bolsa.siguiente();
        respondida = false;
        pistaUsada = false;

        lblpregunta.setText(actual.texto);
        for (int i = 0; i < 3; i++) {
            opciones[i].setText(actual.opciones[i]);
            opciones[i].setBackgroundResource(R.drawable.boton_opcion);
            opciones[i].setEnabled(true);
        }
        lblresultado.setText("");
        lblayuda.setText("");
        btnpista.setEnabled(true);
        btnpista.setText("Ver pista (-" + MotorJuego.COSTO_PISTA + " puntos)");
        panelPregunta.setVisibility(View.VISIBLE);
    }

    private void pedirPista() {
        if (respondida || pistaUsada || actual == null) return;
        pistaUsada = true;
        motor.cobrarPista();
        lblayuda.setText(actual.ayuda == null ? "" : actual.ayuda);
        btnpista.setEnabled(false);
    }

    private void responder(int pos) {
        if (respondida || actual == null) return;
        respondida = true;
        final boolean acierto = pos == actual.correcta;

        for (Button b : opciones) b.setEnabled(false);
        btnpista.setEnabled(false);

        if (acierto) {
            opciones[pos].setBackgroundResource(R.drawable.boton_ok);
            lblresultado.setTextColor(Color.parseColor("#2E9E4F"));
            lblresultado.setText("¡Correcto!");
            sonar(sAcierto);
            vibrar(Build.VERSION.SDK_INT >= 30 ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.VIRTUAL_KEY);
        } else {
            opciones[pos].setBackgroundResource(R.drawable.boton_mal);
            opciones[actual.correcta].setBackgroundResource(R.drawable.boton_ok);
            lblresultado.setTextColor(Color.parseColor("#D64545"));
            lblresultado.setText("Era " + actual.opciones[actual.correcta]);
            sonar(sError);
            vibrar(Build.VERSION.SDK_INT >= 30 ? HapticFeedbackConstants.REJECT : HapticFeedbackConstants.LONG_PRESS);
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                terminarPregunta(acierto);
            }
        }, acierto ? MS_TRAS_ACIERTO : MS_TRAS_ERROR);
    }

    private void terminarPregunta(boolean acierto) {
        if (isFinishing()) return;
        panelPregunta.setVisibility(View.GONE);
        motor.resolverPregunta(acierto);
        if (motor.getEstado() == MotorJuego.Estado.FIN) mostrarFin();
    }

    // ------------------------------------------------------------------
    // Pausa, fin y puntajes
    // ------------------------------------------------------------------

    private void leerMarcas() {
        Cursor c = db2.rawQuery("SELECT puntaje1, puntaje2, puntaje3 FROM Usuarios WHERE id = 1", null);
        if (c.moveToFirst()) {
            mejorPuntaje = c.getInt(0);
            mejorRachaGuardada = c.getInt(1);
            totalAciertos = c.getInt(2);
        }
        c.close();
    }

    /** Guarda mejor puntaje, mejor racha y suma de aciertos. Devuelve true si es récord de puntaje. */
    private boolean guardarResultados() {
        if (guardado) return false;
        guardado = true;
        boolean record = motor.getPuntos() > mejorPuntaje;

        ContentValues v = new ContentValues();
        v.put("puntaje1", Math.max(mejorPuntaje, motor.getPuntos()));
        v.put("puntaje2", Math.max(mejorRachaGuardada, motor.getMejorRacha()));
        v.put("puntaje3", totalAciertos + motor.getAciertos());
        db2.update("Usuarios", v, "id = 1", null);
        return record;
    }

    private void mostrarPausa() {
        lbltitmsg.setText("Pausa");
        lbldetmsg.setText("Puntos: " + motor.getPuntos());
        btnmsg1.setText("Continuar");
        btnmsg2.setText("Salir al menú");
        btnmsg1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                panelMensaje.setVisibility(View.GONE);
                motor.reanudar();
            }
        });
        btnmsg2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarResultados();
                irAlMenu();
            }
        });
        panelMensaje.setVisibility(View.VISIBLE);
    }

    private void mostrarFin() {
        boolean record = guardarResultados();

        lbltitmsg.setText("¡Fin de la partida!");
        StringBuilder sb = new StringBuilder();
        sb.append("Puntos: ").append(motor.getPuntos());
        if (record) sb.append("  ¡NUEVO RÉCORD!");
        else sb.append("\nMejor: ").append(mejorPuntaje);
        sb.append("\nLibros esquivados: ").append(motor.getEsquivados());
        sb.append("\nRespuestas correctas: ").append(motor.getAciertos());
        lbldetmsg.setText(sb.toString());

        btnmsg1.setText("Jugar de nuevo");
        btnmsg2.setText("Menú");
        btnmsg1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Tablero.this, Tablero.class));
                finish();
            }
        });
        btnmsg2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                irAlMenu();
            }
        });
        iniciarMusica(R.raw.musica_fin);
        panelMensaje.setVisibility(View.VISIBLE);
    }

    private void irAlMenu() {
        startActivity(new Intent(Tablero.this, Menu.class));
        finish();
    }

    // ------------------------------------------------------------------
    // Ciclo de vida
    // ------------------------------------------------------------------

    @Override
    protected void alAtras() {
        switch (motor.getEstado()) {
            case CORRIENDO:
                motor.pausar();
                mostrarPausa();
                break;
            case PAUSA:
                panelMensaje.setVisibility(View.GONE);
                motor.reanudar();
                break;
            case LISTO:
                irAlMenu();
                break;
            case FIN:
                irAlMenu();
                break;
            default:
                break;   // con la pregunta en pantalla, el botón atrás no hace nada
        }
    }

    @Override
    protected void onPause() {
        // Al salir de la app se pausa solo, y al volver aparece el panel de pausa
        if (motor.getEstado() == MotorJuego.Estado.CORRIENDO) {
            motor.pausar();
            mostrarPausa();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (sonidos != null) {
            sonidos.release();
            sonidos = null;
        }
        super.onDestroy();
    }
}
