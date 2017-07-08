package com.kycq.refresh.recycler.tab;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

class TabRecyclerFragmentPagerAdapter extends FragmentPagerAdapter {
	
	TabRecyclerFragmentPagerAdapter(FragmentManager fm) {
		super(fm);
	}
	
	@Override
	public Fragment getItem(int position) {
		return new TabRecyclerFragment();
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
