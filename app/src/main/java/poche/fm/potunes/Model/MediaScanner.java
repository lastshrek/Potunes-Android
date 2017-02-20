package poche.fm.potunes.Model;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.util.Log;

/**
 * Created by purchas on 2017/2/13.
 */

public class MediaScanner {
    private MediaScannerConnection mediaScanConn = null;
    private UploadFileSannerClient client = null;
    private String filePath = null;
    private String fileType = null;
    private String[] filePaths = null;

    public MediaScanner(Context context) {
        if (this.client == null) {
            this.client = new UploadFileSannerClient();
        }

        if (this.mediaScanConn == null)
            this.mediaScanConn = new MediaScannerConnection(context, this.client);
    }

    public void scanFile(String filepath, String fileType) {
        Log.i("MediaScanner", "scanFile(" + filepath + ", " + fileType + ")");

        this.filePath = filepath;
        this.fileType = fileType;

        this.mediaScanConn.connect(); // MediaScannerConnection.onMediaScannerConnected()함수 호출
    }

    class UploadFileSannerClient
            implements MediaScannerConnection.MediaScannerConnectionClient {
        UploadFileSannerClient()
        {

        }

        public void onMediaScannerConnected() {
            Log.i("MediaScanner", "onMediaScannerConnected(" + MediaScanner.this.filePath + ", " + MediaScanner.this.fileType + ")");

            if (MediaScanner.this.filePath != null) {
                MediaScanner.this.mediaScanConn.scanFile(MediaScanner.this.filePath, MediaScanner.this.fileType);
            }

            if (MediaScanner.this.filePaths != null) {
                for (String file : MediaScanner.this.filePaths) {
                    MediaScanner.this.mediaScanConn.scanFile(file, MediaScanner.this.fileType);
                }
            }
        }

        public void onScanCompleted(String path, Uri uri) {
            Log.i("MediaScanner", "扫描成功(" + path + ", " + uri.toString() + ")");
            MediaScanner.this.mediaScanConn.disconnect();
        }
    }
}
