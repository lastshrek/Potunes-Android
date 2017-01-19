package poche.fm.potunes.Model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by purchas on 2017/1/8.
 */

public class Track implements Parcelable {

    private int id;
    private String name;
    private String cover;
    private String artist;
    private String url;

    public Track(String name, int id, String cover,String artist, String url) {
        this.name = name;
        this.id = id;
        this.cover = cover;
        this.artist = artist;
        this.url = url;
    }

    public Track() {
        super();
    }

    public Track(Parcel source) {
        id = source.readInt();
        name = source.readString();
        cover = source.readString();
        artist = source.readString();
        url = source.readString();
    }

    public String getTitle() {
        return name;
    }

    public int getID() {
        return id;
    }

    public String getCover() {
        return cover;
    }

    public String getArtist() {
        return artist;
    }

    public String getUrl() { return url; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(cover);
        dest.writeString(artist);
        dest.writeString(url);
    }

    public static final Creator<Track> CREATOR = new Creator<Track>() {

        /**
         * 供外部类反序列化本类数组使用
         */
        @Override
        public Track[] newArray(int size) {
            return new Track[size];
        }

        /**
         * 从Parcel中读取数据
         */
        @Override
        public Track createFromParcel(Parcel source) {
            return new Track(source);
        }
    };

}
