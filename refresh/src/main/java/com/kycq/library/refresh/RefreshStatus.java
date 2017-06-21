package com.kycq.library.refresh;

public interface RefreshStatus<StatusInfo> {
	
	/**
	 * 初始化刷新监听器
	 *
	 * @param listener 刷新监听器
	 */
	void initOnRefreshListener(RefreshLayout.OnRefreshListener listener);
	
	/**
	 * 刷新控件拖动偏移比例
	 *
	 * @param scale 偏移比例
	 */
	void onRefreshScale(float scale);
	
	/**
	 * 刷新准备状态
	 */
	void onRefreshReady();
	
	/**
	 * 刷新中状态
	 */
	void onRefresh();
	
	/**
	 * 刷新结束状态
	 *
	 * @param statusInfo 状态信息
	 * @return true显示状态信息
	 */
	boolean onRefreshComplete(StatusInfo statusInfo);
}
