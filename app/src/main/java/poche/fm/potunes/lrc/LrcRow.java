package poche.fm.potunes.lrc;

/**
 * Created by purchas on 2017/2/3.
 */

public class LrcRow implements Comparable<LrcRow> {

    //开始时间 00：00：00
    private String timeStr;
    // 开始时间 毫米数 为10000
    private int time;
    // 歌词内容
    private String content;
    //该行歌词显示的总时间
    private int totalTime;

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(int totalTime) {
        this.totalTime = totalTime;
    }

    public String getTimeStr() {
        return timeStr;
    }

    public void setTimeStr(String timeStr) {
        this.timeStr = timeStr;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LrcRow() {
        super();
    }

    public LrcRow(String timeStr, int time, String content) {
        super();
        this.timeStr = timeStr;
        this.time = time;
        this.content = content;
    }

    /****
     * 把歌词时间转换为毫秒值  如 将00:10.00  转为10000
     *
     */
    private static int formatTime(String timeStr) {
        timeStr = timeStr.replace('.', ':');
        String[] times = timeStr.split(":");

        return Integer.parseInt(times[0]) * 60 * 1000
                + Integer.parseInt(times[1]) * 1000
                + Integer.parseInt(times[2]);
    }

    @Override
    public int compareTo(LrcRow anotherLrcRow) {
        return (int) (this.time - anotherLrcRow.time);
    }

    @Override
    public String toString() {
        return "LrcRow [timeStr=" + timeStr + ", time=" + time + ", content="
                + content + "]";
    }
}
