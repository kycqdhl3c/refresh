package com.kycq.refresh.basic;

import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kycq.library.refresh.RecyclerAdapter;
import com.kycq.refresh.R;
import com.kycq.refresh.bean.BasicBean;
import com.kycq.refresh.bean.StatusInfo;
import com.kycq.refresh.databinding.ItemBasicLoadingListBinding;
import com.kycq.refresh.databinding.ItemBasicRefreshListBinding;

public abstract class BasicAdapter<AdapterInfo> extends RecyclerAdapter<StatusInfo> {
	/** 初始页码 */
	private final int initPage = 1;
	/** 当前页码 */
	private int currentPage = initPage;
	
	protected AdapterInfo adapterInfo;
	
	/**
	 * 重置数据信息并刷新
	 */
	public final void forceRefresh() {
		resetAdapterInfo(null);
		swipeRefresh();
	}
	
	/**
	 * 获取初始页码
	 *
	 * @return 初始页码
	 */
	public final int getInitPage() {
		return this.initPage;
	}
	
	/**
	 * 获取当前页码
	 *
	 * @return 当前页码
	 */
	public final int getCurrentPage() {
		return this.currentPage;
	}
	
	@Override
	protected void notifyRefreshReady() {
		this.currentPage = this.initPage;
		resetAdapterInfo(null);
		super.notifyRefreshReady();
	}
	
	@Override
	protected void notifyRefresh() {
		this.currentPage = this.initPage;
		super.notifyRefresh();
	}
	
	/**
	 * 数据信息处理
	 *
	 * @param adapterInfo 数据信息
	 */
	public void swipeResult(AdapterInfo adapterInfo) {
		StatusInfo statusInfo = null;
		if (adapterInfo != null) {
			if (adapterInfo instanceof BasicBean) {
				statusInfo = ((BasicBean) adapterInfo).statusInfo;
			} else {
				statusInfo = new StatusInfo();
			}
		}
		boolean isRefreshing = this.currentPage == initPage;
		if (statusInfo != null && statusInfo.isSuccessful()) {
			if (isRefreshing) {
				resetAdapterInfo(adapterInfo);
			} else {
				int oldItemCount = getItemCount();
				updateAdapterInfo(adapterInfo);
				int newItemCount = getItemCount();
				notifyItemRangeInserted(oldItemCount, newItemCount - oldItemCount);
			}
			swipeComplete(statusInfo);
			if (hasMore()) {
				swipeLoadReady();
			}
		} else {
			if (isRefreshing) {
				resetAdapterInfo(null);
			}
			swipeComplete(statusInfo);
		}
	}
	
	@Override
	protected void notifyRefreshComplete(StatusInfo statusInfo) {
		if (statusInfo != null && statusInfo.isSuccessful()) {
			this.currentPage++;
		}
		super.notifyRefreshComplete(statusInfo);
	}
	
	@Override
	protected void notifyLoadComplete(StatusInfo statusInfo) {
		if (statusInfo != null && statusInfo.isSuccessful()) {
			this.currentPage++;
		}
		super.notifyLoadComplete(statusInfo);
	}
	
	/**
	 * 重置数据信息
	 *
	 * @param adapterInfo 数据信息
	 */
	private void resetAdapterInfo(AdapterInfo adapterInfo) {
		this.adapterInfo = adapterInfo;
		notifyDataSetChanged();
	}
	
	/**
	 * 更新数据信息
	 *
	 * @param adapterInfo 数据信息
	 */
	protected abstract void updateAdapterInfo(@NonNull AdapterInfo adapterInfo);
	
	/**
	 * 获取指定位置数据
	 *
	 * @param position 指定位置
	 * @return 数据信息
	 */
	public Object getItem(int position) {
		return null;
	}
	
	/**
	 * 判断是否需加载更多
	 */
	public abstract boolean hasMore();
	
	@Override
	public RefreshHolder<StatusInfo> onCreateRefreshHolder() {
		return new RefreshHolder<StatusInfo>() {
			private ItemBasicRefreshListBinding dataBinding;
			
			@Override
			protected View onCreateView(ViewGroup parent) {
				this.dataBinding = DataBindingUtil.inflate(
						LayoutInflater.from(parent.getContext()),
						R.layout.item_basic_refresh_list,
						parent, false
				);
				return this.dataBinding.getRoot();
			}
			
			@Override
			protected void onRefreshReady() {
				this.dataBinding.tvStatus.setText(R.string.refresh_ready);
			}
			
			@Override
			protected void onRefresh() {
				this.dataBinding.tvStatus.setText(R.string.refreshing);
			}
			
			@Override
			protected void onRefreshComplete(StatusInfo statusInfo) {
				if (statusInfo == null) {
					this.dataBinding.tvStatus.setText(R.string.network_error);
				} else if (!statusInfo.isSuccessful()) {
					this.dataBinding.tvStatus.setText(R.string.complete_failure);
				} else {
					this.dataBinding.tvStatus.setText(R.string.data_empty);
				}
			}
		};
	}
	
	@Override
	public LoadHolder<StatusInfo> onCreateLoadHolder() {
		return new LoadHolder<StatusInfo>() {
			private ItemBasicLoadingListBinding dataBinding;
			
			@Override
			protected View onCreateView(ViewGroup parent) {
				this.dataBinding = DataBindingUtil.inflate(
						LayoutInflater.from(parent.getContext()),
						R.layout.item_basic_loading_list,
						parent, false
				);
				return this.dataBinding.getRoot();
			}
			
			@Override
			protected void onLoadReady() {
				this.dataBinding.tvStatus.setText(R.string.load_ready);
			}
			
			@Override
			protected void onLoading() {
				this.dataBinding.tvStatus.setText(R.string.loading);
			}
			
			@Override
			protected boolean onLoadComplete(StatusInfo statusInfo) {
				if (statusInfo == null) {
					this.dataBinding.tvStatus.setText(R.string.network_error);
					return true;
				} else if (!statusInfo.isSuccessful()) {
					this.dataBinding.tvStatus.setText(R.string.load_failure);
					return true;
				} else {
					this.dataBinding.tvStatus.setText(R.string.load_complete);
					return false;
				}
			}
		};
	}
}
