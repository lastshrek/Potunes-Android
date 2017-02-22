package poche.fm.potunes;

import android.app.Application;
import android.content.Context;
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
    public static Context mContext;

    @Override
    public void onCreate() {

        super.onCreate();
        mContext = this;
        MobclickAgent.startWithConfigure(new MobclickAgent.UMAnalyticsConfig(this, "58a412462ae85b1cf700184f", "poche.fm"));
        MobclickAgent.setDebugMode( true );
        OkGo.init(this);
        JPushInterface.setDebugMode(true);
        JPushInterface.init(this);
    }
}
