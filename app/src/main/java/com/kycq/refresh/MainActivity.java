package com.kycq.refresh;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.kycq.refresh.databinding.ActivityMainBinding;
import com.kycq.refresh.recycler.RecyclerActivity;

public class MainActivity extends AppCompatActivity {
	private ActivityMainBinding dataBinding;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
		
		observeRecycler();
	}
	
	/**
	 * RecyclerView
	 */
	private void observeRecycler() {
		this.dataBinding.setRecyclerClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, RecyclerActivity.class);
				startActivity(intent);
			}
		});
	}
}
