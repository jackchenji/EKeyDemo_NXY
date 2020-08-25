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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * SMSHelper
 *
 * 短信与ADN通道的的管理类
 *
 * @author  by FW.
 * @date    16/12/26
 * @modify  修改者 FW
 */

public class SMSIccEFHelper extends SMSHelper {

	private static final String TAG = FConstant.LOG_TAG + "SMSIccEFHelper";
	private Context mContext;
	private SmsManager mSmsManager= null;
	private CardConCallBack mCardConCallBack = null;

	private Class<?> mClass = null;
	private Method _updateMessageOnIcc = null;//_updateMessageOnIcc
	private Method _getAllMessagesFromIccEfByMode = null;//_getAllMessagesFromIccEfByMode

	private int mSubID = 1;

	private ConditionVariable conditionVariable = new ConditionVariable();
	private ArrayList<SmsMessage> localArrayList = null;

	public SMSIccEFHelper () {}

	public SMSIccEFHelper(Context con, CardConCallBack cardConCallBack){
		mContext = con;
		mCardConCallBack = cardConCallBack;
	}

	@Override
	public boolean open() {
		TMKeyLog.d(TAG, "open");
		try {
			isNeedShift = false;
			mSmsManager = SmsManager.getDefault();
			this.mClass = Class.forName("android.telephony.SmsManager");
			this._updateMessageOnIcc = mClass.getMethod("updateMessageOnIcc", int.class, int.class, byte[].class);
			this._getAllMessagesFromIccEfByMode = mClass.getMethod("getAllMessagesFromIccEfByMode", new Class[] { Integer.TYPE });

			Method[] arrayOfMethod = this.mClass.getMethods();
			for (int i = 0; i < arrayOfMethod.length; i++) {
				TMKeyLog.i(TAG, "arrayOfMethod[" + i + "]:" +arrayOfMethod[i].getName());
			}

			int sid = 1;
			try
			{
				Class SubscriptionManagerClass = Class.forName("android.telephony.SubscriptionManager");
				Class SubscriptionInfoClass = Class.forName("android.telephony.SubscriptionInfo");
				Method methodFrom = SubscriptionManagerClass.getDeclaredMethod("from", new Class[] { Context.class });
				Method methodgetActiveSubscriptionInfoList = SubscriptionManagerClass.getMethod("getActiveSubscriptionInfoList", new  Class[0]);
				Method methodgetSimSlotIndex = SubscriptionInfoClass.getMethod("getSimSlotIndex", new  Class[0]);
				Method methodgetSubscriptionId = SubscriptionInfoClass.getMethod("getSubscriptionId", new  Class[0]);

				Object subscriptionManager = methodFrom.invoke(null, new Object[] { this.mContext });
				List list = (List)methodgetActiveSubscriptionInfoList.invoke(subscriptionManager, new Object[]{});
				for (Iterator localIterator = list.iterator(); localIterator.hasNext(); ) {
					Object subscriptionInfo = localIterator.next();
					int simid = ((Integer)methodgetSimSlotIndex.invoke(subscriptionInfo, new Object[]{})).intValue();
					TMKeyLog.d(TAG, "open>>>simid:" + simid);
					if (simid == 0) {
						sid = ((Integer)methodgetSubscriptionId.invoke(subscriptionInfo, new Object[]{})).intValue();
						TMKeyLog.d(TAG, "open>>>sid:" + sid);
					}
				}
			}
			catch (Exception e) {
				TMKeyLog.e(TAG, "open>>>Exception111111:" + e.getMessage());
				e.printStackTrace();
			}
			try
			{
				Method getSmsManagerForSubscriptionId = this.mClass.getMethod("getSmsManagerForSubscriptionId", new Class[] { Integer.TYPE });
				this.mSmsManager = ((SmsManager)getSmsManagerForSubscriptionId.invoke(null, new Object[] { Integer.valueOf(sid) }));
			} catch (Exception e) {
				TMKeyLog.d(TAG, "getSmsManagerForSubscriptionId catch:" + e.getMessage());
				try {
					Method getDefault = this.mClass.getMethod("getSmsManagerForSubscriber", new Class[] { Integer.TYPE });
					this.mSmsManager = ((SmsManager)getDefault.invoke(null, new Object[] { Integer.valueOf(sid) }));
				} catch (Exception e1) {
					TMKeyLog.d(TAG, "getSmsManagerForSubscriber catch:" + e1.getMessage());
				}
			}

			this.mSubID = sid;
			TMKeyLog.d(TAG, "mSubID:" + mSubID);

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
		boolean resBool = false;
		try {
			resBool = _updateMessageOnIcc(1, 2, FCharUtils.hexString2ByteArray(hexStr));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resBool;
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
		Uri localUri = Uri.parse("content://icc/adn/subId/" + this.mSubID);
		if (localUri != null) {
			TMKeyLog.d(TAG, "localUri is not null,parse-->content://icc/adn/subId/" + this.mSubID);
		}
		if (localUri == null) {
			TMKeyLog.d(TAG, "localUri is null,parse-->content://icc/adn/subId/" + this.mSubID);
			localUri = Uri.parse("content://icc/adn");
		}
		if (localUri == null) {
			TMKeyLog.d(TAG, "localUri is null,parse-->content://icc/adn");
			localUri = Uri.parse("content://icc0/adn");
		}
		if (localUri == null) {
			TMKeyLog.d(TAG, "localUri is null,parse-->content://icc0/adn");
			localUri = Uri.parse("content://icc1/adn");
		}
		if (localUri == null) {
			TMKeyLog.d(TAG, "localUri is null,parse-->content://icc1/adn");
		}
		Cursor localCursor = contentResolver.query(localUri, null, null, null, null);
		if(localUri !=null && localCursor != null){
			TMKeyLog.d(TAG, "localUri is not null, localCursor is not null");
			map= new HashMap();
			map.put("uri", localUri);
			map.put("cursor", localCursor);
		}
		if (localUri == null) {
			TMKeyLog.d(TAG, "localUri is null");
			return null;
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

		//获取所有短信内容
		List<String> list = getAllMessage();

		if (list == null || list.size() < 1) {
			return new ArrayList<String>();
		}
		TMKeyLog.i(TAG, "list.size:" + list.size());
		return list;
	}

	private List<String> getAllMessage () {
		TMKeyLog.d(TAG, "getAllMessage");
		ArrayList<SmsMessage> localArrayList = getAllMessagesFromIccEfByMode(1);
		if (localArrayList == null) {
			TMKeyLog.e(TAG, "localArrayList is null");
			return null;
		}
		if (localArrayList.size() == 0) {
			TMKeyLog.e(TAG, "localArrayList size is 0");
			return null;
		}
		ArrayList<String> list = new ArrayList();
		String res = "";
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
				if (message.getIndexOnIcc() == 2) {//第二条短信固定，不作数据处理
					continue;
				}
				if(res==null || ! res.contains(SIMBaseManager.TAR_CARD)) {
					TMKeyLog.e(TAG, "res is not contains " + SIMBaseManager.TAR_CARD);
					res = null;
					continue;
				}
				int FStart = res.indexOf(SIMBaseManager.TAR_CARD);
				res = res.substring(FStart);
				TMKeyLog.i(TAG, "res:" + res);
				list.add(res);
			} else {
				TMKeyLog.e(TAG, "localArrayList.get(" + i + ") is null");
			}
		}
		if (list == null) {
			TMKeyLog.e(TAG, "localArrayList is null");
			return null;
		}
		if (list.size() == 0) {
			TMKeyLog.e(TAG, "localArrayList size is 0");
			return null;
		}
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
		ArrayList<SmsMessage> localArrayList = getAllMessagesFromIccEfByMode(1);
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

		//以jni实现
		byte[] resByte = tmjni.myGetPDUADN(paramSmsMessage);
		TMKeyLog.e(TAG, "resByte:" + FCharUtils.showResult16Str(resByte));
		return resByte;
	}

	public boolean isSupport () {
		try
		{
			this.mClass = Class.forName("android.telephony.SmsManager");
			this._getAllMessagesFromIccEfByMode = this.mClass.getMethod("getAllMessagesFromIccEfByMode", new Class[] { Integer.TYPE });
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private ArrayList<SmsMessage> getAllMessagesFromIccEfByMode(final int paramInt) {
		TMKeyLog.d(TAG, "getAllMessagesFromIccEfByMode>>>paramInt:" + paramInt);

		new Thread(new Runnable() {
			@Override
			public void run() {
				try{
					localArrayList = (ArrayList) _getAllMessagesFromIccEfByMode.invoke(mSmsManager, new Object[]{Integer.valueOf(paramInt)});
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

		TMKeyLog.e(TAG, "localArrayList is null");
		return null;
	}


	private boolean _updateMessageOnIcc(int paramInt1, int paramInt2, byte[] paramArrayOfByte){
		if(!isOpen){
			return false;
		}
		try{
			Method localMethod = this._updateMessageOnIcc;
			boolean bool = (Boolean)localMethod.invoke(mSmsManager, paramInt1, paramInt2, paramArrayOfByte);
			TMKeyLog.i(TAG, "_updateMessageOnIcc>>>res:"+bool);
			return bool;
		}catch (Exception e){
			isOpen = false;
			e.printStackTrace();
		}
		return false;
	}
}
