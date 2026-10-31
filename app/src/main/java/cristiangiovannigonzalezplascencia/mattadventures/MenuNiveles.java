package cristiangiovannigonzalezplascencia.mattadventures;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;


public class MenuNiveles extends BaseActivity implements OnTouchListener {
    private SQLiteDatabase db2;
    public static int nivel = 1;
    private ImageView imgmundo1,imgmundo2,imgmundo3;
    private TextView lbluno, lbldos, lbltres, lblpn1, lblpn2, lblpn3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_niveles);

        lbluno = (TextView) findViewById(R.id.lbluno);
        lbldos = (TextView) findViewById(R.id.lbldos);
        lbltres = (TextView) findViewById(R.id.lbltres);
        lblpn1 = (TextView) findViewById(R.id.lblpuntaje1);
        lblpn2 = (TextView) findViewById(R.id.lblpuntaje2);
        lblpn3 = (TextView) findViewById(R.id.lblpuntaje3);
        imgmundo1 = (ImageView) findViewById(R.id.imgmundo1);
        imgmundo2 = (ImageView) findViewById(R.id.imgmundo2);
        imgmundo3 = (ImageView) findViewById(R.id.imgmundo3);

        BdUsuarios bd_usu = new BdUsuarios(this, "Usuarios", null, 1);
        db2 = bd_usu.getWritableDatabase();

        Cursor c = db2.rawQuery("SELECT* FROM Usuarios where id =1", null);
        if (c.moveToFirst()) {
            lblpn1.setText("Puntaje " + c.getInt(2));
            lblpn2.setText("Puntaje " + c.getInt(3));
            lblpn3.setText("Puntaje " + c.getInt(4));
        }
        c.close();

        imgmundo1.setOnTouchListener(this);
        imgmundo2.setOnTouchListener(this);
        imgmundo3.setOnTouchListener(this);

        lbluno.setOnClickListener(new IrANivel(1));
        lbldos.setOnClickListener(new IrANivel(2));
        lbltres.setOnClickListener(new IrANivel(3));
    }

    private class IrANivel implements View.OnClickListener {
        private final int destino;
        IrANivel(int destino) { this.destino = destino; }

        @Override
        public void onClick(View view) {
            empezarNivel(destino);
        }
    }

    private void empezarNivel(int n) {
        nivel = n;
        startActivity(new Intent(MenuNiveles.this, Tablero.class));
        finish();
    }

    @Override
    public boolean onTouch(View v, MotionEvent evento)
    {
        if (evento.getAction() == MotionEvent.ACTION_DOWN)
        {
            if (v.getId() == R.id.imgmundo1) empezarNivel(1);
            else if (v.getId() == R.id.imgmundo2) empezarNivel(2);
            else if (v.getId() == R.id.imgmundo3) empezarNivel(3);
        }
        return true;
    }

    @Override
    protected void alAtras()
    {
        startActivity(new Intent(MenuNiveles.this, Menu.class));
        finish();
    }

}
