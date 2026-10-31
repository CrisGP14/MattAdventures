package cristiangiovannigonzalezplascencia.mattadventures;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;

public class Credito extends BaseActivity implements OnTouchListener
{
    private int i=1;
    private ImageView imgavanzar;
    private TextView lblcreditos;
    private String a,b,c;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.credt);

        imgavanzar = (ImageView)findViewById(R.id.imgavanzar);
        lblcreditos = (TextView)findViewById(R.id.lblcreditos);

        a = "Hola!! gracias por haber jugado este videojuego, fue completamente desarrollado por Cristian Giovanni Gonzalez Plascencia, la programacion como el diseño del personaje y de todos los escenarios.";
        b ="Tambien agradecer a algunas personas que me ayudaron con algunos detalles en el videojuego, Fernanda Isabel Escamilla Luna, Cynthia Trinidad Cardona Dávila y Vanessa Sinahi Juarez Gracia";
        c = "Musica Adventure Meme Kevin MacLeod (incompetech.com), Amazing Grace 2011 Kevin MacLeod (incompetech.com), Ambler Kevin MacLeod (incompetech.com), Breakdown Kevin MacLeod (incompetech.com)";

        iniciarMusica(R.raw.starrynight);

        lblcreditos.setText(a);
        imgavanzar.setOnTouchListener(this);

    }

    @Override
    public boolean onTouch(View v, MotionEvent evento)
    {
        if (evento.getAction() == MotionEvent.ACTION_DOWN)
        {
            if (v.getId() == R.id.imgavanzar)
            {
                i++;
                if(i==1) lblcreditos.setText(a);
                else if(i==2) lblcreditos.setText(b);
                else if(i==3) lblcreditos.setText(c);
                else if(i==4) {i=1;lblcreditos.setText(a);}
            }
        }
        return true;
    }

    @Override
    protected void alAtras()
    {
        startActivity(new Intent(Credito.this, Menu.class));
        finish();
    }

}
