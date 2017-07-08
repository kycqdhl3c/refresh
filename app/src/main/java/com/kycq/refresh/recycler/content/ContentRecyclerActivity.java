package com.kycq.refresh.recycler.content;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;

import com.kycq.library.refresh.OnTaskListener;
import com.kycq.refresh.R;
import com.kycq.refresh.bean.ColorListBean;
import com.kycq.refresh.databinding.ActivityContentRecyclerBinding;

public class ContentRecyclerActivity extends AppCompatActivity implements Handler.Callback {
	private ActivityContentRecyclerBinding dataBinding;
	
	private RecyclerListAdapter recyclerListAdapter;
	
	private Handler handler = new Handler(this);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_content_recycler);
		
		observeList();
		observeAction();
		
		this.recyclerListAdapter.swipeRefresh();
	}
	
	/**
	 * 列表
	 */
	private void observeList() {
		this.dataBinding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
		this.dataBinding.recyclerView.getItemAnimator().setChangeDuration(0);
		this.dataBinding.recyclerView.getItemAnimator().setRemoveDuration(0);
		this.dataBinding.recyclerView.getItemAnimator().setAddDuration(0);
		this.dataBinding.recyclerView.getItemAnimator().setMoveDuration(0);
		
		this.recyclerListAdapter = new RecyclerListAdapter(this);
		this.recyclerListAdapter.setRefreshLayout(this.dataBinding.refreshLayout);
		this.recyclerListAdapter.setRecyclerView(this.dataBinding.recyclerView);
		
		this.recyclerListAdapter.setOnTaskListener(new OnTaskListener<Integer>() {
			@Override
			public Integer onTask() {
				handler.sendEmptyMessageDelayed(0, 1000);
				return 0;
			}
			
			@Override
			public void onCancel(Integer integer) {
				handler.removeMessages(integer);
			}
		});
	}
	
	/**
	 * 操作
	 */
	private void observeAction() {
		this.recyclerListAdapter.setOnSwitchTabListener(new RecyclerListAdapter.OnSwitchTabListener() {
			@Override
			public void switchTab(int position) {
				recyclerListAdapter.switchContent();
			}
		});
	}
	
	private int time = 0;
	
	@Override
	public boolean handleMessage(Message msg) {
		time++;
		if (time == 3) {
			System.out.println("empty");
			this.recyclerListAdapter.swipeResult(ColorListBean.create(0, 1));
		} else if (time == 6) {
			System.out.println("error");
			time = 0;
			this.recyclerListAdapter.swipeResult(null);
		} else {
			System.out.println("normal");
			this.recyclerListAdapter.swipeResult(ColorListBean.create(10 + (int) (Math.random() * 10), 5));
		}
		return true;
	}
}
