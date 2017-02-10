package poche.fm.potunes.Model;

import java.io.Serializable;
import java.util.ArrayList;

import poche.fm.potunes.service.PlayerService;

public class PlayState implements Serializable {


    public static final String TAG= "PlayState";

    private int currentPosition = 0;
    private boolean isPlaying = false;
    private long progress = 0;
    private int mode = PlayerService.MODE_ORDER;
    private long duration = 0;


    //在集合中拿到当前的音乐
    public static Track getCurrentMusic(ArrayList<Track> arrayList , int postition){
        if(arrayList!=null && arrayList.size()>postition){
            return arrayList.get(postition);
        } else {
            return null;
        }
    }


    public PlayState copy(){

        PlayState ps = new PlayState();
        ps.setCurrentPosition(currentPosition);
        ps.setProgress(progress);
        ps.setPlaying(isPlaying());
        ps.setDuration(duration);
        ps.setMode(mode);

        return ps;


    }


    public PlayState() {
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(int currentPosition) {
        this.currentPosition = currentPosition;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public int getMode() {

        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public long getProgress() {
        return progress;
    }

    public void setProgress(long progress) {
        this.progress = progress;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }


    @Override
    public String toString() {
        return "PlayState{" +
                "currentPosition=" + currentPosition +
                ", isPlaying=" + isPlaying +
                ", progress=" + progress +
                ", mode=" + mode +
                ", duration=" + duration +
                '}';
    }
}
