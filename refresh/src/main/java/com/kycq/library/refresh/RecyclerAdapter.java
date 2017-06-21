package com.kycq.library.refresh;

import android.support.annotation.IntDef;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;
import android.view.ViewGroup;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public abstract class RecyclerAdapter<StatusInfo> {
	/** 刷新准备 */
	private static final int REFRESH_READY = 1;
	/** 刷新中 */
	private static final int REFRESH = 2;
	/** 刷新结束 */
	private static final int REFRESH_COMPLETE = 3;
	/** 加载准备 */
	private static final int LOAD_READY = 4;
	/** 加载中 */
	private static final int LOADING = 5;
	/** 加载结束 */
	private static final int LOAD_COMPLETE = 6;
	
	@Retention(RetentionPolicy.SOURCE)
	@IntDef({REFRESH_READY,
			REFRESH,
			REFRESH_COMPLETE,
			LOAD_READY,
			LOADING,
			LOAD_COMPLETE})
	private @interface Status {
	}
	
	/** 状态自动判断 */
	public static final int LOAD_MODE_AUTO = 0;
	/** 状态总是显示 */
	public static final int LOAD_MODE_SHOW = 1;
	/** 状态总是隐藏 */
	public static final int LOAD_MODE_HIDE = 2;
	
	@Retention(RetentionPolicy.SOURCE)
	@IntDef({LOAD_MODE_AUTO,
			LOAD_MODE_SHOW,
			LOAD_MODE_HIDE})
	private @interface LoadMode {
	}
	
	/** 刷新控件 */
	private RefreshLayout mRefreshLayout;
	/** 列表控件 */
	private RecyclerView mRecyclerView;
	/** 兼容适配器 */
	private WrapRecyclerAdapter mWrapRecyclerAdapter;
	/** 列表控件滚动监听器 */
	private OnRecyclerScrollListener mOnRecyclerScrollListener;
	
	/** 刷新状态 */
	@Status
	private int mStatus = REFRESH_READY;
	/** 刷新项持有者 */
	private RefreshHolder<StatusInfo> mRefreshHolder;
	/** 加载项持有者 */
	private LoadHolder<StatusInfo> mLoadHolder;
	/** 状态信息 */
	private StatusInfo mStatusInfo;
	/** 自动加载 */
	private boolean mAutoLoad;
	/** 加载模式 */
	@LoadMode
	private int mLoadMode = LOAD_MODE_AUTO;
	
	/** 任务监听器 */
	private OnTaskListener<Object> mOnTaskListener;
	/** 当前任务 */
	private Object mTask;
	
	/** 列表点击监听器 */
	private OnItemClickListener mOnItemClickListener;
	/** 列表长按监听器 */
	private OnItemLongClickListener mOnItemLongClickListener;
	
	/**
	 * 配置刷新控件
	 *
	 * @param refreshLayout 刷新控件
	 */
	public void setRefreshLayout(RefreshLayout refreshLayout) {
		if (mRefreshLayout != null) {
			mRefreshLayout.setOnTaskListener(null);
		}
		mRefreshLayout = refreshLayout;
		if (mRefreshLayout == null) {
			return;
		}
		mRefreshLayout.setOnTaskListener(new OnRecyclerTaskListener(this));
	}
	
	/**
	 * 配置列表控件
	 *
	 * @param recyclerView 列表控件
	 */
	public void setRecyclerView(RecyclerView recyclerView) {
		if (mRecyclerView != null) {
			mRecyclerView.setAdapter(null);
			mRecyclerView.removeOnScrollListener(mOnRecyclerScrollListener);
		}
		mRecyclerView = recyclerView;
		if (mRecyclerView == null) {
			return;
		}
		if (mWrapRecyclerAdapter == null) {
			mWrapRecyclerAdapter = new WrapRecyclerAdapter(this);
		}
		if (mOnRecyclerScrollListener == null) {
			mOnRecyclerScrollListener = new OnRecyclerScrollListener(this);
		}
		mRecyclerView.setAdapter(mWrapRecyclerAdapter);
		mRecyclerView.addOnScrollListener(mOnRecyclerScrollListener);
		
		mRefreshHolder = createRefreshHolder(mRecyclerView);
		if (mRefreshHolder == null) {
			throw new NullPointerException("must return RefreshHolder at onCreateRefreshHolder()");
		}
		mLoadHolder = createLoadHolder(mRecyclerView);
		if (mLoadHolder == null) {
			throw new NullPointerException("must return LoadHolder at onCreateLoadHolder()");
		}
	}
	
	/**
	 * 设置加载模式
	 *
	 * @param loadMode 加载模式
	 */
	public final void setLoadMode(@LoadMode int loadMode) {
		mLoadMode = loadMode;
		notifyDataSetChanged();
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
	 * 设置列表点击监听器
	 *
	 * @param listener 列表点击监听器
	 */
	public void setOnItemClickListener(OnItemClickListener listener) {
		mOnItemClickListener = listener;
	}
	
	/**
	 * 设置列表长按监听器
	 *
	 * @param listener 列表长按监听器
	 */
	public void setOnItemLongClickListener(OnItemLongClickListener listener) {
		mOnItemLongClickListener = listener;
	}
	
	/**
	 * 变更刷新准备状态
	 */
	public final void swipeRefreshReady() {
		if (mRefreshLayout != null) {
			mRefreshLayout.swipeRefreshReady();
		}
		notifyRefreshReady();
	}
	
	/**
	 * 通知刷新准备状态
	 */
	protected void notifyRefreshReady() {
		mStatus = REFRESH_READY;
		if (mOnTaskListener != null && mTask != null) {
			mOnTaskListener.onCancel(mTask);
		}
		
		mRefreshHolder.onRefreshReady();
		
		notifyDataSetChanged();
	}
	
	/**
	 * 变更刷新中状态
	 */
	public final void swipeRefresh() {
		if (mRefreshLayout != null) {
			mRefreshLayout.swipeRefresh();
		} else {
			notifyRefresh();
		}
	}
	
	/**
	 * 通知刷新中状态
	 */
	protected void notifyRefresh() {
		int oldWrapItemCount = mWrapRecyclerAdapter.getItemCount();
		
		mStatus = REFRESH;
		if (mOnTaskListener != null) {
			if (mTask != null) {
				mOnTaskListener.onCancel(mTask);
			}
			mTask = mOnTaskListener.onTask();
		}
		
		mRefreshHolder.onRefresh();
		
		int newWrapItemCount = mWrapRecyclerAdapter.getItemCount();
		if (oldWrapItemCount == newWrapItemCount + 1) {
			notifyItemRemoved(newWrapItemCount);
		}
	}
	
	/**
	 * 变更加载准备状态
	 */
	public final void swipeLoadReady() {
		swipeLoadReady(true);
	}
	
	/**
	 * 变更加载准备状态
	 *
	 * @param autoLoad true自动加载
	 */
	public final void swipeLoadReady(boolean autoLoad) {
		mAutoLoad = autoLoad;
		notifyLoadReady();
	}
	
	/**
	 * 通知加载准备状态
	 */
	private void notifyLoadReady() {
		int oldWrapItemCount = mWrapRecyclerAdapter.getItemCount();
		
		mStatus = LOAD_READY;
		if (mOnTaskListener != null && mTask != null) {
			mOnTaskListener.onCancel(mTask);
		}
		
		mLoadHolder.onLoadReady();
		
		int newWrapItemCount = mWrapRecyclerAdapter.getItemCount();
		if (oldWrapItemCount != newWrapItemCount) {
			notifyItemInserted(oldWrapItemCount);
		}
	}
	
	/**
	 * 变更加载中状态
	 *
	 * @param reload true重新加载
	 */
	private void swipeLoading(boolean reload) {
		if (!reload && mStatus != LOAD_READY) {
			return;
		}
		notifyLoading();
	}
	
	/**
	 * 通知加载中状态
	 */
	private void notifyLoading() {
		int oldWrapItemCount = mWrapRecyclerAdapter.getItemCount();
		
		mStatus = LOADING;
		if (mOnTaskListener != null) {
			if (mTask != null) {
				mOnTaskListener.onCancel(mTask);
			}
			mTask = mOnTaskListener.onTask();
		}
		
		mLoadHolder.onLoading();
		
		int newWrapItemCount = mWrapRecyclerAdapter.getItemCount();
		if (oldWrapItemCount != newWrapItemCount) {
			notifyItemInserted(oldWrapItemCount);
		}
	}
	
	/**
	 * 变更刷新结束/加载结束状态
	 *
	 * @param statusInfo 状态信息
	 */
	public final void swipeComplete(StatusInfo statusInfo) {
		if (mRefreshLayout != null) {
			mRefreshLayout.swipeComplete(statusInfo);
		}
		if (mStatus == REFRESH_READY || mStatus == REFRESH || mStatus == REFRESH_COMPLETE) {
			notifyRefreshComplete(statusInfo);
		} else if (mStatus == LOADING || mStatus == LOAD_READY || mStatus == LOAD_COMPLETE) {
			notifyLoadComplete(statusInfo);
		} else {
			notifyDataSetChanged();
		}
	}
	
	/**
	 * 通知刷新结束状态
	 *
	 * @param statusInfo 状态信息
	 */
	protected void notifyRefreshComplete(StatusInfo statusInfo) {
		int itemCount = getItemCount();
		int oldWrapItemCount = mWrapRecyclerAdapter.getItemCount();
		
		mStatusInfo = statusInfo;
		mStatus = REFRESH_COMPLETE;
		if (mOnTaskListener != null && mTask != null) {
			mOnTaskListener.onCancel(mTask);
		}
		
		mRefreshHolder.onRefreshComplete(statusInfo);
		mLoadHolder.onLoadComplete(statusInfo);
		
		int newWrapItemCount = mWrapRecyclerAdapter.getItemCount();
		if (itemCount == 0) {
			if (newWrapItemCount == 1) {
				notifyItemChanged(0);
			} else if (newWrapItemCount == 0) {
				notifyItemInserted(0);
			}
		} else {
			if (oldWrapItemCount + 1 == newWrapItemCount) {
				notifyItemInserted(oldWrapItemCount);
			}
		}
	}
	
	/**
	 * 通知加载结束状态
	 *
	 * @param statusInfo 状态信息
	 */
	protected void notifyLoadComplete(StatusInfo statusInfo) {
		int oldWrapItemCount = mWrapRecyclerAdapter.getItemCount();
		
		mStatusInfo = statusInfo;
		mStatus = LOAD_COMPLETE;
		if (mOnTaskListener != null && mTask != null) {
			mOnTaskListener.onCancel(mTask);
		}
		
		mLoadHolder.onLoadComplete(statusInfo);
		
		int newWrapItemCount = mWrapRecyclerAdapter.getItemCount();
		if (oldWrapItemCount != newWrapItemCount) {
			notifyItemInserted(oldWrapItemCount);
		}
	}
	
	/**
	 * 获取列表项数量
	 *
	 * @return 列表项数量
	 */
	public abstract int getItemCount();
	
	/**
	 * 获取列表项类型
	 *
	 * @param position 列表项位置
	 * @return 列表项类型
	 */
	public int getItemType(int position) {
		return 0;
	}
	
	/**
	 * 获取列表项ID
	 *
	 * @param position 列表项位置
	 * @return 列表项ID
	 */
	public long getItemId(int position) {
		return View.NO_ID;
	}
	
	/**
	 * 创建列表项
	 *
	 * @param viewType 列表项类型
	 * @return 列表项
	 */
	public abstract RecyclerHolder onCreateItemHolder(int viewType);
	
	/**
	 * 创建刷新列表项
	 *
	 * @param parent 父控件
	 * @return 刷新列表项
	 */
	private RefreshHolder<StatusInfo> createRefreshHolder(ViewGroup parent) {
		RefreshHolder<StatusInfo> refreshHolder = onCreateRefreshHolder();
		refreshHolder.wrapHolder = new WrapHolder(refreshHolder.onCreateView(parent));
		refreshHolder.wrapHolder.itemHolder = refreshHolder;
		refreshHolder.wrapHolder.itemView.setOnClickListener(refreshHolder);
		refreshHolder.onViewCreated(refreshHolder.wrapHolder.itemView);
		return refreshHolder;
	}
	
	/**
	 * 创建刷新列表项
	 *
	 * @return 刷新列表项
	 */
	public abstract RefreshHolder<StatusInfo> onCreateRefreshHolder();
	
	/**
	 * 创建加载列表项
	 *
	 * @param parent 父控件
	 * @return 加载列表项
	 */
	private LoadHolder<StatusInfo> createLoadHolder(ViewGroup parent) {
		LoadHolder<StatusInfo> loadHolder = onCreateLoadHolder();
		loadHolder.wrapHolder = new WrapHolder(loadHolder.onCreateView(parent));
		loadHolder.wrapHolder.itemHolder = loadHolder;
		loadHolder.wrapHolder.itemView.setOnClickListener(loadHolder);
		loadHolder.onViewCreated(loadHolder.wrapHolder.itemView);
		return loadHolder;
	}
	
	/**
	 * 创建加载列表项
	 *
	 * @return 加载列表项
	 */
	public abstract LoadHolder<StatusInfo> onCreateLoadHolder();
	
	public void onViewRecycled(RecyclerHolder recyclerHolder) {
	}
	
	public boolean onFailedToRecycleView(RecyclerHolder recyclerHolder) {
		return false;
	}
	
	public void onViewAttachedToWindow(RecyclerHolder recyclerHolder) {
	}
	
	public void onViewDetachedFromWindow(RecyclerHolder recyclerHolder) {
	}
	
	public void onAttachedToRecyclerView(RecyclerView recyclerView) {
	}
	
	public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
	}
	
	public void setHasStableIds(boolean hasStableIds) {
		mWrapRecyclerAdapter.setHasStableIds(hasStableIds);
	}
	
	public void registerAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
		mWrapRecyclerAdapter.registerAdapterDataObserver(observer);
	}
	
	public void unregisterAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
		mWrapRecyclerAdapter.unregisterAdapterDataObserver(observer);
	}
	
	public final void notifyDataSetChanged() {
		mWrapRecyclerAdapter.notifyDataSetChanged();
	}
	
	public final void notifyItemChanged(int position) {
		mWrapRecyclerAdapter.notifyItemRangeChanged(position, 1);
	}
	
	public final void notifyItemChanged(int position, Object payload) {
		mWrapRecyclerAdapter.notifyItemRangeChanged(position, 1, payload);
	}
	
	public final void notifyItemRangeChanged(int positionStart, int itemCount) {
		mWrapRecyclerAdapter.notifyItemRangeChanged(positionStart, itemCount);
	}
	
	public final void notifyItemRangeChanged(int positionStart, int itemCount, Object payload) {
		mWrapRecyclerAdapter.notifyItemRangeChanged(positionStart, itemCount, payload);
	}
	
	public final void notifyItemInserted(int position) {
		mWrapRecyclerAdapter.notifyItemRangeInserted(position, 1);
	}
	
	public final void notifyItemMoved(int fromPosition, int toPosition) {
		mWrapRecyclerAdapter.notifyItemMoved(fromPosition, toPosition);
	}
	
	public final void notifyItemRangeInserted(int positionStart, int itemCount) {
		mWrapRecyclerAdapter.notifyItemRangeInserted(positionStart, itemCount);
	}
	
	public final void notifyItemRemoved(int position) {
		mWrapRecyclerAdapter.notifyItemRangeRemoved(position, 1);
	}
	
	public final void notifyItemRangeRemoved(int positionStart, int itemCount) {
		mWrapRecyclerAdapter.notifyItemRangeRemoved(positionStart, itemCount);
	}
	
	/**
	 * 列表项持有者
	 */
	public static abstract class ItemHolder extends RecyclerHolder
			implements View.OnClickListener, View.OnLongClickListener {
		
		/**
		 * 绑定数据
		 *
		 * @param position 列表项位置
		 */
		protected abstract void onBindView(int position);
		
		@Override
		public void onClick(View v) {
			if (recyclerAdapter.mOnItemClickListener != null) {
				recyclerAdapter.mOnItemClickListener.onItemClick(this);
			}
		}
		
		@Override
		public boolean onLongClick(View v) {
			return recyclerAdapter.mOnItemLongClickListener != null
					&& recyclerAdapter.mOnItemLongClickListener.onItemLongClick(this);
		}
	}
	
	/**
	 * 无点击事件列表项持有者
	 */
	public static abstract class DisableItemHolder extends ItemHolder {
		
	}
	
	/**
	 * 刷新项持有者
	 *
	 * @param <StatusInfo> 状态信息类型
	 */
	public static abstract class RefreshHolder<StatusInfo> extends RecyclerHolder
			implements View.OnClickListener {
		
		@Override
		public void onClick(View v) {
			swipeRefresh();
		}
		
		/**
		 * 变更刷新中状态
		 */
		protected void swipeRefresh() {
			recyclerAdapter.swipeRefresh();
		}
		
		/**
		 * 刷新准备状态
		 */
		protected abstract void onRefreshReady();
		
		/**
		 * 刷新中状态
		 */
		protected abstract void onRefresh();
		
		/**
		 * 刷新结束状态
		 *
		 * @param statusInfo 状态信息
		 */
		protected abstract void onRefreshComplete(StatusInfo statusInfo);
	}
	
	/**
	 * 加载项持有者
	 *
	 * @param <StatusInfo> 状态信息类型
	 */
	public static abstract class LoadHolder<StatusInfo> extends RecyclerHolder
			implements View.OnClickListener {
		private boolean mReload;
		
		@Override
		public void onClick(View v) {
			swipeLoading();
		}
		
		/**
		 * 变更加载中状态
		 */
		protected final void swipeLoading() {
			recyclerAdapter.swipeLoading(mReload);
		}
		
		/**
		 * 加载准备状态
		 */
		protected abstract void onLoadReady();
		
		/**
		 * 加载中状态
		 */
		protected abstract void onLoading();
		
		/**
		 * 加载结束状态
		 *
		 * @param statusInfo 状态信息
		 * @return true重新加载
		 */
		protected abstract boolean onLoadComplete(StatusInfo statusInfo);
	}
	
	/**
	 * 列表项持有者
	 */
	public static abstract class RecyclerHolder {
		/** 列表适配器 */
		RecyclerAdapter recyclerAdapter;
		/** 兼容列表项持有者 */
		WrapHolder wrapHolder;
		
		/**
		 * 创建列表项控件
		 *
		 * @param parent 父控件
		 * @return 列表项控件
		 */
		protected abstract View onCreateView(ViewGroup parent);
		
		/**
		 * 创建列表项控件
		 *
		 * @param view 列表项控件
		 */
		protected void onViewCreated(View view) {
		}
		
		/**
		 * 获取列表项类型
		 *
		 * @return 列表项类型
		 */
		public int getItemViewType() {
			return wrapHolder.getItemViewType();
		}
		
		/**
		 * 获取列表项ID
		 *
		 * @return 列表项ID
		 */
		public long getItemId() {
			return wrapHolder.getItemId();
		}
		
		/**
		 * 获取布局位置
		 *
		 * @return 布局位置
		 */
		public int getLayoutPosition() {
			return wrapHolder.getLayoutPosition();
		}
		
		/**
		 * 获取列表位置
		 *
		 * @return 列表位置
		 */
		public int getAdapterPosition() {
			return wrapHolder.getAdapterPosition();
		}
		
		/**
		 * 获取原列表位置
		 *
		 * @return 原列表位置
		 */
		public int getOldPosition() {
			return wrapHolder.getOldPosition();
		}
		
		/**
		 * 判断是否可回收
		 *
		 * @return true可回收
		 */
		public boolean isRecyclable() {
			return wrapHolder.isRecyclable();
		}
		
		/**
		 * 配置是否可回收
		 *
		 * @param recyclable true可回收
		 */
		public void setIsRecyclable(boolean recyclable) {
			wrapHolder.setIsRecyclable(recyclable);
		}
		
		/**
		 * 列表添加到视图
		 */
		protected void onViewAttachedToWindow() {
		}
		
		/**
		 * 列表移除出视图
		 */
		protected void onViewDetachedFromWindow() {
		}
	}
	
	/**
	 * 列表点击事件监听器
	 */
	public interface OnItemClickListener {
		
		/**
		 * 点击事件监听
		 *
		 * @param itemHolder 列表项持有者
		 */
		void onItemClick(ItemHolder itemHolder);
	}
	
	/**
	 * 列表长按事件监听器
	 */
	public interface OnItemLongClickListener {
		
		/**
		 * 长按事件监听
		 *
		 * @param itemHolder 列表项持有者
		 * @return true已处理
		 */
		boolean onItemLongClick(ItemHolder itemHolder);
	}
	
	/**
	 * 刷新控件任务监听器
	 */
	private static class OnRecyclerTaskListener extends OnTaskListener<Object> {
		/** 列表适配器 */
		private RecyclerAdapter recyclerAdapter;
		
		/**
		 * 构造方法
		 *
		 * @param recyclerAdapter 列表适配器
		 */
		private OnRecyclerTaskListener(RecyclerAdapter recyclerAdapter) {
			this.recyclerAdapter = recyclerAdapter;
		}
		
		@Override
		public Object onTask() {
			this.recyclerAdapter.notifyRefresh();
			return null;
		}
		
		@Override
		public void onCancel(Object task) {
		}
	}
	
	/**
	 * 列表控件滚动监听器
	 */
	private static class OnRecyclerScrollListener extends RecyclerView.OnScrollListener {
		/** 列表适配器 */
		private RecyclerAdapter recyclerAdapter;
		/** 滚动列表位置 */
		private int[] scrollPositions;
		
		/**
		 * 构造方法
		 *
		 * @param recyclerAdapter 列表适配器
		 */
		private OnRecyclerScrollListener(RecyclerAdapter recyclerAdapter) {
			this.recyclerAdapter = recyclerAdapter;
		}
		
		@Override
		public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
			checkFooter(recyclerView);
		}
		
		@Override
		public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
			checkFooter(recyclerView);
		}
		
		private boolean checkHeader(RecyclerView recyclerView) {
			if (!recyclerAdapter.mAutoLoad || recyclerAdapter.mStatus != LOAD_READY) {
				return false;
			}
			
			int firstVisibleItemPosition;
			RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
			if (manager instanceof LinearLayoutManager) {
				firstVisibleItemPosition = ((LinearLayoutManager) manager).findFirstVisibleItemPosition();
			} else if (manager instanceof StaggeredGridLayoutManager) {
				StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) manager;
				int spanCount = staggeredGridLayoutManager.getSpanCount();
				if (spanCount != scrollPositions.length) {
					scrollPositions = new int[spanCount];
				}
				staggeredGridLayoutManager.findFirstVisibleItemPositions(scrollPositions);
				firstVisibleItemPosition = findMinPosition(scrollPositions);
			} else {
				throw new RuntimeException("Unsupported LayoutManager used. Valid ones are LinearLayoutManager, GridLayoutManager and StaggeredGridLayoutManager");
			}
			
			if (firstVisibleItemPosition == 0) {
				recyclerView.removeCallbacks(mLoadingDelay);
				recyclerView.post(mLoadingDelay);
				return true;
			}
			
			return false;
		}
		
		private int findMinPosition(int[] scrollPositions) {
			int minPosition = scrollPositions[0];
			for (int valuePosition : scrollPositions) {
				if (valuePosition > minPosition) {
					minPosition = valuePosition;
				}
			}
			return minPosition;
		}
		
		private boolean checkFooter(RecyclerView recyclerView) {
			if (!recyclerAdapter.mAutoLoad || recyclerAdapter.mStatus != LOAD_READY) {
				return false;
			}
			
			int lastVisibleItemPosition;
			RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
			if (manager instanceof LinearLayoutManager) {
				lastVisibleItemPosition = ((LinearLayoutManager) manager).findLastVisibleItemPosition();
			} else if (manager instanceof StaggeredGridLayoutManager) {
				StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) manager;
				int spanCount = staggeredGridLayoutManager.getSpanCount();
				if (spanCount != scrollPositions.length) {
					scrollPositions = new int[spanCount];
				}
				staggeredGridLayoutManager.findLastVisibleItemPositions(scrollPositions);
				lastVisibleItemPosition = findMaxPosition(scrollPositions);
			} else {
				throw new RuntimeException("Unsupported LayoutManager used. Valid ones are LinearLayoutManager, GridLayoutManager and StaggeredGridLayoutManager");
			}
			
			int visibleItemCount = manager.getChildCount();
			int totalItemCount = manager.getItemCount();
			if (visibleItemCount >= totalItemCount || lastVisibleItemPosition == totalItemCount - 1) {
				recyclerView.removeCallbacks(mLoadingDelay);
				recyclerView.post(mLoadingDelay);
				return true;
			}
			
			return false;
		}
		
		private int findMaxPosition(int[] scrollPositions) {
			int maxPosition = scrollPositions[0];
			for (int valuePosition : scrollPositions) {
				if (valuePosition > maxPosition) {
					maxPosition = valuePosition;
				}
			}
			return maxPosition;
		}
		
		private Runnable mLoadingDelay = new Runnable() {
			@Override
			public void run() {
				recyclerAdapter.swipeLoading(false);
			}
		};
	}
	
	/**
	 * 兼容列表适配器
	 */
	private static class WrapRecyclerAdapter extends RecyclerView.Adapter<WrapHolder> {
		/** 刷新类别 */
		static final int TYPE_REFRESH = Integer.MAX_VALUE;
		/** 加载类别 */
		static final int TYPE_LOAD = Integer.MIN_VALUE;
		
		private GridLayoutManager.SpanSizeLookup mSpanSizeLookup;
		
		/** 列表适配器 */
		private RecyclerAdapter mRecyclerAdapter;
		
		/**
		 * 构造方法
		 *
		 * @param recyclerAdapter 列表适配器
		 */
		WrapRecyclerAdapter(RecyclerAdapter recyclerAdapter) {
			mRecyclerAdapter = recyclerAdapter;
		}
		
		/**
		 * 获取列表适配器
		 *
		 * @return 列表适配器
		 */
		public final <T> T getRecyclerAdapter() {
			// noinspection unchecked
			return (T) mRecyclerAdapter;
		}
		
		@Override
		public int getItemCount() {
			if (mRecyclerAdapter == null) {
				return 0;
			}
			int itemCount = mRecyclerAdapter.getItemCount();
			if (mRecyclerAdapter.mStatus == REFRESH_READY
					|| (mRecyclerAdapter.mStatus == REFRESH && itemCount == 0)
					|| (mRecyclerAdapter.mStatus == REFRESH_COMPLETE && itemCount == 0)) {
				return 1;
			}
			if ((mRecyclerAdapter.mStatus == REFRESH_COMPLETE && mRecyclerAdapter.mLoadMode == LOAD_MODE_SHOW)
					|| mRecyclerAdapter.mStatus == LOAD_READY
					|| mRecyclerAdapter.mStatus == LOADING
					|| (mRecyclerAdapter.mStatus == LOAD_COMPLETE && mRecyclerAdapter.mLoadMode != LOAD_MODE_HIDE)) {
				itemCount++;
			}
			return itemCount;
		}
		
		@Override
		public int getItemViewType(int position) {
			int itemCount = mRecyclerAdapter.getItemCount();
			if (mRecyclerAdapter.mStatus == REFRESH_READY
					|| (mRecyclerAdapter.mStatus == REFRESH && itemCount == 0)
					|| (mRecyclerAdapter.mStatus == REFRESH_COMPLETE && itemCount == 0)) {
				return TYPE_REFRESH;
			}
			if ((mRecyclerAdapter.mStatus == REFRESH_COMPLETE
					|| mRecyclerAdapter.mStatus == LOAD_READY
					|| mRecyclerAdapter.mStatus == LOADING
					|| mRecyclerAdapter.mStatus == LOAD_COMPLETE)
					&& position == itemCount) {
				return TYPE_LOAD;
			}
			return mRecyclerAdapter.getItemType(position);
		}
		
		@Override
		public long getItemId(int position) {
			int viewType = getItemViewType(position);
			if (viewType == TYPE_REFRESH || viewType == TYPE_LOAD) {
				return View.NO_ID;
			}
			return mRecyclerAdapter.getItemId(position);
		}
		
		@Override
		public WrapHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			if (viewType == TYPE_REFRESH) {
				return mRecyclerAdapter.mRefreshHolder.wrapHolder;
			} else if (viewType == TYPE_LOAD) {
				return mRecyclerAdapter.mLoadHolder.wrapHolder;
			} else {
				RecyclerHolder recyclerHolder = mRecyclerAdapter.onCreateItemHolder(viewType);
				if (recyclerHolder == null) {
					throw new NullPointerException("must return RecyclerHolder at onCreateItemHolder(viewType)");
				}
				recyclerHolder.wrapHolder = new WrapHolder(recyclerHolder.onCreateView(parent));
				recyclerHolder.wrapHolder.itemHolder = recyclerHolder;
				recyclerHolder.onViewCreated(recyclerHolder.wrapHolder.itemView);
				return recyclerHolder.wrapHolder;
			}
		}
		
		@Override
		public void onBindViewHolder(WrapHolder holder, int position) {
			if (holder.itemHolder instanceof DisableItemHolder) {
				DisableItemHolder disableItemHolder = (DisableItemHolder) holder.itemHolder;
				disableItemHolder.recyclerAdapter = mRecyclerAdapter;
				disableItemHolder.onBindView(position);
			} else if (holder.itemHolder instanceof ItemHolder) {
				ItemHolder itemHolder = (ItemHolder) holder.itemHolder;
				itemHolder.recyclerAdapter = mRecyclerAdapter;
				holder.itemView.setOnClickListener(itemHolder);
				holder.itemView.setOnLongClickListener(itemHolder);
				itemHolder.onBindView(position);
			} else if (holder.itemHolder instanceof RefreshHolder) {
				RefreshHolder refreshHolder = (RefreshHolder) holder.itemHolder;
				refreshHolder.recyclerAdapter = mRecyclerAdapter;
				if (mRecyclerAdapter.mStatus == REFRESH_READY) {
					// noinspection unchecked
					refreshHolder.onRefreshReady();
				} else if (mRecyclerAdapter.mStatus == REFRESH) {
					refreshHolder.onRefresh();
				} else {
					// noinspection unchecked
					refreshHolder.onRefreshComplete(mRecyclerAdapter.mStatusInfo);
				}
			} else if (holder.itemHolder instanceof LoadHolder) {
				LoadHolder loadHolder = (LoadHolder) holder.itemHolder;
				loadHolder.recyclerAdapter = mRecyclerAdapter;
				if (mRecyclerAdapter.mStatus == LOAD_READY) {
					loadHolder.onLoadReady();
				} else if (mRecyclerAdapter.mStatus == LOADING) {
					loadHolder.onLoading();
				} else {
					// noinspection unchecked
					loadHolder.mReload = loadHolder.onLoadComplete(mRecyclerAdapter.mStatusInfo);
				}
			} else {
				throw new RuntimeException("holder must extends ItemHolder");
			}
		}
		
		@Override
		public void onViewRecycled(WrapHolder holder) {
			mRecyclerAdapter.onViewRecycled(holder.itemHolder);
			holder.itemHolder.recyclerAdapter = null;
		}
		
		@Override
		public boolean onFailedToRecycleView(WrapHolder holder) {
			return mRecyclerAdapter.onFailedToRecycleView(holder.itemHolder);
		}
		
		@Override
		public void onViewAttachedToWindow(WrapHolder holder) {
			mRecyclerAdapter.onViewAttachedToWindow(holder.itemHolder);
			holder.itemHolder.onViewAttachedToWindow();
			
			if (holder.itemHolder instanceof RefreshHolder
					|| holder.itemHolder instanceof LoadHolder) {
				ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
				if (lp != null && lp instanceof StaggeredGridLayoutManager.LayoutParams) {
					((StaggeredGridLayoutManager.LayoutParams) lp).setFullSpan(true);
				}
			}
		}
		
		@Override
		public void onViewDetachedFromWindow(WrapHolder holder) {
			mRecyclerAdapter.onViewDetachedFromWindow(holder.itemHolder);
			holder.itemHolder.onViewDetachedFromWindow();
		}
		
		@Override
		public void onAttachedToRecyclerView(RecyclerView recyclerView) {
			mRecyclerAdapter.onAttachedToRecyclerView(recyclerView);
			
			if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
				final GridLayoutManager manager = ((GridLayoutManager) recyclerView.getLayoutManager());
				final GridLayoutManager.SpanSizeLookup spanSizeLookup = manager.getSpanSizeLookup();
				if (mSpanSizeLookup == null || spanSizeLookup != mSpanSizeLookup) {
					mSpanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
						@Override
						public int getSpanSize(int position) {
							int viewType = getItemViewType(position);
							return viewType == TYPE_REFRESH || viewType == TYPE_LOAD ?
									manager.getSpanCount() : (spanSizeLookup != null ? spanSizeLookup.getSpanSize(position) : 1);
						}
					};
					manager.setSpanSizeLookup(mSpanSizeLookup);
				}
			}
		}
		
		@Override
		public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
			mRecyclerAdapter.onDetachedFromRecyclerView(recyclerView);
			if (mRecyclerAdapter.mOnTaskListener != null
					&& mRecyclerAdapter.mTask != null) {
				// noinspection unchecked
				mRecyclerAdapter.mOnTaskListener.onCancel(mRecyclerAdapter.mTask);
			}
		}
	}
	
	/**
	 * 兼容列表项持有者
	 */
	static class WrapHolder extends RecyclerView.ViewHolder {
		private RecyclerHolder itemHolder;
		
		WrapHolder(View itemView) {
			super(itemView);
		}
	}
	
	/**
	 * 列表项装饰
	 */
	public static abstract class ItemDecoration extends RecyclerView.ItemDecoration {
		
		/**
		 * 判断是否忽略绘制
		 *
		 * @param view   子控件
		 * @param parent 父控件
		 * @param state  列表状态
		 * @return true忽略
		 */
		public boolean isSkipDraw(View view, RecyclerView parent, RecyclerView.State state) {
			if (view.getVisibility() == View.GONE) {
				return true;
			}
			RecyclerView.ViewHolder viewHolder = parent.getChildViewHolder(view);
			if (viewHolder instanceof WrapHolder) {
				RecyclerHolder recyclerHolder = ((WrapHolder) viewHolder).itemHolder;
				return recyclerHolder instanceof RefreshHolder || recyclerHolder instanceof LoadHolder;
			}
			return false;
		}
	}
}
