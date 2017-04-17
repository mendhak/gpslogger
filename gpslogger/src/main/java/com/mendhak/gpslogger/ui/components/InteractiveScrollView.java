package com.mendhak.gpslogger.ui.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

public class InteractiveScrollView extends ScrollView {

    private OnBottomReachedListener onBottomReachedListener;
    private OnTopReachedListener onTopReachedListener;
    private OnScrolledDownListener onScrolledDownListener;
    private OnScrolledUpListener onScrolledUpListener;

    public InteractiveScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public InteractiveScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public InteractiveScrollView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setFadingEdgeLength(0);
        setVerticalFadingEdgeEnabled(false);
        setHorizontalFadingEdgeEnabled(false);
        setOverScrollMode(ScrollView.OVER_SCROLL_NEVER);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        View view = (View) getChildAt(getChildCount()-1);
        int diff = (view.getBottom()-(getHeight()+getScrollY()));

        if (Math.abs(diff) <= 20 && onBottomReachedListener != null) {
            onBottomReachedListener.onBottomReached(t);
        } else if (getScrollY() <= 0 && onTopReachedListener != null) {
            onTopReachedListener.onTopReached(t);
        } else{
            if(t<oldt){
                if (onScrolledUpListener != null){
                    onScrolledUpListener.onScrolledUp(t);
                }

            }else if (t>oldt){
                if (onScrolledDownListener != null){
                    onScrolledDownListener.onScrolledDown(t);
                }
            }
        }

        super.onScrollChanged(l, t, oldl, oldt);
    }
    //Getters & Setters

    public OnBottomReachedListener getOnBottomReachedListener() {
        return onBottomReachedListener;
    }

    public void setOnBottomReachedListener(OnBottomReachedListener onBottomReachedListener) {
        this.onBottomReachedListener = onBottomReachedListener;
    }

    public OnTopReachedListener getOnTopReachedListener() {
        return onTopReachedListener;
    }

    public void setOnTopReachedListener(OnTopReachedListener onTopReachedListener) {
        this.onTopReachedListener = onTopReachedListener;
    }

    public void setOnScrolledDownListener(OnScrolledDownListener onScrolledDownListener){
        this.onScrolledDownListener = onScrolledDownListener;
    }

    public OnScrolledDownListener getOnScrolledDownListener() {
        return onScrolledDownListener;
    }

    public void setOnScrolledUpListener(OnScrolledUpListener onScrolledUpListener){
        this.onScrolledUpListener = onScrolledUpListener;
    }

    public OnScrolledUpListener getOnScrolledUpListener() {
        return onScrolledUpListener;
    }


    /**
     * Event listener.
     */
    public interface OnBottomReachedListener{
        public void onBottomReached(int scrollY);
    }
    public interface OnTopReachedListener{
        public void onTopReached(int scrollY);
    }
    public interface OnScrolledDownListener{
        public void onScrolledDown(int scrollY);
    }
    public interface OnScrolledUpListener{
        public void onScrolledUp(int scrollY);
    }
}