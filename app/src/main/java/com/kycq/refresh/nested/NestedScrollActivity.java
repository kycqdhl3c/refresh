package com.kycq.refresh.nested;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.kycq.refresh.R;
import com.kycq.refresh.databinding.ActivityNestedScrollBinding;

public class NestedScrollActivity extends AppCompatActivity {
	private ActivityNestedScrollBinding dataBinding;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_nested_scroll);
	}
	
}
