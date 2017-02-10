package poche.fm.potunes.utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.KeyEvent;

import java.util.Vector;

import poche.fm.potunes.R;

/**
 * Created by purchas on 2017/2/10.
 */

public class TextUtil {
    private float mTextPosx = 0;// x坐标
    private float mTextPosy = 0;// y坐标
    private float mTextWidth = 0;// 绘制宽度
    private float mTextHeight = 0;// 绘制高度
    private int mFontHeight = 0;// 绘制字体高度
    private int mPageLineNum = 0;// 每一页显示的行数
    private int mCanvasBGColor = 0;// 背景颜色
    private int mFontColor = 0;// 字体颜色
    private int mAlpha = 0;// Alpha值
    private int mRealLine = 0;// 字符串真实的行数
    private int mCurrentLine = 0;// 当前行
    private int mTextSize = 0;// 字体大小
    private String mStrText = "";
    private Vector mString = null;
    private Paint mPaint = null;

    public TextUtil(String StrText, float x, float y, float w, float h, int bgcolor,
                    int textcolor, int alpha, int textsize) {
        mPaint = new Paint();
        mString = new Vector();
        this.mStrText = StrText;
        Log.d("", "TextUtil: " + y);
        this.mTextPosx = x;
        this.mTextPosy = y;
        this.mTextWidth = w;
        this.mTextHeight = h;
        this.mCanvasBGColor = bgcolor;
        this.mFontColor = textcolor;
        this.mAlpha = alpha;
        this.mTextSize = textsize;
    }

    public void InitText() {
        mString.clear();// 清空Vector
        // 对画笔属性的设置
//      mPaint.setARGB(this.mAlpha, Color.red(this.mFontColor), Color
//              .green(this.mFontColor), Color.blue(this.mFontColor));
        mPaint.setTextSize(this.mTextSize);
        mPaint.setAntiAlias(true);
        this.GetTextIfon();
    }

    /**
     * 得到字符串信息包括行数，页数等信息
     */
    public void GetTextIfon() {
        char ch;
        int w = 0;
        int istart = 0;
        Paint.FontMetrics fm = mPaint.getFontMetrics();// 得到系统默认字体属性
        mFontHeight = (int) (Math.ceil(fm.descent - fm.top) + 2);// 获得字体高度
        mPageLineNum = (int)(mTextHeight) / mFontHeight;// 获得行数
        int count = this.mStrText.length();
        for (int i = 0; i < count; i++) {
            ch = this.mStrText.charAt(i);
            float[] widths = new float[1];
            String str = String.valueOf(ch);
            mPaint.getTextWidths(str, widths);
            if (ch == '\n') {
                mRealLine++;// 真实的行数加一
                mString.addElement(this.mStrText.substring(istart, i));
                istart = i + 1;
                w = 0;
            } else {
                w += (int) Math.ceil(widths[0]);
                if (w > this.mTextWidth) {
                    mRealLine++;// 真实的行数加一
                    mString.addElement(this.mStrText.substring(istart, i));
                    istart = i;
                    i--;
                    w = 0;
                } else {
                    if (i == count - 1) {
                        mRealLine++;// 真实的行数加一
                        mString.addElement(this.mStrText.substring(istart,
                                count));
                    }
                }
            }
        }
    }

    /**
     * 绘制字符串
     *
     * @param canvas
     */
    public void DrawText(Canvas canvas) {
        for (int i = this.mCurrentLine, j = 0; i < this.mRealLine; i++, j++) {
            if (j > this.mPageLineNum) {
                break;
            }
            mPaint.setColor(Color.TRANSPARENT);
            Rect targetRect = new Rect((int)mTextPosx, (int)mTextPosy, (int)mTextWidth, (int)mTextHeight);
            Paint.FontMetricsInt fontMetrics = mPaint.getFontMetricsInt();
            canvas.drawRect(targetRect, mPaint);
            mPaint.setColor(mFontColor);
            mPaint.setTextAlign(Paint.Align.CENTER);

            int baseline = 0;
            if (j == 0) {
                baseline = (targetRect.bottom + targetRect.top) / 2 - (fontMetrics.bottom - fontMetrics.top) / 2;
            } else {
                baseline = (targetRect.bottom + targetRect.top) / 2 + (fontMetrics.bottom - fontMetrics.top) / 2;
            }

            canvas.drawText((String) (mString.elementAt(i)), targetRect.centerX(), baseline, mPaint);
        }
    }
    /**
     * 翻页等按键处理
     * @param keyCode
     * @param event
     * @return
     */
    public boolean KeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP)
        {
            if (this.mCurrentLine > 0)
            {
                this.mCurrentLine--;
            }
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
        {
            if ((this.mCurrentLine + this.mPageLineNum) < (this.mRealLine - 1))
            {
                this.mCurrentLine++;
            }
        }
        return false;
    }
}
