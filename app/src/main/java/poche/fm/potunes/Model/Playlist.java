package poche.fm.potunes.Model;

/**
 * Created by purchas on 2017/1/7.
 */

public class Playlist {
    private String title;
    private int id;
    private String cover;

    public Playlist(String title, int id, String cover) {
        this.title = title;
        this.id = id;
        this.cover = cover;
    }

    public String getTitle() {
        return title;
    }

    public int getID() {
        return id;
    }

    public String getCover() {
        return cover;
    }
}
