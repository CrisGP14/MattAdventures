package cristiangiovannigonzalezplascencia.mattadventures;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.VideoView;

public class ComoJugar extends BaseActivity
{
    private VideoView videohiw;
    private Button btnback;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.comojugar);

        videohiw =(VideoView)findViewById(R.id.videohow);
        btnback = (Button)findViewById(R.id.btnback);

        Uri path = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.tuto);
        videohiw.setVideoURI(path);
        videohiw.setMediaController(new MediaController(this));
        videohiw.start();
        videohiw.requestFocus();

        btnback.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        alAtras();
                    }
                });

        iniciarMusica(R.raw.starrynight);
    }

    @Override
    protected void alAtras()
    {
        startActivity(new Intent(ComoJugar.this, Menu.class));
        finish();
    }
}
