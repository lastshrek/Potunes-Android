package poche.fm.potunes;

import android.app.Application;
import android.content.Intent;


import com.lzy.okgo.OkGo;
import com.umeng.analytics.MobclickAgent;
import com.zhuiji7.filedownloader.download.DownLoadService;



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
        this.startService(new Intent(this, DownLoadService.class));
    }
}
