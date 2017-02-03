package poche.fm.potunes.lrc;

import android.content.Context;

import java.util.List;

/**
 * Created by purchas on 2017/2/3.
 */

public interface PLrcView {


    void init(Context context);

    void setLrcRows(List<LrcRow> lrcRows);

    void seekTo(int progress, boolean fromSeekBar, boolean fromSeekBarByUser);

    void reset();
}
