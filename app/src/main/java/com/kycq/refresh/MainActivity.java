package com.kycq.refresh;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.kycq.refresh.databinding.ActivityMainBinding;
import com.kycq.refresh.nested.tab.TabNestedActivity;
import com.kycq.refresh.recycler.content.ContentRecyclerActivity;
import com.kycq.refresh.recycler.normal.NormalRecyclerActivity;
import com.kycq.refresh.recycler.tab.TabRecyclerActivity;

public class MainActivity extends AppCompatActivity {
	private ActivityMainBinding dataBinding;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
		
		observeNormalRecycler();
		observeTabRecycler();
		observeContentRecycler();
		
		observeTabNested();
	}
	
	/**
	 * RecyclerView - Normal
	 */
	private void observeNormalRecycler() {
		this.dataBinding.setNormalRecyclerClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, NormalRecyclerActivity.class);
				startActivity(intent);
			}
		});
	}
	
	/**
	 * RecyclerView - Tab
	 */
	private void observeTabRecycler() {
		this.dataBinding.setTabRecyclerClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, TabRecyclerActivity.class);
				startActivity(intent);
			}
		});
	}
	
	/**
	 * RecyclerView - Content
	 */
	private void observeContentRecycler() {
		this.dataBinding.setContentRecyclerClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, ContentRecyclerActivity.class);
				startActivity(intent);
			}
		});
	}
	
	/**
	 * NestedScrollView - Tab
	 */
	private void observeTabNested() {
		this.dataBinding.setTabNestedClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, TabNestedActivity.class);
				startActivity(intent);
			}
		});
	}
}
