package com.kycq.refresh.nested.tab;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kycq.library.refresh.OnTaskListener;
import com.kycq.refresh.R;
import com.kycq.refresh.bean.StatusInfo;
import com.kycq.refresh.databinding.FragmentTabNestedBinding;

public class TabNestedFragment extends Fragment implements Handler.Callback {
	private boolean isCreated;
	private boolean isVisible;
	
	private FragmentTabNestedBinding dataBinding;
	
	private Handler handler = new Handler(this);
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.dataBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_tab_nested, container, false);
		return this.dataBinding.getRoot();
	}
	
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		observeContent();
		
		this.isCreated = true;
		if (this.isVisible) {
			this.dataBinding.refreshLayout.swipeRefresh();
		}
	}
	
	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);
		this.isVisible = isVisibleToUser;
		if (this.isCreated && this.isVisible) {
			this.dataBinding.refreshLayout.swipeRefresh();
		}
	}
	
	/**
	 * 内容
	 */
	private void observeContent() {
		this.dataBinding.refreshLayout.setOnTaskListener(new OnTaskListener<Integer>() {
			@Override
			public Integer onTask() {
				handler.sendEmptyMessageDelayed(0, 2000);
				return 0;
			}
			
			@Override
			public void onCancel(Integer integer) {
				handler.removeMessages(integer);
			}
		});
	}
	
	@Override
	public boolean handleMessage(Message msg) {
		this.dataBinding.refreshLayout.swipeComplete(new StatusInfo());
		return true;
	}
}
