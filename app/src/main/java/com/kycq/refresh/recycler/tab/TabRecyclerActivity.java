package com.kycq.refresh.recycler.tab;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.kycq.refresh.R;
import com.kycq.refresh.databinding.ActivityTabRecyclerBinding;

public class TabRecyclerActivity extends AppCompatActivity {
	private ActivityTabRecyclerBinding dataBinding;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_tab_recycler);
		
		observeContent();
	}
	
	/**
	 * 内容
	 */
	private void observeContent() {
		this.dataBinding.viewPager.setAdapter(new TabRecyclerFragmentPagerAdapter(getSupportFragmentManager()));
		this.dataBinding.tabLayout.setupWithViewPager(this.dataBinding.viewPager);
	}
}
