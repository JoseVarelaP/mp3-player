package uv.mp3;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

public class DetailsActivity extends Activity {
    MediaPlayer player;
    Thread posThread;
    Uri mediaUri;
    int pos;

    @Override
    protected void onCreate (@Nullable Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_details);

        SeekBar sbProgress = findViewById (R.id.sbProgress);
        sbProgress.setOnSeekBarChangeListener (new MySeekBarChangeListener ());

        player = new MediaPlayer ();
        player.setOnPreparedListener (mediaPlayer -> {
            posThread = new Thread (() -> {
                try {
                    if( player != null && sbProgress != null ) {
                        while (player.isPlaying()) {
                            Thread.sleep(1000);
                            if( player != null && sbProgress != null )
                                sbProgress.setProgress(player.getCurrentPosition());
                        }
                    }
                } catch (InterruptedException in) { in.printStackTrace (); }
            });

            sbProgress.setMax (mediaPlayer.getDuration ());
            if (pos > -1) mediaPlayer.seekTo (pos);
            mediaPlayer.start ();
            posThread.start ();
        });

        mediaUri = Uri.parse(getIntent().getStringExtra("audioURL"));
        String nCancion = getIntent().getStringExtra("nombreCancion");
        String nArtista = getIntent().getStringExtra("nombreArtista");

        if (player.isPlaying ()) {
            posThread.interrupt ();
            player.stop ();
            player.seekTo (0);
            sbProgress.setProgress (0);
            pos = -1;
        }

        // ImageView MuestraImagen = findViewById( R.id.imageCover );
        TextView Nombre = findViewById( R.id.songName );
        TextView Artista = findViewById( R.id.artistName );

        try {
            player.setDataSource(getBaseContext (), mediaUri);
            player.prepare ();
            // MuestraImagen.setImageURI( nImagen );
            Nombre.setText( nCancion );
            Artista.setText( nArtista );

        } catch (IOException ex) { ex.printStackTrace (); }


        ImageButton button = findViewById( R.id.Accion );
        button.setOnClickListener( v -> {
            if (player.isPlaying()) {
                player.pause();
                button.setImageResource( R.drawable.ic_play_arrow_black_48dp );
            } else {
                player.start();
                button.setImageResource( R.drawable.ic_pause_black_48dp );
            }
        });

    }

    @Override
    protected void onSaveInstanceState (@NonNull Bundle outState) {
        super.onSaveInstanceState (outState);

        outState.putString ("SONG", mediaUri != null ? mediaUri.toString (): "");
        outState.putInt ("PROGRESS", player != null ?  player.getCurrentPosition () : -1);
        outState.putBoolean ("ISPLAYING", player != null && player.isPlaying ());

        if (player.isPlaying ()) {
            posThread.interrupt ();

            player.stop ();
            player.seekTo (0);
            player.release ();
            player = null;
        }
    }

    @Override
    protected void onRestoreInstanceState (@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState (savedInstanceState);

        mediaUri = Uri.parse (savedInstanceState.getString ("SONG"));
        pos = savedInstanceState.getInt ("PROGRESS");
        boolean isPlaying = savedInstanceState.getBoolean ("ISPLAYING");

        if (player == null) return;

        try {
            player.reset ();
            player.setDataSource (getBaseContext (), mediaUri);
            if (isPlaying) player.prepareAsync ();
        } catch (IOException | IllegalStateException ioex) {
            ioex.printStackTrace ();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Destruye el reproductor tambien para evitar choques.
        if (player != null && player.isPlaying ()) {
            player.stop ();
            player.release ();
        }

        player = null;
    }

    @Override
    protected void onDestroy () {
        super.onDestroy();
        // cleanup

        if (player != null && player.isPlaying ()) {
            player.stop ();
            player.release ();
        }

        player = null;
    }

    @Override
    protected void onResume() {
        super.onResume ();
    }

    @Override
    protected void onStart() {
        super.onStart ();

        Intent intent = getIntent ();

    }

    class MySeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onProgressChanged (SeekBar seekBar, int i, boolean b) {
            if (b) {
                player.pause ();
                player.seekTo (i);
                player.start ();
            }
        }

        @Override
        public void onStartTrackingTouch (SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch (SeekBar seekBar) {}

    }
}
