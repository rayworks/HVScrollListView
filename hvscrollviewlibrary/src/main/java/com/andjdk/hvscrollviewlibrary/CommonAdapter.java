/**
 * Copyright (c) 2016, andjdk@163.com All Rights Reserved.
 * #                                                   #
 * #                       _oo0oo_                     #
 * #                      o8888888o                    #
 * #                      88" . "88                    #
 * #                      (| -_- |)                    #
 * #                      0\  =  /0                    #
 * #                    ___/`---'\___                  #
 * #                  .' \\|     |# '.                 #
 * #                 / \\|||  :  |||# \                #
 * #                / _||||| -:- |||||- \              #
 * #               |   | \\\  -  #/ |   |              #
 * #               | \_|  ''\---/''  |_/ |             #
 * #               \  .-\__  '-'  ___/-. /             #
 * #             ___'. .'  /--.--\  `. .'___           #
 * #          ."" '<  `.___\_<|>_/___.' >' "".         #
 * #         | | :  `- \`.;`\ _ /`;.`/ - ` : | |       #
 * #         \  \ `_.   \_ __\ /__ _/   .-` /  /       #
 * #     =====`-.____`.___ \_____/___.-`___.-'=====    #
 * #                       `=---='                     #
 * #     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~   #
 * #                                                   #
 * #               佛祖保佑         永无BUG            #
 * #                                                   #
 */
package com.andjdk.hvscrollviewlibrary;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * listview通用的adapter类
 *
 * @param <T>
 * @author tanxiongliang
 */
public abstract class CommonAdapter<T> extends BaseAdapter {
    private Context mContext;
    private LayoutInflater mInflater;
    private List<T> mDatas;
    private int layoutId;
    private HashSet<View> movableViewList = new LinkedHashSet<>();
    private LinearLayout moveViewLayout;


    private HeaderColumnWidthRetriever columnWidthRetriever;

    public CommonAdapter(Context mContext, List<T> mDatas, int layoutId,
                         HeaderColumnWidthRetriever columnWidthRetriever) {
        super();

        this.mContext = mContext;
        this.mDatas = mDatas;
        this.layoutId = layoutId;
        this.columnWidthRetriever = columnWidthRetriever;
        mInflater = LayoutInflater.from(mContext);
    }

    @Override
    public int getCount() {
        return mDatas == null ? 0 : mDatas.size();
    }

    @Override
    public T getItem(int position) {
        if (null != mDatas) {
            return mDatas.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = ViewHolder.get(mContext, convertView, parent, layoutId, position);

        // update the column width
        int[] widthArray = columnWidthRetriever.getHeaderWidth();

        LinearLayout moveLayout = holder.getView(R.id.move_layout);
        LinearLayout headLayout = holder.getView(R.id.head_view);
        int headerChildCount = headLayout.getChildCount();
        for (int i = 0; i < headerChildCount; i++) {
            TextView textView = (TextView) headLayout.getChildAt(i);
            ((LinearLayout.LayoutParams) textView.getLayoutParams()).width = widthArray[i];
        }
        for (int j = 0; j < moveLayout.getChildCount(); j++) {
            TextView textView = (TextView) moveLayout.getChildAt(j);
            ((LinearLayout.LayoutParams) textView.getLayoutParams()).width = widthArray[j + headerChildCount];
        }

        holder.getConvertView().requestLayout();

        moveLayout.setScrollX(offsetX);

        movableViewList.add(moveLayout);
        convert(holder, getItem(position), position, movableViewList);

        return holder.getConvertView();
    }

    public abstract void convert(ViewHolder holder, T t, int position, Collection<View> movableViewList);


    public HashSet<View> getMovableViewList() {
        return movableViewList;
    }

    public CommonAdapter setMovableViewList(HashSet<View> movableViewList) {
        this.movableViewList = movableViewList;
        return this;
    }

    // 可滑动区域的横向偏移值
    private int offsetX = 0;

    public void setHorizontalOffset(int offsetX) {
        this.offsetX = offsetX;
    }

    @LayoutRes
    public int getHeaderLayout() {
        return R.layout.header_def_layout;
    }
}
