package com.kisen.lib;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.animation.Interpolator;

/**
 * 标题：自动滚动RecyclerView
 * 作者：kisen
 * 版本：
 * 创建时间：on 2017/6/30 13:29.
 */
public class AutoScrollRecyclerView extends RecyclerView {

    private static final String TAG = AutoScrollRecyclerView.class.getSimpleName();
    private static final int SPEED = 100;
    /**
     * 滑动估值器
     */
    private UniformSpeedInterpolator mInterpolator;
    /**
     * 单位之间滑动的dx和dy
     */
    private int mSpeedDx, mSpeedDy;
    /**
     * 滑动速度，默认100
     */
    private int currentSpeed = SPEED;
    /**
     * 是否无限循环显示列表
     */
    private boolean mLoopEnabled;
    /**
     * 是否反向滑动
     */
    private boolean mReverse;
    /**
     * 是否开启自动滑动
     */
    private boolean isOpenAuto;
    /**
     * 用户是否可以手动滑动屏幕
     */
    private boolean mCanScrollByTouch;
    /**
     * 用户是否点击屏幕
     */
    private boolean mPointTouch;

    public AutoScrollRecyclerView(Context context) {
        this(context, null);
    }

    public AutoScrollRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoScrollRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mInterpolator = new UniformSpeedInterpolator();
    }

    /**
     * 开始滑动
     */
    public void openAutoScroll() {
        openAutoScroll(currentSpeed, false);
    }

    /**
     * 开始滑动
     *
     * @param speed   滑动距离（决定滑动速度）
     * @param reverse 是否反向滑动
     */
    public void openAutoScroll(int speed, boolean reverse) {
        mReverse = reverse;
        currentSpeed = speed;
        isOpenAuto = true;
        notifyLayoutManager();
    }

    /**
     * 自动滑动时是否可以手动滑动
     */
    public void canScrollByTouch(boolean b) {
        mCanScrollByTouch = b;
    }

    /**
     * 设置是否无限循环显示列表
     */
    public void setLoopEnabled(boolean loopEnabled) {
        this.mLoopEnabled = loopEnabled;
    }

    /**
     * 是否无限滑动
     */
    public boolean isLoopEnabled() {
        return mLoopEnabled;
    }

    /**
     * 设置是否反向
     */
    public void setReverse(boolean reverse) {
        mReverse = reverse;
        notifyLayoutManager();
    }

    private void startSmoothScroll() {
        int absSpeed = Math.abs(currentSpeed);
        int d = mReverse ? -absSpeed : absSpeed;
        smoothScrollBy(d, d, mInterpolator);
    }

    private void notifyLayoutManager() {
        LayoutManager layoutManager = getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            ((LinearLayoutManager) layoutManager).setReverseLayout(mReverse);
        } else {
            ((StaggeredGridLayoutManager) layoutManager).setReverseLayout(mReverse);
        }
    }

    @Override
    public void swapAdapter(Adapter adapter, boolean removeAndRecycleExistingViews) {
        super.swapAdapter(generateAdapter(adapter), removeAndRecycleExistingViews);
    }

    @Override
    public void setAdapter(Adapter adapter) {
        super.setAdapter(generateAdapter(adapter));
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if (mCanScrollByTouch) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mPointTouch = true;
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    if (isOpenAuto) {
                        return true;
                    }
            }
            return super.onInterceptTouchEvent(e);
        } else return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (mCanScrollByTouch) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (isOpenAuto) {
                        mPointTouch = false;
                        startSmoothScroll();
                        return true;
                    }
            }
            return super.onTouchEvent(e);
        } else return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (isOpenAuto)
            startSmoothScroll();
    }

    @Override
    public void onScrolled(int dx, int dy) {
        if (mPointTouch) {
            mSpeedDx = 0;
            mSpeedDy = 0;
            return;
        }
        boolean vertical;
        if (dx == 0) {//垂直滚动
            mSpeedDy += dy;
            vertical = true;
        } else {//水平滚动
            mSpeedDx += dx;
            vertical = false;
        }

        if (vertical) {
            if (Math.abs(mSpeedDy) >= Math.abs(currentSpeed)) {
                mSpeedDy = 0;
                startSmoothScroll();
            }
        } else {
            if (Math.abs(mSpeedDx) >= Math.abs(currentSpeed)) {
                mSpeedDx = 0;
                startSmoothScroll();
            }
        }
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private NestingRecyclerViewAdapter generateAdapter(Adapter adapter) {
        return new NestingRecyclerViewAdapter(this, adapter);
    }

    /**
     * 自定义估值器
     * 使列表匀速滑动
     */
    private static class UniformSpeedInterpolator implements Interpolator {
        @Override
        public float getInterpolation(float input) {
            return input;
        }
    }

    /**
     * 自定义Adapter容器，使列表可以无限循环显示
     */
    private static class NestingRecyclerViewAdapter<VH extends RecyclerView.ViewHolder>
            extends RecyclerView.Adapter<VH> {

        private AutoScrollRecyclerView mRecyclerView;
        RecyclerView.Adapter<VH> mAdapter;


        NestingRecyclerViewAdapter(AutoScrollRecyclerView recyclerView, RecyclerView.Adapter<VH> adapter) {
            mAdapter = adapter;
            mRecyclerView = recyclerView;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            return mAdapter.onCreateViewHolder(parent, viewType);
        }

        @Override
        public void registerAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
            super.registerAdapterDataObserver(observer);
            mAdapter.registerAdapterDataObserver(observer);
        }

        @Override
        public void unregisterAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
            super.unregisterAdapterDataObserver(observer);
            mAdapter.unregisterAdapterDataObserver(observer);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            mAdapter.onBindViewHolder(holder, generatePosition(position));
        }

        @Override
        public void setHasStableIds(boolean hasStableIds) {
            super.setHasStableIds(hasStableIds);
            mAdapter.setHasStableIds(hasStableIds);
        }

        @Override
        public int getItemCount() {
            //如果是无限滚动模式，设置有无限个item
            return getLoopEnable() ? Integer.MAX_VALUE : mAdapter.getItemCount();
        }

        @Override
        public int getItemViewType(int position) {
            return mAdapter.getItemViewType(generatePosition(position));
        }

        @Override
        public long getItemId(int position) {
            return mAdapter.getItemId(generatePosition(position));
        }

        /**
         * 根据当前滚动模式返回对应position
         */
        private int generatePosition(int position) {
            return getLoopEnable() ? getActualPosition(position) : position;
        }

        /**
         * 返回Item实际的位置
         *
         * @param position 开始滚动以后的位置，会无限增长
         * @return Item实际位置
         */
        private int getActualPosition(int position) {
            int itemCount = mAdapter.getItemCount();
            return position >= itemCount ? position % itemCount : position;

        }

        private boolean getLoopEnable() {
            return mRecyclerView.mLoopEnabled;
        }
    }

}
