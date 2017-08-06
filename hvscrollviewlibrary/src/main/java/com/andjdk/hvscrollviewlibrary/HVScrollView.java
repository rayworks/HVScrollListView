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

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Locale;

import static com.andjdk.hvscrollviewlibrary.DisplayUtil.dip2px;

/**
 * Created by andjdk on 2015/11/3.
 */
public class HVScrollView extends RelativeLayout implements HeaderColumnWidthRetriever {
    /**
     * 列表头的高和宽
     */
    private LinearLayout mLayoutTitleMovable;
    private float mStartX = 0;
    private int mMoveOffsetX = 0;
    private int mFixX = 0;

    private String[] mFixLeftListColumnsText;
    private int[] mFixLeftListColumnsWidth;

    private String[] mMovableListColumnsText = new String[]{};
    private int[] mMovableListColumnsWidth = null;

    private ListView mStockListView;
    private CommonAdapter mAdapter;

    private Collection<View> mMovableViewList;

    private Context context;
    private int mMovableTotalWidth = 0;

    private int mMoveViewWidth = 70;
    private int mFixViewWidth = 80;
    private int mItemViewHeight = 50;

    private LinearLayout footerLayout;

    private boolean isLoading;  //正在加载
    private int mLastVisibleItem;
    private int mTotalItemCount;
    private int mVisibleItemCount;
    private int mFirstVisibleItem;
    private int fullScreenWidth;

    private int touchSlop;


    public HVScrollView(Context context) {
        this(context, null);
    }

    public HVScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HVScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;

        final ViewConfiguration vc = ViewConfiguration.get(context);
        touchSlop = vc.getScaledTouchSlop();
    }

    private void initView() {
        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        View headLayout = buildHeadLayout();
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(totalHeaderWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        linearLayout.addView(headLayout, layoutParams); //totalHeaderWidth

        linearLayout.addView(buildMovableListView());

        this.addView(linearLayout, new LayoutParams(LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // flush the layout immediately
        linearLayout.requestLayout();
    }

    private LinkedList<String> headerTexts = new LinkedList<>();

    /***
     * Column width array
     */
    private int[] columnWidthArray;

    @Override
    public int[] getHeaderWidth() {
        return columnWidthArray;
    }

    static int totalHeaderWidth = 0;

    public static int getTotalHeaderWidth() {
        return totalHeaderWidth;
    }

    private View buildHeadLayout() {
        // ---------------------------------------
        // FixedHeaderLayout | MovableHeaderLayout
        // ---------------------------------------
        //  FixedBodyLayout  |  MovableBodyLayout
        //  FixedBodyLayout  |  MovableBodyLayout
        //  FixedBodyLayout  |  MovableBodyLayout
        //  FixedBodyLayout  |  MovableBodyLayout
        //  FixedBodyLayout  |  MovableBodyLayout
        //  FixedBodyLayout  |  MovableBodyLayout
        // ---------------------------------------
        LinkedList<TextView> textViews = new LinkedList<>();

        View headerView = LayoutInflater.from(context).inflate(mAdapter.getHeaderLayout(), null);

        ViewGroup fixedLayout = (ViewGroup) headerView.findViewById(R.id.header_fixed_layout);
        ViewGroup dynamicLayout = (ViewGroup) headerView.findViewById(R.id.header_move_layout);

        fullScreenWidth = context.getResources().getDisplayMetrics().widthPixels;
        System.out.println(">>>> full screen width : " + fullScreenWidth);

        mLayoutTitleMovable = (LinearLayout) dynamicLayout;

        TextView textView = null;
        int fixedChildCount = fixedLayout.getChildCount();
        for (int i = 0; i < fixedChildCount; i++) {
            textView = (TextView) fixedLayout.getChildAt(i);
            textViews.add(textView);
        }
        for (int j = 0; j < dynamicLayout.getChildCount(); j++) {
            textView = (TextView) dynamicLayout.getChildAt(j);
            textViews.add(textView);
        }

        headerTexts.addAll(Arrays.asList(mFixLeftListColumnsText));
        headerTexts.addAll(Arrays.asList(mMovableListColumnsText));

        int headerSize = headerTexts.size();
        int viewSize = textViews.size();
        if (headerSize != viewSize) {
            throw new IllegalArgumentException(String.format(Locale.ENGLISH, "View %d | Text %d",
                    viewSize, headerSize));
        }

        columnWidthArray = new int[headerSize];
        totalHeaderWidth = 0;
        mMovableTotalWidth = 0;

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        for (int k = 0; k < headerSize; k++) {
            TextView txt = textViews.get(k);
            txt.setLayoutParams(layoutParams);

            txt.setText(headerTexts.get(k));

            txt.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) (txt.getLayoutParams());

            columnWidthArray[k] = txt.getMeasuredWidth();
            totalHeaderWidth += columnWidthArray[k];

            System.out.println(String.format(Locale.ENGLISH, ">>> Index view %d measured width %d",
                    k, columnWidthArray[k]));

            // reset the Movable width range
            if (k >= fixedChildCount) {
                mMovableTotalWidth += columnWidthArray[k];
                mMovableListColumnsWidth[k - fixedChildCount] = columnWidthArray[k];
            }
        }
        System.out.println(">>> total header width : " + totalHeaderWidth);
        System.out.println(">>> total Movable header width : " + mMovableTotalWidth);

        return headerView;
    }


    private View buildMovableListView() {
        RelativeLayout linearLayout = new RelativeLayout(getContext());
        mStockListView = new ListView(getContext());
        if (null != mAdapter) {
            mStockListView.setAdapter(mAdapter);
            mMovableViewList = mAdapter.getMovableViewList();
        }

        footerLayout = new LinearLayout(getContext());
        footerLayout.setGravity(Gravity.CENTER);
        ProgressBar progressBar = new ProgressBar(getContext());
        footerLayout.addView(progressBar);
        footerLayout.setVisibility(GONE);
        mStockListView.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                //当滑动到底部时
                if (mTotalItemCount == mLastVisibleItem && scrollState == SCROLL_STATE_IDLE) {
                    if (!isLoading) {
                        isLoading = true;
                        if (null != onLoadMoreListener) {
                            onLoadMoreListener.onLoadingMore();
                            footerLayout.setVisibility(View.VISIBLE);//显示底部布局
                        }
                    }
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                mFirstVisibleItem = firstVisibleItem;
                mTotalItemCount = totalItemCount;
                mLastVisibleItem = firstVisibleItem + visibleItemCount;
                mVisibleItemCount = visibleItemCount;
            }
        });
        mStockListView.setOnItemClickListener(mOnItemClickListener);
        mStockListView.setOnItemLongClickListener(mOnItemLongClickListener);
        linearLayout.addView(footerLayout, LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        linearLayout.addView(mStockListView, new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT));
        return linearLayout;
    }

    public void onLoadingComplete() {
        if (null != footerLayout) {
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    footerLayout.setVisibility(GONE);
                    isLoading = false;
                    mStockListView.setSelection(mLastVisibleItem - mVisibleItemCount + 1);
                }
            });
        }
    }

    /***
     * Sets the composite Adapter. It will trigger a layout update.
     *
     * @param adapter
     */
    public <A extends CommonAdapter> void setAdapter(A adapter) {
        this.mAdapter = adapter;
        initView();
    }


    private AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (null != onItemClickedListener) {
                onItemClickedListener.onItemClick(parent, view, position, id);
            }
        }
    };

    private AdapterView.OnItemLongClickListener mOnItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            if (null != onItemLongClickedListener) {
                onItemLongClickedListener.onItemLongClick(parent, view, position, id);
            }
            return false;
        }
    };

    private OnItemClickedListener onItemClickedListener;
    private OnItemLongClickedListener onItemLongClickedListener;

    /**
     * 列表item单机事件
     */
    public void setOnItemClick(OnItemClickedListener onItemClickedListener) {
        this.onItemClickedListener = onItemClickedListener;
    }

    /**
     * 列表item长按事件
     */
    public void setOnItemLongClick(OnItemLongClickedListener onItemLongClickedListener) {
        this.onItemLongClickedListener = onItemLongClickedListener;
    }

    private TextView addListHeaderTextView(String headerName, int AWidth, LinearLayout fixHeadLayout) {
        TextView textView = new TextView(getContext());
        textView.setText(headerName);
        textView.setGravity(Gravity.CENTER);
        fixHeadLayout.addView(textView, AWidth, dip2px(context, 50));
        return textView;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mStartX = ev.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                int offsetX = (int) Math.abs(ev.getX() - mStartX);
                if (offsetX > touchSlop) {
                    return true;
                } else {
                    return false;
                }
            case MotionEvent.ACTION_UP:
                actionUP();
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    private void actionUP() {
        if (mFixX < 0) {
            mFixX = 0;
            mLayoutTitleMovable.scrollTo(0, 0);
            if (null != mMovableViewList) {

                updateAdapterViewHorizontalOffset(0);
                for (View view : mMovableViewList) {
                    view.scrollTo(0, 0);
                }

            } else {
                if (getMovableLayoutLimitWidth() + Math.abs(mFixX) > MovableTotalWidth()) {
                    int offsetX = MovableTotalWidth() - getMovableLayoutLimitWidth();
                    mLayoutTitleMovable.scrollTo(offsetX, 0);

                    updateAdapterViewHorizontalOffset(offsetX);
                    if (null != mMovableViewList) {
                        for (View view : mMovableViewList) {
                            view.scrollTo(offsetX, 0);
                        }
                    }
                }
            }
        }
    }

    private int getMovableLayoutLimitWidth() {
        return /*mLayoutTitleMovable.getWidth()*/ fullScreenWidth - mMovableListColumnsWidth[0];
    }

    private void updateAdapterViewHorizontalOffset(int offset) {
        if (mAdapter != null)
            ((CommonAdapter) mAdapter).setHorizontalOffset(offset);
    }


    private int MovableTotalWidth() {
        if (0 == mMovableTotalWidth) {
            for (int i = 0; i < mMovableListColumnsWidth.length; i++) {
                mMovableTotalWidth = mMovableTotalWidth + mMovableListColumnsWidth[i];
            }
        }
        return mMovableTotalWidth;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mStartX = event.getX();
                return true;
            case MotionEvent.ACTION_MOVE:
                int offsetX = (int) Math.abs(event.getX() - mStartX);
                if (offsetX > touchSlop) {
                    mMoveOffsetX = (int) (mStartX - event.getX() + mFixX);
                    if (0 > mMoveOffsetX) {
                        mMoveOffsetX = 0;
                    } else {
                        if ((getMovableLayoutLimitWidth() + mMoveOffsetX) > MovableTotalWidth()) {
                            mMoveOffsetX = MovableTotalWidth() - getMovableLayoutLimitWidth();
                        }
                    }
                    mLayoutTitleMovable.scrollTo(mMoveOffsetX, 0);
                    if (null != mMovableViewList) {
                        updateAdapterViewHorizontalOffset(mMoveOffsetX);

                        for (View view : mMovableViewList) {
                            view.scrollTo(mMoveOffsetX, 0);
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                mFixX = mMoveOffsetX; // mFixX + (int) ((int) ev.getX() - mStartX)
                actionUP();
                break;

        }

        return super.onTouchEvent(event);
    }

    /***
     * 必须先初始化顶部标题栏
     *
     * @param fixedHeaderNames
     * @param movableHeaderNames
     */
    public void setHeaderListData(String[] fixedHeaderNames, String[] movableHeaderNames) {
        if (movableHeaderNames == null || fixedHeaderNames == null) {
            throw new IllegalArgumentException("Input names can't be null");
        }

        this.mMovableListColumnsText = movableHeaderNames;
        mMovableListColumnsWidth = new int[movableHeaderNames.length];
        for (int i = 0; i < movableHeaderNames.length; i++) {
            mMovableListColumnsWidth[i] = dip2px(context, mMoveViewWidth);
        }
        mFixLeftListColumnsWidth = new int[]{dip2px(context, mFixViewWidth)};
        mFixLeftListColumnsText = fixedHeaderNames;
    }


    private OnHeaderClickedListener onHeaderClickedListener = null;

    public OnHeaderClickedListener getOnHeaderClickedListener() {
        return onHeaderClickedListener;
    }

    public void setOnHeaderClickedListener(OnHeaderClickedListener onHeaderClickedListener) {
        this.onHeaderClickedListener = onHeaderClickedListener;
    }

    /**
     * 列头点击事件
     */
    public interface OnHeaderClickedListener {
        public void onHeadViewClick(String string);

    }

    private OnLoadMoreListener onLoadMoreListener;

    public OnLoadMoreListener getOnLoadMoreListener() {
        return onLoadMoreListener;
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    public interface OnLoadMoreListener {
        void onLoadingMore();
    }

    /**
     * listview item单击事件
     */
    public interface OnItemClickedListener {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id);

    }

    /**
     * listview item单击事件
     */
    public interface OnItemLongClickedListener {
        public void onItemLongClick(AdapterView<?> parent, View view, int position, long id);

    }

}

