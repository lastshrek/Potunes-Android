package poche.fm.potunes.Model;

/**
 * Created by purchas on 2017/1/8.
 */

public class Track {

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

}
