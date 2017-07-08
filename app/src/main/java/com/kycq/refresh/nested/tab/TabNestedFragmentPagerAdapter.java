package com.kycq.refresh.nested.tab;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

class TabNestedFragmentPagerAdapter extends FragmentPagerAdapter {
	
	TabNestedFragmentPagerAdapter(FragmentManager fm) {
		super(fm);
	}
	
	@Override
	public Fragment getItem(int position) {
		return new TabNestedFragment();
	}
	
	@Override
	public CharSequence getPageTitle(int position) {
		return String.valueOf(position);
	}
	
	@Override
	public int getCount() {
		return 5;
	}
}
