package com.kycq.refresh.recycler.tab;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kycq.library.refresh.OnTaskListener;
import com.kycq.refresh.R;
import com.kycq.refresh.bean.ColorListBean;
import com.kycq.refresh.databinding.FragmentTabRecyclerBinding;

public class TabRecyclerFragment extends Fragment implements Handler.Callback {
	private boolean isCreated;
	private boolean isVisible;
	
	private FragmentTabRecyclerBinding dataBinding;
	
	private RecyclerListAdapter recyclerListAdapter;
	
	private Handler handler = new Handler(this);
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.dataBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_tab_recycler, container, false);
		return this.dataBinding.getRoot();
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		observeList();
		
		this.isCreated = true;
		if (this.isVisible) {
			this.recyclerListAdapter.swipeRefresh();
		}
	}
	
	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);
		this.isVisible = isVisibleToUser;
		if (this.isCreated && this.isVisible) {
			this.recyclerListAdapter.swipeRefresh();
		}
	}
	
	/**
	 * 列表
	 */
	private void observeList() {
		this.dataBinding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		
		this.recyclerListAdapter = new RecyclerListAdapter(getContext());
		this.recyclerListAdapter.setRefreshLayout(this.dataBinding.refreshLayout);
		this.recyclerListAdapter.setRecyclerView(this.dataBinding.recyclerView);
		
		this.recyclerListAdapter.setOnTaskListener(new OnTaskListener<Integer>() {
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
		this.recyclerListAdapter.swipeResult(ColorListBean.create(20, 5));
		return true;
	}
}
