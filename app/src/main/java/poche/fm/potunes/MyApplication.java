package poche.fm.potunes;

import android.app.Application;

import com.lzy.okgo.OkGo;
import com.lzy.okserver.download.DownloadManager;

import javax.net.ssl.HostnameVerifier;

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
    }


}
