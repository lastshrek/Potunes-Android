package poche.fm.potunes.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.util.FileDownloadSerialQueue;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.request.GetRequest;
import com.lzy.okserver.download.DownloadInfo;
import com.lzy.okserver.download.DownloadManager;
import com.lzy.okserver.listener.DownloadListener;
import com.sdsmdg.tastytoast.TastyToast;
import com.zhuiji7.filedownloader.download.DownLoadListener;
import com.zhuiji7.filedownloader.download.DownLoadManager;
import com.zhuiji7.filedownloader.download.DownLoadService;
import com.zhuiji7.filedownloader.download.dbcontrol.bean.SQLDownLoadInfo;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.LitePal;
import org.litepal.crud.DataSupport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import poche.fm.potunes.Model.LocalTracksEvent;
import poche.fm.potunes.Model.MediaScanner;
import poche.fm.potunes.Model.Track;

public class DownloadService extends Service {
    
    private String TAG = "DownloadService";
    private MediaScanner mediaScanner;
    private Context mContext;
    private int msg;

    private DownLoadManager manager;
    private DownLoadListener downloadListener;
    private String targetFolder = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Potunes/Music/";

    public DownloadService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        mContext = getBaseContext();
        mediaScanner = new MediaScanner(mContext);
        LitePal.initialize(mContext);

        manager = DownLoadService.getDownLoadManager();

        downloadListener = new DownLoadListener() {
            @Override
            public void onStart(SQLDownLoadInfo sqlDownLoadInfo) {

            }

            @Override
            public void onProgress(SQLDownLoadInfo sqlDownLoadInfo, boolean isSupportBreakpoint) {

            }

            @Override
            public void onStop(SQLDownLoadInfo sqlDownLoadInfo, boolean isSupportBreakpoint) {

            }

            @Override
            public void onError(SQLDownLoadInfo sqlDownLoadInfo) {

            }

            @Override
            public void onSuccess(SQLDownLoadInfo sqlDownLoadInfo) {

            }
        };




//                Track track = (Track) task.getTag();
//                track.setIsDownloaded(1);
//                track.setUrl(task.getTargetFilePath());
//                track.save();
//
//
//
//                mediaScanner.scanFile(task.getTargetFilePath(), null);





    }

    // DownloadManagerListener
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        msg = intent.getIntExtra("MSG", 0);

        SharedPreferences preferences = getSharedPreferences("user", Context.MODE_PRIVATE);
        switch (msg) {
            case 1:
                Track track = (Track) intent.getSerializableExtra("track");
//                checkFiles(track);
//                downloadManager.stopAllTask();
                manager.addTask(track.getUrl(), track.getUrl(), ddd);

                enqueueTrack(track);

                break;
            case 2:
                //获取本地Tracks数据
                String json = preferences.getString("Tracks", "Tracks");
                String album = preferences.getString("album", "album");
                Gson gson = new Gson();
                //获取专辑名称
                ArrayList<Track> datas = gson.fromJson(json, new TypeToken<List<Track>>(){}.getType());
                for (Track mTrack : datas) {
                    mTrack.setAlbum(album);
                    enqueueTrack(mTrack);
                }
                break;
            case 3:
                manager.stopAllTask();
                break;
            case 4:
                manager.startAllTask();
                break;
            case 5:
                manager.stopAllTask();
                manager.c.clearAllTaskData();
                break;
            case 6:
                String tag = intent.getStringExtra("URL");
                //restart download
//                downloadManager.restartTask(tag);
                break;
            case 7:
                String fileName = intent.getStringExtra("SCAN");
                mediaScanner.scanFile(fileName, null);
        }

        return super.onStartCommand(intent,flags, startId);
    }
    private void enqueueTrack(Track track) {
        String downloadTitle = track.getArtist() + " - " + track.getTitle() + ".mp3";
        downloadTitle = downloadTitle.replace("/", " ");

        List<Track> results = DataSupport.where("url = ?", targetFolder + downloadTitle).find(Track.class);
        if (!results.isEmpty()) {
            TastyToast.makeText(mContext, "该文件已经下载过啦", TastyToast.LENGTH_SHORT, TastyToast.WARNING);
            return;
        }

        BaseDownloadTask.InQueueTask baseDownloadTask = fileDownloader.create(track.getUrl())
                .setTag(track)
                .setListener(targetListener)
                .setPath(targetFolder + downloadTitle, false)
                .setForceReDownload(true)
                .asInQueueTask();



        queue.enqueue(baseDownloadTask);
    }





    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(LocalTracksEvent event) {
        Log.d(TAG, "onMessageEvent: ==========");
        if (event.local.equals("resume")) {

        }
    }
    private void checkFiles(Track track) {
//        if (downloadManager.getDownloadInfo(track.getUrl()) != null) {
//            Toast.makeText(mContext, track.getTitle() + " downloaded", Toast.LENGTH_SHORT).show();
//        } else {
//            GetRequest request = OkGo.get(track.getUrl());
//            downloadManager.addTask(track.getUrl(), track, request, downloadListener);
//        }
    }
    private boolean queryFromDB(int trackID) {
        List<Track> results = DataSupport.where("track_id = ?" , "" + trackID).find(Track.class);
        if(results.size() > 0) {
            Track result = results.get(0);
            return result.getIsDownloaded() > 0;
        }
        return false;
    }



    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onDestroy() {
       super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

}
