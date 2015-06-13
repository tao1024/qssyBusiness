package com.zkc.Service;

import java.io.InputStream;

import com.hzh.dsw.AppData;
import com.hzh.dsw.CollectGoodsActivity;
//import com.zkc.barvodeScan.MainActivity;
import com.zkc.beep.ServiceBeepManager;
import com.zkc.io.EmGpio;
import com.zkc.pc700.helper.ScanGpio;
import com.zkc.pc700.helper.SerialPort;

import android.R.bool;
import android.app.Application;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class CaptureService extends Service {

	private static final String TAG = "CaptureService";
	//private ServiceBeepManager beepManager;// ɨ��ɹ���ʾ��
	private byte[] leaveSetting = new byte[] { 0x04, (byte) 0xC8, 0x04, 0x00,
			(byte) 0xFF, 0x30 };// 出厂设置

	private byte[] packedData = new byte[] { 0x07, (byte) 0xC6, 0x04, 0x08,
			0x00, (byte) 0xEB, 0x07, (byte) 0xFE, 0x35 };// 读取数据格式

	private String choosed_serial = "/dev/ttyMT0";// 串口号
	public static SerialPort serialPort = null;
	private InputStream mInputStream;// ��ȡ��Ϣ��

	// private int choosed_buad = 9600;// 波特率
	private int choosed_buad = 115200;// 波特率
	public static ReadThread readThread;// ��ȡ��Ϣ�����߳�
	private static byte[] getbuffer = new byte[1024];// ��������Ϣ
	private static int getsize = 0;// ������Ϣ����
	public static ScanGpio scanGpio = new ScanGpio();
	public static boolean isCloseRead=true;
	public static boolean isChoosed=false;
	
	public int serveid =0;
	
	public static ICallback callback;
//	private int presize = 10;
	public int presize = 10;
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.v(TAG, "CaptureService onBind");

		return null;
	}

	/**
	 * ��ȡ��Ϣ����
	 * 
	 * @author zkc-soft2
	 * 
	 */
	public class ReadThread extends Thread {

		@Override
		public void run() {

			while (!interrupted()) {
				try {

					int size;
					byte[] buffer = new byte[1024];

					if (mInputStream == null)
						return;
					// ��ȡ������Ϣ
					size = mInputStream.read(buffer);
					Log.v(TAG, size + "");
					// �����ȡ������Ϣ
					for (int i = 0; i < size; i++) {
						getbuffer[getsize + i] = buffer[i];
					}
					// ����������Ϣ����
					getsize += size;
					handler.post(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							Message m = new Message();
							m.arg1 = getsize;
							m.obj = getbuffer;
							handler.sendMessage(m);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();

					return;
				}
			}

		}
	}

	/**
	 * ��ʾ��ϢHandle
	 */
	Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			byte[] getdata = (byte[]) msg.obj;

			int sizs = msg.arg1;
			byte[] setData = new byte[sizs];
			boolean isfull = false;// �Ƿ�ȡ����������
			boolean isLuanMa = false;// �״��Ƿ�����������ݡ�4, -48, 0, 0, -1, 44��

			for (int i = 0; i < sizs; i++) {
				setData[i] = getdata[i];
				if (getdata[0] == 0) {
					if (getdata[i] == 13) {
						isfull = true;
					}
				} else {
					isfull = true;
				}

				if (getdata[i] == -1) {
					isLuanMa = true;
				}
			}
			if (isfull) {
				AppData.getInstance().setScanOpen(false);
				try {
					String getStringPort;
					// ת��ɨ����ϢΪ�ַ�������ʽUTF-8
/*					
					if (isLuanMa) {
						setData = new byte[sizs];

						for (int j = 7; j < sizs; j++) {
							setData[j - 7] = getdata[j];
						}
						getStringPort = new String(setData, 0, sizs, "ASCII");
					} else {
						getStringPort = new String(setData, 0, sizs, "ASCII");
					}
*/					
					int pos=0;
					for (int j = 0; j < sizs; j++) {
						if((getdata[j]>=0x30&&getdata[j]<=0x39)||
							(getdata[j]>=0x41&&getdata[j]<=0x5A)||
							(getdata[j]>=0x61&&getdata[j]<=0x7A)||
							(getdata[j]==0x2b)||
							(getdata[j]==0x2d)){
							setData[pos++] = getdata[j];
						}
					}
					sizs = pos;	
					/*暂屏蔽
					  if(sizs/presize>1){
						sizs = presize;
					}else{
						presize = sizs;
					}*/
					getStringPort = new String(setData, 0, sizs, "ASCII");
					
					String getStrings = getStringPort.trim() + "";
					if (isCloseRead) { 
					if (serialPort != null) { 
						if (getStrings != "") {
							/*
							//System.out.println(setData);
							for (int j = 0; j < setData.length; j++) {
								//System.out.println(setData[j]);
							}*/
							// ����ɨ��ɹ���ʾ��
							//beepManager.playBeepSoundAndVibrate();
							String getString = getStringPort.trim();//CE11982
							//MainActivity.et_code.setText(getString);
							CaptureService.callback.onScanned(getString);
						}
					}
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	};

	@Override
	public void onCreate() {
		super.onCreate();
		Log.v(TAG, "CaptureService onCreate");

		// ������ʾ��
/*
		beepManager = new ServiceBeepManager(this);
		beepManager.updatePrefs();
*/
		// �򿪵�Դ
		// scanGpio.openPower();

	}

	public static void clerkMessage() {
		getsize = 0;// ɨ����Ϣ������0
		getbuffer = new byte[1024];// ���ɨ����Ϣ
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.v(TAG, "CaptureService onStart");
		super.onStart(intent, startId);
		
		if (isChoosed) {
			// ������Դ
			scanGpio.openPower();
			try {
				// ���Ӵ���
				if (serialPort==null) {
					serialPort = new SerialPort(choosed_serial, choosed_buad, 0);
				}
			//	
				mInputStream = serialPort.getInputStream();
				// ���ý��ո�ʽ
				serialPort.send_Instruct(packedData);
				// ������ȡ��Ϣ����
				readThread = new ReadThread();
				readThread.start();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			isChoosed=false;
		}

		if (AppData.getInstance().isStartOpen()) {
			AppData.getInstance().setStartOpen(false);
			// ����ı���
/*			
			if (MainActivity.et_code != null) {
				MainActivity.et_code.setText("");
			}
*/			
			// ������ݣ���ɨ��
			CaptureService.clerkMessage();
			CaptureService.scanGpio.openScan();

			new Thread(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					AppData.getInstance().setScanOpen(false);
				}
			}).start();

		}
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		Log.v(TAG, "CaptureService onDestroy");

		if (readThread != null) {
			readThread.interrupt();

		}
		if (serialPort != null) {
			serialPort.closeSeria();
			serialPort = null;
		}
		super.onDestroy();
	}

	public interface ICallback {
		public void onScanned(String paramString);
	}
}
