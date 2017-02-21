package poche.fm.potunes.Model;

import java.util.List;

/**
 * Created by purchas on 2017/2/21.
 */


// 发送下载整张专辑的通知
public class DownloadAlbumMessage {
    public final List<Track> tracks;

    public DownloadAlbumMessage(List<Track> tracks) {
        this.tracks = tracks;
    }
}
