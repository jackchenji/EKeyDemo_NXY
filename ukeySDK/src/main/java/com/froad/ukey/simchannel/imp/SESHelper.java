package com.froad.ukey.simchannel.imp;

import android.content.ContentValues;
import android.content.Context;

import com.froad.ukey.constant.FConstant;
import com.froad.ukey.interf.CardConCallBack;
import com.froad.ukey.simchannel.SIMHelper;
import com.froad.ukey.simchannel.oma.SuperOMA;
import com.froad.ukey.utils.np.FCharUtils;
import com.froad.ukey.utils.np.TMKeyLog;

import org.simalliance.openmobileapi.service.ISmartcardService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * SESHelper
 *
 * OMA通道的的管理类
 *
 * @author  by FW.
 * @date    16/12/26
 * @modify  修改者 FW
 */
public class SESHelper extends SIMHelper {

	private static final String TAG = FConstant.LOG_TAG + "SESHelper";

	private String receiveStr = null;//接收的字符串

	private Context mContext;
	private CardConCallBack mCardConCallBack;

    private SuperOMA mSuperOMA;
	private boolean isInit;
	private int initSeCount = 0;

	public SESHelper(){}

	public SESHelper(Context con, CardConCallBack cardConCallBack){

		try {
			TMKeyLog.i(TAG, "creating SEService object...");
			mContext = con;
			this.mCardConCallBack = cardConCallBack;
			isOpen = false;
			mSuperOMA = new SuperOMA(this.mContext, new SuperOMA.CallBack()
			{
				public void serviceConnected(final SuperOMA service, final ISmartcardService iservice)
				{
					TMKeyLog.d(TAG, "serviceConnected");
					mSuperOMA = service;
					omaServie = iservice;
					TMKeyLog.d(TAG, "serviceConnected>>>isInit:" + isInit);
					if (! isInit) {
						return;
					}
					TMKeyLog.d(TAG, "serviceConnected>>>select AID");
					new Thread() {
						public void run() {
							if (!service.select(FCharUtils.hexString2ByteArray(AID))) {
								mSuperOMA = null;
								omaConTimeOut();
							} else {
								isOpen = true;
								omaConSuccess();
							}
						}
					}.start();
					initSeCount ++;
				}
			});
			this.mSuperOMA.connect();
			TMKeyLog.d(TAG, "connect end");
		} catch (Exception e) {
			TMKeyLog.e(TAG, "creating SEService objec exception...");
			e.printStackTrace();
			isOpen = false;
			omaConTimeOut();
		}
	}

	public SIMHelper initSimHelper () {
		this.isInit = true;
		checkSelect();
		return this;
	}

	public void checkSelect() {
		TMKeyLog.d(TAG, "checkSelect");
		if (this.mSuperOMA == null) {
			TMKeyLog.d(TAG, "mSuperOMA is null");
		} else if (this.mSuperOMA.isConnect()){
			TMKeyLog.d(TAG, "mSuperOMA is connect");
		}
		if (omaServie == null) {
			TMKeyLog.d(TAG, "omaServie is null");
		}
		if ((this.mSuperOMA != null) &&
				(!this.mSuperOMA.isConnect()) && (omaServie != null)) {
			this.mSuperOMA.setService((ISmartcardService) omaServie);
		}
		TMKeyLog.d(TAG, "checkSelect>>>isOpen:" + isOpen + ">>>mSuperOMA:" + mSuperOMA);
		if ((this.isOpen) || (this.mSuperOMA == null)) return;
		TMKeyLog.d(TAG, "checkSelect>>>reSelect");
		new Thread(){
			public void run() {
				isOpen = mSuperOMA.select(FCharUtils.hexString2ByteArray(AID));
				if (!isOpen){
					omaConTimeOut();
				} else {
					omaConSuccess();
				}
			}
		}.start();
		initSeCount ++;
	}

	/**
	 * 判断是否已经打开通道
	 *
	 * @return true 已打开; false 未打开
     */
	public boolean open() {
		return isOpen;
	}

	/**
	 * 关闭通道
	 *
	 * @return 是否关闭成功, true 关闭成功; false 关闭失败
     */
	@Override
	public boolean close() {
		TMKeyLog.d(TAG, "close");
		try{
			if (mSuperOMA != null) {
				this.mSuperOMA.close();
			}
		}catch (Exception e){
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public List<String> receiveDataList(){
		List<String> list = new ArrayList<String>();
		list.add(receiveData());
		return list;
	}
	
	@Override
	public String receiveData() {
		TMKeyLog.i(TAG, "receiveStr :==> " + receiveStr);
		return receiveStr;
	}

	/**
	 * 发送数据
	 *
	 * @param hexStr
	 * @return
	 * @throws IOException
     */
	@Override
	public boolean transmitHexData(String hexStr) throws IOException {
//		checkSelect();
		if (!isOpen) {
			return false;
		}
		receiveStr = FCharUtils.bytesToHexStr(this.mSuperOMA.transmit(FCharUtils.hexString2ByteArray(hexStr)));
		TMKeyLog.d(TAG, "receiveStr:" + receiveStr);
		if (receiveStr != null && ! "".equals(receiveStr)) {
			int recLen = receiveStr.length();
			if (recLen >= 4) {
				String sw12 = receiveStr.substring(recLen - 4);
				if (sw12.equals("9000") || sw12.contains("91")) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * OMA连接失败
	 */
	private void omaConTimeOut () {
		TMKeyLog.d(TAG, "omaConTimeOut");
		if (mCardConCallBack != null) {
			mCardConCallBack.OmaOpenState(false, "OMA通道连接失败");
		}
	}

	/**
	 * OMA连接成功
	 */
	private void omaConSuccess () {
		if (mCardConCallBack != null) {
			mCardConCallBack.OmaOpenState(true, "OMA通道连接成功");
		}
	}

	@Override
	public ContentValues getContentValues(String hexStr){
		return null;
	}

	@Override
	public boolean insetContentValues(List<ContentValues> list){
		return true;
	}

	@Override
	public boolean isSupport() {
		return false;
	}

	public void deleteInitSeCount () {
		initSeCount -- ;
	}

	public int getInitSeCount() {
		return initSeCount;
	}
}
