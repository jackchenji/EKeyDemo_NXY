package com.cn.froad.ekeydemo_nxy.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by chenji on 2018/10/12.
 */
public class SharedPrefs {
	Context con;
	private static final String SHARE_PREFERENCE_KEY_XGPUSH_ACCOUNT = "fft_sdk_sp_name";
	public SharedPrefs(Context context)
	{
		this.con=context;
	}


	/*存数据*/
	public  static void saveParameters(Context con, String spname, String spstr)
	{
		SharedPreferences sp= con.getSharedPreferences(SHARE_PREFERENCE_KEY_XGPUSH_ACCOUNT, Activity.MODE_PRIVATE);
		sp.edit().putString(spname,spstr).commit();
	}	
	/*获取数据*/
	public  static String getParameters(Context con, String spname)
	{
		SharedPreferences sp= con.getSharedPreferences(SHARE_PREFERENCE_KEY_XGPUSH_ACCOUNT, Activity.MODE_PRIVATE);
		String param=sp.getString(spname, "");
		return param;
	}

	/*删除数据*/
	public  static void removeParameters(Context con, String spname)
	{
		SharedPreferences sp=
		con.getSharedPreferences(SHARE_PREFERENCE_KEY_XGPUSH_ACCOUNT, Activity.MODE_PRIVATE);
		sp.edit().remove(spname).commit();
		}
}
