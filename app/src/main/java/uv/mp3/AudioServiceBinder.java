package uv.mp3;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import java.io.IOException;

public class AudioServiceBinder extends Binder {

    // Guarda la ubicacion del archivo
    private Uri audioFileUri = null;

    // Save web audio file url.
    private String audioFileUrl = "";

    // Check if stream audio.
    private boolean streamAudio = false;

    // El reproductor mismo para reproducir contenido.
    private MediaPlayer audioPlayer = null;

    // Contexto, necesario para interactuar externalmente.
    private Context context = null;

    // Necesario para el progreso.
    private Handler audioProgressUpdateHandler;

    public final int UPDATE_AUDIO_PROGRESS_BAR = 1;

    public Context getContext() { return context; }
    public void setContext(Context context) { this.context = context; }
    public boolean isStreamAudio() { return streamAudio; }
    public void setStreamAudio(boolean streamAudio) { this.streamAudio = streamAudio; }
    public Uri getAudioFileUri() { return audioFileUri; }
    public void setAudioFileUri(Uri audioFileUri) { this.audioFileUri = audioFileUri; }
    public Handler getAudioProgressUpdateHandler() { return audioProgressUpdateHandler; }
    public void setProgress( int ms ) { this.audioPlayer.seekTo(ms); }

    public void setAudioProgressUpdateHandler(Handler audioProgressUpdateHandler) {
        this.audioProgressUpdateHandler = audioProgressUpdateHandler;
    }

    public void startAudio()
    {
        initAudioPlayer();
        if(audioPlayer!=null) {
            audioPlayer.start();
        }
    }

    public void pauseAudio()
    {
        if(audioPlayer!=null) {
            audioPlayer.pause();
        }
    }

    public void stopAudio()
    {
        if(audioPlayer!=null) {
            audioPlayer.stop();
            destroyAudioPlayer();
        }
    }

    public boolean isPlaying()
    {
        if(audioPlayer!=null){
            return audioPlayer.isPlaying();
        }
        return false;
    }

    private void initAudioPlayer()
    {
        try {
            if (audioPlayer == null) {
                audioPlayer = new MediaPlayer();

                if( TextUtils.isEmpty(getAudioFileUri().toString()) )
                    return;

                audioPlayer.setDataSource(getContext(), getAudioFileUri());

                audioPlayer.prepare();

                Thread updateAudioProgressThread = new Thread()
                {
                    @Override
                    public void run() {
                        while(true)
                        {
                            Message updateAudioProgressMsg = new Message();
                            updateAudioProgressMsg.what = UPDATE_AUDIO_PROGRESS_BAR;

                            // Agrega soporte para la barra de progreso.
                            // Viendo el progreso, la mejor opción es mostrar cada segundo.
                            audioProgressUpdateHandler.sendMessage(updateAudioProgressMsg);

                            try {
                                Thread.sleep(1000);
                            }catch(InterruptedException ex)
                            {
                                ex.printStackTrace();
                            }
                        }
                    }
                };
                // Run above thread object.
                updateAudioProgressThread.start();
            }
        }catch(IOException ex)
        {
            ex.printStackTrace();
        }
    }

    // Destruye el reprodutor.
    public void destroyAudioPlayer()
    {
        if(audioPlayer!=null)
        {
            if(audioPlayer.isPlaying())
            {
                audioPlayer.stop();
            }
            audioPlayer.release();
            audioPlayer = null;
        }
    }

    // Regresa el progreso.
    public int getCurrentAudioPosition()
    {
        int ret = 0;
        if(audioPlayer != null)
        {
            ret = audioPlayer.getCurrentPosition();
        }
        return ret;
    }

    // Duracion completa de la canción.
    public int getTotalAudioDuration()
    {
        int ret = 0;
        if(audioPlayer != null)
        {
            ret = audioPlayer.getDuration();
        }
        return ret;
    }

    // Progreso actual de la canción.
    public int getAudioProgress()
    {
        int ret = 0;
        int currAudioPosition = getCurrentAudioPosition();
        int totalAudioDuration = getTotalAudioDuration();
        if(totalAudioDuration > 0) {
            ret = (currAudioPosition * 100) / totalAudioDuration;
        }
        return ret;
    }
}