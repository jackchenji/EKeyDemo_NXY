package com.cn.froad.ekeydemo_nxy.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.cn.froad.ekeydemo_nxy.R;

/**
 * 适用于在界面布局中始终保持指定尺寸
 * Created by zhangming on 16/7/25.
 */
public class FixedView extends LinearLayout {
    private static final String TAG = "FixedView";
    private int fixed_width = 0;
    private int fixed_height = 0;

    public FixedView(Context context) {
        this(context, null);
    }

    public FixedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FixedView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.fixed_size);
        try {
            fixed_width = typedArray.getDimensionPixelSize(R.styleable.fixed_size_android_layout_width, fixed_width);
            fixed_height = typedArray.getDimensionPixelSize(R.styleable.fixed_size_android_layout_height, fixed_height);
        } finally {
            typedArray.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (fixed_width != 0 || fixed_height != 0) {
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(fixed_width, MeasureSpec.EXACTLY);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(fixed_height, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }


}