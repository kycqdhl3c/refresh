package com.kycq.refresh.recycler.content;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kycq.refresh.R;
import com.kycq.refresh.basic.BasicAdapter;
import com.kycq.refresh.bean.ColorListBean;
import com.kycq.refresh.bean.StatusInfo;
import com.kycq.refresh.databinding.IncludeContentTabBinding;
import com.kycq.refresh.databinding.ItemTextListBinding;

class RecyclerListAdapter extends BasicAdapter<ColorListBean> {
	/** 标签 */
	private static final int TAB = 1;
	/** 条目 */
	private static final int ENTRY = 2;
	/** 状态 */
	private static final int STATUS = 3;
	
	/** 准备状态 */
	private static final int STATUS_READY = 1;
	/** 刷新状态 */
	private static final int STATUS_REFRESH = 2;
	/** 完成状态 */
	private static final int STATUS_COMPLETE = 3;
	
	private LayoutInflater inflater;
	
	private RecyclerListAdapter.OnSwitchTabListener onSwitchTabListener;
	private TabHolder tabHolder;
	
	private int status = STATUS_READY;
	private WrapRefreshHolder refreshHolder;
	
	RecyclerListAdapter(Context context) {
		this.inflater = LayoutInflater.from(context);
		
		this.tabHolder = new TabHolder();
	}
	
	@Override
	public void setRecyclerView(RecyclerView recyclerView) {
		super.setRecyclerView(recyclerView);
	}
	
	void setOnSwitchTabListener(OnSwitchTabListener listener) {
		this.onSwitchTabListener = listener;
	}
	
	void switchContent() {
		if (this.adapterInfo != null) {
			if (this.status == STATUS_READY) {
				int goodsCount = this.adapterInfo.list.size();
				this.adapterInfo.list.clear();
				notifyItemRangeRemoved(1, goodsCount);
				notifyItemInserted(1);
			}
			this.status = STATUS_REFRESH;
			if (this.refreshHolder != null) {
				this.refreshHolder.notifyRefresh();
			}
		}
		swipeRefresh();
	}
	
	@Override
	public void swipeResult(ColorListBean adapterInfo) {
		StatusInfo statusInfo = null;
		if (adapterInfo != null) {
			statusInfo = adapterInfo.statusInfo;
		}
		boolean isRefreshing = getCurrentPage() == getInitPage();
		if (statusInfo != null && statusInfo.isSuccessful()) {
			if (isRefreshing) {
				resetAdapterInfo(adapterInfo);
				if (adapterInfo.list.isEmpty()) {
					this.status = STATUS_COMPLETE;
				} else {
					this.status = STATUS_READY;
				}
				if (this.refreshHolder != null) {
					this.refreshHolder.notifyRefreshComplete(statusInfo);
				}
			} else {
				int oldItemCount = getItemCount();
				updateAdapterInfo(adapterInfo);
				int newItemCount = getItemCount();
				notifyItemRangeInserted(oldItemCount, newItemCount - oldItemCount);
			}
			swipeComplete(statusInfo);
			if (hasMore()) {
				swipeLoadReady();
			}
		} else {
			if (this.status == STATUS_REFRESH) {
				this.status = STATUS_COMPLETE;
				if (this.refreshHolder != null) {
					this.refreshHolder.notifyRefreshComplete(statusInfo);
				}
				swipeComplete(new StatusInfo());
				return;
			}
			if (isRefreshing) {
				resetAdapterInfo(null);
			}
			swipeComplete(statusInfo);
		}
	}
	
	@Override
	protected void updateAdapterInfo(@NonNull ColorListBean colorListBean) {
		this.adapterInfo.list.addAll(colorListBean.list);
	}
	
	@Override
	public Integer getItem(int position) {
		return this.adapterInfo.list.get(position - 1);
	}
	
	@Override
	public boolean hasMore() {
		return this.adapterInfo != null && getCurrentPage() <= this.adapterInfo.pageCount;
	}
	
	@Override
	public int getItemCount() {
		int count = 0;
		if (this.adapterInfo == null) {
			return count;
		}
		count++;
		if (this.status != STATUS_READY) {
			count++;
			return count;
		}
		return this.adapterInfo.list.size() + 1;
	}
	
	@Override
	public int getItemType(int position) {
		if (position == 0) {
			return TAB;
		}
		if (this.status != STATUS_READY && position == 1) {
			return STATUS;
		}
		return ENTRY;
	}
	
	@Override
	public RecyclerHolder onCreateItemHolder(int viewType) {
		if (viewType == TAB) {
			return tabHolder;
		}
		if (viewType == STATUS) {
			if (this.refreshHolder == null) {
				this.refreshHolder = new WrapRefreshHolder();
			}
			return this.refreshHolder;
		}
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
	
	private class TabHolder extends ItemHolder {
		private IncludeContentTabBinding dataBinding;
		
		@Override
		protected View onCreateView(ViewGroup parent) {
			this.dataBinding = DataBindingUtil.inflate(
					inflater,
					R.layout.include_content_tab,
					parent, false
			);
			return this.dataBinding.getRoot();
		}
		
		@Override
		protected void onViewCreated(View view) {
			this.dataBinding.tabLayoutSwitch.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
				@Override
				public void onTabSelected(TabLayout.Tab tab) {
					onSwitchTabListener.switchTab(tab.getPosition());
				}
				
				@Override
				public void onTabUnselected(TabLayout.Tab tab) {
				}
				
				@Override
				public void onTabReselected(TabLayout.Tab tab) {
				}
			});
		}
		
		@Override
		protected void onBindView(int position) {
		}
	}
	
	interface OnSwitchTabListener {
		
		void switchTab(int position);
	}
}