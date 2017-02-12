package poche.fm.potunes;

import android.app.Application;

import com.lzy.okgo.OkGo;
import com.lzy.okserver.download.DownloadManager;

import javax.net.ssl.HostnameVerifier;

/**
 * Created by purchas on 2017/1/22.
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {

        super.onCreate();

        OkGo.init(this);
        try {
            OkGo.getInstance()
                    .setCertificates();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
