package poche.fm.potunes;

import android.app.Application;
import android.os.Environment;


import com.lzy.okgo.OkGo;
import com.lzy.okserver.download.DownloadManager;
import com.lzy.okserver.download.DownloadService;
import com.umeng.analytics.MobclickAgent;
import cn.jpush.android.api.JPushInterface;

/**
 * Created by purchas on 2017/1/22.
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {

        super.onCreate();
        OkGo.init(this);
        JPushInterface.setDebugMode(true);
        JPushInterface.init(this);
        MobclickAgent.startWithConfigure(new MobclickAgent.UMAnalyticsConfig(this, "58a412462ae85b1cf700184f", "poche.fm"));
        MobclickAgent.setDebugMode( true );
        // 设置DownloadManager
        DownloadManager downloadManager = DownloadManager.getInstance();
        downloadManager.setTargetFolder(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Potunes/Music/");
        downloadManager.getThreadPool().setCorePoolSize(1);
    }
}
