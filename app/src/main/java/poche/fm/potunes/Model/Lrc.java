package poche.fm.potunes.Model;

/**
 * Created by purchas on 2017/2/3.
 */

public class Lrc {
    private String lrc;
    private String lrc_cn;

    public String getLrc() {
        return lrc;
    }

    public String getLrc_cn() {
        return lrc_cn;
    }

    public Lrc(String lrc, String lrc_cn) {
        this.lrc = lrc;
        this.lrc_cn = lrc_cn;
    }

}
