package uv.mp3;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Handler;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DetailsActivity extends Activity {
    private AudioServiceBinder audioServiceBinder = null;
    private Handler audioProgressUpdateHandler = null;
    private ProgressBar backgroundAudioProgress;
    MediaPlayer player;
    Thread posThread;
    Uri mediaUri;

    @Override
    protected void onCreate (@Nullable Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_details);

        SeekBar sbProgress = findViewById (R.id.sbProgress);
        sbProgress.setOnSeekBarChangeListener (new MySeekBarChangeListener ());

        audioServiceBinder = new AudioServiceBinder();

        mediaUri = Uri.parse(getIntent().getStringExtra("audioURL"));
        String nCancion = getIntent().getStringExtra("nombreCancion");
        String nArtista = getIntent().getStringExtra("nombreArtista");

        TextView Nombre = findViewById( R.id.songName );
        TextView Artista = findViewById( R.id.artistName );

        audioServiceBinder.setAudioFileUri(mediaUri);
        createAudioProgressbarUpdater();

        backgroundAudioProgress = findViewById( R.id.backgroundaudioprogress );

        audioServiceBinder.setContext(getApplicationContext());
        audioServiceBinder.startAudio();

        // Esto deberia quedar aqui?
        // (Problablemente puede estar en el servicio tambien)
        audioServiceBinder.GetPlayer().setOnPreparedListener (mediaPlayer -> {
            posThread = new Thread (() -> {
                try {
                    if( audioServiceBinder != null ) {
                        while ( audioServiceBinder.isPlaying() ) {
                            Thread.sleep(1000);
                            if( audioServiceBinder != null )
                                sbProgress.setProgress(audioServiceBinder.getCurrentAudioPosition());
                        }
                    }
                } catch (InterruptedException in) { in.printStackTrace (); }
            });

            sbProgress.setMax ( audioServiceBinder.getTotalAudioDuration() );
            posThread.start ();
        });

        Nombre.setText( nCancion );
        Artista.setText( nArtista );

        ImageButton button = findViewById( R.id.Accion );
        button.setOnClickListener( v -> {
            if (audioServiceBinder.isPlaying()) {
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

    @Override
    protected void onDestroy () { super.onDestroy(); }

    @Override
    protected void onResume() {
        super.onResume ();
    }

    @Override
    protected void onStart() { super.onStart (); }

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
