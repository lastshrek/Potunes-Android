package poche.fm.potunes.lrc;

import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by purchas on 2017/2/3.
 */
class LrcEntry implements Comparable<LrcEntry> {
    private long time;
    private String text;
    private StaticLayout staticLayout;
    private TextPaint paint;
    private String TAG = "LrcEntry ======";

    private LrcEntry(long time, String text) {
        this.time = time;
        this.text = text;
    }

    void init(TextPaint paint, int width) {
        this.paint = paint;
        staticLayout = new StaticLayout(text, paint, width, Layout.Alignment.ALIGN_CENTER, 1f, 0f, false);
    }

    long getTime() {
        return time;
    }

    StaticLayout getStaticLayout() {
        return staticLayout;
    }

    float getTextHeight() {
        if (paint == null || staticLayout == null) {
            return 0;
        }
        return staticLayout.getLineCount() * paint.getTextSize();
    }

    @Override
    public int compareTo(LrcEntry entry) {
        if (entry == null) {
            return -1;
        }
        return (int) (time - entry.getTime());
    }

    static List<LrcEntry> parseLrc(String lrcText, String chLrcText) {
        if (TextUtils.isEmpty(lrcText)) {
            return null;
        }

        List<LrcEntry> entryList = new ArrayList<>();
        String[] array = lrcText.split("\\[");

        for (String line : array) {
            List<LrcEntry> list = parseLine(line);
            if (list != null && !list.isEmpty()) {
                entryList.addAll(list);
            }
        }
        if (chLrcText != null) {
            String[] chArray = chLrcText.split("\\[");

            for (String line: chArray) {
                List<LrcEntry> list = parseLine(line);

                for (LrcEntry entry: entryList) {
                    if (list != null && !list.isEmpty()) {
                        for (LrcEntry chEntry: list) {
                            if (chEntry.getTime() == entry.getTime()) {
                                entry.text = entry.text + "\n" + chEntry.text;
                            }
                        }
                    }
                }
            }
        }

        Collections.sort(entryList);
        return entryList;
    }



    private static List<LrcEntry> parseLine(String line) {
        if (TextUtils.isEmpty(line)) {
            return null;
        }


        line = line.trim();
        //02:56.91]That's so us! That's so us!\n
        String[] array = line.split("\\]");
        String times = array[0];
        if (array.length == 1) {
            return null;
        }
        String text = array[1];
        text = text.replaceAll("\\\\n", "");

        List<LrcEntry> entryList = new ArrayList<>();

        Matcher timeMatcher = Pattern.compile("(\\d\\d):(\\d\\d)\\.(\\d\\d)").matcher(times);
        while (timeMatcher.find()) {
            long min = Long.parseLong(timeMatcher.group(1));
            long sec = Long.parseLong(timeMatcher.group(2));
            long mil = Long.parseLong(timeMatcher.group(3));
            long time = min * DateUtils.MINUTE_IN_MILLIS + sec * DateUtils.SECOND_IN_MILLIS + mil * 10;
            entryList.add(new LrcEntry(time, text));
        }
        return entryList;
    }
}
