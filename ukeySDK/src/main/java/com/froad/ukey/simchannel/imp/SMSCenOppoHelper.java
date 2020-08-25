package com.froad.ukey.simchannel.imp;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ConditionVariable;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

import com.froad.ukey.constant.FConstant;
import com.froad.ukey.interf.CardConCallBack;
import com.froad.ukey.jni.tmjni;
import com.froad.ukey.manager.SIMBaseManager;
import com.froad.ukey.utils.np.FCharUtils;
import com.froad.ukey.utils.np.TMKeyLog;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SMSCenOppoHelper
 *
 * 短信中心号码通道处理
 *
 * @author  by FW.
 * @date    16/12/26
 * @modify  修改者 FW
 */

public class SMSCenOppoHelper extends SMSHelper {

	private static final String TAG = FConstant.LOG_TAG + "SMSCenOppoHelper";
	private Context mContext;
	private SmsManager mSmsManager= null;
	private CardConCallBack mCardConCallBack = null;

	private Class<?> mClass = null;
	private Method method1 = null;//_getAllMessagesFromIcc
	private Method method2 = null;//_updateMessageOnIcc

	private Method _getSmscAddressFromIcc = null;
	private Method _setSmscAddressToIcc = null;
	private Method _colorToIcc = null;
	private String smsCenterBak = null;

	private ConditionVariable conditionVariable = new ConditionVariable();
	private ArrayList<SmsMessage> localArrayList = null;

	public SMSCenOppoHelper () {}

	public SMSCenOppoHelper(Context con, CardConCallBack cardConCallBack){
		mContext = con;
		mCardConCallBack = cardConCallBack;
		mSmsManager = SmsManager.getDefault();
	}

	@Override
	public boolean open() {
		TMKeyLog.d(TAG, "open");
		try {
			isNeedShift = false;
			tmjni.openDeviceADN(this, int.class, int.class, byte[].class);

			Method getDefault = this.mClass.getMethod("getSmsManagerForSubscriber", new Class[] { Integer.TYPE });
			this.mSmsManager = ((SmsManager)getDefault.invoke(null, new Object[] { Integer.valueOf(1) }));
			this._getSmscAddressFromIcc = this.mClass.getMethod("getSmscAddressFromIcc", new Class[]{});
			this._setSmscAddressToIcc = this.mClass.getMethod("setSmscAddressToIcc", new Class[] { String.class });
			this._colorToIcc = this.mClass.getMethod("colorCopyTextMessageToIccCard",
					new Class[] { String.class, String.class, List.class, Integer.TYPE, Long.TYPE });
			if (this.mClass != null)
			{
				if (this._getSmscAddressFromIcc != null) {
					this.smsCenterBak = getSmscAddressFromIcc();
					TMKeyLog.d(TAG, "smsCenterBak:" + smsCenterBak);
				} else {
					isOpen = false;
					return isOpen;
				}
			}
			isOpen = hasCard();
			if (isOpen) { //有卡，通道打开成功
				mCardConCallBack.AdnOpenState(true, "SMSP通道连接成功");//连接成功
			}else{
				isOpen = false;
				mCardConCallBack.AdnOpenState(false, "SMSP通道连接失败");//连接失败
			}
		} catch (Exception e) {
			isOpen = false;
			mCardConCallBack.AdnOpenState(false, "SMSP通道连接失败");//连接失败
			e.printStackTrace();
		}
		return isOpen;
	}

	@Override
	public boolean close() {
		return true;
	}

	/**
	 * 短信方式传输数据
	 *
	 * @param hexStr
	 * @return
     */
	@Override
	public boolean transmitHexData(String hexStr){
		TMKeyLog.d(TAG, "transmitHexData...");
		if(! isOpen){
			return false;
		}

		int res = tmjni.transmitHexDataADN(this, hexStr);
		if (res == 0) {
			return true;
		}
		return false;
	}

	@Override
	public ContentValues getContentValues(String hexStr){
		TMKeyLog.i(TAG, "getContentValues...");
		return super.getContentValues(hexStr);
	}

	@Override
	public boolean insetContentValues(List<ContentValues> list) throws Exception{
		TMKeyLog.i(TAG, "insetContentValues...");

		ContentResolver contentResolver = mContext.getContentResolver();
		Map<String, Object> map = null;
		Uri localUri = null;
		Cursor localCursor = null;
		
		TMKeyLog.i(TAG, "before insert list size:" + list.size());
		ContentValues contentValues = null;

		for(int i=0;i<list.size();i++){
			TMKeyLog.i(TAG, "insetContentValues =================< "+ i + " >=================");
			contentValues = list.get(i);
			if(contentValues == null) {
				TMKeyLog.i(TAG, "contentValues is null");
				return false;
			}
			map =getADNCursor();
			if(map == null) {
				TMKeyLog.i(TAG, "map is null");
				return false;
			}

			localUri = (Uri) map.get("uri");
			localCursor =  (Cursor) map.get("cursor");
			/********** 先插入，失败后再更新 **********/
			boolean writeADNRes = writeADNData(localCursor,localUri, contentResolver, contentValues);
			TMKeyLog.d(TAG, "writeADNRes:" + writeADNRes);
			/********** 先插入，失败后再更新 **********/
		}
		return true;
	}

	private Map<String, Object> getADNCursor(){
		TMKeyLog.d(TAG, "getADNCursor");
		Map<String, Object> map = null;
		ContentResolver contentResolver = mContext.getContentResolver();

		Uri localUri = null;
		localUri = tmjni.initUriADN ();
		if (localUri == null) {
			TMKeyLog.d(TAG, "localUri is null");
			return null;
		}
		Cursor localCursor = contentResolver.query(localUri, null, null, null, null);
		if(localUri !=null && localCursor != null){
			TMKeyLog.d(TAG, "localUri is not null, localCursor is not null");
			map= new HashMap();
			map.put("uri", localUri);
			map.put("cursor", localCursor);
		}
		return map;
	}

	@Override
	public String receiveData() {
		return null;
	}

	@Override
	public List<String> receiveDataList() {
		TMKeyLog.i(TAG, "receiveDataList");
		ArrayList<String> list = new ArrayList();
		String res = "";
		//获取所有短信内容
		ArrayList<SmsMessage> localArrayList = method1();
		if (localArrayList == null) {
			TMKeyLog.e(TAG, "localArrayList is null");
			return list;
		}
		if (localArrayList.size() == 0) {
			TMKeyLog.e(TAG, "localArrayList size is 0");
			return list;
		}
		SmsMessage message = null;
		for(int i=0;i<localArrayList.size();i++){
			message = localArrayList.get(i);
			TMKeyLog.d(TAG, "message[" + i + "].getIndexOnSim is " + message.getIndexOnSim() + ">>>getIndexOnIcc:" + message.getIndexOnIcc());
			if (message != null) {
				TMKeyLog.e(TAG, "localSmsMessage is not null");
				byte[] arrayOfByte1 = myGetPDU(message);

				if (arrayOfByte1 != null) {
					res = FCharUtils.showResult16Str(arrayOfByte1);
				}
				TMKeyLog.e(TAG, "res=="+res);
				//以jni实现
				long recRes = tmjni.receiveDataListADN(res, message, list);
				if (recRes == 0) {
					continue;
				} else if (recRes == 2) {//jni异常
					return list;
				}
			} else {
				TMKeyLog.e(TAG, "localArrayList.get(" + i + ") is null");
			}
		}

		TMKeyLog.i(TAG, "list.size:" + list.size());
		return list;
	}

	/**
	 * 判断是否有卡
	 *
	 * @return true 有卡; false 无卡
     */
	private boolean hasCard() {
		TMKeyLog.d(TAG, "hasCard...");
		adnWriteType = 1;
		ArrayList<SmsMessage> localArrayList = method1();
		if (localArrayList == null){
			TMKeyLog.e(TAG, "localarraylist is null");
			return false;
		}
		int al = localArrayList.size();
		TMKeyLog.i(TAG, "localArrayList.size:" + al);
		if (al >= 1) {
			for(int i=0;i<al;i++){
				TMKeyLog.i(TAG, "i:" + i);
				SmsMessage localSmsMessage = localArrayList.get(i);
				if(localSmsMessage !=null){
					byte[] arrayOfByte =  myGetPDU(localSmsMessage);
					TMKeyLog.d(TAG, "getMessageBody:" + localSmsMessage.getMessageBody()
							+ ">>>getOriginatingAddress:" + localSmsMessage.getOriginatingAddress()
					);
					if (arrayOfByte != null) {
						String str = FCharUtils.showResult16Str(arrayOfByte);

						int cRes = tmjni.hasCardADN(str);
						if (cRes == 0) {
							TMKeyLog.i(TAG, "hasCard true>>>isNeedShift:"+isNeedShift);
							if (Integer.parseInt(SIMBaseManager.CardSmsVersion, 16) > SIMBaseManager.MAXCARDVERSION) { //大于客户端可识别的最大版本号
								TMKeyLog.d(TAG, "Error---------> CardSmsVersion > MAXCARDVERSION");
								return false;
							}
							SIMBaseManager.parseCardInfo ();
							return true;
						}
					}
				}
			}
		}else{
			return false;
		}
		return false;
	}

	/**
	 * 获取短信的PDU数据
	 *
	 * @param paramSmsMessage
	 * @return 字节数组
     */
	private byte[] myGetPDU(SmsMessage paramSmsMessage){
		TMKeyLog.i(TAG, "myGetPDU>>>isNeedShift:" + isNeedShift);
		if (paramSmsMessage == null){
			TMKeyLog.e(TAG, "paramSmsMessage is null");
			return null;
		}
		TMKeyLog.d(TAG, "index is :" + paramSmsMessage.getIndexOnIcc());
		//以jni实现下面代码
		byte[] resByte = tmjni.myGetPDUADN(paramSmsMessage);
		TMKeyLog.e(TAG, "resByte:" + FCharUtils.showResult16Str(resByte));
		return resByte;
	}

	/**
	 * 获取所有短信数据
	 *
	 * @return 所有短信数据的集合
     */
	private ArrayList<SmsMessage> method1(){
		TMKeyLog.i(TAG, "method1");
		new Thread(new Runnable() {
			@Override
			public void run() {
				try{
					localArrayList = tmjni.method1ADN(SMSCenOppoHelper.this);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					conditionVariable.open();
				}
			}
		}).start();
		conditionVariable.close();
		boolean noTime = conditionVariable.block(READ_SMS_TIME_OUT);
		TMKeyLog.e(TAG, "noTime:" + noTime);
		if (noTime && localArrayList != null) {
			TMKeyLog.i(TAG, "localArrayList is not null>>>size:" + localArrayList.size());
			return localArrayList;
		}
		if (mCardConCallBack != null) {//连接超时回调
			mCardConCallBack.AdnOpenState(false, "ADN通道连接失败");//连接失败
		}
		TMKeyLog.e(TAG, "localArrayList is null");
		return null;
	}

	/**
	 * 短信中心号码方式发送数据
	 * @param sendStr
	 * @param init 将短信中心号码还原为系统的值
	 * @return
	 */
	public boolean writeBySmsCenter(String sendStr, boolean init) {
		TMKeyLog.d(TAG, "writeBySmsCenter...");
		//短信中心号码更新方式
		if ((this._setSmscAddressToIcc == null) || (this._colorToIcc == null)) {
			return false;
		}
		boolean rest = false;
		sendStr = "+98" + sendStr;
		String appendStr = ",145";
		if (this.smsCenterBak != null) {
			String[] ass = smsCenterBak.split(",");
			if (ass.length == 2) {
				appendStr = "," + ass[1];
			}
		}
		try {
			sendStr = "\"" + sendStr + "\"" + appendStr;
			TMKeyLog.d(TAG, "writeBySmsCenter>>>sendStr:" + sendStr);
			rest = setSmscAddressToIcc(sendStr);
		} catch (Exception e) {
			TMKeyLog.d(TAG, "writeBySmsCenter>>>e1:" + e.getMessage());
		}
		TMKeyLog.d(TAG, "writeBySmsCenter>>>rest:" + rest);
		try {
			if (this.smsCenterBak != null && (init || ! rest)) {
				boolean writeRes = setSmscAddressToIcc(this.smsCenterBak);
				TMKeyLog.d(TAG, "writeBySmsCenter>>>writeRes:" + writeRes);
			}
		} catch (Exception e) {
			TMKeyLog.d(TAG, "writeBySmsCenter>>>e2:" + e.getMessage());
		}
		return rest;
	}

	private boolean setSmscAddressToIcc(String msmc) throws Exception{
		TMKeyLog.d(TAG, "setSmscAddressToIcc...");
		boolean ret = false;
		ret = ((Boolean)this._setSmscAddressToIcc.invoke(this.mSmsManager, new Object[] { msmc })).booleanValue();
		TMKeyLog.d("pagekpang", "smsc:" + msmc);
		return ret;
	}

	private String getSmscAddressFromIcc() {
		TMKeyLog.d(TAG, "getSmscAddressFromIcc...");
		String ret = null;
		try {
			ret = (String)this._getSmscAddressFromIcc.invoke(this.mSmsManager, new Object[]{});
		} catch (Exception localException) {
		}
		return ret;
	}

	public boolean isSupport()
	{
		TMKeyLog.d(TAG, "isSupport...");
//		return false;
		try
		{
			this.mClass = Class.forName("android.telephony.SmsManager");

			this._getSmscAddressFromIcc = this.mClass.getMethod("getSmscAddressFromIcc", new Class[]{});
			this._setSmscAddressToIcc = this.mClass.getMethod("setSmscAddressToIcc", new Class[] { String.class });
			this._colorToIcc = this.mClass.getMethod("colorCopyTextMessageToIccCard",
					new Class[] { String.class, String.class, List.class, Integer.TYPE, Long.TYPE });
		} catch (Exception e) {
			return false;
		}
		return (this._getSmscAddressFromIcc != null) && (this._setSmscAddressToIcc != null) && (this._colorToIcc != null);
	}
}
