package poche.fm.potunes.Model;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import poche.fm.potunes.R;
import poche.fm.potunes.TrackListActivity;

/**
 * Created by purchas on 2017/1/7.
 */

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {

    private Context mContext;
    private List<Playlist> mPlaylist;

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView cover;
        TextView title;

        public ViewHolder(View view) {
            super(view);
            cardView = (CardView) view;
            cover = (ImageView) view.findViewById(R.id.playlist_cover);
            title = (TextView) view.findViewById(R.id.playlist_title);
        }
    }

    public PlaylistAdapter(List<Playlist> playlist) {
        mPlaylist = playlist;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mContext == null) {
            mContext = parent.getContext();
        }
        View view = LayoutInflater.from(mContext).inflate(R.layout.playlist_item, parent, false);
        // 点击事件
        final ViewHolder holder = new ViewHolder(view);
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getAdapterPosition();

                Playlist playlist = mPlaylist.get(position);

                Intent intent = new Intent(mContext, TrackListActivity.class);

                intent.putExtra(TrackListActivity.PLAYLIST_ID, playlist.getID());

                intent.putExtra(TrackListActivity.TITLE, playlist.getTitle());

                mContext.startActivity(intent);
            }
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Playlist playlist = mPlaylist.get(position);
        holder.title.setText(playlist.getTitle());
        Glide.with(mContext).load(playlist.getCover()).into(holder.cover);
    }

    @Override
    public int getItemCount() {
        return mPlaylist.size();
    }

}
