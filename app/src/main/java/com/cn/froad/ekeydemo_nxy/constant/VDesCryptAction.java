/**
 * 
 */
package com.cn.froad.ekeydemo_nxy.constant;

import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

/**
 * @author Michael lidl1@yuchengtech.com
 */
public class VDesCryptAction {


	private String zpk; // 密钥

	private String acNo; // 账号

	private String encPin; // 密文

	private static final char[] bcdLookup = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	private static String TriDes = "DESede/ECB/NoPadding";
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ecc.emp.flow.EMPAction#execute(com.ecc.emp.core.Context)
	 */
	public static String execute(String zpkValue, String acNoValue)  {
		// TODO des加密或解密

		System.out.println("VDesCryptAction--账号:" + acNoValue);
		System.out.println("VDesCryptAction--密钥:" + zpkValue);

		String hRes = decode(zpkValue, acNoValue);

		return hRes;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	


	public static String decode(String key, String data) {
		if (data == null)
			return null;
		try {
			
			byte k16[] = hexToBytes(key);
			byte datas[] = hexToBytes(data);
			byte result[] = trides_crypt(k16, datas);
			String encRes = bytesToHex(result);
			System.out.println("3DES加密结果:" + encRes);
			return encRes;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	



	public static byte[] trides_crypt(byte key[], byte data[]) {
		try {
			byte[] k = new byte[24];

			int len = data.length;
			if (data.length % 8 != 0) {
				len = data.length - data.length % 8 + 8;
			}
			byte[] needData = null;
			if (len != 0)
				needData = new byte[len];

			for (int i = 0; i < len; i++) {
				needData[i] = 0x00;
			}

			System.arraycopy(data, 0, needData, 0, data.length);

			if (key.length == 16) {
				System.arraycopy(key, 0, k, 0, key.length);
				System.arraycopy(key, 0, k, 16, 8);
			} else {
				System.arraycopy(key, 0, k, 0, 24);
			}

			KeySpec ks = new DESedeKeySpec(k);
			SecretKeyFactory kf = SecretKeyFactory.getInstance("DESede");
			SecretKey ky = kf.generateSecret(ks);

			Cipher c = Cipher.getInstance(TriDes);
			c.init(Cipher.ENCRYPT_MODE, ky);
			return c.doFinal(needData);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}
	
	public static byte[] hexToBytes(String str) {
		if (str == null) {
			return null;
		} else if (str.length() < 2) {
			return null;
		} else {
			int len = str.length() / 2;
			byte[] buffer = new byte[len];
			for (int i = 0; i < len; i++) {
				buffer[i] = (byte) Integer.parseInt(
						str.substring(i * 2, i * 2 + 2), 16);
			}
			return buffer;
		}
	}
	
	
	public static final String bytesToHex(byte[] bcd) {
		StringBuffer s = new StringBuffer(bcd.length * 2);

		for (int i = 0; i < bcd.length; i++) {
			s.append(bcdLookup[(bcd[i] >>> 4) & 0x0f]);
			s.append(bcdLookup[bcd[i] & 0x0f]);
		}

		return s.toString();
	}


	
	public String getZpk() {
		return zpk;
	}

	public void setZpk(String zpk) {
		this.zpk = zpk;
	}

	public String getAcNo() {
		return acNo;
	}

	public void setAcNo(String acNo) {
		this.acNo = acNo;
	}

	public String getEncPin() {
		return encPin;
	}

	public void setEncPin(String encPin) {
		this.encPin = encPin;
	}


}
