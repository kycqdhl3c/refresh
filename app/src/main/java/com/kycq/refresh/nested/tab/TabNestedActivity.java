package com.kycq.refresh.nested.tab;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.kycq.refresh.R;
import com.kycq.refresh.databinding.ActivityTabNestedBinding;

public class TabNestedActivity extends AppCompatActivity {
	private ActivityTabNestedBinding dataBinding;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_tab_nested);
		
		observeContent();
	}
	
	/**
	 * 内容
	 */
	private void observeContent() {
		this.dataBinding.viewPager.setAdapter(new TabNestedFragmentPagerAdapter(getSupportFragmentManager()));
		this.dataBinding.tabLayout.setupWithViewPager(this.dataBinding.viewPager);
	}
}
