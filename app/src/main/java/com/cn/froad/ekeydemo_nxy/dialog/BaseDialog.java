package com.cn.froad.ekeydemo_nxy.dialog;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.view.View;
import android.widget.Toast;

import com.cn.froad.ekeydemo_nxy.R;

/**
 * @author Created by Simen.
 * @date 创建日期 2017/7/25 14:51
 * @modify 修改者 Simen
 */
public class BaseDialog extends Dialog {

    public interface onDialogBtnListener {
        void onRightComplete(BaseDialog dialog, View view);
        void onLeftComplete(BaseDialog dialog, View view);
    }

    public static class DialogListener implements onDialogBtnListener{

        @Override
        public void onRightComplete(BaseDialog dialog, View view) {

        }

        @Override
        public void onLeftComplete(BaseDialog dialog, View view) {

        }
    }

    public BaseDialog(@NonNull Context context) {
        //默认设置无边框、背景透明、全屏、无标题的样式
        super(context, R.style.TransparentDialog);
    }

    public BaseDialog(@NonNull Context context, @StyleRes int themeResId) {
        super(context, themeResId);
    }

}
