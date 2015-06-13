package com.zkc.barvodeScan;

import java.io.IOException;
import java.io.InputStream;

import com.zkc.Service.CaptureService;
import com.hzh.dsw.R;
import com.zkc.pc700.helper.SerialPort;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";

	Button btnOpen, btnEdit;
	public static EditText et_code;
	private Button emptyBtn;

	public static boolean isScanOpen = false; // �Ƿ��Ѵ�ɨ������
	public static boolean isStartOpen = false;

	SetResultThread resultThread;
	private byte[] choosedData = new byte[] { 0x07, (byte) 0xC6, 0x04, 0x08,
			0x00, (byte) 0x9C, 0x0A, (byte) 0xFE, (byte) 0x81 };// �޸�һά������115200
	private byte[] choosedData2 = new byte[] { 0x07, (byte) 0xC6, 0x04, 0x08,
			0x00, (byte) 0x9C, 0x0A, (byte) 0xFE, (byte) 0x81 };// �޸Ķ�ά������115200
	// ����service ��ʽ2
	private String choosed_serial = "/dev/ttyMT0";// ���ں�
	private void bindService() {
		Intent intent = new Intent(MainActivity.this, CaptureService.class);
		Log.i(TAG, "bindService()");
		bindService(intent, conn, MainActivity.BIND_AUTO_CREATE);
	}

	private ServiceConnection conn = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			Log.i(TAG, "onServiceDisconnected()");
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub

		}
	};

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == 66 || keyCode == 136 || keyCode == 135) {
			MainActivity.isStartOpen = true;
			CaptureService.isCloseRead=true;
			// ��ɨ�輤��
			Intent newIntent = new Intent(getApplicationContext(),
					CaptureService.class);
			newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startService(newIntent);

			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_scan);
		CaptureService.isCloseRead=false;
		if (getIntent().getData() != null) {
			// �����Ƿ���ɨ������
			resultThread = new SetResultThread();
			resultThread.start();
		}

		et_code = (EditText) findViewById(R.id.et_code);

		et_code.setText("");
		// �˳�
		btnEdit = (Button) findViewById(R.id.btnExit);
		btnEdit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				finish();
			}
		});
		// �����Ϣ
		emptyBtn = (Button) findViewById(R.id.emptyBtn);
		emptyBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				et_code.setText("");
			}
		});
		// ����ɨ��
		btnOpen = (Button) findViewById(R.id.btnOpen);
		btnOpen.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				MainActivity.isStartOpen = true;
				CaptureService.isCloseRead=true;
				Intent newIntent = new Intent(getApplicationContext(),
						CaptureService.class);
				newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startService(newIntent);
			}

		});
		if (CaptureService.serialPort == null) {
			// ������Դ
			CaptureService.scanGpio.openPower();
			try {
				// ���Ӵ���
				CaptureService.serialPort = new SerialPort(choosed_serial, 9600, 0);
				// ���ô��ڲ�����
				CaptureService.serialPort.send_Instruct(choosedData);
				Thread.sleep(200);
				CaptureService.serialPort.send_Instruct(choosedData2);
				Thread.sleep(200); 
				CaptureService.serialPort.closeSeria();
				Thread.sleep(200);
				CaptureService.serialPort = null;
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				CaptureService.serialPort = new SerialPort(choosed_serial, 115200, 0);
				CaptureService.isChoosed=true;
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	/**
	 * ���ͷ���ֵ�߳�
	 * 
	 * @author zkc-soft2
	 * 
	 */
	public class SetResultThread extends Thread {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			while (!interrupted()) {
				if (et_code.getText().toString().trim().length() > 0) {
					// ��ɨ�����ݷ���ֵ����
					Intent intent = getIntent();
					intent.putExtra("code", et_code.getText().toString().trim());
					setResult(201, intent);
					finish();
				}
			}
		}
	}

	@Override
	protected void onResume() {

		//System.out.println("onResume" + "open");
		Log.v("onResume", "open");
		super.onResume();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		if (CaptureService.serialPort != null) {

			if (CaptureService.readThread != null) {
				CaptureService.readThread.interrupt();
			}

			if (CaptureService.serialPort.closeSeria()) {
				CaptureService.serialPort = null;
			}
			CaptureService.isChoosed=true;
		}
		
		CaptureService.isCloseRead=false;
		CaptureService.scanGpio.closeScan();
		// �رյ�Դ
		CaptureService.scanGpio.closePower();

		Log.v("onPause", "close");
		//System.out.println("onPause close");
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		if (resultThread != null) {
			resultThread.interrupt();
		}
		if (CaptureService.serialPort != null) {

			if (CaptureService.readThread != null) {
				CaptureService.readThread.interrupt();
			}

			if (CaptureService.serialPort.closeSeria()) {
				CaptureService.serialPort = null;
			}
			CaptureService.isChoosed=true;
		}
		CaptureService.isCloseRead=false;
		CaptureService.scanGpio.closeScan();
		// �رյ�Դ
		CaptureService.scanGpio.closePower();
		Log.v("onDestroy", "close");
		//System.out.println("onDestroy close");
		
		super.onDestroy();
	}
	@Override
	public void onBackPressed() {
		 
			new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(R.string.popup_title)
					.setMessage(R.string.popup_message)
					.setPositiveButton(R.string.popup_yes,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									finish();
								}
							}).setNegativeButton(R.string.popup_no, null)
					.show();
		}
}
