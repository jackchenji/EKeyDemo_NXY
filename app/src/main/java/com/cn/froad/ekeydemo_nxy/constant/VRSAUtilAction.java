package com.cn.froad.ekeydemo_nxy.constant;

import com.cn.froad.ekeydemo_nxy.BuildConfig;
import com.froad.ukey.utils.np.FCharUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;

import javax.crypto.Cipher;

public class VRSAUtilAction {

	private String plainPin; // 明文
	private String encPin; // 密文
	private String PriPath; // 私钥保存位置
	private String PubPath; // 公钥保存位置
	private String accno; // 账号保存位置
	private String kf; // 随机串保存位置

	private static final char[] bcdLookup = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	/**
	 * 返回RSA解密结果，将3DES密钥与加密数据通过"_"连接
	 * @param s
	 * @return
	 */
	public static String execute(String s) {
		// TODO Auto-generated method stub
		String PRIVATEKEYStr = BuildConfig.prikey;
		byte[] PRIVATEKEY = FCharUtils.hexString2ByteArray(PRIVATEKEYStr);
//		System.out.println("RSA私钥Hex：" + FCharUtils.bytesToHexStr(PRIVATEKEY));
		RSAPrivateKey privateKey = (RSAPrivateKey) byteToObject(PRIVATEKEY);
		String cardNo = "";
		String VkeyId = "";
		String kc = "";
		try {
			byte[] output = decrypt(privateKey, hexToBytes(s));
			String yuanwenOut = bytesToHex(output);
			System.out.println("RSA解密原文Hex:" + yuanwenOut);

			if (yuanwenOut != null && yuanwenOut.length() >= 48) {
				String tempT = yuanwenOut.substring(yuanwenOut.length() - 48,
						yuanwenOut.length());
				kc = tempT.substring(0, 32);
				cardNo = tempT.substring(32, 48);
				VkeyId = cardNo.substring(0, cardNo.length() - 1);
			}
			System.out.println("VRSAUtilAction--账号:" + "|" + cardNo);
			System.out.println("VRSAUtilAction--序列号:" + "|" + VkeyId);
			System.out.println("VRSAUtilAction--随机串:" + "|" + kc);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return kc + "_" + cardNo;
	}
	
	public static byte[] toByteArray(String filename) {
		File f = new File(filename);
		if (!f.exists()) {
			// throw new FileNotFoundException(filename);
			return null;
		}
		ByteArrayOutputStream bos = new ByteArrayOutputStream((int) f.length());
		BufferedInputStream in = null;
		try {
			in = new BufferedInputStream(new FileInputStream(f));
			int buf_size = 1024;
			byte[] buffer = new byte[buf_size];
			int len = 0;
			while (-1 != (len = in.read(buffer, 0, buf_size))) {
				bos.write(buffer, 0, len);
			}
			return bos.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
			// throw e;
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				bos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	public static Object byteToObject(byte[] bytes) {
		Object obj = null;
		try {
			// bytearray to object
			ByteArrayInputStream bi = new ByteArrayInputStream(bytes);
			ObjectInputStream oi = new ObjectInputStream(bi);

			obj = oi.readObject();
			bi.close();
			oi.close();
		} catch (Exception e) {
			System.out.println("translation" + e.getMessage());
			e.printStackTrace();
		}
		return obj;
	}

	public static final byte[] hexToBytes(String s) {
		byte[] bytes;
		bytes = new byte[s.length() / 2];

		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2),
					16);
		}

		return bytes;
	}

	public static byte[] decrypt(PrivateKey privateKey, byte[] data)
			throws Exception {
		Cipher ci = Cipher.getInstance("RSA/None/NoPadding",
				new org.bouncycastle.jce.provider.BouncyCastleProvider());
		ci.init(Cipher.DECRYPT_MODE, privateKey);
		return ci.doFinal(data);
	}

	public static final String bytesToHex(byte[] bcd) {
		StringBuffer s = new StringBuffer(bcd.length * 2);

		for (int i = 0; i < bcd.length; i++) {
			s.append(bcdLookup[(bcd[i] >>> 4) & 0x0f]);
			s.append(bcdLookup[bcd[i] & 0x0f]);
		}

		return s.toString();
	}

	public String getPlainPin() {
		return plainPin;
	}

	public void setPlainPin(String plainPin) {
		this.plainPin = plainPin;
	}

	public String getEncPin() {
		return encPin;
	}

	public void setEncPin(String encPin) {
		this.encPin = encPin;
	}

	public String getPriPath() {
		return PriPath;
	}

	public void setPriPath(String priPath) {
		PriPath = priPath;
	}

	public String getPubPath() {
		return PubPath;
	}

	public void setPubPath(String pubPath) {
		PubPath = pubPath;
	}

	public String getAccno() {
		return accno;
	}

	public void setAccno(String accno) {
		this.accno = accno;
	}

	public String getKf() {
		return kf;
	}

	public void setKf(String kf) {
		this.kf = kf;
	}

}
