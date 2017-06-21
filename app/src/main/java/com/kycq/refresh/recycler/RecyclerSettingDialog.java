package com.kycq.refresh.recycler;

import android.app.Dialog;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.kycq.library.refresh.RecyclerAdapter;
import com.kycq.refresh.R;
import com.kycq.refresh.databinding.DialogRecyclerSettingBinding;

class RecyclerSettingDialog extends Dialog {
	private DialogRecyclerSettingBinding dataBinding;
	
	private OnSettingListener onSettingListener;
	
	RecyclerSettingDialog(Context context, OnSettingListener listener) {
		super(context);
		
		this.onSettingListener = listener;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.dataBinding = DataBindingUtil.inflate(
				getLayoutInflater(),
				R.layout.dialog_recycler_setting,
				null, false
		);
		setContentView(this.dataBinding.getRoot());
		
		observeConfirm();
	}
	
	/**
	 * 确定
	 */
	private void observeConfirm() {
		this.dataBinding.setConfirmClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
				
				int loadMode;
				if (dataBinding.rbAuto.isSelected()) {
					loadMode = RecyclerAdapter.LOAD_MODE_AUTO;
				} else if (dataBinding.rbShow.isSelected()) {
					loadMode = RecyclerAdapter.LOAD_MODE_SHOW;
				} else {
					loadMode = RecyclerAdapter.LOAD_MODE_HIDE;
				}
				
				String listCountStr = dataBinding.etListCount.getText().toString().trim();
				if (listCountStr.length() <= 0) {
					listCountStr = getContext().getString(R.string.the_default_number_of_data);
				}
				int listCount;
				try {
					listCount = Integer.parseInt(listCountStr);
				} catch (Exception ignored) {
					listCount = Integer.parseInt(getContext().getString(R.string.the_default_number_of_data));
				}
				
				String pageCountStr = dataBinding.etPageCount.getText().toString().trim();
				if (pageCountStr.length() <= 0) {
					pageCountStr = getContext().getString(R.string.the_default_number_of_page);
				}
				int pageCount;
				try {
					pageCount = Integer.parseInt(pageCountStr);
				} catch (Exception ignored) {
					pageCount = Integer.parseInt(getContext().getString(R.string.the_default_number_of_page));
				}
				
				onSettingListener.onSetting(loadMode, listCount, pageCount);
			}
		});
	}
	
	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		
		Window window = getWindow();
		if (window == null) {
			return;
		}
		
		WindowManager.LayoutParams layoutParams = window.getAttributes();
		DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
		layoutParams.width = (int) (displayMetrics.widthPixels * 0.8);
		window.setAttributes(layoutParams);
	}
	
	interface OnSettingListener {
		
		/**
		 * 设置信息
		 *
		 * @param loadMode  加载模式
		 * @param listCount 列表数量
		 * @param pageCount 页面数量
		 */
		void onSetting(int loadMode, int listCount, int pageCount);
	}
}
