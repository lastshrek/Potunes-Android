package poche.fm.potunes.adapter;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.CoordinatorLayout;
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
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.cocosw.bottomsheet.BottomSheet;
import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder;
import com.github.rubensousa.bottomsheetbuilder.adapter.BottomSheetItemClickListener;

import org.litepal.crud.DataSupport;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import poche.fm.potunes.MainActivity;
import poche.fm.potunes.Model.MediaScanner;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.R;
import poche.fm.potunes.service.PlayerService;

/**
 * Created by purchas on 2017/2/5.
 */

public class DownloadedTrackAdapter extends RecyclerView.Adapter<DownloadedTrackAdapter.ViewHolder> {
    private ArrayList<Track> mTrackList;
    private Context mContext;
    private String TAG = "TrackItem";
    private PlayerService mPlayerService;
    private CoordinatorLayout coordinatorLayout;

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
                MainActivity main = (MainActivity) mContext;
                mPlayerService = main.getPlayerService();
                if (mPlayerService != null && mTrackList != null) {
                    mPlayerService.tracks = mTrackList;
                    mPlayerService.play(holder.getAdapterPosition(), mContext);
                }
            }
        });

        holder.menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int position = holder.getAdapterPosition();
                final Track track = mTrackList.get(position);
                new BottomSheet.Builder(mContext, R.style.BottomSheet_Dialog).title("选择您想进行的操作").sheet(R.menu.local_track_share_menu)
                        .listener(new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case R.id.set_ringtone:
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            if (!Settings.System.canWrite(mContext)) {
                                                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                                intent.setData(Uri.parse("package:" + mContext.getPackageName()));
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                mContext.startActivity(intent);
                                                return;
                                            }
                                        }
                                        setRingtone(track);
                                        break;
                                    case R.id.delete:
                                        File file = new File(track.getUrl());
                                        if (file.exists()) {
                                            MediaScanner scan = new MediaScanner(mContext);
                                            scan.scanFile(track.getUrl(), null);
                                            mTrackList.remove(position);
                                            notifyDataSetChanged();
                                            file.delete();
                                        }
                                        List<Track> tracks = DataSupport.where("url = ?", track.getUrl()).find(Track.class);
                                        if (tracks.size() > 0) {
                                            for (Track result: tracks) {
                                                result.delete();
                                            }
                                        }
                                        break;
                                }
                            }
                        }).show();
            }
        });
        return holder;
    }

    private void setRingtone(Track track) {
        String ringtoneuri = Environment.getExternalStorageDirectory().getAbsolutePath() + "/media/ringtone";
        File file1 = new File(ringtoneuri);
        file1.mkdirs();
        File newSoundFile = new File(ringtoneuri, track.getTitle() + ".AMR");
        Uri mUri = Uri.parse("file://" + track.getUrl());

        ContentResolver mCr = mContext.getContentResolver();
        AssetFileDescriptor soundFile;
        try {
            soundFile = mCr.openAssetFileDescriptor(mUri, "r");
        } catch (FileNotFoundException e) {
            soundFile = null;
        }

        try {
            byte[] readData = new byte[1024];
            FileInputStream fis = soundFile.createInputStream();
            FileOutputStream fos = new FileOutputStream(newSoundFile);
            int i = fis.read(readData);

            while (i != -1) {
                fos.write(readData, 0, i);
                i = fis.read(readData);
            }

            fos.close();
        } catch (IOException io) {
            io.printStackTrace();
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, newSoundFile.getAbsolutePath());
        values.put(MediaStore.MediaColumns.TITLE, track.getTitle());
        values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3");
        values.put(MediaStore.MediaColumns.SIZE, newSoundFile.length());
        values.put(MediaStore.Audio.Media.ARTIST, R.string.app_name);
        values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
        values.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
        values.put(MediaStore.Audio.Media.IS_ALARM, false);
        values.put(MediaStore.Audio.Media.IS_MUSIC, false);

        Uri uri = MediaStore.Audio.Media.getContentUriForPath(newSoundFile.getAbsolutePath());
        Uri newUri = mCr.insert(uri, values);
        try {
            Uri rUri = RingtoneManager.getValidRingtoneUri(mContext);
            if (rUri != null) {
                Log.d(TAG, "setRingtone: " + rUri);
            }
            RingtoneManager.setActualDefaultRingtoneUri(mContext.getApplicationContext(), RingtoneManager.TYPE_RINGTONE, newUri);
            Toast.makeText(mContext, R.string.set_ringtone_success, Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            t.printStackTrace();
        }
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
