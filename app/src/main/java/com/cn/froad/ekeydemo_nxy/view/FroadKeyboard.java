/**
 *
 */
package com.cn.froad.ekeydemo_nxy.view;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.cn.froad.ekeydemo_nxy.R;
import com.froad.ukey.utils.np.TMKeyLog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * 输入密码键盘，有按下效果
 */
public class FroadKeyboard extends View implements OnKeyboardActionListener {

      private String TAG = "FroadKeyboard";
      private Context mContext;
      private View contentView;
      private KeyboardView mKeyboardView;
      private Keyboard mKeyboard;

      private TextView tv_title;
      private Button btn_neg, btn_pos;//取消
      private EditText p1, p2, p3, p4, p5, p6;
      private ImageView point1, point2, point3, point4, point5, point6;


      private View mParentView;
      private PopupWindow mKeyboardWindow;

      private boolean randomkeys = true;    //数字按键是否随机
      public static int screenw = -1;//未知宽高
      public static int screenh = -1;
      public static int screenh_nonavbar = -1;    //不包含导航栏的高度
      public static int real_scontenth = -1;    //实际内容高度，  计算公式:屏幕高度-导航栏高度-电量栏高度

      public static float density = 1.0f;
      public static int densityDpi = 160;

      private OnConfirmClickListener onConfirmClickListener;

      public FroadKeyboard(Context context, View view) {
            super(context);
            initAttributes(context);
            initKeyboard(context);
            this.mContext = context;
            this.mParentView = view;
      }

      /**
       * @param context
       * @param attrs
       */
      public FroadKeyboard(Context context, AttributeSet attrs) {
            super(context, attrs);
            initAttributes(context);
            initKeyboard(context);
            this.mContext = context;
      }

      /**
       * @param context
       * @param attrs
       * @param defStyle
       */
      public FroadKeyboard(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            initAttributes(context);
            initKeyboard(context);
            this.mContext = context;
      }

      private void initKeyboard(Context context) {
            mKeyboard = new Keyboard(context, R.xml.froadinputkeyboard);

            contentView = LayoutInflater.from(context).inflate(R.layout.froad_keyboard_view, null);
            mKeyboardView = (KeyboardView) contentView.findViewById(R.id.keyboard_view);


            p1 = (EditText) contentView.findViewById(R.id.password1);
            p2 = (EditText) contentView.findViewById(R.id.password2);
            p3 = (EditText) contentView.findViewById(R.id.password3);
            p4 = (EditText) contentView.findViewById(R.id.password4);
            p5 = (EditText) contentView.findViewById(R.id.password5);
            p6 = (EditText) contentView.findViewById(R.id.password6);

            point1 = (ImageView) contentView.findViewById(R.id.point1);
            point2 = (ImageView) contentView.findViewById(R.id.point2);
            point3 = (ImageView) contentView.findViewById(R.id.point3);
            point4 = (ImageView) contentView.findViewById(R.id.point4);
            point5 = (ImageView) contentView.findViewById(R.id.point5);
            point6 = (ImageView) contentView.findViewById(R.id.point6);

            btn_neg = (Button) contentView.findViewById(R.id.btn_neg);
            btn_pos = (Button) contentView.findViewById(R.id.btn_pos);
            tv_title = (TextView) contentView.findViewById(R.id.tv_title);


            btn_pos.setOnClickListener(new OnClickListener() {
                  @Override
                  public void onClick(View arg0) {
                        String password = submit();
                        TMKeyLog.d(TAG, "onClick>>>password:" + password);
                        if (!"".equals(password) && password != null) {
                              onConfirmClickListener.onClick(password);
                              hideKeyboard();
                              clear();
                        }
                  }
            });

            btn_neg.setOnClickListener(new OnClickListener() {
                  @Override
                  public void onClick(View arg0) {
                        clear();
                        hideKeyboard();
                  }
            });


            mKeyboardView.setKeyboard(mKeyboard);
            mKeyboardView.setEnabled(true);
            mKeyboardView.setPreviewEnabled(false);
            mKeyboardView.setOnKeyboardActionListener(this);

            mKeyboardWindow = new PopupWindow(contentView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mKeyboardWindow.setAnimationStyle(R.style.froad_animation_fade_style);
      }

      public void showKeyboard() {
            if (null != mKeyboardWindow) {
                  if (!mKeyboardWindow.isShowing()) {
                        if (randomkeys) {
                              randomdigkey(mKeyboard);
                        }
                        mKeyboardView.setKeyboard(mKeyboard);

                        mKeyboardWindow.showAtLocation(this.mParentView, Gravity.BOTTOM, 0, 0);
                        mKeyboardWindow.update();
                  }
            }
      }


      public void hideKeyboard() {
            if (null != mKeyboardWindow) {
                  if (mKeyboardWindow.isShowing()) {
                        mKeyboardWindow.dismiss();
                  }
            }
      }

      public boolean isShowing() {
            if (null != mKeyboardWindow) {
                  return mKeyboardWindow.isShowing();
            }
            return false;
      }


      public void onDetachedFromWindow() {
            super.onDetachedFromWindow();

            hideKeyboard();

            mKeyboardWindow = null;
            mKeyboardView = null;
            mKeyboard = null;

            mParentView = null;
      }

      @Override
      public void onPress(int primaryCode) {

      }

      @Override
      public void onRelease(int primaryCode) {

      }

      @Override
      public void onKey(int primaryCode, int[] keyCodes) {
            if (primaryCode == Keyboard.KEYCODE_CANCEL) {// 隐藏键盘
                  clear();
                  hideKeyboard();
            } else if (primaryCode == Keyboard.KEYCODE_DELETE) {// 回退
                  del();
            } else if (0x0 <= primaryCode && primaryCode <= 0x7f) {
                  //可以直接输入的字符(如0-9,.)，他们在键盘映射xml中的keycode值必须配置为该字符的ASCII码
                  setValue(Character.toString((char) primaryCode));
            } else if (primaryCode > 0x7f) {
                  //Key mkey = getKeyByKeyCode(primaryCode);
                  //可以直接输入的字符(如0-9,.)，他们在键盘映射xml中的keycode值必须配置为该字符的ASCII码
                  //editable.insert(start, mkey.label);
            } else {
                  //其他一些暂未开放的键指令，如next到下一个输入框等指令
            }
      }

      @Override
      public void onText(CharSequence text) {
      }

      @Override
      public void swipeLeft() {
      }

      @Override
      public void swipeRight() {
      }

      @Override
      public void swipeDown() {
      }

      @Override
      public void swipeUp() {
      }

      private void initAttributes(Context context) {
            initScreenParams(context);
            this.setLongClickable(false);
      }

      private boolean isNumber(String str) {
            String wordstr = "0123456789";
            if (wordstr.indexOf(str) > -1) {
                  return true;
            }
            return false;
      }

      private void randomdigkey(Keyboard mKeyboard) {
            if (mKeyboard == null) {
                  return;
            }

            List<Key> keyList = mKeyboard.getKeys();

            // 查找出0-9的数字键
            List<Key> newkeyList = new ArrayList<Key>();
            for (int i = 0, size = keyList.size(); i < size; i++) {
                  Key key = keyList.get(i);
                  CharSequence label = key.label;
                  if (label != null && isNumber(label.toString())) {
                        newkeyList.add(key);
                  }
            }

            int count = newkeyList.size();

            List<KeyModel> resultList = new ArrayList<KeyModel>();

            LinkedList<KeyModel> temp = new LinkedList<KeyModel>();

            for (int i = 0; i < count; i++) {
                  temp.add(new KeyModel(48 + i, i + ""));
            }

            Random rand = new SecureRandom();
            rand.setSeed(SystemClock.currentThreadTimeMillis());

            for (int i = 0; i < count; i++) {
                  int num = rand.nextInt(count - i);
                  KeyModel model = temp.get(num);
                  resultList.add(new KeyModel(model.getCode(), model.getLable()));
                  temp.remove(num);
            }

            for (int i = 0, size = newkeyList.size(); i < size; i++) {
                  Key newKey = newkeyList.get(i);
                  KeyModel resultmodle = resultList.get(i);
                  newKey.label = resultmodle.getLable();
                  newKey.codes[0] = resultmodle.getCode();
            }
      }

      class KeyModel {

            private Integer code;
            private String label;

            public KeyModel(Integer code, String lable) {
                  this.code = code;
                  this.label = lable;
            }

            public Integer getCode() {
                  return code;
            }

            public void setCode(Integer code) {
                  this.code = code;
            }

            public String getLable() {
                  return label;
            }

            public void setLabel(String lable) {
                  this.label = lable;
            }
      }

      /**
       * 密度转换为像素值
       *
       * @param dp
       * @return
       */
      public static int dpToPx(Context context, float dp) {
            final float scale = context.getResources().getDisplayMetrics().density;
            return (int) (dp * scale + 0.5f);
      }

      /**
       * 计算密码键盘高度
       *
       * @param context
       */
      private void initScreenParams(Context context) {
            DisplayMetrics dMetrics = new DisplayMetrics();
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            display.getMetrics(dMetrics);

            screenw = dMetrics.widthPixels;
            screenh = dMetrics.heightPixels;
            density = dMetrics.density;
            densityDpi = dMetrics.densityDpi;

            screenh_nonavbar = screenh;

            int ver = Build.VERSION.SDK_INT;

            // 新版本的android 系统有导航栏，造成无法正确获取高度
            if (ver == 13) {
                  try {
                        Method mt = display.getClass().getMethod("getRealHeight");
                        screenh_nonavbar = (Integer) mt.invoke(display);
                  } catch (Exception e) {
                  }
            } else if (ver > 13) {
                  try {
                        Method mt = display.getClass().getMethod("getRawHeight");
                        screenh_nonavbar = (Integer) mt.invoke(display);
                  } catch (Exception e) {
                  }
            }

            real_scontenth = screenh_nonavbar - getStatusBarHeight(context);
      }

      /**
       * 电量栏高度
       *
       * @return
       */
      public static int getStatusBarHeight(Context context) {
            Class<?> c = null;
            Object obj = null;
            Field field = null;
            int x = 0,
                    sbar = 0;
            try {
                  c = Class.forName("com.android.internal.R$dimen");
                  obj = c.newInstance();
                  field = c.getField("status_bar_height");
                  x = Integer.parseInt(field.get(obj).toString());
                  sbar = context.getResources().getDimensionPixelSize(x);
            } catch (Exception e1) {
                  e1.printStackTrace();
            }

            return sbar;
      }


      /**
       * 确认时调用
       */
      public interface OnConfirmClickListener {
            void onClick(String password);
      }

      public void setOnConfirmClickListener(OnConfirmClickListener listener) {
            this.onConfirmClickListener = listener;
      }

      /**
       * 删除
       *
       */
      public void del() {
            if (!TextUtils.isEmpty(p6.getText())) {
                  p6.setText("");
                  addPoint(5);
            } else if (!TextUtils.isEmpty(p5.getText())) {
                  p5.setText("");
                  addPoint(4);
            } else if (!TextUtils.isEmpty(p4.getText())) {
                  p4.setText("");
                  addPoint(3);
            } else if (!TextUtils.isEmpty(p3.getText())) {
                  p3.setText("");
                  addPoint(2);
            } else if (!TextUtils.isEmpty(p2.getText())) {
                  p2.setText("");
                  addPoint(1);
            } else if (!TextUtils.isEmpty(p1.getText())) {
                  p1.setText("");
                  addPoint(0);
            }
      }

      /**
       * 设值
       *
       * @param 、1,2、3、4、5、6、7、8、9、0
       */
      private void setValue(String text) {
            if (TextUtils.isEmpty(p1.getText())) {
                  p1.setText(text);
                  addPoint(1);
            } else if (TextUtils.isEmpty(p2.getText())) {
                  p2.setText(text);
                  addPoint(2);
            } else if (TextUtils.isEmpty(p3.getText())) {
                  p3.setText(text);
                  addPoint(3);
            } else if (TextUtils.isEmpty(p4.getText())) {
                  p4.setText(text);
                  addPoint(4);
            } else if (TextUtils.isEmpty(p5.getText())) {
                  p5.setText(text);
                  addPoint(5);
            } else if (TextUtils.isEmpty(p6.getText())) {
                  p6.setText(text);
                  addPoint(6);
            }
      }

      /**
       * 清除键盘值
       */
      private void clear() {
            p1.setText("");
            p2.setText("");
            p3.setText("");
            p4.setText("");
            p5.setText("");
            p6.setText("");

            point1.setVisibility(View.GONE);
            point2.setVisibility(View.GONE);
            point3.setVisibility(View.GONE);
            point4.setVisibility(View.GONE);
            point5.setVisibility(View.GONE);
            point6.setVisibility(View.GONE);
            addPoint(0);
      }

      /**
       * 获取真正的密码
       *
       * @return
       */
      private String submit() {
            String password = p1.getText().toString() + p2.getText().toString()
                    + p3.getText().toString() + p4.getText().toString()
                    + p5.getText().toString() + p6.getText().toString();
            if (!TextUtils.isEmpty(password) && password.length() == 6) {
                  return password;
            }
            return null;
      }

      /**
       * 设置密码黑点
       *
       * @param position
       */
      private void addPoint(int position) {
            switch (position) {
                  case 0:
                        point1.setVisibility(View.GONE);
                        point2.setVisibility(View.GONE);
                        point3.setVisibility(View.GONE);
                        point4.setVisibility(View.GONE);
                        point5.setVisibility(View.GONE);
                        point6.setVisibility(View.GONE);
                        break;
                  case 1:
                        point1.setVisibility(View.VISIBLE);
                        point2.setVisibility(View.GONE);
                        point3.setVisibility(View.GONE);
                        point4.setVisibility(View.GONE);
                        point5.setVisibility(View.GONE);
                        point6.setVisibility(View.GONE);
                        break;
                  case 2:
                        point2.setVisibility(View.VISIBLE);
                        point1.setVisibility(View.VISIBLE);
                        point3.setVisibility(View.GONE);
                        point4.setVisibility(View.GONE);
                        point5.setVisibility(View.GONE);
                        point6.setVisibility(View.GONE);
                        break;
                  case 3:
                        point3.setVisibility(View.VISIBLE);
                        point1.setVisibility(View.VISIBLE);
                        point2.setVisibility(View.VISIBLE);
                        point4.setVisibility(View.GONE);
                        point5.setVisibility(View.GONE);
                        point6.setVisibility(View.GONE);
                        break;
                  case 4:
                        point4.setVisibility(View.VISIBLE);
                        point1.setVisibility(View.VISIBLE);
                        point2.setVisibility(View.VISIBLE);
                        point3.setVisibility(View.VISIBLE);
                        point5.setVisibility(View.GONE);
                        point6.setVisibility(View.GONE);
                        break;
                  case 5:
                        point4.setVisibility(View.VISIBLE);
                        point1.setVisibility(View.VISIBLE);
                        point2.setVisibility(View.VISIBLE);
                        point3.setVisibility(View.VISIBLE);
                        point5.setVisibility(View.VISIBLE);
                        point6.setVisibility(View.GONE);
                        break;
                  case 6:
                        point4.setVisibility(View.VISIBLE);
                        point1.setVisibility(View.VISIBLE);
                        point2.setVisibility(View.VISIBLE);
                        point3.setVisibility(View.VISIBLE);
                        point5.setVisibility(View.VISIBLE);
                        point6.setVisibility(View.VISIBLE);
                        break;
                  default:
                        break;
            }
      }

}
