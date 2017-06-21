package com.kycq.refresh.bean;

public class StatusInfo {
	/** 请求失败 */
	public static final int FAILURE = 1001;
	/** 网络错误 */
	public static final int NETWORK_ERROR = 300;
	
	/** 状态码 */
	public int statusCode;
	/** 状态信息 */
	public String statusContent;
	
	public StatusInfo() {
		
	}
	
	public StatusInfo(int statusCode) {
		this.statusCode = statusCode;
	}
	
	public boolean isSuccessful() {
		return statusCode == 0;
	}
}
