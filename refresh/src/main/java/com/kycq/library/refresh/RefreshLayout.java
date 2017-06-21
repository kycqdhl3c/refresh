package com.kycq.library.refresh;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.annotation.IntDef;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class RefreshLayout extends ViewGroup implements NestedScrollingParent, NestedScrollingChild {
	/** 无效操作点 */
	private static final int INVALID_POINTER = -1;
	
	/** 状态自动判断 */
	public static final int STATUS_MODE_AUTO = 0;
	/** 状态总是显示 */
	public static final int STATUS_MODE_SHOW = 1;
	/** 状态总是隐藏 */
	public static final int STATUS_MODE_HIDE = 2;
	
	@Retention(RetentionPolicy.SOURCE)
	@IntDef({STATUS_MODE_AUTO, STATUS_MODE_SHOW, STATUS_MODE_HIDE})
	private @interface StatusMode {
	}
	
	/** 刷新准备 */
	private static final int STATUS_REFRESH_READY = 0;
	/** 刷新中 */
	private static final int STATUS_REFRESHING = 1;
	/** 刷新结束 */
	private static final int STATUS_REFRESH_COMPLETE = 2;
	
	@Retention(RetentionPolicy.SOURCE)
	@IntDef({STATUS_REFRESH_READY, STATUS_REFRESHING, STATUS_REFRESH_COMPLETE})
	private @interface Status {
	}
	
	private NestedScrollingParentHelper mNestedScrollingParentHelper;
	private NestedScrollingChildHelper mNestedScrollingChildHelper;
	private int[] mConsumed = new int[2];
	private int[] mOffsetInWindow = new int[2];
	
	private int mTouchSlop;
	private boolean mIsNestedScrollInProgress;
	private boolean mIsUnderTouch;
	private boolean mIsBeingDragged;
	private int mActivePointerId = INVALID_POINTER;
	private int mLastMotionY;
	
	private SmoothScroller mSmoothScroller;
	private int mCurrentPosition;
	private Rect mTempRect = new Rect();
	
	/** 状态模式 */
	@StatusMode
	private int mStatusMode = STATUS_MODE_AUTO;
	/** 刷新状态 */
	@Status
	private int mStatus = STATUS_REFRESH_READY;
	/** 刷新顶部监听器 */
	private RefreshHeader mRefreshHeader;
	/** 顶部视图 */
	private View mViewHeader;
	/** 刷新状态监听器 */
	private RefreshStatus mRefreshStatus;
	/** 状态视图 */
	private View mViewStatus;
	/** 对象视图 */
	private View mViewTarget;
	
	/** 任务监听器 */
	private OnTaskListener<Object> mOnTaskListener;
	/** 当前任务 */
	private Object mTask;
	/** 刷新监听器 */
	private OnRefreshListener mOnRefreshListener;
	/** 偏移比例监听器 */
	private OnScaleListener mOnScaleListener;
	
	/**
	 * 构造方法
	 *
	 * @param context 设备上下文环境
	 */
	public RefreshLayout(Context context) {
		this(context, null);
	}
	
	/**
	 * 构造方法
	 *
	 * @param context 设备上下文环境
	 * @param attrs   属性集
	 */
	public RefreshLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
		mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
		setNestedScrollingEnabled(true);
		
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		
		mSmoothScroller = new SmoothScroller();
		
		LayoutInflater inflater = LayoutInflater.from(context);
		
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RefreshLayout);
		int refreshHeaderLayoutId = a.getResourceId(R.styleable.RefreshLayout_refresh_viewHeader, -1);
		if (refreshHeaderLayoutId != -1) {
			View viewHeader = inflater.inflate(refreshHeaderLayoutId, this, false);
			setViewHeader(viewHeader);
		}
		int refreshStatusLayoutId = a.getResourceId(R.styleable.RefreshLayout_refresh_viewStatus, -1);
		if (refreshStatusLayoutId != -1) {
			View viewStatus = inflater.inflate(refreshStatusLayoutId, this, false);
			setViewStatus(viewStatus);
		}
		// noinspection WrongConstant
		setStatusMode(a.getInt(R.styleable.RefreshLayout_refresh_statusMode, STATUS_MODE_AUTO));
		a.recycle();
	}
	
	/**
	 * 设置顶部视图
	 *
	 * @param viewHeader 顶部视图
	 */
	public void setViewHeader(View viewHeader) {
		if (mViewHeader != null) {
			removeView(mViewHeader);
			mRefreshHeader = null;
			mViewHeader = null;
		}
		if (viewHeader == null) {
			return;
		}
		if (!(viewHeader instanceof RefreshHeader)) {
			throw new IllegalArgumentException("viewHeader must implement the RefreshHeader interface!");
		}
		addView(viewHeader);
		mRefreshHeader = (RefreshHeader) viewHeader;
		mViewHeader = viewHeader;
	}
	
	/**
	 * 设置状态视图
	 *
	 * @param viewStatus 状态视图
	 */
	public void setViewStatus(View viewStatus) {
		if (mViewStatus != null) {
			removeView(mViewStatus);
			mRefreshStatus = null;
			mViewStatus = null;
			if (mOnRefreshListener != null) {
				mOnRefreshListener.refreshLayout = null;
				mOnRefreshListener = null;
			}
		}
		if (viewStatus == null) {
			return;
		}
		if (!(viewStatus instanceof RefreshStatus)) {
			throw new IllegalArgumentException("viewStatus must implement the RefreshStatus interface!");
		}
		addView(viewStatus);
		mRefreshStatus = (RefreshStatus) viewStatus;
		mViewStatus = viewStatus;
		
		mOnRefreshListener = new OnRefreshListener();
		mOnRefreshListener.refreshLayout = this;
		mRefreshStatus.initOnRefreshListener(mOnRefreshListener);
		
		initStatusMode();
	}
	
	/**
	 * 设置状态模式
	 *
	 * @param statusMode 状态模式
	 */
	public void setStatusMode(@StatusMode int statusMode) {
		mStatusMode = statusMode;
		initStatusMode();
	}
	
	/**
	 * 设置任务监听器
	 *
	 * @param listener 任务监听器
	 * @param <Task>   任务类型
	 */
	public <Task> void setOnTaskListener(OnTaskListener<Task> listener) {
		if (mOnTaskListener != null && mTask != null) {
			mOnTaskListener.onCancel(mTask);
		}
		// noinspection unchecked
		mOnTaskListener = (OnTaskListener<Object>) listener;
	}
	
	/**
	 * 设置偏移比例监听器
	 *
	 * @param listener 偏移比例监听器
	 */
	public void setOnScaleListener(OnScaleListener listener) {
		mOnScaleListener = listener;
	}
	
	/**
	 * 确认对象视图
	 */
	private void ensureTarget() {
		if (mViewTarget != null) {
			return;
		}
		
		int childCount = getChildCount();
		for (int index = 0; index < childCount; index++) {
			View child = getChildAt(index);
			if (!child.equals(mViewHeader) && !child.equals(mViewStatus)) {
				mViewTarget = child;
				break;
			}
		}
		
		initStatusMode();
	}
	
	/**
	 * 通知下拉刷新状态
	 */
	private void notifyPullToRefresh() {
		if (mRefreshHeader != null) {
			mRefreshHeader.onPullToRefresh();
		}
	}
	
	/**
	 * 通知释放刷新状态
	 */
	private void notifyReleaseToRefresh() {
		if (mRefreshHeader != null) {
			mRefreshHeader.onReleaseToRefresh();
		}
	}
	
	/**
	 * 变更刷新准备状态
	 */
	public void swipeRefreshReady() {
		ensureTarget();
		notifyRefreshReady();
		mSmoothScroller.scrollToStart();
	}
	
	/**
	 * 通知刷新准备状态
	 */
	private void notifyRefreshReady() {
		mStatus = STATUS_REFRESH_READY;
		if (mRefreshStatus != null) {
			mRefreshStatus.onRefreshReady();
		}
		toggleStatus(true);
	}
	
	/**
	 * 变更刷新中状态
	 */
	public void swipeRefresh() {
		swipeRefresh(false);
	}
	
	/**
	 * 变更刷新中状态
	 *
	 * @param scrollToRefresh true滑动到刷新位置
	 */
	public void swipeRefresh(boolean scrollToRefresh) {
		ensureTarget();
		if (mStatus != STATUS_REFRESHING && scrollToRefresh) {
			mSmoothScroller.scrollToRefresh();
		}
		notifyRefresh();
	}
	
	/**
	 * 通知刷新中状态
	 */
	private void notifyRefresh() {
		mStatus = STATUS_REFRESHING;
		if (mOnTaskListener != null) {
			if (mTask != null) {
				mOnTaskListener.onCancel(mTask);
			}
			mTask = mOnTaskListener.onTask();
		}
		if (mRefreshHeader != null) {
			mRefreshHeader.onRefresh();
		}
		if (mRefreshStatus != null) {
			mRefreshStatus.onRefresh();
		}
		toggleStatus(true);
	}
	
	/**
	 * 变更刷新结束状态
	 */
	public void swipeComplete() {
		swipeComplete(null);
	}
	
	/**
	 * 变更刷新结束状态
	 *
	 * @param statusInfo   状态信息
	 * @param <StatusInfo> 状态信息类型
	 */
	public <StatusInfo> void swipeComplete(StatusInfo statusInfo) {
		ensureTarget();
		mSmoothScroller.scrollToStart();
		notifyRefreshComplete(statusInfo);
	}
	
	/**
	 * 通知刷新结束状态
	 *
	 * @param statusInfo   状态信息
	 * @param <StatusInfo> 状态信息类型
	 */
	private <StatusInfo> void notifyRefreshComplete(StatusInfo statusInfo) {
		mStatus = STATUS_REFRESH_COMPLETE;
		if (mRefreshHeader != null) {
			// noinspection unchecked
			mRefreshHeader.onRefreshComplete(statusInfo);
		}
		if (mRefreshStatus != null) {
			// noinspection unchecked
			toggleStatus(mRefreshStatus.onRefreshComplete(statusInfo));
		}
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (mOnTaskListener != null && mTask != null) {
			mOnTaskListener.onCancel(mTask);
		}
		if (mOnRefreshListener != null) {
			mOnRefreshListener.refreshLayout = null;
			mOnRefreshListener = null;
		}
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		ensureTarget();
		if (!isEnabled() || mIsNestedScrollInProgress || canChildScrollUp()) {
			return false;
		}
		
		int actionIndex = event.getActionIndex();
		final int action = event.getActionMasked();
		
		switch (action) {
			case MotionEvent.ACTION_DOWN:
				mIsUnderTouch = true;
				if (mIsBeingDragged = !mSmoothScroller.isFinished()) {
					final ViewParent parent = getParent();
					if (parent != null) {
						parent.requestDisallowInterceptTouchEvent(true);
					}
				}
				
				if (!mSmoothScroller.isFinished()) {
					mSmoothScroller.abort();
				}
				
				mActivePointerId = event.getPointerId(0);
				mLastMotionY = getMotionEventY(event, actionIndex);
				startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
				break;
			case MotionEvent.ACTION_MOVE:
				mIsUnderTouch = true;
				final int activeActionIndex = event.findPointerIndex(mActivePointerId);
				if (activeActionIndex == -1) {
					break;
				}
				
				final int y = getMotionEventY(event, activeActionIndex);
				int deltaY = mLastMotionY - y;
				if (!mIsBeingDragged && Math.abs(deltaY) > mTouchSlop) {
					if (dispatchNestedScroll(0, 0, 0, deltaY, mOffsetInWindow) && mOffsetInWindow[1] != 0) {
						mIsBeingDragged = true;
						deltaY += mOffsetInWindow[1];
						mLastMotionY = y - mOffsetInWindow[1];
					}
					if (dispatchNestedPreScroll(0, deltaY, mConsumed, mOffsetInWindow)) {
						mIsBeingDragged = true;
						mLastMotionY = y - mOffsetInWindow[1];
					}
					if (!mIsBeingDragged) {
						if (deltaY < 0) {
							mIsBeingDragged = true;
						}
						mLastMotionY = y;
					}
				}
				break;
			case MotionEventCompat.ACTION_POINTER_DOWN:
				mIsUnderTouch = true;
				mActivePointerId = event.getPointerId(actionIndex);
				mLastMotionY = getMotionEventY(event, actionIndex);
				break;
			case MotionEventCompat.ACTION_POINTER_UP:
				onSecondaryPointerUp(event);
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				mIsUnderTouch = false;
				mIsBeingDragged = false;
				mActivePointerId = INVALID_POINTER;
				stopNestedScroll();
				break;
		}
		
		return mIsBeingDragged;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mIsNestedScrollInProgress || !isEnabled()) {
			return false;
		}
		
		int actionIndex = event.getActionIndex();
		final int action = event.getActionMasked();
		
		switch (action) {
			case MotionEvent.ACTION_DOWN:
				mIsUnderTouch = true;
				if (mIsBeingDragged = !mSmoothScroller.isFinished()) {
					final ViewParent parent = getParent();
					if (parent != null) {
						parent.requestDisallowInterceptTouchEvent(true);
					}
				}
				
				if (!mSmoothScroller.isFinished()) {
					mSmoothScroller.abort();
				}
				
				mActivePointerId = event.getPointerId(0);
				mLastMotionY = getMotionEventY(event, actionIndex);
				startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
				break;
			case MotionEvent.ACTION_MOVE:
				mIsUnderTouch = true;
				final int activeActionIndex = event.findPointerIndex(mActivePointerId);
				if (activeActionIndex == -1) {
					break;
				}
				
				final int y = getMotionEventY(event, activeActionIndex);
				int deltaY = mLastMotionY - y;
				if (mCurrentPosition > 0) {
					movePosition(-deltaY);
					mLastMotionY = y;
				} else {
					if (dispatchNestedScroll(0, 0, 0, deltaY, mOffsetInWindow)) {
						deltaY += mOffsetInWindow[1];
						mLastMotionY = y - mOffsetInWindow[1];
					}
					if (!movePosition(-deltaY)) {
						if (dispatchNestedPreScroll(0, deltaY, mConsumed, mOffsetInWindow)) {
							mLastMotionY = y - mOffsetInWindow[1];
						}
					}
				}
				break;
			case MotionEventCompat.ACTION_POINTER_DOWN:
				mIsUnderTouch = true;
				mActivePointerId = event.getPointerId(actionIndex);
				mLastMotionY = getMotionEventY(event, actionIndex);
				break;
			case MotionEventCompat.ACTION_POINTER_UP:
				onSecondaryPointerUp(event);
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				mIsUnderTouch = false;
				mIsBeingDragged = false;
				mActivePointerId = INVALID_POINTER;
				stopNestedScroll();
				break;
		}
		
		return true;
	}
	
	private void onSecondaryPointerUp(MotionEvent event) {
		final int pointerIndex = MotionEventCompat.getActionIndex(event);
		final int pointerId = event.getPointerId(pointerIndex);
		if (pointerId == mActivePointerId) {
			final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
			mActivePointerId = event.getPointerId(newPointerIndex);
			mLastMotionY = getMotionEventY(event, newPointerIndex);
		}
	}
	
	private int getMotionEventY(MotionEvent event, int pointerIndex) {
		return (int) (event.getY(pointerIndex) + 0.5f);
	}
	
	@Override
	public void setNestedScrollingEnabled(boolean enabled) {
		mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
	}
	
	@Override
	public boolean isNestedScrollingEnabled() {
		return mNestedScrollingChildHelper.isNestedScrollingEnabled();
	}
	
	@Override
	public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
		return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
	}
	
	@Override
	public boolean startNestedScroll(int axes) {
		boolean isNestedScroll = mNestedScrollingChildHelper.startNestedScroll(axes);
		if (isNestedScroll) {
			mIsUnderTouch = true;
		}
		return isNestedScroll;
	}
	
	@Override
	public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {
		mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, nestedScrollAxes);
		mSmoothScroller.abort();
		startNestedScroll(nestedScrollAxes);
		mIsNestedScrollInProgress = true;
	}
	
	@Override
	public int getNestedScrollAxes() {
		return mNestedScrollingParentHelper.getNestedScrollAxes();
	}
	
	@Override
	public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
		return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
	}
	
	@Override
	public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
		dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, mOffsetInWindow);
		if (!isEnabled()) {
			return;
		}
		int delta = mOffsetInWindow[1] + dyUnconsumed;
		movePosition(-delta);
	}
	
	@Override
	public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
		return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
	}
	
	@Override
	public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
		if (isEnabled() && mCurrentPosition > 0 && dy > 0 && movePosition(-dy)) {
			consumed[1] += dy;
		} else {
			dispatchNestedPreScroll(dx, dy, consumed, mOffsetInWindow);
		}
	}
	
	@Override
	public void onStopNestedScroll(View target) {
		mNestedScrollingParentHelper.onStopNestedScroll(target);
		mIsNestedScrollInProgress = false;
		mIsUnderTouch = false;
		stopNestedScroll();
	}
	
	@Override
	public void stopNestedScroll() {
		if (mRefreshHeader == null) {
			return;
		}
		
		int refreshPosition = mRefreshHeader.getRefreshOffsetPosition();
		if (mStatus != STATUS_REFRESHING
				&& mCurrentPosition >= refreshPosition) {
			notifyRefresh();
		}
		if (!mIsUnderTouch) {
			if (mStatus == STATUS_REFRESHING) {
				if (mCurrentPosition > refreshPosition) {
					mSmoothScroller.scrollToRefresh();
				} else {
					mSmoothScroller.scrollToStart();
				}
			} else if (mStatus != STATUS_REFRESHING) {
				mSmoothScroller.scrollToStart();
			}
		}
		mNestedScrollingChildHelper.stopNestedScroll();
	}
	
	protected boolean canChildScrollUp() {
		View target = mViewTarget.getVisibility() == VISIBLE ? mViewTarget : mViewStatus;
		return target != null && ViewCompat.canScrollVertically(target, -1);
	}
	
	private boolean movePosition(int delta) {
		if (mRefreshHeader == null) {
			return false;
		}
		
		int maxPosition = mViewHeader.getMeasuredHeight();
		delta = delta / 3;
		int toPosition = mCurrentPosition + delta;
		
		if (toPosition < 0) {
			if (mCurrentPosition == 0) {
				return false;
			}
			toPosition = 0;
		} else if (toPosition > maxPosition) {
			if (mCurrentPosition == maxPosition) {
				return false;
			}
			toPosition = maxPosition;
		}
		
		offsetPosition(toPosition);
		
		return true;
	}
	
	private void offsetPosition(int toPosition) {
		int lastPosition = mCurrentPosition;
		mCurrentPosition = toPosition;
		int offset = toPosition - lastPosition;
		
		int refreshOffsetPosition = mRefreshHeader.getRefreshOffsetPosition();
		
		if (mStatus != STATUS_REFRESHING) {
			if ((lastPosition == 0 && mCurrentPosition > 0)
					|| (lastPosition >= refreshOffsetPosition) && mCurrentPosition < refreshOffsetPosition) {
				notifyPullToRefresh();
			} else if (lastPosition < refreshOffsetPosition && mCurrentPosition >= refreshOffsetPosition) {
				notifyReleaseToRefresh();
			}
		}
		
		float scale = (float) (1.0 * mCurrentPosition / mViewHeader.getMeasuredHeight());
		if (mOnScaleListener != null) {
			mOnScaleListener.onScale(scale);
		}
		
		offsetLayout(offset, scale);
	}
	
	private void offsetLayout(int offset, float scale) {
		if (mViewHeader != null) {
			ViewCompat.offsetTopAndBottom(mViewHeader, offset);
			mRefreshHeader.onRefreshScale(scale);
		}
		if (mViewStatus != null) {
			ViewCompat.offsetTopAndBottom(mViewStatus, offset);
			mRefreshStatus.onRefreshScale(scale);
		}
		if (mViewTarget != null) {
			ViewCompat.offsetTopAndBottom(mViewTarget, offset);
		}
	}
	
	/**
	 * 初始化状态模式
	 */
	private void initStatusMode() {
		if (mViewStatus != null) {
			if (mStatusMode == STATUS_MODE_SHOW) {
				mViewStatus.setVisibility(VISIBLE);
			} else if (mStatusMode == STATUS_MODE_HIDE) {
				mViewStatus.setVisibility(GONE);
			} else if (mStatusMode == STATUS_MODE_AUTO) {
				mViewStatus.setVisibility(VISIBLE);
			}
			if (isInEditMode()) {
				mViewStatus.setVisibility(GONE);
			}
		}
		
		if (mViewTarget != null) {
			if (mStatusMode == STATUS_MODE_SHOW) {
				mViewTarget.setVisibility(GONE);
			} else if (mStatusMode == STATUS_MODE_HIDE) {
				mViewTarget.setVisibility(VISIBLE);
			} else if (mStatusMode == STATUS_MODE_AUTO) {
				mViewTarget.setVisibility(GONE);
			}
			if (isInEditMode()) {
				mViewTarget.setVisibility(VISIBLE);
			}
		}
		
		if (mStatus == STATUS_REFRESH_READY) {
			return;
		}
		notifyRefreshReady();
	}
	
	/**
	 * 切换状态显示/隐藏
	 *
	 * @param isShowStatus true显示 false隐藏
	 */
	private void toggleStatus(boolean isShowStatus) {
		if (mViewStatus != null) {
			if (mStatusMode == STATUS_MODE_SHOW) {
				if (isShowStatus) {
					mViewStatus.setVisibility(VISIBLE);
				} else {
					mViewStatus.setVisibility(GONE);
				}
			} else if (mStatusMode == STATUS_MODE_HIDE) {
				mViewStatus.setVisibility(GONE);
			} else if (mStatusMode == STATUS_MODE_AUTO) {
				if (!isShowStatus && mViewStatus.getVisibility() == VISIBLE) {
					mViewStatus.setVisibility(GONE);
				}
			}
		}
		
		if (mViewTarget != null) {
			if (mStatusMode == STATUS_MODE_SHOW) {
				if (isShowStatus) {
					mViewTarget.setVisibility(GONE);
				} else {
					mViewTarget.setVisibility(VISIBLE);
				}
			} else if (mStatusMode == STATUS_MODE_HIDE) {
				mViewTarget.setVisibility(VISIBLE);
			} else if (mStatusMode == STATUS_MODE_AUTO) {
				if (!isShowStatus && mViewTarget.getVisibility() == GONE) {
					mViewTarget.setVisibility(VISIBLE);
				}
			}
		}
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
		if (mViewTarget == null) {
			ensureTarget();
		}
		if (mViewTarget == null) {
			return;
		}
		
		measureHeader(widthMeasureSpec, heightMeasureSpec);
		measureStatus(widthMeasureSpec, heightMeasureSpec);
		measureTarget(widthMeasureSpec, heightMeasureSpec);
	}
	
	/**
	 * 计算顶部视图
	 *
	 * @param widthMeasureSpec  宽度配置
	 * @param heightMeasureSpec 高度配置
	 */
	private void measureHeader(int widthMeasureSpec, int heightMeasureSpec) {
		if (mViewHeader == null) {
			return;
		}
		
		MarginLayoutParams lp = (MarginLayoutParams) mViewHeader.getLayoutParams();
		int childWidthMeasureSpec = getChildMeasureSpec(
				widthMeasureSpec,
				getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin,
				lp.width
		);
		int childHeightMeasureSpec = getChildMeasureSpec(
				heightMeasureSpec,
				getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin,
				lp.height
		);
		mViewHeader.measure(childWidthMeasureSpec, childHeightMeasureSpec);
	}
	
	/**
	 * 计算状态视图
	 *
	 * @param widthMeasureSpec  宽度配置
	 * @param heightMeasureSpec 高度配置
	 */
	private void measureStatus(int widthMeasureSpec, int heightMeasureSpec) {
		if (mViewStatus == null) {
			return;
		}
		
		MarginLayoutParams lp = (MarginLayoutParams) mViewStatus.getLayoutParams();
		int childWidthMeasureSpec = getChildMeasureSpec(
				widthMeasureSpec,
				getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin,
				lp.width
		);
		int childHeightMeasureSpec = getChildMeasureSpec(
				heightMeasureSpec,
				getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin,
				lp.height
		);
		mViewStatus.measure(childWidthMeasureSpec, childHeightMeasureSpec);
	}
	
	/**
	 * 计算对象视图
	 *
	 * @param widthMeasureSpec  宽度配置
	 * @param heightMeasureSpec 高度配置
	 */
	private void measureTarget(int widthMeasureSpec, int heightMeasureSpec) {
		MarginLayoutParams lp = (MarginLayoutParams) mViewTarget.getLayoutParams();
		int childWidthMeasureSpec = getChildMeasureSpec(
				widthMeasureSpec,
				getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin,
				lp.width
		);
		int childHeightMeasureSpec = getChildMeasureSpec(
				heightMeasureSpec,
				getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin,
				lp.height
		);
		mViewTarget.measure(childWidthMeasureSpec, childHeightMeasureSpec);
	}
	
	@Override
	protected boolean checkLayoutParams(LayoutParams p) {
		return p instanceof MarginLayoutParams;
	}
	
	@Override
	protected MarginLayoutParams generateDefaultLayoutParams() {
		return new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
	}
	
	@Override
	protected MarginLayoutParams generateLayoutParams(LayoutParams p) {
		return new MarginLayoutParams(p);
	}
	
	@Override
	public MarginLayoutParams generateLayoutParams(AttributeSet attrs) {
		return new MarginLayoutParams(getContext(), attrs);
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (mViewTarget == null) {
			ensureTarget();
		}
		if (mViewTarget == null) {
			return;
		}
		
		layoutHeader();
		layoutStatus();
		layoutTarget();
	}
	
	/**
	 * 布局顶部视图
	 */
	private void layoutHeader() {
		if (mViewHeader == null) {
			return;
		}
		
		MarginLayoutParams lp = (MarginLayoutParams) mViewHeader.getLayoutParams();
		mTempRect.left = getPaddingLeft() + lp.leftMargin;
		mTempRect.top = getPaddingTop() + mCurrentPosition - lp.bottomMargin - mViewHeader.getMeasuredHeight();
		mTempRect.right = mTempRect.left + mViewHeader.getMeasuredWidth();
		mTempRect.bottom = mTempRect.top + mViewHeader.getMeasuredHeight();
		
		mViewHeader.layout(mTempRect.left, mTempRect.top, mTempRect.right, mTempRect.bottom);
	}
	
	/**
	 * 布局状态视图
	 */
	private void layoutStatus() {
		if (mViewStatus == null) {
			return;
		}
		
		MarginLayoutParams lp = (MarginLayoutParams) mViewStatus.getLayoutParams();
		mTempRect.left = getPaddingLeft() + lp.leftMargin;
		mTempRect.top = getPaddingTop() + mCurrentPosition + lp.topMargin;
		mTempRect.right = mTempRect.left + mViewStatus.getMeasuredWidth();
		mTempRect.bottom = mTempRect.top + mViewStatus.getMeasuredHeight();
		
		mViewStatus.layout(mTempRect.left, mTempRect.top, mTempRect.right, mTempRect.bottom);
	}
	
	/**
	 * 布局对象视图
	 */
	private void layoutTarget() {
		MarginLayoutParams lp = (MarginLayoutParams) mViewTarget.getLayoutParams();
		mTempRect.left = getPaddingLeft() + lp.leftMargin;
		mTempRect.top = getPaddingTop() + mCurrentPosition + lp.topMargin;
		mTempRect.right = mTempRect.left + mViewTarget.getMeasuredWidth();
		mTempRect.bottom = mTempRect.top + mViewTarget.getMeasuredHeight();
		
		mViewTarget.layout(mTempRect.left, mTempRect.top, mTempRect.right, mTempRect.bottom);
	}
	
	@Override
	public boolean hasNestedScrollingParent() {
		return mNestedScrollingChildHelper.hasNestedScrollingParent();
	}
	
	@Override
	public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
		return false;
	}
	
	@Override
	public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
		return false;
	}
	
	@Override
	public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
		return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
	}
	
	@Override
	public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
		return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
	}
	
	private class SmoothScroller extends Animation {
		private int mFromPosition;
		private int mToPosition;
		
		SmoothScroller() {
			setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {
				}
				
				@Override
				public void onAnimationEnd(Animation animation) {
					finish();
				}
				
				@Override
				public void onAnimationRepeat(Animation animation) {
				}
			});
		}
		
		void scrollToStart() {
			if (mRefreshHeader == null) {
				return;
			}
			
			int refreshOffsetPosition = mRefreshHeader.getRefreshOffsetPosition();
			
			mFromPosition = mCurrentPosition;
			mToPosition = 0;
			if (mFromPosition == mToPosition) {
				finish();
				return;
			}
			if (refreshOffsetPosition == 0) {
				setDuration(400);
			} else {
				setDuration((long) (1.0 * mFromPosition / refreshOffsetPosition * 400));
			}
			clearAnimation();
			startAnimation(this);
		}
		
		void scrollToRefresh() {
			if (mRefreshHeader == null) {
				return;
			}
			
			int refreshOffsetPosition = mRefreshHeader.getRefreshOffsetPosition();
			
			mFromPosition = mCurrentPosition;
			mToPosition = refreshOffsetPosition;
			if (mToPosition == 0) {
				mToPosition = -1;
			}
			
			if (mFromPosition == mToPosition) {
				finish();
				return;
			}
			if (refreshOffsetPosition == 0 || refreshOffsetPosition >= mViewHeader.getMeasuredHeight()) {
				setDuration(400);
			} else if (mFromPosition > mToPosition) {
				setDuration((long) (1.0 * (mFromPosition - mToPosition) / (mViewHeader.getMeasuredWidth() - refreshOffsetPosition) * 400));
			} else {
				setDuration((long) (1.0 * (mToPosition - mFromPosition) / (refreshOffsetPosition - mFromPosition) * 400));
			}
			
			clearAnimation();
			startAnimation(this);
		}
		
		void finish() {
		}
		
		void abort() {
			cancel();
		}
		
		boolean isFinished() {
			return !hasStarted() || hasEnded();
		}
		
		boolean isCanceled() {
			return getStartTime() == Long.MIN_VALUE;
		}
		
		@Override
		protected void applyTransformation(float interpolatedTime, Transformation t) {
			if (mToPosition < 0) {
				mToPosition = mRefreshHeader.getRefreshOffsetPosition();
			}
			int durationPosition = mToPosition - mFromPosition;
			offsetPosition((int) (interpolatedTime * durationPosition) + mFromPosition);
		}
	}
	
	/**
	 * 刷新监听器
	 */
	public static class OnRefreshListener {
		private RefreshLayout refreshLayout;
		
		/**
		 * 调用刷新功能
		 */
		public final void onRefresh() {
			if (this.refreshLayout == null) {
				return;
			}
			if (this.refreshLayout.mStatus == STATUS_REFRESH_READY
					|| this.refreshLayout.mStatus == STATUS_REFRESH_COMPLETE) {
				this.refreshLayout.notifyRefresh();
			}
		}
	}
	
	/**
	 * 偏移比例监听器
	 */
	public interface OnScaleListener {
		
		/**
		 * 拖动偏移比例
		 *
		 * @param scale 偏移比例
		 */
		void onScale(float scale);
	}
}
