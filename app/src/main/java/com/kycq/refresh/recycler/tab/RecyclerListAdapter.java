package com.kycq.refresh.recycler.tab;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kycq.refresh.R;
import com.kycq.refresh.basic.BasicAdapter;
import com.kycq.refresh.bean.ColorListBean;
import com.kycq.refresh.databinding.ItemTextListBinding;

class RecyclerListAdapter extends BasicAdapter<ColorListBean> {
	private LayoutInflater inflater;
	
	RecyclerListAdapter(Context context) {
		this.inflater = LayoutInflater.from(context);
	}
	
	@Override
	protected void updateAdapterInfo(@NonNull ColorListBean colorListBean) {
		this.adapterInfo.list.addAll(colorListBean.list);
	}
	
	@Override
	public Integer getItem(int position) {
		return this.adapterInfo.list.get(position);
	}
	
	@Override
	public boolean hasMore() {
		return this.adapterInfo != null && getCurrentPage() <= this.adapterInfo.pageCount;
	}
	
	@Override
	public int getItemCount() {
		return this.adapterInfo != null ? this.adapterInfo.list.size() : 0;
	}
	
	@Override
	public RecyclerHolder onCreateItemHolder(int viewType) {
		return new ItemHolder() {
			private ItemTextListBinding dataBinding;
			
			@Override
			protected View onCreateView(ViewGroup parent) {
				this.dataBinding = DataBindingUtil.inflate(
						inflater,
						R.layout.item_text_list,
						parent, false
				);
				return this.dataBinding.getRoot();
			}
			
			@Override
			protected void onBindView(int position) {
				this.dataBinding.setColor(getItem(position));
			}
		};
	}
}
