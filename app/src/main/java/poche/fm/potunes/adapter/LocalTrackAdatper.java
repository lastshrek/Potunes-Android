package poche.fm.potunes.adapter;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
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

import com.bumptech.glide.load.engine.Resource;
import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder;
import com.github.rubensousa.bottomsheetbuilder.adapter.BottomSheetItemClickListener;

import org.litepal.crud.DataSupport;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import poche.fm.potunes.MainActivity;
import poche.fm.potunes.Model.MediaScanner;
import poche.fm.potunes.Model.MediaUtil;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.R;
import poche.fm.potunes.fragment.MoreFragment;
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
                    mPlayerService.play(holder.getAdapterPosition(), mContext);
                }
            }
        });

        holder.menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int position = holder.getAdapterPosition();
                final BottomSheetDialog dialog = new BottomSheetBuilder(mContext, R.style.AppTheme_BottomSheetDialog)
                        .setMode(BottomSheetBuilder.MODE_LIST)
                        .setMenu(R.menu.local_share_menu)
                        .setItemClickListener(new BottomSheetItemClickListener() {
                            Track track = mTrackList.get(position);

                            @Override
                            public void onBottomSheetItemClick(MenuItem item) {
                                String title = item.getTitle().toString();
                                if (title.equals("设置为铃声")) {
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
                                    return;
                                }

                                if (title.equals("删除")) {
                                    Track track = mTrackList.get(position);
                                    File file = new File(track.getUrl());
                                    if (file.exists()) {
                                        MediaScanner scan = new MediaScanner(mContext);
                                        scan.scanFile(track.getUrl(), null);
                                        mTrackList.remove(position);
                                        notifyDataSetChanged();
                                        file.delete();
                                        List<Track> tracks = DataSupport.where("url = ?", track.getUrl()).find(Track.class);
                                        if (tracks.size() > 0) {
                                            for (Track result: tracks) {
                                                result.delete();
                                            }
                                        }
                                    }
                                    return;
                                }
                            }
                        })
                        .createDialog();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();

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
