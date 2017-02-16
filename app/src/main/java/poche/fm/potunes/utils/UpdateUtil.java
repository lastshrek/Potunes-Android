package poche.fm.potunes.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.FileCallback;
import com.lzy.okserver.download.DownloadManager;
import com.lzy.okserver.download.DownloadService;

import java.io.File;
import java.text.NumberFormat;
import java.util.List;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import poche.fm.potunes.Model.Version;
import poche.fm.potunes.R;

/**
 * Created by purchas on 2017/2/16.
 */

public class UpdateUtil {
    private String TAG = "UpdateUtil";
    protected static final int CHECKUPDATE = 1;

    /* 下载中 */
    private static final int DOWNLOAD = 1;
    /* 下载结束 */
    private static final int DOWNLOAD_FINISH = 2;
    /* 下载保存路径 */
    private String mSavePath;
    /* 记录进度条进度 */
    private int progress;
    /* 是否取消更新 */
    private boolean cancelUpdate = false;

    private Context mContext;
    /* 更新进度条 */
    private ProgressBar mProgress;
    private Dialog mDownloadDialog;
    private String nowVersionName;
    private Version version;
    private Thread checkThread;
    private boolean isSilenced = false;


    private Handler sHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CHECKUPDATE:
                    if (!version.getVersion_name().equals(nowVersionName)) {
                        showDialog();
                    } else {
                        if (!isSilenced) {
                            Toast.makeText(mContext, R.string.now_latest, Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
            }
        }
    };

    private Runnable check = new Runnable() {
        @Override
        public void run() {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url("https://poche.fm/api/app/android/version")
                        .build();
                Response response = client.newCall(request).execute();
                String responseData = response.body().string();
                Gson gson = new Gson();
                List<Version> versions = gson.fromJson(responseData, new TypeToken<List<Version>>(){}.getType());
                if (versions.isEmpty()) {
                    Toast.makeText(mContext, R.string.now_latest, Toast.LENGTH_SHORT).show();
                    return;
                }
                Message msg = Message.obtain();
                msg.what = CHECKUPDATE;
                version = versions.get(0);

                sHandler.sendEmptyMessage(CHECKUPDATE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public UpdateUtil(Context context) {
        this.mContext = context;
    }



    public void checkUpdate(boolean isSilenced) {
        this.isSilenced = isSilenced;
        getLatestVersion();
    }
     //检查软件是否有更新版本
    private void getLatestVersion() {
        // 获取当前软件版本
        nowVersionName = getVersionCode(mContext);
        checkThread = new Thread(check);
        checkThread.start();

    }
    // 获取当前版本号
    private String getVersionCode(Context context) {
        String versionName = "";
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            versionName = pi.versionName;
            if (versionName == null || versionName.length() <= 0) {
                return "";
            }
        } catch (Exception e) {
            Log.e("VersionInfo", "Exception", e);
        }
        return versionName;
    }
    // show dialog
    private void showDialog() {
        new MaterialDialog.Builder(mContext)
                .title("发现新版本v" + version.getVersion_name())
                .content(version.getDescription())
                .positiveText(R.string.update_now)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull final MaterialDialog dialog, @NonNull DialogAction which) {
                        //下载Apk
                        dialog.dismiss();
                        final MaterialDialog downloadDialog = new MaterialDialog.Builder(mContext)
                                .title(R.string.on_downloading)
                                .content(R.string.please_wait)
                                .progress(false, 100, true)
                                .show();
                        OkGo.get("http://s.poche.fm/android/Potunes.apk")
                                .tag(mContext)
                                .execute(new FileCallback() {
                                    @Override
                                    public void onSuccess(File file, Call call, Response response) {
                                        Toast.makeText(mContext, R.string.on_apk_download_complete, Toast.LENGTH_SHORT).show();
                                        downloadDialog.hide();
                                        downloadDialog.dismiss();
                                        installApk(file.getAbsolutePath());
                                    }
                                    @Override
                                    public void downloadProgress(long currentSize, long totalSize, float progress, long networkSpeed) {
                                        downloadDialog.setProgress((int)(progress * 100));
                                    }
                                    @Override
                                    public void onError(Call call, Response response, Exception e) {
                                        Log.d(TAG, "onError: " + e);
                                    }
                                });
                    }
                })
                .negativeText(R.string.update_later)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.hide();
                        dialog.dismiss();
                    }
                }).show();

    }

    /**
     * 安装APK文件
     */
    private void installApk(String path) {
        Uri uri = Uri.fromFile(new File(path));
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        mContext.startActivity(intent);
    }
}
