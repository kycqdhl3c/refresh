package com.kycq.refresh.bean;

import java.util.ArrayList;
import java.util.Random;

public class ColorListBean extends BasicBean {
	/** 颜色列表 */
	public ArrayList<Integer> list;
	/** 页面数量 */
	public int pageCount;
	
	/**
	 * 创建颜色列表信息
	 *
	 * @param listCount 列表数量
	 * @param pageCount 页面数量
	 * @return 颜色列表信息
	 */
	public static ColorListBean create(int listCount, int pageCount) {
		ColorListBean colorListBean = new ColorListBean();
		colorListBean.list = new ArrayList<>();
		for (int index = 0; index < listCount; index++) {
			Random random = new Random();
			colorListBean.list.add(0xff000000 | random.nextInt(0x00ffffff));
		}
		colorListBean.pageCount = pageCount;
		return colorListBean;
	}
}
