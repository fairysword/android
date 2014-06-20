package com.baidu.iaccount.widget;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.baidu.iaccount.base.ObserverManager;
import com.baidu.iaccount.util.L;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Xfermode;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * 饼图列表页
 * 
 * @author xiong.junhui
 * 
 */
public class PieListView extends View implements
		ObserverManager<PieListView.OnItemSelectionChangeListener> {

	public interface OnItemSelectionChangeListener {
		public void OnItemSelectionChange(PieData pd);
	}

	/**
	 * 
	 */
	Paint mPaint = new Paint();

	/**
	 * 
	 */
	RectF mPieRectF = new RectF();

	/**
	 * 
	 */
	RectF mPieHlRectF = new RectF();

	//
	// for center views
	//

	/**
	 * 
	 */
	Rect mCenterViewRect = new Rect();
	/**
	 * 
	 */
	Path mCenterViewPath = new Path();

	/**
	 * 
	 */
	View mCenterView;

	/**
	 * not work, why?
	 */
	Xfermode mCenterPaintMode = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);

	/**
	 * 
	 */
	final static int PIE_MARGIN = 20;

	/**
	 * 突出的扇形两边的空白距离
	 */
	final static float HLIGHT_MG_SIDE = 1.8f;

	/**
	 * 突出的扇形弧边的空白距离
	 */
	final static float HLIGHT_MG_ARC = 11f;

	/**
	 * 
	 */
	float mStartAngle = 0f;

	/**
	 * 
	 */
	float mScrollAngle = 0f;

	List<MotionEvent> mFlingMotions = new ArrayList<MotionEvent>();

	//
	// data
	//

	/**
	 * @author xiong.junhui
	 * 
	 */
	static public class PieData {
		public String getLabel() {
			return label;
		}

		public float getCount() {
			return count;
		}

		public int getColor() {
			return color;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public void setCount(float count) {
			this.count = count;
		}

		public void setColor(int color) {
			this.color = color;
		}

		public PieData(String label, float count, int color) {
			super();
			this.label = label;
			this.count = count;
			this.color = color;
		}

		String label;
		float count;
		int color;

		float startAngle;
		float sweepAngle;

		@Override
		public String toString() {
			JSONObject j = new JSONObject();
			try {
				j.put("label", label);
				j.put("count", count);
				j.put("startAngle", startAngle);
				j.put("sweepAngle", sweepAngle);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return j.toString();
		}
	}

	int mEmptyColor = android.R.color.darker_gray;

	/**
	 * 选中时旋转的终点角度
	 */
	int mHlAngle = 90;

	/**
	 * 
	 */
	List<PieData> mData = new ArrayList<PieData>();

	/**
	 * 
	 */
	float mTotalCount = 0f;

	/**
	 * 被选中的pie
	 */
	PieData mSelectedPie;

	public PieListView(Context context, AttributeSet attrs) {
		super(context, attrs);

		setClickable(true);

		mPaint.setStrokeWidth(6);
		mPaint.setAntiAlias(true);
		mPaint.setStyle(Paint.Style.FILL);

		if (!isInEditMode()) {
			mGestureDetector = new GestureDetector(context, mOnGestureListener);
		}

	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(
				widthMeasureSpec,
				MeasureSpec.makeMeasureSpec(
						MeasureSpec.getSize(widthMeasureSpec),
						MeasureSpec.getMode(heightMeasureSpec)));
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		// set RECTs
		mPieRectF.set(0, 0, getWidth(), getHeight());
		mPieRectF.inset(PIE_MARGIN, PIE_MARGIN);
		mPieHlRectF.set(mPieRectF);
		mPieRectF.round(mCenterViewRect);
		mCenterViewRect.inset(mCenterViewRect.width() / 4,
				mCenterViewRect.height() / 4);
		

		if (mTotalCount == 0) {
			mPaint.setColor(getResources().getColor(mEmptyColor));
			canvas.drawArc(mPieRectF, 0, 360, true, mPaint);
		} else {
			float startAngle = moduloValue(mStartAngle + mScrollAngle);

			for (PieData pd : mData) {
				if (pd != null) {
					mPaint.setColor(getResources().getColor(pd.color));
					pd.startAngle = startAngle;

					if (isInSelectedAngle(pd) && !mSelectRunnable.mStillFling) {
						setSelectedPie(pd);
					}

					RectF rect = mPieRectF;
					if (pd.equals(mSelectedPie)) {
						boolean hl = false;
						if (pd.sweepAngle < 160) {
							hl = true;
						}

						canvas.save();
						{
							float radian = (float) angle2Radian(pd.startAngle
									+ pd.sweepAngle / 2);
							float radHalf = (float) angle2Radian(pd.sweepAngle / 2);
							float hd = (float) (HLIGHT_MG_SIDE / Math
									.sin(radHalf));
							float dx = (float) (hd * Math.cos(radian));
							float dy = (float) (hd * Math.sin(radian));

							if (hl) {
								rect = mPieHlRectF;
								rect.offset(dx, dy);
								canvas.translate(dx, dy);
								rect.inset(2 * hd - HLIGHT_MG_ARC, 2 * hd
										- HLIGHT_MG_ARC);
							}

							canvas.drawArc(rect, pd.startAngle, pd.sweepAngle,
									true, mPaint);
						}
						canvas.restore();
					} else {
						canvas.drawArc(rect, pd.startAngle, pd.sweepAngle,
								true, mPaint);
					}
					startAngle += pd.sweepAngle;
					startAngle %= 360;
				}
			}
		}

		// 绘制中心区域背景
		canvas.save();
		{
			int mode = 0;
			if (mode == 1) {
				mCenterViewPath.reset();
				canvas.clipPath(mCenterViewPath);
				mCenterViewPath.addCircle(mCenterViewRect.exactCenterX(),
						mCenterViewRect.exactCenterY(),
						mCenterViewRect.width() / 2, Path.Direction.CCW);
				canvas.clipPath(mCenterViewPath, Region.Op.REPLACE);
				canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.SRC);
			} else {
				mPaint.setColor(getResources().getColor(android.R.color.white));
				Xfermode om = mPaint.getXfermode();
				mPaint.setXfermode(mCenterPaintMode);
				canvas.drawCircle(mCenterViewRect.exactCenterX(),
						mCenterViewRect.exactCenterY(),
						mCenterViewRect.width() / 2, mPaint);
				mPaint.setXfermode(om);
			}
		}
		canvas.restore();

		//
		if (mCenterView != null) {
			drawCenterView(canvas, mCenterView, mCenterViewRect);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (isClickable() || isLongClickable()) {
			switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_UP: {
				mStartAngle += mScrollAngle;
				mScrollAngle = 0;
				break;
			}

			case MotionEvent.ACTION_MOVE: {
				mFlingMotions.add(MotionEvent.obtain(event));
				break;
			}

			default:
				break;
			}

			mGestureDetector.onTouchEvent(event);

			PointF p = new PointF(event.getX(), event.getY());
			if (isTouchCenter(p) && mCenterView != null) {
				mCenterView.dispatchTouchEvent(event);
			}

			return true;
		}
		return false;
	}

	/**
	 * 设置选中角度
	 * 
	 * @param angle
	 */
	public void setSelectedAngle(int angle) {
		mHlAngle = moduloValue(angle);
	}

	public void setEmptyColor(int emptyColor) {
		if (emptyColor > 0 && emptyColor != mEmptyColor) {
			mEmptyColor = emptyColor;
		}
	}

	//
	// data operation
	//

	public void set(List<PieData> pds) {
		changeSelectedPie(null);

		mData.clear();
		mTotalCount = 0;

		if (pds != null && pds.size() > 0) {
			mData.addAll(pds);
		}

		for (PieData pd : mData) {
			mTotalCount += pd.count;
		}

		for (PieData pd : mData) {
			pd.sweepAngle = 360 * ((float) pd.count / mTotalCount);
		}

		invalidate();
	}

	public void add(PieData pd) {
		if (pd == null) {
			return;
		}
		mData.add(pd);
		invalidate();
	}

	public void add(int index, PieData pd) {
		if (pd == null) {
			return;
		}
		mData.add(index, pd);
		invalidate();
	}

	public void remove(PieData pd) {
		mData.remove(pd);
		invalidate();
	}

	public void remove(int index) {
		mData.remove(index);
		invalidate();
	}

	public void clear() {
		mData.clear();
		invalidate();
	}

	public PieData getSelectedPie() {
		return mSelectedPie;
	}

	public void setSelectedPie(int location) {
		if (changeSelectedPie(mData.get(location))) {
			notifyObservers(mPieSelectionObservers, mSelectedPie);
			invalidate();
		}
	}

	public void setSelectedPie(PieData pieData) {
		if (mData.contains(pieData) && changeSelectedPie(pieData)) {
			notifyObservers(mPieSelectionObservers, mSelectedPie);
			invalidate();
		}
	}

	/**
	 * 如果点中的是当前选中的pie，则重置选中状态，否则切换选中的pie
	 * 
	 * @param pd
	 * @return
	 */
	private boolean changeSelectedPie(PieData pd) {
		boolean change = false;

		change = pd != null ? !pd.equals(mSelectedPie) : mSelectedPie != null;
		if (change) {
			mSelectedPie = pd;
		}
		// if (pd != null) {
		// if (!pd.equals(mSelectedPie)) {
		// mSelectedPie = pd;
		// return true;
		// } else {
		// // mSelectedPie = null;
		// return false;
		// }
		// } else {
		//
		// }
		return change;
	}

	public void setCenterView(View center) {
		mCenterView = center;
		invalidate();
	}

	private void drawCenterView(Canvas canvas, View v, Rect r) {
		canvas.save();
		{
			v.measure(getWidth(), getHeight());
			float x = (r.left + r.right) / 2 - v.getMeasuredWidth() / 2;
			float y = (r.top + r.bottom) / 2 - v.getMeasuredHeight() / 2;
			canvas.translate(x, y);
			v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
			v.draw(canvas);
		}
		canvas.restore();
	}

	private boolean isTouchCenter(PointF p) {
		float dist = caculateDistance(p);
		return (dist < mCenterViewRect.width() / 2);
	}

	private PieData getTouchedPie(PointF p) {
		final float angle = caculateAngle(p);
		final float d2c = caculateDistance(p);

		for (PieData pd : mData) {
			boolean isTouched = true;
			// 1.check distance
			isTouched &= d2c > mCenterViewRect.width() / 2
					&& d2c <= mPieRectF.width() / 2;
			// 2.check angle
			float sa = pd.startAngle;
			float ea = pd.startAngle + pd.sweepAngle;
			float tangle = angle < sa ? angle + 360 : angle;
			isTouched &= (tangle <= ea && tangle >= sa);

			if (isTouched) {
				return pd;
			}
		}
		return null;
	}

	private float caculateDeterminant(PointF p1, PointF p2, PointF p3) {
		return (p1.x * p2.y - p2.x * p1.y) // 行列式值
				+ (p2.x * p3.y - p3.x * p2.y) + (p3.x * p1.y - p1.x * p3.y);
	}

	// private double caculateFlingAngle(List<MotionEvent> mes) {
	// if (mes == null || mes.size() <= 0) {
	// return 0;
	// }
	//
	// double sumAngle = 0f;
	// MotionEvent me1;
	// MotionEvent me2;
	// PointF v1 = new PointF();
	// PointF v2 = new PointF();
	//
	// for (int loc = 0, n = mes.size() - 1; loc < n; loc++) {
	// me1 = mes.get(loc);
	// me2 = mes.get(loc + 1);
	// if (me1 != null && me2 != null) {
	// v1.x = me1.getX() - mPieRectF.centerX();
	// v1.y = me1.getY() - mPieRectF.centerY();
	// v2.x = me2.getX() - mPieRectF.centerX();
	// v2.y = me2.getY() - mPieRectF.centerY();
	// sumAngle += caculateVectorAngle(v1, v2);
	// }
	// }
	//
	// return sumAngle;
	// }

	// private double caculateVectorAngle(PointF v1, PointF v2) {
	// float vInnerProduct = v1.x * v2.x + v1.y * v2.y;
	// double v1Value = Math.sqrt(v1.x * v1.x + v1.y * v1.y);
	// double v2Value = Math.sqrt(v2.x * v2.x + v2.y * v2.y);
	//
	// double cosValue = vInnerProduct / (v1Value * v2Value);
	// if (cosValue > 1.0f) {
	// cosValue = 1.0f;
	// }
	// if (cosValue < -1.0f) {
	// cosValue = -1.0f;
	// }
	// double arccos = Math.acos(cosValue);
	// double angle = radian2Angle(arccos);
	//
	// return angle;
	// }

	private float caculateAngle(PointF p) {
		float deltaX = p.x - mPieRectF.centerX();
		float deltaY = p.y - mPieRectF.centerY();
		float radian = (float) Math.atan2(deltaY, deltaX);
		float angle = (float) (radian2Angle(radian));
		angle = (float) moduloValue(angle);
		return angle;
	}

	private float caculateDistance(PointF p) {
		float deltaX = p.x - mPieRectF.centerX();
		float deltaY = p.y - mPieRectF.centerY();
		float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
		return distance;
	}

	private boolean isInSelectedAngle(PieData pd) {
		float sa = moduloValue(pd.startAngle);

		if (sa > mHlAngle) {
			sa -= 360;
		}
		return (sa <= mHlAngle && sa + pd.sweepAngle >= mHlAngle);
	}

	static private double radian2Angle(double radian) {
		return ((radian / Math.PI) * 180);
	}

	static private double angle2Radian(double angle) {
		return ((angle / 180) * Math.PI);
	}

	static private float moduloValue(float orignValue) {
		return ((orignValue + 360) % 360);
	}

	static private int moduloValue(int orignValue) {
		return ((orignValue + 360) % 360);
	}

	GestureDetector.OnGestureListener mOnGestureListener = new GestureDetector.OnGestureListener() {

		@Override
		public boolean onDown(MotionEvent e) {
			mFlingRunnable.willStopIfNoFlingAgain();
			mFlingMotions.clear();
			return true;
		}

		@Override
		public void onShowPress(MotionEvent e) {

		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			mFlingRunnable.endFling();

			changeSelectedPie(null);
			PieData pd = getTouchedPie(new PointF(e.getX(), e.getY()));

			if (pd != null) {
				if (!mSelectRunnable.mStillFling) {
					mSelectRunnable.startFling(pd.startAngle + pd.sweepAngle
							/ 2, mHlAngle);
				}
			}
			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			mFlingRunnable.endFling();
			mSelectRunnable.endFling();

			float angle1 = caculateAngle(new PointF(e1.getX(), e1.getY()));
			float angle2 = caculateAngle(new PointF(e2.getX(), e2.getY()));
			mScrollAngle = moduloValue(angle2 - angle1);
			invalidate();
			return true;
		}

		@Override
		public void onLongPress(MotionEvent e) {

		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			if (mFlingMotions.size() <= 0) {
				return false;
			}

			// 判断是顺时针还是逆时针?
			int loc = 0;
			loc = (int) Math.floor(mFlingMotions.size() / 2f);
			MotionEvent me = mFlingMotions.get(loc);
			PointF p2 = null;
			if (me != null) {
				p2 = new PointF(me.getX(), me.getY());
			} else {
				return false;
			}
			final PointF cp = new PointF(mPieRectF.centerX(),
					mPieRectF.centerY()); // 圆心点
			final PointF p1 = new PointF(e1.getX(), e1.getY());
			float det = caculateDeterminant(cp, p1, p2);

			// 计算旋转角度
			// mFlingMotions.add(0, MotionEvent.obtain(e1));
			// mFlingMotions.add(MotionEvent.obtain(e2));
			// double angle = caculateFlingAngle(mFlingMotions); // 向量之间的夹角
			// if (det > 0) {// clockwise
			// // angle = Math.abs(angle);
			// angle = (ea - sa + 360) % 360;
			// } else {// count-clockwise
			// // angle = -Math.abs(angle);
			// angle = (ea - sa - 360) % 360;
			// }
			double sa = caculateAngle(p1);
			double ea = caculateAngle(p2);
			double angle = 0f;
			if (det > 0) {// clockwise
				angle = (ea - sa + 360) % 360;
			} else {// count-clockwise
				angle = (ea - sa - 360) % 360;
			}

			// 计算角速度
			double velocityAngle = 0f;
			long deltaTime = e2.getEventTime() - e1.getEventTime();
			velocityAngle = angle / deltaTime; // 角速度
			mFlingRunnable.startFling(velocityAngle);

			// 重置
			for (MotionEvent e : mFlingMotions) {
				e.recycle();
			}
			mFlingMotions.clear();

			return true;
		}

	};

	/**
	 * 给定初始角速度和运动时间
	 * 
	 * @author xiong.junhui
	 * 
	 */
	class FlingRunnable implements Runnable {
		final static int FLING_DURATION = 400;
		final static int FLING_STEP_TIME = 30;
		long mFlingStartTime = 0;
		boolean mTimePending;
		double mVelocityAngle;

		public void startFling(double velocityAngle) {
			L.d("velocityAngle=" + velocityAngle);
			mTimePending = true;
			mFlingStartTime = System.currentTimeMillis();
			mVelocityAngle = velocityAngle;
			post(this);
		}

		public void willStopIfNoFlingAgain() {
			mTimePending = false;
		}

		public void endFling() {
			mTimePending = false;
			removeCallbacks(this);
		}

		@Override
		public void run() {
			double stepAngle = FLING_STEP_TIME * mVelocityAngle;
			L.d("stepAngle=" + stepAngle);
			mStartAngle = (float) ((mStartAngle + stepAngle) % 360);
			invalidate();
			long totalFlingTime = System.currentTimeMillis() - mFlingStartTime;
			if (totalFlingTime >= FLING_DURATION) {
				mTimePending = false;
			} else { // 按时间减少角速度
				mVelocityAngle = (Math.sqrt(FLING_DURATION * FLING_DURATION
						- totalFlingTime * totalFlingTime) / FLING_DURATION)
						* mVelocityAngle;
			}
			if (mTimePending) {
				postDelayed(this, FLING_STEP_TIME);
			}
		}

	}

	FlingRunnable mFlingRunnable = new FlingRunnable();

	/**
	 * 指定终止角度和运行速度
	 * 
	 * @author xiong.junhui
	 * 
	 */
	class SelectRunnable implements Runnable {
		long mStartTime = 0L;
		float mDeltaAngle = 0f;
		static final float VELOCITY_ANGLE = 0.2f;
		boolean mStillFling;
		boolean cw = false;

		public void startFling(float startAngle, float destAngle) {
			startAngle = moduloValue(startAngle);
			destAngle = moduloValue(destAngle);

			// 1. CW
			float deltaCW = (destAngle - startAngle + 360) % 360;
			deltaCW = Math.abs(deltaCW);
			float deltaCCW = (destAngle - startAngle - 360) % 360;
			deltaCCW = Math.abs(deltaCCW);
			cw = deltaCW < deltaCCW ? true : false;
			mDeltaAngle = cw ? deltaCW : deltaCCW;

			mStartTime = System.currentTimeMillis();
			post(this);
			mStillFling = true;
		}

		@Override
		public void run() {
			if (mDeltaAngle <= 0) {
				removeCallbacks(this);
				mStillFling = false;
				invalidate(); // 重绘
				return;
			}

			if (mStillFling) {
				float step = VELOCITY_ANGLE
						* (System.currentTimeMillis() - mStartTime);

				if (cw) {
					mStartAngle += step;
				} else {
					mStartAngle -= step;
				}
				mStartAngle = moduloValue(mStartAngle);
				invalidate();

				mDeltaAngle -= step;
				mStartTime = System.currentTimeMillis();
				postDelayed(this, 10);
			}
		}

		public void endFling() {
			mStillFling = false;
		}

	}

	SelectRunnable mSelectRunnable = new SelectRunnable();

	GestureDetector mGestureDetector;

	BaseObserverManager<OnItemSelectionChangeListener> mPieSelectionObservers = new BaseObserverManager<PieListView.OnItemSelectionChangeListener>() {

		@Override
		public void notifyObservers(
				ObserverManager<OnItemSelectionChangeListener> om, Object data) {
			for (OnItemSelectionChangeListener l : om.getObservers()) {
				l.OnItemSelectionChange((PieData) data);
			}
		}
	};

	@Override
	public void registerObserver(OnItemSelectionChangeListener l) {
		mPieSelectionObservers.registerObserver(l);
	}

	@Override
	public void unregisterObserver(OnItemSelectionChangeListener l) {
		mPieSelectionObservers.unregisterObserver(l);
	}

	@Override
	public void unregisterAll() {
		mPieSelectionObservers.unregisterAll();
	}

	@Override
	public void notifyObservers(
			ObserverManager<OnItemSelectionChangeListener> om, Object data) {
		mPieSelectionObservers.notifyObservers(mPieSelectionObservers, data);
	}

	@Override
	public List<OnItemSelectionChangeListener> getObservers() {
		return mPieSelectionObservers.getObservers();
	}

	//
	// 自定义手势识别
	//

	// /**
	// * press时的pie
	// */
	// PieData mPressedPie;
	//
	// class XjhGestureDetector {
	// final static int TOUCH_MODE_RESET = 0;
	//
	// final static int TOUCH_MODE_DOWN = 1;
	//
	// final static int TOUCH_MODE_TAP = 2;
	//
	// final static int TOUCH_MODE_SCROLL = 3;
	//
	// int mTouchMode = TOUCH_MODE_RESET;
	//
	// class CheckTap implements Runnable {
	//
	// @Override
	// public void run() {
	// mTouchMode = TOUCH_MODE_TAP;
	// }
	//
	// }
	//
	// CheckTap mCheckTap = new CheckTap();
	// /**
	// * 按下时候的点的位置
	// */
	// float mPressedAngle;
	// private int mScrollThreshold = 5;
	//
	// public boolean onTouchEvent(MotionEvent event) {
	// if (isClickable() || isLongClickable()) {
	// PointF p = new PointF(event.getX(), event.getY());
	// int action = event.getAction() & MotionEvent.ACTION_MASK;
	//
	// switch (action) {
	// case MotionEvent.ACTION_DOWN: {
	// mTouchMode = TOUCH_MODE_DOWN;
	// mPressedAngle = caculateAngle(p);
	// mPressedPie = getTouchedPie(p);
	// postDelayed(mCheckTap, ViewConfiguration.getTapTimeout());
	// break;
	// }
	//
	// case MotionEvent.ACTION_MOVE: {
	// float angle = caculateAngle(p);
	// if (Math.abs(angle - mPressedAngle) > mScrollThreshold) {
	// // we are scrolling
	// mTouchMode = TOUCH_MODE_SCROLL;
	// }
	//
	// if (mTouchMode == TOUCH_MODE_SCROLL && mPressedPie != null) {
	// L.d("xjh" + (angle - mPressedAngle));
	// mStartAngle += (angle - mPressedAngle);
	// mStartAngle = (mStartAngle + 360) % 360;
	// mPressedAngle = angle;
	// removeCallbacks(mCheckTap);
	// invalidate();
	// }
	//
	// break;
	// }
	//
	// case MotionEvent.ACTION_UP: {
	// PieData pd = getTouchedPie(p);
	// if (pd != null) {
	// if (pd.equals(mPressedPie)) {
	// if (mTouchMode == TOUCH_MODE_TAP
	// || mTouchMode == TOUCH_MODE_DOWN) {
	// setSelectedPie(pd);
	// L.d(pd != null ? pd.toString()
	// : "no pie touched");
	// }
	// }
	// }
	//
	// mTouchMode = TOUCH_MODE_RESET;
	// mPressedPie = null;
	// break;
	// }
	//
	// case MotionEvent.ACTION_CANCEL: {
	// mTouchMode = TOUCH_MODE_RESET;
	// mPressedPie = null;
	// break;
	// }
	//
	// }
	//
	// return true;
	// }
	// return false;
	// }
	// }
	//
	// XjhGestureDetector mXjhGestureDetector = new XjhGestureDetector();

}
