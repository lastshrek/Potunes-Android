package poche.fm.potunes.Model;

import com.google.gson.annotations.SerializedName;

import org.litepal.crud.DataSupport;

/**
 * Created by purchas on 2017/1/7.
 */

public class Playlist extends DataSupport{
    private String title;
    @SerializedName("id")
    private int playlist_id;
    private String cover;

    public Playlist(String title, int playlist_id, String cover) {
        this.title = title;
        this.playlist_id = playlist_id;
        this.cover = cover;
    }

    public String getTitle() {
        return title;
    }

    public int getPlaylist_id() {
        return playlist_id;
    }

    public String getCover() {
        return cover;
    }

    public void setPlaylist_id(int playlist_id) {
        this.playlist_id = playlist_id;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public void setTitle(String title) {
        this.title = title;
    }


}
