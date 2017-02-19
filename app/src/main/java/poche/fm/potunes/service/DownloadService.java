package poche.fm.potunes.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.request.GetRequest;
import com.lzy.okserver.download.DownloadInfo;
import com.lzy.okserver.download.DownloadManager;
import com.lzy.okserver.listener.DownloadListener;
import com.lzy.okserver.task.ExecutorWithListener;

import org.greenrobot.eventbus.EventBus;
import org.litepal.LitePal;
import org.litepal.crud.DataSupport;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import poche.fm.potunes.MainActivity;
import poche.fm.potunes.Model.MediaScanner;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.SplashActivity;
import poche.fm.potunes.utils.NetworkHelper;
import poche.fm.potunes.utils.SharedPreferencesUtil;

public class DownloadService extends Service {
    
    private String TAG = "DownloadService";
    public DownloadManager downloadManager;
    private DownloadListener downloadListener;
    private MediaScanner mediaScanner;
    private Context mContext;
    private int msg;
    private ArrayList<Track> tracks = new ArrayList<>();

    public DownloadService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LitePal.initialize(getBaseContext());
        mContext = getBaseContext();
        mediaScanner = new MediaScanner(mContext);
        downloadListener = new DownloadListener() {
            @Override
            public void onProgress(DownloadInfo downloadInfo) {}

            @Override
            public void onFinish(DownloadInfo downloadInfo) {
                Track track = (Track) downloadInfo.getData();
                if (downloadManager.getDownloadInfo(track.getUrl()) != null) {
                    downloadManager.removeTask(track.getUrl(), false);
                }
                // 重命名文件
                String downloadTitle = track.getArtist() + " - " + track.getTitle() + ".mp3";
                downloadTitle = downloadTitle.replace("/", " ");
                // 将数据库的已下载修改状态
                track.setIsDownloaded(1);
                track.setUrl(downloadManager.getTargetFolder() + downloadTitle);
                track.save();


                // rename
                File old = new File(downloadManager.getTargetFolder(), downloadInfo.getFileName());
                File rename = new File(downloadManager.getTargetFolder(), downloadTitle);
                old.renameTo(rename);

                mediaScanner.scanFile(downloadManager.getTargetFolder() + downloadTitle, null);
                if (downloadManager.getAllTask().size() == 0) {
                    Toast.makeText(getBaseContext(), "全部歌曲下载完毕", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(DownloadInfo downloadInfo, String errorMsg, Exception e) {
                Track track = (Track) downloadInfo.getData();
                if (errorMsg != null) Toast.makeText(getBaseContext(), track.getTitle() + "下载失败，尝试重新下载", Toast.LENGTH_SHORT).show();
            }
        };

        // 设置DownloadManager
        downloadManager = com.lzy.okserver.download.DownloadService.getDownloadManager();
        downloadManager.setTargetFolder(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Potunes/Music/");
        downloadManager.getThreadPool().setCorePoolSize(1);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        msg = intent.getIntExtra("MSG", 0);

        SharedPreferences preferences = getSharedPreferences("user", Context.MODE_PRIVATE);
        switch (msg) {
            case 1:
                Log.d(TAG, "onStartCommand: 单曲下载");
                Track track = (Track) intent.getSerializableExtra("track");
                checkFiles(track);
                downloadManager.stopAllTask();
                break;
            case 2:
                Log.d(TAG, "onStartCommand: 专辑下载");
                //获取本地Tracks数据
                String json = preferences.getString("Tracks", "Tracks");
                String album = preferences.getString("album", "album");
                Gson gson = new Gson();
                tracks.clear();
                //获取专辑名称
                ArrayList<Track> datas = gson.fromJson(json, new TypeToken<List<Track>>(){}.getType());
                for (Track mTrack : datas) {
                    mTrack.setAlbum(album);
                    checkFiles(mTrack);
                }
                downloadManager.stopAllTask();
                break;
            case 3:
                downloadManager.pauseAllTask();
                break;
            case 4:
                Log.d(TAG, "onStartCommand: 继续下载" + downloadManager.getAllTask().size());
                downloadManager.startAllTask();
                break;
            case 5:
                downloadManager.pauseAllTask();
                downloadManager.removeAllTask();
                break;
            case 6:
                String tag = intent.getStringExtra("URL");
                downloadManager.restartTask(tag);
                break;
            case 7:
                String fileName = intent.getStringExtra("SCAN");
                mediaScanner.scanFile(fileName, null);
        }

        return super.onStartCommand(intent,flags, startId);
    }





    private boolean queryFromDB(int trackID) {
        List<Track> results = DataSupport.where("track_id = ?" , "" + trackID).find(Track.class);
        if(results.size() > 0) {
            Track result = results.get(0);
            return result.getIsDownloaded() > 0;
        }
        return false;
    }
    private void checkFiles(Track track) {
        if (downloadManager.getDownloadInfo(track.getUrl()) != null || queryFromDB(track.getID())) {
            Toast.makeText(mContext, track.getTitle() + "downloaded", Toast.LENGTH_SHORT).show();
        } else {
            GetRequest request = OkGo.get(track.getUrl());
            downloadManager.addTask(track.getUrl(), track, request, downloadListener);
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onDestroy() {
       super.onDestroy();
    }

}
