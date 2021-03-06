package poche.fm.potunes.Model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import org.litepal.annotation.Column;
import org.litepal.crud.DataSupport;

import java.io.Serializable;

/**
 * Created by purchas on 2017/1/8.
 */

public class Track extends DataSupport implements Serializable {
    @SerializedName("id")
    private int track_id;
    private String name;
    private String cover;
    private String artist;
    private String url;
    private String album;
    @Column(defaultValue = "0")
    private int isDownloaded;
    @Column(defaultValue = "0")
    private long albumid;

    public Track(String name, int track_id, String cover,String artist, String url, String album, int isDownloaded, long albumid) {
        this.name = name;
        this.track_id = track_id;
        this.cover = cover;
        this.artist = artist;
        this.url = url;
        this.album = album;
        this.isDownloaded = isDownloaded;
        this.albumid = albumid;
    }

    public Track() {
        super();
    }

    public String getTitle() {
        return name;
    }

    public int getID() {
        return track_id;
    }

    public String getCover() {
        return cover;
    }

    public String getArtist() {
        return artist;
    }

    public String getUrl() { return url; }

    public String getAlbum() {
        return album;
    }

    public int getIsDownloaded() {
        return isDownloaded;
    }

    public long getAlbumid() {
        return albumid;
    }


    public void setTrack_id(int track_id) {
        this.track_id = track_id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setAlbum(String album) {
        this.album = album;
    }


    public void setIsDownloaded(int isDownloaded) {
        this.isDownloaded = isDownloaded;
    }

    public void setAlbumid(long albumid) {
        this.albumid = albumid;
    }

//    @Override
//    public int describeContents() {
//        return 0;
//    }
//
//    @Override
//    public void writeToParcel(Parcel dest, int flags) {
//        dest.writeInt(track_id);
//        dest.writeString(name);
//        dest.writeString(cover);
//        dest.writeString(artist);
//        dest.writeString(url);
//        dest.writeString(album);
//        dest.writeInt(isDownloaded);
//    }

//    public static final Parcelable.Creator<Track> CREATOR = new Creator<Track>() {
//
//        /**
//         * 供外部类反序列化本类数组使用
//         */
//        @Override
//        public Track[] newArray(int size) {
//            return new Track[size];
//        }
//
//        /**
//         * 从Parcel中读取数据
//         */
//        @Override
//        public Track createFromParcel(Parcel source) {
//            return new Track(source);
//        }
//    };

}
