package poche.fm.potunes.fragment;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import poche.fm.potunes.MainActivity;
import poche.fm.potunes.Model.MediaUtil;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.R;
import poche.fm.potunes.service.PlayerService;

/**
 * Created by purchas on 2017/2/13.
 */

public class LocalTrackAdatper extends RecyclerView.Adapter<LocalTrackAdatper.ViewHolder>{
    private ArrayList<Track> mTrackList = new ArrayList<>();
    private Context mContext;
    private FragmentManager mFragmentManager;
    private String TAG = "TrackItem";
    private PlayerService mPlayerService;



    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView artist;
        TextView name;
        ImageView menu;


        public ViewHolder(View view) {
            super(view);
            cover = (ImageView) view.findViewById(R.id.track_cover);
            artist = (TextView) view.findViewById(R.id.track_artist);
            name = (TextView) view.findViewById(R.id.track_title);
            menu = (ImageView) view.findViewById(R.id.track_item_menu);

        }
    }

    public LocalTrackAdatper(List<Track> trackList, FragmentManager fragmentManager) {
        for (Track track: trackList) {
            mTrackList.add(track);
        }
        mFragmentManager = fragmentManager;
    }

    @Override
    public LocalTrackAdatper.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mContext == null) {
            mContext = parent.getContext();
        }
        View view = LayoutInflater.from(mContext).inflate(R.layout.track_item, parent, false);
        TypedValue typedValue = new TypedValue();
        mContext.getTheme().resolveAttribute(R.attr.selectableItemBackground, typedValue, true);
        view.setBackgroundResource(typedValue.resourceId);
        final LocalTrackAdatper.ViewHolder holder = new LocalTrackAdatper.ViewHolder(view);



        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MainActivity main = (MainActivity) mContext;
                mPlayerService = main.getPlayerService();

                if (mPlayerService != null && mTrackList != null) {
                    mPlayerService.tracks = mTrackList;
                    mPlayerService.play(holder.getAdapterPosition());
                }
            }
        });

        holder.menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getAdapterPosition();
                Bitmap image = ((BitmapDrawable)holder.cover.getDrawable()).getBitmap();
                byte[] byteArray = bmpToByteArray(image, false);
                MoreFragment morefragment = MoreFragment.newInstance(mTrackList.get(position), 0, byteArray);
                morefragment.show(((AppCompatActivity) mContext).getSupportFragmentManager(), "music");

            }
        });
        return holder;
    }

    private static byte[] bmpToByteArray(final Bitmap bmp, final boolean needRecycle) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, output);
        if (needRecycle) {
            bmp.recycle();
        }

        byte[] result = output.toByteArray();
        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public void onBindViewHolder(final LocalTrackAdatper.ViewHolder holder, int position) {
        Track track = mTrackList.get(position);
        holder.artist.setText(track.getArtist());
        holder.name.setText(track.getTitle());
        holder.itemView.setClickable(true);

        long songid = track.getID();
        long albumid = track.getAlbumid();
        holder.cover.setImageBitmap(MediaUtil.getArtwork(mContext, songid, albumid, true, true));
    }



    @Override
    public int getItemCount() {
        return mTrackList.size();
    }
}