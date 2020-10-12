package uv.mp3;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.LinkedList;
import java.util.List;

class AudioModel {
    long id;
    String songTitle;
    String songPath;
    String songArtist;
}

public class MainActivity extends Activity {
    public static final int REQUEST_CODE = 1001;
    public static final int REQUEST_CODE_EXTERNAL_STORAGE = 1002;

    // Hay que iniciar el RecyclerView para mostrar los elementos.
    RecyclerView lv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lv = findViewById (R.id.list1);
        lv.setLayoutManager (new LinearLayoutManager (getBaseContext(), RecyclerView.VERTICAL, false));
        lv.addItemDecoration (new DividerItemDecoration (getBaseContext (), DividerItemDecoration.VERTICAL));

        // Hay que pedir el elemento para cargar los audios.
        // Si no, entonces tendremos un error/choque debido a la estancia de acceso ilegal de archivos.
        int perm = getBaseContext ().checkSelfPermission (Manifest.permission.READ_EXTERNAL_STORAGE);
        if (perm != PackageManager.PERMISSION_GRANTED) {
            requestPermissions (
                    new String [] { Manifest.permission.READ_EXTERNAL_STORAGE },
                    REQUEST_CODE_EXTERNAL_STORAGE
            );
        } else {
            loadAudios ();
        }

    }

    void loadAudios () {
        String [] columns = {
                MediaStore.Audio.Artists._ID,
                MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DISPLAY_NAME,
        };

        String order = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;

        Cursor cursor =  getBaseContext().getContentResolver().query (MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, columns, null, null, order);
        if (cursor == null) return;

        LinkedList<AudioModel> artists = new LinkedList<> ();

        for (int i = 0; i < cursor.getCount (); i++) {
            cursor.moveToPosition (i);
            AudioModel audioModel = new AudioModel ();

            int index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            long id = cursor.getLong(index);
            audioModel.id = id;

            index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            audioModel.songArtist = cursor.getString(index);

            index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            audioModel.songTitle = cursor.getString(index);

            index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            audioModel.songPath = cursor.getString(index);

            artists.add (audioModel);
        }

        cursor.close ();

        ListadoMusica adapter = new ListadoMusica (getApplicationContext(), artists);
        lv.setAdapter (adapter);
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult (requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_CODE:
                if (grantResults.length > 0 && grantResults [0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText (getBaseContext(),"¡Permiso concedido!", Toast.LENGTH_LONG).show ();
                }
                break;
            case REQUEST_CODE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults [0] == PackageManager.PERMISSION_GRANTED) {
                    loadAudios ();
                }
        }
    }

    /**
     * Callback invocado después de llamar a startActivityForResult
     *
     * @param requestCode código de verificación de la llamadas al método
     * @param resultCode resultado: OK, CANCEL, etc.
     * @param data información resultante, si existe
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}

// Clase personalizada que se utilizara en Recycler view.
class ListadoMusica extends RecyclerView.Adapter<ListadoMusica.MyViewHolder> {
    private Context context;
    private List<AudioModel> data;

    public ListadoMusica (Context context, List<AudioModel> data) {
        this.data = data;
        this.context = context;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from (context).inflate (R.layout.list_item, parent, false);
        return new MyViewHolder (view);
    }

    @Override
    public void onBindViewHolder (@NonNull MyViewHolder holder, int position) {
        holder.text1.setText(data.get(position).songTitle);
        holder.text2.setText(data.get(position).songArtist);
        holder.tvUbicacion.setText("null");
        holder.tvUbicacion.setText(data.get(position).songPath);

        holder.itemView.setOnClickListener (v -> {
            Uri contentUri = ContentUris.withAppendedId (
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    data.get (position).id
            );

            Intent intent = new Intent( context, DetailsActivity.class );
            intent.putExtra( "audioURL", contentUri.toString() );
            intent.putExtra( "nombreCancion", data.get (position).songTitle );
            intent.putExtra( "nombreArtista", data.get (position).songArtist );
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity( intent );
        });
    }

    @Override
    public int getItemCount () {
        return data.size ();
    }


    /**
     * Mantiene referencia al componente que interesa reutilizar en la vista
     */
    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView text1;
        TextView text2;
        TextView tvUbicacion;

        public MyViewHolder (@NonNull View itemView) {
            super(itemView);
            text1 = itemView.findViewById (R.id.tvItem);
            text2 = itemView.findViewById (R.id.tvItem2);
            tvUbicacion = itemView.findViewById (R.id.tvUbicacion);
        }
    }

}