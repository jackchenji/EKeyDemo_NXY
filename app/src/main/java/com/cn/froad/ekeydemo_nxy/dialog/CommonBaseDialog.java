package com.cn.froad.ekeydemo_nxy.dialog;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.cn.froad.ekeydemo_nxy.R;
import com.cn.froad.ekeydemo_nxy.utils.RefResTool;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * @author Created by Simen.
 * @date 创建日期 2017/7/25 14:52
 * @modify 修改者 Simen
 */
public class CommonBaseDialog extends BaseDialog {

    private static final String TAG = "CommonBaseDialog";
    private  boolean isAlert;
    @BindView(R.id.right_tv)
    TextView right_tv;
    @BindView(R.id.left_tv)
    TextView left_tv;
    @BindView(R.id.common_dialog_title)
    TextView contentText;
    @BindView(R.id.one_button_container)
    View oneButton;
    @BindView(R.id.two_buttons_container)
    View twoButton;
    @BindView(R.id.alert_tv)
    TextView alert_tv;

    private String titleText;
    private String leftButtonText;
    private String rightButtonText;
    private String contentTextViewText;

    private onDialogBtnListener mOnDialogBtnListener;
    private TextView contentView;

    public CommonBaseDialog(@NonNull Context context, boolean isAlert, String content, String leftBtnText, String rightBtnText) {
        this(context, isAlert, null, content, leftBtnText, rightBtnText);
    }
    public CommonBaseDialog(@NonNull Context context, boolean isAlert, String content, @StringRes int leftBtnTextId, @StringRes int rightBtnTextId) {

        this(context, isAlert, null, content, RefResTool.getString(leftBtnTextId), RefResTool.getString(rightBtnTextId));
    }

    public CommonBaseDialog(@NonNull Context context, boolean isAlert, String title, String content, String leftBtnText, String rightBtnText) {
        super(context);
        this.leftButtonText = leftBtnText;
        this.rightButtonText = rightBtnText;
        this.contentTextViewText = content;
        this.isAlert = isAlert;
        this.titleText = title;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (! TextUtils.isEmpty(titleText)) {
            setContentView(R.layout.common_base_dialog_with_title_layout);
            contentView =  findViewById(R.id.common_dialog_content);
            ButterKnife.bind(this);
            contentText.setText(Html.fromHtml(titleText));
            contentView.setText(Html.fromHtml(contentTextViewText));
        }
        else {
            setContentView(R.layout.common_base_dialog_layout);
            ButterKnife.bind(this);
            contentText.setText(Html.fromHtml(contentTextViewText));
        }
        if (isAlert) {
            oneButton.setVisibility(View.VISIBLE);
            twoButton.setVisibility(View.GONE);
            alert_tv.setText(leftButtonText);
        }
        else {
            oneButton.setVisibility(View.GONE);
            twoButton.setVisibility(View.VISIBLE);
            right_tv.setText(rightButtonText);
            left_tv.setText(leftButtonText);
        }
    }

    public void setTitleText(String titleText) {
        this.titleText = titleText;
        setViewText(contentText,Html.fromHtml(titleText));
    }

    public void setRightButtonText(String rightButtonText) {
        this.rightButtonText = rightButtonText;
        setViewText(right_tv,rightButtonText);
    }

    public void setLeftButtonText(String leftButtonText) {
        this.leftButtonText = leftButtonText;
        setViewText(left_tv,leftButtonText);
        setViewText(alert_tv,leftButtonText);
    }

    public static void setViewText (TextView view, CharSequence text){
        if (view!=null){
            view.setText(text);
        }
    }

    public void setContentTextViewText(String contentTextViewText) {
        this.contentTextViewText = contentTextViewText;
        if (! TextUtils.isEmpty(titleText)) {
            setViewText(contentText,Html.fromHtml(titleText));
            setViewText(contentView,Html.fromHtml(contentTextViewText));
        }
        else {
            setViewText(contentText,Html.fromHtml(contentTextViewText));
        }
    }

    public void clearAllText (){
        setContentTextViewText("");
        setLeftButtonText("");
        setRightButtonText("");
        setTitleText("");
    }

    public void setAlert(boolean alert) {
        isAlert = alert;
        if (isAlert) {
            setViewVisibility(oneButton,View.VISIBLE);
            setViewVisibility(twoButton,View.GONE);
            setViewText(alert_tv,leftButtonText);
        }
        else {
            setViewVisibility(oneButton,View.GONE);
            setViewVisibility(twoButton,View.VISIBLE);
            setViewText(right_tv,rightButtonText);
            setViewText(left_tv,leftButtonText);
        }
    }
    public static void setViewVisibility(View view , int visibility ){
        if (view!=null){
            view.setVisibility(visibility);
        }
    }

    public void setOnDialogBtnListener(onDialogBtnListener mOnDialogBtnListener) {
        this.mOnDialogBtnListener = mOnDialogBtnListener;
    }

    @OnClick({R.id.right_tv, R.id.alert_tv, R.id.left_tv})
    public void onViewClick(View view) {
        if (view.getId() == R.id.right_tv || view.getId() == R.id.alert_tv) {
            if (mOnDialogBtnListener != null) {
                mOnDialogBtnListener.onRightComplete(this, view);
            }
        }
        else if (view.getId() == R.id.left_tv) {
            if (mOnDialogBtnListener != null) {
                mOnDialogBtnListener.onLeftComplete(this, view);
            }
        }
    }
}
