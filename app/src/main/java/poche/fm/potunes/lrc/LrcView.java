package poche.fm.potunes.lrc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import poche.fm.potunes.R;
import poche.fm.potunes.utils.TextUtil;

@SuppressLint("DrawAllocation")
public class LrcView extends View {
	private static final int SCROLL_TIME = 500;
	private static final String DEFAULT_TEXT = "暂未找到歌词";
	private String TAG = "LrcView";

	private List<LrcEntry> mLrcEntryList = new ArrayList<>();

	private long mNextTime = 0l; // 保存下一句开始的时间

	private int mViewWidth; // view的宽度
	private int mLrcHeight; // lrc界面的高度
	private int mRows;      // 多少行
	private int mCurrentLine = 0; // 当前行
	private int mOffsetY;   // y上的偏移
	private int mMaxScroll; // 最大滑动距离=一行歌词高度+歌词间距

	private float mTextSize; // 字体
	private float mDividerHeight; // 行间距
	
	private Rect mTextBounds;
	private TextPaint mCurrentPaint = new TextPaint(); // 当前歌词的大小


	private Scroller mScroller;
	private int  normalTextColor;
	private int currentTextColor;


	public LrcView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	public LrcView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mScroller = new Scroller(context, new LinearInterpolator());
		inflateAttributes(attrs);
	}
	// 初始化操作
	private void inflateAttributes(AttributeSet attrs) {
		// 解析自定义属性
		TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.Lrc);
		mTextSize = ta.getDimension(R.styleable.Lrc_textSize, 50.0f);
		mRows = ta.getInteger(R.styleable.Lrc_rows, 12);
		mDividerHeight = ta.getDimension(R.styleable.Lrc_dividerHeight, LrcUtils.dp2px(getContext(), 16));

		normalTextColor = ta.getColor(R.styleable.Lrc_normalTextColor, 0xffffffff);
		currentTextColor = ta.getColor(R.styleable.Lrc_currentTextColor, 0xFFFF4081);
		ta.recycle();


		// 计算lrc面板的高度
		mLrcHeight = (int) (mTextSize + mDividerHeight) * mRows + 5;
		
		// 初始化paint
		mCurrentPaint.setTextSize(mTextSize);
		mCurrentPaint.setColor(currentTextColor);
		mCurrentPaint.setAntiAlias(true);
		
		mTextBounds = new Rect();
		mCurrentPaint.getTextBounds(DEFAULT_TEXT, 0, DEFAULT_TEXT.length(), mTextBounds);
		mMaxScroll = (int) (mTextBounds.height() + mDividerHeight);
	}
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		// 重新设置view的高度
		int measuredHeightSpec = MeasureSpec.makeMeasureSpec(mLrcHeight, MeasureSpec.AT_MOST);
		super.onMeasure(widthMeasureSpec, measuredHeightSpec);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		// 获取view宽度
		mViewWidth = getMeasuredWidth();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		float centerY = (getMeasuredHeight() + mTextBounds.height() - mDividerHeight) / 2;
		mCurrentPaint.setColor(currentTextColor);
		if (mLrcEntryList.isEmpty()) {
			canvas.drawText(DEFAULT_TEXT,
					(mViewWidth - mCurrentPaint.measureText(DEFAULT_TEXT)) / 2,
					centerY, mCurrentPaint);

			return;
		}
		// 画当前行
		float currentY = centerY - mLrcEntryList.get(mCurrentLine).getTextHeight() / 2;
		drawText(canvas, mLrcEntryList.get(mCurrentLine).getStaticLayout(), currentY);

		// 画当前行上面的
		mCurrentPaint.setColor(normalTextColor);
		float upY = currentY;
		for (int i = mCurrentLine - 1; i >= 0; i--) {
			upY -= mDividerHeight + mLrcEntryList.get(i).getTextHeight();
			drawText(canvas, mLrcEntryList.get(i).getStaticLayout(), upY);
		}

		// 画当前行下面的
		float downY = currentY + mLrcEntryList.get(mCurrentLine).getTextHeight() + mDividerHeight;
		for (int i = mCurrentLine + 1; i < mLrcEntryList.size(); i++) {
			drawText(canvas, mLrcEntryList.get(i).getStaticLayout(), downY);
			downY += mLrcEntryList.get(i).getTextHeight() + mDividerHeight;
		}

	}

	private void drawText(Canvas canvas, StaticLayout staticLayout, float y) {
		canvas.save();
		canvas.translate(mDividerHeight, y);
		staticLayout.draw(canvas);
		canvas.restore();
	}
	
	@Override
	public void computeScroll() {
		if(mScroller.computeScrollOffset()) {
			mOffsetY = mScroller.getCurrY();
			if(mScroller.isFinished()) {
				int cur = mScroller.getCurrX();
				mCurrentLine = cur <= 1 ? 0 : cur - 1;
				mOffsetY = 0;
			}
			postInvalidate();
		}
	}

	// 外部提供方法传入当前播放时间
	public synchronized void changeCurrent(long time) {
		// 如果当前时间小于下一句开始的时间
		// 直接return
		if (mNextTime > time) {
			return;
		}
		
		// 每次进来都遍历存放的时间
		int timeSize = mLrcEntryList.size();
		for (int i = 0; i < timeSize; i++) {
			
			// 解决最后一行歌词不能高亮的问题
			if(mNextTime == mLrcEntryList.get(timeSize - 1).getTime()) {
				mNextTime += 60 * 1000;
				mScroller.abortAnimation();
				mScroller.startScroll(timeSize, 0, 0, mMaxScroll, SCROLL_TIME);
				postInvalidate();
				return;
			}

			if (mLrcEntryList.get(i).getTime() > time) {
				mNextTime = mLrcEntryList.get(i).getTime();
				mScroller.abortAnimation();
				mScroller.startScroll(i, 0, 0, mMaxScroll, SCROLL_TIME);
				postInvalidate();

				return;
			}
		}
	}
	// 拖动进度条时
	public void onDrag(int progress) {
		for(int i=0;i<mLrcEntryList.size();i++) {
			if((mLrcEntryList.get(i).getTime()) > progress) {
				mNextTime = i == 0 ? 0 : mLrcEntryList.get(i-1).getTime();
				return;
			}
		}
	}

	// 加载Lrc
	public void loadLrc(final String lrcText, final String chLrcText) {
		reset();

		setTag(lrcText);
		AsyncTask<String, Integer, List<LrcEntry>> loadLrcTask = new AsyncTask<String, Integer, List<LrcEntry>>() {
			@Override
			protected List<LrcEntry> doInBackground(String... params) {
				return LrcEntry.parseLrc(params[0], params[1]);
			}

			@Override
			protected void onPostExecute(List<LrcEntry> lrcEntries) {
				if (getTag() == lrcText) {
					onLrcLoaded(lrcEntries);
					setTag(null);
				}
			}
		};
		loadLrcTask.execute(lrcText, chLrcText);
	}

	private void onLrcLoaded(List<LrcEntry> entryList) {
		if (entryList != null && !entryList.isEmpty()) {
			mLrcEntryList.addAll(entryList);
		}

		if (hasLrc()) {
			initEntryList();
			initNextTime();
		}
		postInvalidate();
	}

	private void initEntryList() {
		if (getWidth() == 0) {
			return;
		}

		Collections.sort(mLrcEntryList);

		for (LrcEntry lrcEntry : mLrcEntryList) {
			lrcEntry.init(mCurrentPaint, (int)(mViewWidth - mDividerHeight * 2));
		}
	}



	private void initNextTime() {
		if (mLrcEntryList.size() > 1) {
			mNextTime = mLrcEntryList.get(1).getTime();
		} else {
			mNextTime = Long.MAX_VALUE;
		}
	}

	private void reset() {
		mLrcEntryList.clear();
		mCurrentLine = 0;
		mNextTime = 0l;
	}
	
	// 是否设置歌词
	public boolean hasLrc() {
		return !mLrcEntryList.isEmpty();
	}



    private static class LrcLine implements Comparable<LrcLine> {
        long time;
        String line;
        @Override
        public int compareTo(LrcLine another) {
            return (int) (time - another.time);
        }
    }
}
