package com.zkc.pc700.helper;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.telephony.CellLocation;
import android.util.Log;

public class SerialPort {

	private static final String TAG = "SerialPort";

	private FileDescriptor mFd;
	private FileInputStream mFileInputStream;
	private FileOutputStream mFileOutputStream;

	public boolean RootCommand(String command) {
		Process process = null;
		DataOutputStream os = null;
		try {
			process = Runtime.getRuntime().exec("su");
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes(command + "\n");
			os.writeBytes("exit\n");
			os.flush();
			process.waitFor();
		} catch (Exception e) {
			Log.d("*** DEBUG ***", "ROOT REE" + e.getMessage());
			return false;
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				process.destroy();
			} catch (Exception e) {
			}
		}
		return true;
	}

	/**
	 * ����ָ��
	 */
	public void send_Instruct(byte[] buffers) {
		byte[] buffer = null;
		// String str = instruct + "\r\n";
		try {
			buffer = buffers;
		} catch (Exception e) {
			buffer = buffers;
			e.printStackTrace();
		}
		try {
			if (getOutputStream() != null)
				// ����ָ��
				getOutputStream().write(buffer);
		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	public SerialPort(String device, int baudrate, int flags)
			throws SecurityException, IOException {

		/* Check access permission */
		String cmd = "chmod 777 " + device + "\n" + "exit\n";
		if (RootCommand(cmd)) {
			//System.out.println("ok");
		} else {
			//System.out.println("no");
		}
		try {
		//	mFd = new FileDescriptor();
			mFd = open(device, baudrate, flags);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (mFd == null) {
			Log.e(TAG, "native open returns null");
			throw new IOException();
		}
		mFileInputStream = new FileInputStream(mFd);
		mFileOutputStream = new FileOutputStream(mFd);
	}

	public boolean closeSeria() {
		try {
			close();
			mFileOutputStream.close();
			mFileInputStream.close();
			return true;
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		 
		}
		 return false;

	}

	// Getters and setters
	public InputStream getInputStream() {
		return mFileInputStream;
	}

	public OutputStream getOutputStream() {
		return mFileOutputStream;
	}

	// JNI
	public native FileDescriptor open(String path, int baudrate, int flags);

	public native void close();

	{
		System.loadLibrary("PC700");
	}

}