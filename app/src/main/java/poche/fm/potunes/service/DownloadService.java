package poche.fm.potunes.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.request.GetRequest;
import com.lzy.okserver.download.DownloadInfo;
import com.lzy.okserver.download.DownloadManager;
import com.lzy.okserver.listener.DownloadListener;
import com.lzy.okserver.task.ExecutorWithListener;

import org.litepal.LitePal;
import org.litepal.crud.DataSupport;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import poche.fm.potunes.Model.Track;

public class DownloadService extends Service implements ExecutorWithListener.OnAllTaskEndListener{
    
    private String TAG = "DownloadService";
    private DownloadManager downloadManager;
    private DownloadListener downloadListener;
    private int msg;
    private ArrayList<Track> tracks = new ArrayList<>();
    private List<DownloadInfo> allTask = new ArrayList<>();



    public DownloadService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        LitePal.initialize(getBaseContext());

        // 设置DownloadManager
        downloadManager = com.lzy.okserver.download.DownloadService.getDownloadManager();
        downloadManager.setTargetFolder(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Music/");
        downloadManager.getThreadPool().setCorePoolSize(1);

        downloadListener = new DownloadListener() {
            @Override
            public void onProgress(DownloadInfo downloadInfo) {
                Log.d(TAG, "onProgress: " + downloadInfo.getProgress());
            }

            @Override
            public void onFinish(DownloadInfo downloadInfo) {
                Track track = (Track) downloadInfo.getData();
                Toast.makeText(getBaseContext(), track.getTitle() +  "下载完成", Toast.LENGTH_SHORT).show();
                if (downloadManager.getDownloadInfo(track.getUrl()) != null) {
                    downloadManager.removeTask(track.getUrl(), false);
                }

                // 将数据库的已下载修改状态
                track.setIsDownloaded(1);
                track.save();

                // 重命名文件
                String downloadTitle = track.getArtist() + " - " + track.getTitle() + ".mp3";
                downloadTitle = downloadTitle.replace("/", " ");
                File old = new File(downloadManager.getTargetFolder(), downloadInfo.getFileName());
                File rename = new File(downloadManager.getTargetFolder(), downloadTitle);
                old.renameTo(rename);

            }

            @Override
            public void onError(DownloadInfo downloadInfo, String errorMsg, Exception e) {
                Log.d(TAG, "onError: ===============" + errorMsg);
            }
        };

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        msg = intent.getIntExtra("MSG", 0);
        SharedPreferences preferences = getSharedPreferences("user", Context.MODE_PRIVATE);
        switch (msg) {
            case 1:
                Track track = (Track) intent.getSerializableExtra("track");
                checkFiles(track);
                break;
            case 2:
                //获取本地Tracks数据
                String json = preferences.getString("Tracks", "Tracks");
                Gson gson = new Gson();
                tracks.clear();
                //获取专辑名称
                ArrayList<Track> datas = gson.fromJson(json, new TypeToken<List<Track>>(){}.getType());
                for (Track mTrack : datas) {
                    checkFiles(mTrack);
                }
        }

        return super.onStartCommand(intent,flags, startId);
    }

    private boolean queryFromDB(int trackID) {
        List<Track> results = DataSupport.where("track_id = ?" , "" + trackID).find(Track.class);
        if(results.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    private void checkFiles(Track track) {
        if (downloadManager.getDownloadInfo(track.getUrl()) != null || queryFromDB(track.getID())) {
//            Toast.makeText(getBaseContext(), "任务已经在下载列表中", Toast.LENGTH_SHORT).show();
        } else {
            track.save();
            GetRequest request = OkGo.get(track.getUrl());
            downloadManager.addTask(track.getUrl(), track, request, downloadListener);
        }
    }

    @Override
    public void onAllTaskEnd() {
        for (DownloadInfo downloadInfo : allTask) {
            if (downloadInfo.getState() != DownloadManager.FINISH) {
                Toast.makeText(getBaseContext(), "所有下载线程结束，部分下载未完成", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Toast.makeText(getBaseContext(), "所有下载任务完成", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
       super.onDestroy();
        //记得移除，否者会回调多次
        downloadManager.getThreadPool().getExecutor().removeOnAllTaskEndListener(this);
    }

    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            
        }
    }


}
