package com.kycq.library.refresh;

public abstract class OnTaskListener<Task> {
	
	/**
	 * 请求任务
	 *
	 * @return 当前任务
	 */
	public abstract Task onTask();
	
	/**
	 * 取消任务
	 *
	 * @param task 当前任务
	 */
	public abstract void onCancel(Task task);
}
