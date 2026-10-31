package cristiangiovannigonzalezplascencia.mattadventures;

import android.app.Activity;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

/**
 * Clase base de todas las pantallas del juego.
 *
 * Concentra lo que cambió en las versiones modernas de Android para que
 * cada Activity no tenga que repetirlo:
 *  - Pantalla completa inmersiva (la API vieja FLAG_FULLSCREEN ya no basta).
 *  - Botón "atrás": desde Android 16 (targetSdk 36) onBackPressed() ya no se llama,
 *    hay que registrar un OnBackInvokedCallback.
 *  - Medidas reales de la pantalla.
 *  - Música: se pausa/reanuda sola y siempre se libera (antes se filtraba).
 */
public abstract class BaseActivity extends Activity {

    private MediaPlayer musica;
    private OnBackInvokedCallback callbackAtras;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registrarAtras();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        ocultarBarras();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) ocultarBarras();
    }

    // ------------------------------------------------------------------
    // Pantalla completa
    // ------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    private void ocultarBarras() {
        Window w = getWindow();
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = w.getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            w.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    // ------------------------------------------------------------------
    // Botón atrás
    // ------------------------------------------------------------------

    /** Lo que hace el botón atrás en esta pantalla. Por defecto nada (como en el juego original). */
    protected void alAtras() {
    }

    private void registrarAtras() {
        if (Build.VERSION.SDK_INT >= 33) {
            callbackAtras = new OnBackInvokedCallback() {
                @Override
                public void onBackInvoked() {
                    alAtras();
                }
            };
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, callbackAtras);
        }
    }

    /** Android 12 o menor sigue llamando a este método. */
    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        alAtras();
    }

    // ------------------------------------------------------------------
    // Medidas de pantalla (en píxeles, pantalla completa)
    // ------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    private Rect limitesPantalla() {
        if (Build.VERSION.SDK_INT >= 30) {
            return getWindowManager().getCurrentWindowMetrics().getBounds();
        }
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        return new Rect(0, 0, dm.widthPixels, dm.heightPixels);
    }

    protected int anchoPantalla() {
        return limitesPantalla().width();
    }

    protected int altoPantalla() {
        return limitesPantalla().height();
    }

    // ------------------------------------------------------------------
    // Música
    // ------------------------------------------------------------------

    /** Inicia música en bucle desde res/raw. Si ya había otra, la detiene. */
    protected void iniciarMusica(int rawId) {
        detenerMusica();
        musica = MediaPlayer.create(this, rawId);
        if (musica != null) {
            musica.setLooping(true);
            musica.start();
        }
    }

    protected void detenerMusica() {
        if (musica != null) {
            try {
                musica.stop();
            } catch (IllegalStateException ignorada) {
                // ya estaba detenida
            }
            musica.release();
            musica = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (musica != null) musica.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (musica != null && musica.isPlaying()) musica.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detenerMusica();
        if (Build.VERSION.SDK_INT >= 33 && callbackAtras != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(callbackAtras);
            callbackAtras = null;
        }
    }
}
