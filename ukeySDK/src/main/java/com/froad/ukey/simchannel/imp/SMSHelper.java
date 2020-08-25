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
import com.froad.ukey.simchannel.SIMHelper;
import com.froad.ukey.utils.np.FCharUtils;
import com.froad.ukey.utils.np.TMKeyLog;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
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

public class SMSHelper extends SIMHelper {

	private static final String TAG = FConstant.LOG_TAG + "SMSHelper";
	private Context mContext;
	private SmsManager mSmsManager= null;
	private CardConCallBack mCardConCallBack = null;

	private Class<?> mClass = null;
	private Method method1 = null;//_getAllMessagesFromIcc
	private Method method2 = null;//_updateMessageOnIcc

	private String PhoneTagStr = "tag";
	private String PhoneNumStr = "number";

	public static int adnWriteType = 1;//1--update，2--insert
	public boolean isCounterNotZero = false;//联系人数量是否不为0

	private ConditionVariable conditionVariable = new ConditionVariable();
	private ArrayList<SmsMessage> localArrayList = null;

	public SMSHelper () {}

	public SMSHelper(Context con, CardConCallBack cardConCallBack){
		mContext = con;
		mCardConCallBack = cardConCallBack;
		mSmsManager = SmsManager.getDefault();
	}

	@Override
	public SIMHelper initSimHelper() {
		return this;
	}

	@Override
	public boolean open() {
		TMKeyLog.d(TAG, "open");
		try {
			isNeedShift = false;
			tmjni.openDeviceADN(this, int.class, int.class, byte[].class);

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

		int res = tmjni.transmitHexDataADN(this, hexStr);
		if (res == 0) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public ContentValues getContentValues(String hexStr){
		TMKeyLog.i(TAG, "getContentValues...");
		ContentValues localContentValues1 = new ContentValues();
		if (Integer.parseInt(SIMBaseManager.CardSmsVersion, 16) >= SIMBaseManager.CARDVERSION_02) { //新版ADN通信协议
			int hl = hexStr.length();
			if (hl <= 16) {
				return null;
			}
			if (hl > 30) {
				return null;
			}
			//将数据拆分成控制字节部分和数据部分通过通讯录模式写入
			String orderStr = hexStr.substring(0, 8 * 2);
			String dataStr = hexStr.substring(8 * 2);

			String tagStr = dataStr;
			String numStr = orderStr;
			TMKeyLog.d(TAG, "getContentValues>>>PhoneTagStr:" + PhoneTagStr + ">>>PhoneNumStr:" + PhoneNumStr);
			localContentValues1.put(PhoneTagStr, tagStr);
			localContentValues1.put(PhoneNumStr, numStr);
		} else {
			int hl = hexStr.length();
			if (hl <= 20) {
				return null;
			}
			if (hl > 34) {
				return null;
			}
			//将数据拆分成控制字节部分和数据部分通过通讯录模式写入
			String orderStr = hexStr.substring(0, 10 * 2);
			String dataStr = hexStr.substring(10 * 2);

			String tagStr = dataStr;
			String numStr = orderStr;
			TMKeyLog.d(TAG, "getContentValues>>>PhoneTagStr:" + PhoneTagStr + ">>>PhoneNumStr:" + PhoneNumStr);
			localContentValues1.put(PhoneTagStr, tagStr);
			localContentValues1.put(PhoneNumStr, numStr);
		}
		return localContentValues1;
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

	/**
	 * 写ADN
	 * @param localCursor
	 * @param localUri
	 * @param contentResolver
	 * @param contentValues
	 * @return
	 */
	public boolean writeADNData (Cursor localCursor, Uri localUri, ContentResolver contentResolver, ContentValues contentValues) {
		TMKeyLog.i(TAG, "localUri:" + localUri.toString());
		TMKeyLog.i(TAG, "localursor.getCount:" + localCursor.getCount());
		TMKeyLog.i(TAG, "PhoneTagStr:" + contentValues.getAsString(PhoneTagStr));
		TMKeyLog.i(TAG, "PhoneNumStr:" + contentValues.getAsString(PhoneNumStr));
		return updateADNData_NEW(localCursor,localUri, contentResolver, contentValues);
	}

	/**
	 * 新版ADN协议，先更新再插入，不需要删除
	 * @param localCursor
	 * @param localUri
	 * @param contentResolver
	 * @param contentValues
	 * @return
	 */
	public boolean updateADNData_NEW (Cursor localCursor, Uri localUri, ContentResolver contentResolver, ContentValues contentValues) {
		TMKeyLog.d(TAG, "updateADNData_NEW>>>PhoneTagStr:" + PhoneTagStr + ">>>adnWriteType:" + adnWriteType);
		int cursorCount = localCursor.getCount();
		TMKeyLog.d(TAG, "cursorCount:" + cursorCount);
		if (cursorCount == 0) {
			isCounterNotZero = false;
			Uri uri = null;
			try {
				uri = contentResolver.insert(localUri, contentValues);
				TMKeyLog.e(TAG, "inset list content success111");
			} catch (Exception e) {
				TMKeyLog.e(TAG, "inset list content Exception：" + e.getMessage());
				e.printStackTrace();
			}
			if(uri == null) {
				TMKeyLog.e(TAG, "inset list content fails");
				//插入数据失败认为插入数据成功
//					return false;
			} else {
				TMKeyLog.e(TAG, "inset list content success222");
				if (SIMBaseManager.CardSmsVersion.equals("00") || SIMBaseManager.CardSmsVersion.equals("01")) {
					//删除联系人
					simDelete(contentValues, contentResolver, localUri);
				}
//					return true;
			}
		} else {
			isCounterNotZero = true;
			if (! localCursor.isLast()) {
				boolean toNext = localCursor.moveToNext();
				TMKeyLog.d(TAG, "moveToNext>>>toNext:" + toNext);
			}
			if(adnWriteType == 1) {//update
				TMKeyLog.e(TAG, "update list content start");
				String tagData = contentValues.getAsString(PhoneTagStr);
				String numData = contentValues.getAsString(PhoneNumStr);
				int nameIndex = localCursor.getColumnIndex("name");
				TMKeyLog.d(TAG, "nameIndex:" + nameIndex);
				String old_name = localCursor.getString(nameIndex);
				TMKeyLog.d(TAG, "old_name:" + old_name);
				int numberIndex = localCursor.getColumnIndex("number");
				TMKeyLog.d(TAG, "numberIndex:" + numberIndex);
				String old_number = localCursor.getString(numberIndex);
				TMKeyLog.d(TAG, "old_number:" + old_number);
				String newTag = "newTag";
				String newNumber = "newNumber";
				contentValues.put(newTag, tagData);
				contentValues.put(newNumber, numData);
				contentValues.put(PhoneTagStr, old_name);
				contentValues.put(PhoneNumStr, old_number);
				int updateRes = -1;
				try {
					updateRes = contentResolver.update(localUri, contentValues, null, null);
				} catch (Exception e) {
					TMKeyLog.e(TAG, "update list content Exception:" + e.getMessage());
					e.printStackTrace();
				}
				TMKeyLog.e(TAG, "update list content updateRes:" + updateRes);
				if (updateRes <= 0) {//更新失败
					TMKeyLog.e(TAG, "update list content fails");
				} else {//更新成功
					TMKeyLog.e(TAG, "update list content success");
				}
			} else {//insert
				TMKeyLog.e(TAG, "insert list content start");
				Uri uri = null;
				try {
					uri = contentResolver.insert(localUri, contentValues);
				} catch (Exception e) {
					TMKeyLog.e(TAG, "insert list content Exception:" + e.getMessage());
					e.printStackTrace();
				}
				if(uri != null) {//插入联系人成功
					TMKeyLog.e(TAG, "insert list content success");
					if (SIMBaseManager.CardSmsVersion.equals("00") || SIMBaseManager.CardSmsVersion.equals("01")) {
						//删除联系人
						simDelete(contentValues, contentResolver, localUri);
					}
				} else {
					TMKeyLog.e(TAG, "insert list content fail");
				}
			}
		}
		if (localCursor != null) {
			localCursor.close();
		}
		return true;
	}

	/**
	 * 老版ADN协议
	 * @param localCursor
	 * @param localUri
	 * @param contentResolver
	 * @param contentValues
	 * @return
	 */
	public boolean insertADNData (Cursor localCursor, Uri localUri, ContentResolver contentResolver, ContentValues contentValues) {
		TMKeyLog.d(TAG, "insertADNData");
		int cursorCount = localCursor.getCount();
		TMKeyLog.d(TAG, "cursorCount:" + cursorCount);
		if (cursorCount == 0) {
			Uri uri = null;
			try {
				uri = contentResolver.insert(localUri, contentValues);
				TMKeyLog.e(TAG, "inset list content success");
			} catch (Exception e) {
				TMKeyLog.e(TAG, "inset list content Exception：" + e.getMessage());
				e.printStackTrace();
			}
			if(uri == null) {
				TMKeyLog.e(TAG, "inset list content fails");
				//插入数据失败认为插入数据成功
//					return false;
			} else {
				//删除联系人
				simDelete(contentValues, contentResolver, localUri);
//					return true;
			}
		} else {
			if (! localCursor.isLast()) {
				boolean toNext = localCursor.moveToNext();
				TMKeyLog.d(TAG, "moveToNext>>>toNext:" + toNext);
			}
			if (adnWriteType == 1) {//update
				TMKeyLog.e(TAG, "update list content start");
				String tagData = contentValues.getAsString(PhoneTagStr);
				String numData = contentValues.getAsString(PhoneNumStr);
				int nameIndex = localCursor.getColumnIndex("name");
				TMKeyLog.d(TAG, "nameIndex:" + nameIndex);
				String old_name = localCursor.getString(nameIndex);
				TMKeyLog.d(TAG, "old_name:" + old_name);
				int numberIndex = localCursor.getColumnIndex("number");
				TMKeyLog.d(TAG, "numberIndex:" + numberIndex);
				String old_number = localCursor.getString(numberIndex);
				TMKeyLog.d(TAG, "old_number:" + old_number);
				String newTag = "newTag";
				String newNumber = "newNumber";
				contentValues.put(newTag, tagData);
				contentValues.put(newNumber, numData);
				contentValues.put(PhoneTagStr, old_name);
				contentValues.put(PhoneNumStr, old_number);
				int updateRes = -1;
				try {
					updateRes = contentResolver.update(localUri, contentValues, null, null);
				} catch (Exception e) {
					TMKeyLog.e(TAG, "update list content Exception:" + e.getMessage());
					e.printStackTrace();
				}
				TMKeyLog.e(TAG, "update list content updateRes:" + updateRes);
				if (updateRes <= 0) {//更新失败
					TMKeyLog.e(TAG, "update list content fails");
				} else {//更新成功
					TMKeyLog.e(TAG, "update list content success");
				}
			} else {//insert
				TMKeyLog.e(TAG, "insert list content start");
				Uri uri = null;
				try {
					uri = contentResolver.insert(localUri, contentValues);
				} catch (Exception e) {
					TMKeyLog.e(TAG, "insert list content Exception:" + e.getMessage());
					e.printStackTrace();
				}
				if(uri != null) {//插入联系人成功
					TMKeyLog.e(TAG, "insert list content success");
					//删除联系人
					simDelete(contentValues, contentResolver, localUri);
				} else {
					TMKeyLog.e(TAG, "insert list content failed");
				}
			}
		}
		if (localCursor != null) {
			localCursor.close();
		}
		return true;
	}

	private Map<String, Object> getADNCursor(){
		TMKeyLog.d(TAG, "getADNCursor");
		Map<String, Object> map = null;
		ContentResolver contentResolver = mContext.getContentResolver();

		Uri localUri = Uri.parse("content://icc/adn");
		if (localUri == null) {
			localUri = Uri.parse("content://icc0/adn");
		}
		if (localUri == null) {
			localUri = Uri.parse("content://icc1/adn");
		}
		if (localUri == null) {
			TMKeyLog.d(TAG, "localUri is null");
			return null;
		}
		TMKeyLog.d(TAG, "localUri:" + localUri.toString());
		Cursor nowCursor;
		if ((nowCursor = mContext.getContentResolver().query(localUri, null, null, null, null)) == null) {
			return null;
		}
		TMKeyLog.d(TAG, "nowCursor:" + nowCursor.getCount());
		Cursor localCursor = contentResolver.query(localUri, null, null, null, null);
		if(localUri !=null && localCursor != null){
			TMKeyLog.d(TAG, "localUri is not null, localCursor is not null");
			TMKeyLog.d(TAG, "localCursor.getCount--->" + localCursor.getCount());
			map= new HashMap();
			map.put("uri", localUri);
			map.put("cursor", localCursor);
		}
		return map;
	}

	/**
	 * 删除通讯录中单条数据
	 * @param paramContentValues
	 */
	private void simDelete(ContentValues paramContentValues, ContentResolver contentResolver, Uri mUri) {
		TMKeyLog.d(TAG, "simDelete");
		String tag = paramContentValues.get(PhoneTagStr).toString();
		String number = paramContentValues.get(PhoneNumStr).toString();
		String where = PhoneTagStr + "='" + tag + "'";
		where += " AND " + PhoneNumStr + "='" + number + "'";
		TMKeyLog.d(TAG, "simDelete>>>where:" + where);
		int delRes = contentResolver.delete(mUri, where, null);
		if(delRes <= 0) {//删除数据
			TMKeyLog.e(TAG, "delete content fails 111");
			where = "tag ='" + tag + "'";
			where += " AND " + PhoneNumStr + "='" + number + "'";
			TMKeyLog.d(TAG, "simDelete>>>where:" + where);
			delRes = contentResolver.delete(mUri, where, null);
			if(delRes <= 0) {//删除数据
				TMKeyLog.e(TAG, "delete content fails 222");
			} else {
				TMKeyLog.d(TAG, "delete content success 222");
			}
		} else {
			TMKeyLog.d(TAG, "delete content success 111");
		}
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
		TMKeyLog.d(TAG, "hasCard");
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
//		byte[] resByte = paramSmsMessage.getPdu();
//		if (resByte == null) {
//			String msgBody = paramSmsMessage.getMessageBody();
//			resByte = FCharUtils.stringToByteArray(msgBody);
//		}
//		if (isNeedShift) { //电信卡
//			resByte = ByteUtil.dealShift(resByte, 5);
//		} else { //移动联通卡
//		}
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
					localArrayList = tmjni.method1ADN(SMSHelper.this);
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

	public void setPhoneTagStr (String phoneTag) {
		PhoneTagStr = phoneTag;
	}

	public  boolean isSupport () {
		TMKeyLog.d(TAG, "isSupport...");
		try
		{
			this.mClass = Class.forName("android.telephony.SmsManager");

			method1 = this.mClass.getMethod("getAllMessagesFromIcc", new Class[0]);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return this.method1 != null;
	}
}
