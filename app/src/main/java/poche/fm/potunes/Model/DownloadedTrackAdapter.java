package poche.fm.potunes.Model;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
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
import java.util.ArrayList;

import poche.fm.potunes.DownloadedTracksActivity;
import poche.fm.potunes.R;
import poche.fm.potunes.TrackListActivity;
import poche.fm.potunes.domain.AppConstant;
import poche.fm.potunes.fragment.MoreFragment;
import poche.fm.potunes.service.PlayerService;

/**
 * Created by purchas on 2017/2/5.
 */

public class DownloadedTrackAdapter extends RecyclerView.Adapter<DownloadedTrackAdapter.ViewHolder> {
    private ArrayList<Track> mTrackList;
    private Context mContext;
    private String TAG = "TrackItem";

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

    public DownloadedTrackAdapter(ArrayList<Track> trackList) {
        mTrackList = trackList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mContext == null) {
            mContext = parent.getContext();
        }
        View view = LayoutInflater.from(mContext).inflate(R.layout.track_item, parent, false);
        TypedValue typedValue = new TypedValue();
        mContext.getTheme().resolveAttribute(R.attr.selectableItemBackground, typedValue, true);
        view.setBackgroundResource(typedValue.resourceId);
        final ViewHolder holder = new ViewHolder(view);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 存储当前歌曲播放位置
                int position = holder.getAdapterPosition();
                SharedPreferences preference = mContext.getSharedPreferences("user",Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preference.edit();
                editor.putInt("position", position);
                editor.apply();

                Track track = mTrackList.get(position);
                Intent intent = new Intent();
                intent.putExtra("url", track.getUrl());
                intent.putExtra("MSG", AppConstant.PlayerMsg.PLAY_MSG);
                intent.putExtra("TRACKS", mTrackList);
                intent.putExtra("position", position);
                intent.setClass(mContext, PlayerService.class);
                Activity activity = (DownloadedTracksActivity) mContext;
                activity.startService(intent);
            }
        });

        holder.menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getAdapterPosition();
                Bitmap image = ((BitmapDrawable)holder.cover.getDrawable()).getBitmap();
                byte[] byteArray = bmpToByteArray(image, false);
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
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Track track = mTrackList.get(position);
        holder.artist.setText(track.getArtist());
        holder.name.setText(track.getTitle());
        String thumb = track.getCover() + "!/fw/150";
        Glide
                .with(mContext)
                .load(thumb)
                .asBitmap()
                .placeholder(R.drawable.ic_launcher)
                .into(new SimpleTarget<Bitmap>(100, 100) {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                        holder.cover.setImageBitmap(resource);
                    }
                });
        holder.itemView.setClickable(true);
    }

    @Override
    public int getItemCount() {
        return mTrackList.size();
    }
}
