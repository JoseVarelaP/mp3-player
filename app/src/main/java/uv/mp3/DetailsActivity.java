package uv.mp3;

import android.content.ComponentName;
import android.content.Context;
import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Handler;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.content.ServiceConnection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

public class DetailsActivity extends Activity {
    private AudioServiceBinder audioServiceBinder = null;
    private Handler audioProgressUpdateHandler = null;
    private ProgressBar backgroundAudioProgress;
    MediaPlayer player;
    Thread posThread;
    Uri mediaUri;
    int pos;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // Cast and assign background service's onBind method returned iBander object.
            audioServiceBinder = (AudioServiceBinder) iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    @Override
    protected void onCreate (@Nullable Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_details);

        SeekBar sbProgress = findViewById (R.id.sbProgress);
        sbProgress.setOnSeekBarChangeListener (new MySeekBarChangeListener ());

        audioServiceBinder = new AudioServiceBinder();

        // Esto deberia quedar aqui?
        // (Problablemente puede estar en el servicio tambien)
        player = new MediaPlayer ();
        player.setOnPreparedListener (mediaPlayer -> {
            posThread = new Thread (() -> {
                try {
                    if( audioServiceBinder != null && sbProgress != null ) {
                        while (audioServiceBinder.isPlaying()) {
                            Thread.sleep(1000);
                            if( audioServiceBinder != null && sbProgress != null )
                                sbProgress.setProgress(audioServiceBinder.getAudioProgress());
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

        backgroundAudioProgress.setVisibility(ProgressBar.VISIBLE);

        /*
        if (player.isPlaying ()) {
            posThread.interrupt ();
            player.stop ();
            player.seekTo (0);
            sbProgress.setProgress (0);
            pos = -1;
        }
        */

        // ImageView MuestraImagen = findViewById( R.id.imageCover );
        TextView Nombre = findViewById( R.id.songName );
        TextView Artista = findViewById( R.id.artistName );

        audioServiceBinder.setAudioFileUri(mediaUri);
        createAudioProgressbarUpdater();

        backgroundAudioProgress = (ProgressBar)findViewById( R.id.backgroundaudioprogress );

        audioServiceBinder.setAudioProgressUpdateHandler( audioProgressUpdateHandler );
        audioServiceBinder.setContext(getApplicationContext());
        audioServiceBinder.startAudio();

        Nombre.setText( nCancion );
        Artista.setText( nArtista );

        ImageButton button = findViewById( R.id.Accion );
        button.setOnClickListener( v -> {
            if (player.isPlaying()) {
                audioServiceBinder.pauseAudio();
                button.setImageResource( R.drawable.ic_play_arrow_black_48dp );
            } else {
                audioServiceBinder.startAudio();
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

        if ( audioServiceBinder.isPlaying() ) {
            posThread.interrupt ();

            // audioServiceBinder.stopAudio();
            // audioServiceBinder.setProgress(0);
            // audioServiceBinder.destroyAudioPlayer();
        }
    }

    /*
    @Override
    protected void onRestoreInstanceState (@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState (savedInstanceState);

        mediaUri = Uri.parse (savedInstanceState.getString ("SONG"));
        pos = savedInstanceState.getInt ("PROGRESS");
        boolean isPlaying = savedInstanceState.getBoolean ("ISPLAYING");

        if (audioServiceBinder == null) return;

        try {
            player.setDataSource (getBaseContext (), mediaUri);
            if (isPlaying) player.prepareAsync ();
        } catch (IOException | IllegalStateException ioex) {
            ioex.printStackTrace ();
        }
    }
    */

    @Override
    protected void onDestroy () {
        super.onDestroy();
        // cleanup

        if (audioServiceBinder != null && audioServiceBinder.isPlaying ()) {
            audioServiceBinder.destroyAudioPlayer();
        }
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
                audioServiceBinder.pauseAudio();
                audioServiceBinder.setProgress( i );
                audioServiceBinder.startAudio();
            }
        }

        @Override
        public void onStartTrackingTouch (SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch (SeekBar seekBar) {}

    }

    private void createAudioProgressbarUpdater()
    {
        if(audioProgressUpdateHandler==null) {
            audioProgressUpdateHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what == audioServiceBinder.UPDATE_AUDIO_PROGRESS_BAR) {

                        if( audioServiceBinder != null) {
                            int progAct =audioServiceBinder.getAudioProgress();

                            backgroundAudioProgress.setProgress(progAct*10);
                        }
                    }
                }
            };
        }
    }
}
