package com.kycq.library.refresh;

public interface RefreshHeader<StatusInfo> {
	
	/**
	 * 获取刷新偏移位置
	 *
	 * @return 刷新偏移位置
	 */
	int getRefreshOffsetPosition();
	
	/**
	 * 刷新控件拖动偏移比例
	 *
	 * @param scale 偏移比例
	 */
	void onRefreshScale(float scale);
	
	/**
	 * 下拉刷新状态
	 */
	void onPullToRefresh();
	
	/**
	 * 释放刷新状态
	 */
	void onReleaseToRefresh();
	
	/**
	 * 刷新中状态
	 */
	void onRefresh();
	
	/**
	 * 刷新结束状态
	 *
	 * @param statusInfo 状态信息
	 */
	void onRefreshComplete(StatusInfo statusInfo);
}
