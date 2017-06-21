package com.kycq.refresh.recycler;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.kycq.library.refresh.OnTaskListener;
import com.kycq.refresh.R;
import com.kycq.refresh.bean.ColorListBean;
import com.kycq.refresh.bean.StatusInfo;
import com.kycq.refresh.databinding.ActivityRecyclerBinding;

public class RecyclerActivity extends AppCompatActivity {
	private ActivityRecyclerBinding dataBinding;
	
	private RecyclerSettingDialog recyclerSettingDialog;
	
	private RecyclerListAdapter recyclerListAdapter;
	
	/** 列表数量 */
	private int listCount;
	/** 页面数量 */
	private int pageCount;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_recycler);
		
		this.listCount = Integer.parseInt(getString(R.string.the_default_number_of_data));
		this.pageCount = Integer.parseInt(getString(R.string.the_default_number_of_page));
		
		observeList();
		observeRefreshReady();
		observeRefreshing();
		observeCompleteSuccess();
		observeCompleteFailure();
		observeCompleteError();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_setting, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (this.recyclerSettingDialog == null) {
			this.recyclerSettingDialog = new RecyclerSettingDialog(this,
					new RecyclerSettingDialog.OnSettingListener() {
						@Override
						public void onSetting(int loadMode, int listCount, int pageCount) {
							recyclerListAdapter.setLoadMode(loadMode);
							RecyclerActivity.this.listCount = listCount;
							RecyclerActivity.this.pageCount = pageCount;
						}
					});
		}
		this.recyclerSettingDialog.show();
		return true;
	}
	
	/**
	 * 列表
	 */
	private void observeList() {
		this.dataBinding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
		
		this.recyclerListAdapter = new RecyclerListAdapter(this);
		this.recyclerListAdapter.setRefreshLayout(this.dataBinding.refreshLayout);
		this.recyclerListAdapter.setRecyclerView(this.dataBinding.recyclerView);
		this.recyclerListAdapter.setOnTaskListener(new OnTaskListener<Integer>() {
			@Override
			public Integer onTask() {
				return recyclerListAdapter.getCurrentPage();
			}
			
			@Override
			public void onCancel(Integer integer) {
				
			}
		});
	}
	
	/**
	 * 准备刷新
	 */
	private void observeRefreshReady() {
		this.dataBinding.setRefreshReadyClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				recyclerListAdapter.swipeRefreshReady();
			}
		});
	}
	
	/**
	 * 刷新中
	 */
	private void observeRefreshing() {
		this.dataBinding.setRefreshingClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				recyclerListAdapter.swipeRefresh();
			}
		});
	}
	
	/**
	 * 请求成功
	 */
	private void observeCompleteSuccess() {
		this.dataBinding.setCompleteSuccessClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				recyclerListAdapter.swipeResult(ColorListBean.create(listCount, pageCount));
			}
		});
	}
	
	/**
	 * 请求失败
	 */
	private void observeCompleteFailure() {
		this.dataBinding.setCompleteFailureClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ColorListBean colorListBean = new ColorListBean();
				colorListBean.statusInfo = new StatusInfo(StatusInfo.FAILURE);
				recyclerListAdapter.swipeResult(colorListBean);
			}
		});
	}
	
	/**
	 * 请求错误
	 */
	private void observeCompleteError() {
		this.dataBinding.setCompleteErrorClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ColorListBean colorListBean = new ColorListBean();
				colorListBean.statusInfo = new StatusInfo(StatusInfo.NETWORK_ERROR);
				recyclerListAdapter.swipeResult(colorListBean);
			}
		});
	}
}
