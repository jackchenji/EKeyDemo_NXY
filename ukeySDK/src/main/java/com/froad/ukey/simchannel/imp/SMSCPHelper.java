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
 * smsCpHelper
 *
 * 短信与ADN通道的的管理类
 *
 * @author  by FW.
 * @date    16/12/26
 * @modify  修改者 FW
 */

public class SMSCPHelper extends SMSHelper {

	private static final String TAG = FConstant.LOG_TAG + "SMSCPHelper";
	private static SMSCPHelper smsCpHelper= null;
	private Context mContext;
	private SmsManager mSmsManager= null;
	private CardConCallBack mCardConCallBack = null;
	private final long CONNECT_TIME_OUT = 5000;//连接超时时间

	private Class<?> mClass = null;
	private Method method1 = null;//_getAllMessagesFromIcc
	private Method method2 = null;//_updateMessageOnIcc

	private ConditionVariable conditionVariable = new ConditionVariable();
	private ArrayList<SmsMessage> localArrayList = null;

	private SMSCPHelper(){
	}

	public static SMSCPHelper getInstance(Context con, CardConCallBack cardConCallBack) {
		TMKeyLog.d(TAG, "getInstance...");
		if(smsCpHelper == null) {
			TMKeyLog.d(TAG, "smsCpHelper is null...");
			smsCpHelper = new SMSCPHelper();
		} else {
			TMKeyLog.d(TAG, "smsCpHelper is not null...");
		}
		smsCpHelper.mContext = con;
		smsCpHelper.mCardConCallBack = cardConCallBack;
		smsCpHelper.mSmsManager = SmsManager.getDefault();
		return smsCpHelper;
	}

	@Override
	public boolean open() {
		TMKeyLog.d(TAG, "open");
		try {
			this.mClass = Class.forName("com.yulong.android.telephony.CPSmsManager");

			this.method1 = this.mClass.getMethod("updateDualMessageOnIccBySlotId", new Class[] { Integer.TYPE, Integer.TYPE, Byte[].class, Integer.TYPE });
			this.method2 = this.mClass.getMethod("getDualAllMessagesFromIcc", new Class[] { Integer.TYPE });

			isOpen = hasCard();
			if (isOpen) { //有卡，通道打开成功
				mCardConCallBack.AdnOpenState(true, "ADN通道连接成功");//连接成功
			}else{
				isOpen = false;
				mCardConCallBack.AdnOpenState(false, "ADN通道连接失败");//连接失败
			}
		} catch (Exception e) {
			isOpen = false;
			mCardConCallBack.AdnOpenState(false, "ADN通道连接失败");//连接失败
			e.printStackTrace();
		}
		return isOpen;
	}

	@Override
	public boolean close() {
		smsCpHelper = null;
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
		if(! isOpen){
			return false;
		}

		int res = tmjni.transmitHexDataADN(smsCpHelper, hexStr);
		if (res == 0) {
			return true;
		} else {
			return false;
		}
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
					localArrayList = tmjni.method1ADN(SMSCPHelper.this);
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

	public boolean isSupport()
	{
		try {
			this.mClass = Class.forName("com.yulong.android.telephony.CPSmsManager");
			return true;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}
}
