package poche.fm.potunes.Model;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import poche.fm.potunes.PlayerActivity;
import poche.fm.potunes.R;
import poche.fm.potunes.domain.AppConstant;
import poche.fm.potunes.fragment.QuciControlsFragment;

/**
 * Created by purchas on 2017/1/8.
 */

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.ViewHolder> {

    private ArrayList<Track> mTrackList;
    private Context mContext;


    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView artist;
        TextView name;

        public ViewHolder(View view) {
            super(view);
            cover = (ImageView) view.findViewById(R.id.track_cover);
            artist = (TextView) view.findViewById(R.id.track_artist);
            name = (TextView) view.findViewById(R.id.track_title);
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
        final ViewHolder holder = new ViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int position = holder.getAdapterPosition();

                Track track = mTrackList.get(position);

                Intent playerIntent = new Intent(mContext, PlayerActivity.class);

                playerIntent.putExtra(PlayerActivity.TRACKLIST, mTrackList);

                playerIntent.putExtra(PlayerActivity.TRACKID, position);

                mContext.startActivity(playerIntent);

                Intent intent = new Intent();

                intent.putExtra("url", track.getUrl());

                intent.putExtra("MSG", AppConstant.PlayerMsg.PLAY_MSG);

                intent.putExtra("TRACKS", mTrackList);

                intent.putExtra("position", position);

                intent.setClass(mContext, PlayerService.class);

                Activity activity = (Activity) mContext;

                activity.startService(intent);



//

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
