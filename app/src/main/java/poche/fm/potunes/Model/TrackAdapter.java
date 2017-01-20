package poche.fm.potunes.Model;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import poche.fm.potunes.R;
import poche.fm.potunes.TrackListActivity;
import poche.fm.potunes.domain.AppConstant;
import poche.fm.potunes.fragment.MoreFragment;

/**
 * Created by purchas on 2017/1/8.
 */

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.ViewHolder> {

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

    public TrackAdapter(ArrayList<Track> trackList) {
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

                int position = holder.getAdapterPosition();

                Track track = mTrackList.get(position);

                Intent intent = new Intent();

                intent.putExtra("url", track.getUrl());

                intent.putExtra("MSG", AppConstant.PlayerMsg.PLAY_MSG);

                intent.putExtra("TRACKS", mTrackList);

                intent.putExtra("position", position);

                intent.setClass(mContext, PlayerService.class);

                Activity activity = (TrackListActivity) mContext;

                activity.startService(intent);

                // 存储当前歌曲播放位置
                SharedPreferences preference = mContext.getSharedPreferences("user",Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preference.edit();
                editor.putInt("position", position);
                editor.commit();


            }
        });

        holder.menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getAdapterPosition();

                Log.d(TAG, "onClick: =================" + position);
                MoreFragment morefragment = MoreFragment.newInstance(mTrackList.get(position), 0);
                morefragment.show(((AppCompatActivity) mContext).getSupportFragmentManager(), "music");

            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Track track = mTrackList.get(position);
        holder.artist.setText(track.getArtist());
        holder.name.setText(track.getTitle());
        String thumb = track.getCover() + "!/fw/100";
        Glide.with(mContext).load(thumb).into(holder.cover);
        holder.itemView.setClickable(true);
    }

    @Override
    public int getItemCount() {
        return mTrackList.size();
    }

}
