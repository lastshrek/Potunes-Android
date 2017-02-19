package poche.fm.potunes.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder;
import com.github.rubensousa.bottomsheetbuilder.adapter.BottomSheetItemClickListener;

import org.greenrobot.eventbus.EventBus;
import org.litepal.crud.DataSupport;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import poche.fm.potunes.Model.LocalAlbumMessageEvent;
import poche.fm.potunes.Model.MediaScanner;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.R;

/**
 * Created by purchas on 2017/2/5.
 */

public class DownloadAlbumAdapter extends RecyclerView.Adapter<DownloadAlbumAdapter.ViewHolder> {
    private ArrayList<Track> mAlbumList;
    private Context mContext;
    private String TAG = "DownloadAlbumItem";
    private MediaScanner scanner;
    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView cover;
        TextView name;
        ImageView menu;

        public ViewHolder(View view) {
            super(view);
            cover = (ImageView) view.findViewById(R.id.download_album_cover);
            name = (TextView) view.findViewById(R.id.download_album_title);
            menu = (ImageView) view.findViewById(R.id.download_album_menu);
        }
    }

    public DownloadAlbumAdapter(ArrayList<Track> trackList) {
        mAlbumList = trackList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mContext == null) {
            mContext = parent.getContext();
        }
        scanner = new MediaScanner(mContext);
        View view = LayoutInflater.from(mContext).inflate(R.layout.download_album_item, parent, false);
        TypedValue typedValue = new TypedValue();
        mContext.getTheme().resolveAttribute(R.attr.selectableItemBackground, typedValue, true);
        view.setBackgroundResource(typedValue.resourceId);
        final ViewHolder holder = new ViewHolder(view);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getAdapterPosition();
                Track track = mAlbumList.get(position);
                EventBus.getDefault().post(new LocalAlbumMessageEvent(track.getAlbum()));
            }
        });

        holder.menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int position = holder.getAdapterPosition();
                final BottomSheetDialog dialog = new BottomSheetBuilder(mContext, R.style.AppTheme_BottomSheetDialog)
                        .setMode(BottomSheetBuilder.MODE_LIST)
                        .setMenu(R.menu.playlist_menu)
                        .setItemClickListener(new BottomSheetItemClickListener() {
                            @Override
                            public void onBottomSheetItemClick(MenuItem item) {

                                Track track = mAlbumList.get(position);
                                List<Track> list = DataSupport.where("album = ?", track.getAlbum()).find(Track.class);
                                for (Track result: list) {
                                    File file = new File(result.getUrl());
                                    if (file.exists()) {
                                        scanner = new MediaScanner(mContext);
                                        scanner.scanFile(result.getUrl(), null);
                                        file.delete();
                                        result.delete();
                                    }
                                }
                                mAlbumList.remove(position);
                                notifyDataSetChanged();
                                Toast.makeText(mContext, "删除专辑成功", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .createDialog();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();



            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(final DownloadAlbumAdapter.ViewHolder holder, int position) {
        Track track = mAlbumList.get(position);
        holder.name.setText(track.getAlbum());
        Log.d(TAG, "onBindViewHolder: " + track.getAlbum());
        String thumb = track.getCover() + "!/fw/150";
        Glide.with(mContext).load(thumb).into(holder.cover);
        holder.itemView.setClickable(true);
    }

    @Override
    public int getItemCount() {
        return mAlbumList.size();
    }
}
