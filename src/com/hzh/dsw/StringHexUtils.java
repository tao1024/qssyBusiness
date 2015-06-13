package com.hzh.dsw;

import java.io.ByteArrayOutputStream;

import android.util.Log;

/**
 * <pre>
 *  Achievo Automation
 *  File: Utils.java
 *  Author:joony.li
 *  Date:涓嬪崍04:56:34
 *  Description:TODO
 * </pre>
 */
public class StringHexUtils {

	// 杞寲瀛楃涓蹭负鍗佸叚杩涘埗缂栫爜
	public static String toHexString(String s) {
		String str = "";
		for (int i = 0; i < s.length(); i++) {
			int ch = (int) s.charAt(i);
			String s4 = Integer.toHexString(ch);
			str = str + s4;
		}
		return str;
	}

	/**
	 * 杞寲鍗佸叚杩涘埗缂栫爜涓哄瓧绗︿覆
	 * 
	 * @param s
	 * @return
	 */
	public static String toStringHex(String s) {
		byte[] baKeyword = new byte[s.length() / 2];
		for (int i = 0; i < baKeyword.length; i++) {
			try {
				baKeyword[i] = (byte) (0xff & Integer.parseInt(
						s.substring(i * 2, i * 2 + 2), 16));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			s = new String(baKeyword, "utf-8");// UTF-16le:Not
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return s;
	}

	/**
	 * 16杩涘埗鏁板瓧瀛楃闆�
	 */
	private static String hexString = "0123456789ABCDEF";

	/**
	 * 灏嗗瓧绗︿覆缂栫爜鎴�16杩涘埗鏁板瓧,閫傜敤浜庢墍鏈夊瓧绗︼紙鍖呮嫭涓枃锛�
	 */
	public static String encode(String str) {
		// 鏍规嵁榛樿缂栫爜鑾峰彇瀛楄妭鏁扮粍
		byte[] bytes = str.getBytes();
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		// 灏嗗瓧鑺傛暟缁勪腑姣忎釜瀛楄妭鎷嗚В鎴�2浣�16杩涘埗鏁存暟
		for (int i = 0; i < bytes.length; i++) {
			sb.append(hexString.charAt((bytes[i] & 0xf0) >> 4));
			sb.append(hexString.charAt((bytes[i] & 0x0f) >> 0));
		}
		return sb.toString();
	}

	/**
	 * 灏�16杩涘埗鏁板瓧瑙ｇ爜鎴愬瓧绗︿覆,閫傜敤浜庢墍鏈夊瓧绗︼紙鍖呮嫭涓枃锛�
	 */
	public static String decode(String bytes) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(
				bytes.length() / 2);
		// 灏嗘瘡2浣�16杩涘埗鏁存暟缁勮鎴愪竴涓瓧鑺�
		for (int i = 0; i < bytes.length(); i += 2)
			baos.write((hexString.indexOf(bytes.charAt(i)) << 4 | hexString
					.indexOf(bytes.charAt(i + 1))));
		return new String(baos.toByteArray());
	}

	public static String Bytes2HexString(byte[] b) {
		String ret = "";
		for (int i = 0; i < b.length; i++) {
			String hex = Integer.toHexString(b[i] & 0xFF);
			if (hex.length() == 1) {
				hex = '0' + hex;
			}
			ret += hex.toUpperCase();
		}
		return ret;
	}

	 public static byte[] hexStr2Bytes(String paramString)
	  {
	    int i = paramString.length() / 2;
	    //System.out.println(i);
	    byte[] arrayOfByte = new byte[i];
	    int j = 0;
	    while (true)
	    {
	      if (j >= i)
	        return arrayOfByte;
	      int k = 1 + j * 2;
	      int l = k + 1;
	      arrayOfByte[j] = (byte)(0xFF & Integer.decode("0x" + paramString.substring(j * 2, k) + paramString.substring(k, l)).intValue());
	      ++j;
	    }
	  }
	
	 
	 public static String hexStr2Str(String paramString)
	  {
	    char[] arrayOfChar = paramString.toCharArray();
	    byte[] arrayOfByte = new byte[paramString.length() / 2];
	    int i = 0;
	    while (true)
	    {
	      if (i >= arrayOfByte.length)
	        return new String(arrayOfByte);
	      arrayOfByte[i] = (byte)(0xFF & 16 * "0123456789ABCDEF".indexOf(arrayOfChar[(i * 2)]) + "0123456789ABCDEF".indexOf(arrayOfChar[(1 + i * 2)]));
	      ++i;
	    }
	  }
}

/*
 * $Log: av-env.bat,v $
 */