package poche.fm.potunes.Model;

/**
 * Created by purchas on 2017/2/16.
 */

public class Version {
    private int id;
    private String version;
    private String description;

    public void setVersion_id(int version_id) {
        this.id = version_id;
    }

    public void setVersion_name(String name) {
        this.version = name;
    }

    public void setDescription(String des) {
        this.description = des;
    }

    public int getVersion_id() {
        return id;
    }

    public String getVersion_name() {
        return version;
    }

    public String getDescription() {
        return description;
    }

}
