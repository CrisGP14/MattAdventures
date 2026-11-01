package cristiangiovannigonzalezplascencia.mattadventures;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/** Reglas del juego (antes era un video del juego anterior). */
public class ComoJugar extends BaseActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.comojugar);

        Button btnback = (Button)findViewById(R.id.btnback);
        btnback.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        alAtras();
                    }
                });

        iniciarMusica(R.raw.musica_menu);
    }

    @Override
    protected void alAtras()
    {
        startActivity(new Intent(ComoJugar.this, Menu.class));
        finish();
    }
}
