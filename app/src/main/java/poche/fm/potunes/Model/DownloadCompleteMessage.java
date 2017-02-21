package poche.fm.potunes.Model;

import com.lzy.okserver.download.DownloadInfo;

/**
 * Created by purchas on 2017/2/21.
 */

public class DownloadCompleteMessage {
    public DownloadInfo info;
    public DownloadCompleteMessage(DownloadInfo info) {
        this.info = info;
    }
}
