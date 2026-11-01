package cristiangiovannigonzalezplascencia.mattadventures;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class Credito extends BaseActivity
{
    private int i=0;
    private TextView lblcreditos, lblpagina;
    private String[] paginas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.credt);

        lblcreditos = (TextView)findViewById(R.id.lblcreditos);
        lblpagina = (TextView)findViewById(R.id.lblpagina);
        Button btnsiguiente = (Button)findViewById(R.id.btnsiguiente);
        Button btnmenu = (Button)findViewById(R.id.btnmenu);

        paginas = new String[] {
            "¡Hola! Gracias por haber jugado este videojuego. Fue desarrollado por crisgp14 (GitHub), tanto la programación como el diseño del personaje y de todos los escenarios.",
            "Música y efectos de sonido: composiciones originales creadas para este videojuego, en estilo chiptune."
        };

        iniciarMusica(R.raw.musica_menu);
        mostrar();

        btnsiguiente.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                i = (i + 1) % paginas.length;
                mostrar();
            }
        });
        btnmenu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                alAtras();
            }
        });
    }

    private void mostrar()
    {
        lblcreditos.setText(paginas[i]);
        lblpagina.setText("Créditos  " + (i + 1) + "/" + paginas.length);
    }

    @Override
    protected void alAtras()
    {
        startActivity(new Intent(Credito.this, Menu.class));
        finish();
    }

}
