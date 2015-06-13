package com.hzh.dsw;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

import com.hzh.dsw.WeightFeeActivity.SetResultThread;
import com.zkc.Service.CaptureService;
import com.zkc.pc700.helper.SerialPort;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class WeightFeeActivity extends Activity implements CaptureService.ICallback {
	private static final String TAG = "WeightFeeActivity";
	
	public static final int MESSAGE_UPDATE_CONSIGNEE_LISTVIEW = 1;
	public static final int MESSAGE_SHOW_SCAN_MSG = 2;	
	public static final int MESSAGE_UPDATE_GOODS_LISTVIEW = 3;
	public static final int MESSAGE_UPDATE_WEIGHT_LISTVIEW = 4;
	
	JSONObject csgnrslt = null, goodsrslt = null, submitrslt = null;
	JSONObject jsoncustomer = null;
	
	EditText etexprnum, etweight,etcsgn;
	TextView tvcptcnt;
	private Button btnMsg,btnConfirm;	
	
	String csgn = "";
	String exprnum = "";
	String weight = "0.00";
	String token;

	int customer_index = 0, product_index = 0;
	int scan_flag = 0;
	
	private ArrayAdapter<String> mGoodsArrayAdapter;
	private ArrayAdapter<String> mGoodsWgtArrayAdapter;
	
	boolean isSetCsgn = false, isSetGoods = false;

	private Spinner spigoods = null;

	ArrayList<String> arrayList = new ArrayList<String>();
	
	
	private int count=0;
	private long lastTime,curTime;

	SetResultThread resultThread;
	private byte[] choosedData = new byte[] { 0x07, (byte) 0xC6, 0x04, 0x08,
			0x00, (byte) 0x9C, 0x0A, (byte) 0xFE, (byte) 0x81 };// �޸�һά������115200
	private byte[] choosedData2 = new byte[] { 0x07, (byte) 0xC6, 0x04, 0x08,
			0x00, (byte) 0x9C, 0x0A, (byte) 0xFE, (byte) 0x81 };// �޸Ķ�ά������115200
	// ����service ��ʽ2
	private String choosed_serial = "/dev/ttyMT0";// ���ں�
	private void bindService() {
		Intent intent = new Intent(WeightFeeActivity.this, CaptureService.class);
		Log.i(TAG, "bindService()");
		bindService(intent, conn, WeightFeeActivity.BIND_AUTO_CREATE);
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
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 设置布局
		setContentView(R.layout.activity_weightfee);
		token = AppData.getInstance().getToken();

		tvcptcnt = (TextView) findViewById(R.id.cptcnt);
		etcsgn = (EditText) findViewById(R.id.etcsgn);
		etexprnum = (EditText) findViewById(R.id.etexprnum);
		etweight = (EditText) findViewById(R.id.etweight);
		btnMsg = (Button) findViewById(R.id.btnMsg);
		btnConfirm = (Button) findViewById(R.id.btnConfirm);
		btnConfirm.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				onScanned(etexprnum.getText().toString());
			}
		});
		
		// 控件初始化
		etweight.setText((CharSequence) weight);
		etweight.setEnabled(false);
		
		AppData.getInstance().setEtwght(etweight);
		
		CaptureService.callback = WeightFeeActivity.this;
		
		mGoodsWgtArrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item);
		ListView declwgtListView = (ListView) findViewById(R.id.lvresult);
		declwgtListView.setAdapter(mGoodsWgtArrayAdapter);
		
		AppData.getInstance().setScanOpen(false);
		AppData.getInstance().setStartOpen(false);
		CaptureService.isCloseRead=false;
		if (getIntent().getData() != null) {
			// �����Ƿ���ɨ������
			resultThread = new SetResultThread();
			resultThread.start();
		}
		
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

		Button btn_goback = (Button) findViewById(R.id.btngoback);
		btn_goback.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
    			AppData.getInstance().finishActivity(WeightFeeActivity.this, "确认返回吗？", "确定", "取消");     
			}
		});
	}
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		if (keyCode == 4) {
			AppData.getInstance().finishActivity(WeightFeeActivity.this, "确认返回吗？", "确定", "取消");  			
		}
		else if (keyCode == 66 || keyCode == 136 || keyCode == 135) {
/*			
			curTime=System.currentTimeMillis();
			if((curTime-lastTime)>2000){
				lastTime=curTime;
*/								
//				etexprnum.setEnabled(false);	
				
				AppData.getInstance().setStartOpen(true);
				CaptureService.isCloseRead=true;
				// ��ɨ�輤��
				Intent newIntent = new Intent(getApplicationContext(),
						CaptureService.class);
				newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startService(newIntent);

				return true;
//			}
		}		
		return super.onKeyDown(keyCode, event);
	}

	public void showMessage(int msgType, String infomsg, boolean isSound) {
		try {
			if (isSound) {
				if (msgType == AppData.PROMPT_SUCCESS) {
					MediaPlayer mediaplay=MediaPlayer.create(this, R.raw.success);
					mediaplay.start();
				} else if (msgType == AppData.PROMPT_QUERY) {
					MediaPlayer mediaplay=MediaPlayer.create(this, R.raw.query);
					mediaplay.start();
				} /*else if (msgType == AppData.PROMPT_WARN) {
					MediaPlayer mediaplay=MediaPlayer.create(this, R.raw.warning);
					mediaplay.start();
				} */else {
					MediaPlayer mediaplay=MediaPlayer.create(this, R.raw.error);
					mediaplay.start();
				}
			}
			mHandler.obtainMessage(	MESSAGE_SHOW_SCAN_MSG, -1,-1, infomsg).sendToTarget();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {			
			case MESSAGE_UPDATE_WEIGHT_LISTVIEW: {
				try {
					
					mGoodsWgtArrayAdapter.insert(msg.obj.toString(), 0);
					count++;
					tvcptcnt.setText("入库称重(" + count + ")");					
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}
			case MESSAGE_SHOW_SCAN_MSG: {
				try {
					btnMsg.setText((CharSequence) (String)(msg.obj));
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}			
			}
		}
	};
	
	public class SetResultThread extends Thread {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			while (!interrupted()) {
				if (etexprnum.getText().toString().trim().length() > 0) {
					// ��ɨ�����ݷ���ֵ����
					Intent intent = getIntent();
					intent.putExtra("code", etexprnum.getText().toString().trim());
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
	
	public void onScanned(String paramString) {
		String flagstr=paramString.substring(0, 2);
		if(flagstr.equals("CE"))//CE
		{
			csgn = paramString;//CE11982
			
			etcsgn.setText(csgn);
			
			//调取声音
			showMessage(AppData.PROMPT_SUCCESS, "收货人："+csgn, true);
			mGoodsWgtArrayAdapter.clear();
			count=0;
			tvcptcnt.setText("入库称重(" + count + ")");//应该附上该收货人（customer_name）的包裹数量
													//或者重新开个接口，提交customer_name，返回包裹数量，再赋值
//			etcsgn.setFocusable(false);
//			etcsgn.setEnabled(false);
			
			scan_flag |= 1; //scan_flag = scan_flag|1;
		}	
		else if(paramString.length()>0 && paramString!=null)
		{
			exprnum = paramString;
			etexprnum.setText(exprnum);
			showMessage(AppData.PROMPT_SUCCESS,"快递单号: " + paramString, false);
			
			scan_flag |= 2; //scan_flag = scan_flag|2;
		}
		
		if(scan_flag !=3)return;
		scan_flag = 1;
		
		weight = etweight.getText().toString();
				
		/*mGoodsWgtArrayAdapter.insert(exprnum + "   "	+ weight, 0);
		count++;
		tvcptcnt.setText("入库称重(" + count + ")");*/
		
		BigDecimal flw = new BigDecimal(weight);
		weight = flw.setScale(2,BigDecimal.ROUND_HALF_UP).toString();
		
		try{			
			SubmitThread scnThread = new SubmitThread(csgn,exprnum,weight,token);
			scnThread.start();			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private class SubmitThread extends Thread {
		private final String thrd_csgn;
		private final String thrd_exprnum;
		private final String thrd_weight;
		private final String thrd_token;

		public SubmitThread(String cs_csgn,String cs_exprnum,String cs_weight,String cs_token) {
			thrd_csgn = cs_csgn;
			thrd_exprnum = cs_exprnum;
			thrd_weight = cs_weight;
			thrd_token = cs_token;
		}

		public void run() {
			setName("SubmitThread");
			try {
				long begint = System.currentTimeMillis();
				PostScanDataRequest();
				long endt = System.currentTimeMillis();

				// Log.i(TAG,thrd_declaration_no+": "+(endt-begint));
				//showMessage(AppData.PROMPT_SUCCESS, thrd_customer_id + ": "
				//		+ (endt - begint), false);

			} catch (HttpException e) {
				System.err.println("Fatal protocol violation: "
						+ e.getMessage());
			} catch (IOException e) {
				System.err.println("Fatal transport error: " + e.getMessage());
			}
		}

		public int PostScanDataRequest() throws HttpException, IOException {
			HttpClient client = new HttpClient();
			StringBuilder sb = new StringBuilder();
			InputStream ins = null;
			int statusCode = HttpStatus.SC_METHOD_FAILURE;
			String result;

			PostMethod method = new PostMethod(
					"http://transport.360-express.com/index.php?route=api/waybill/weight");

			NameValuePair[] param = {
					new NameValuePair("customer_no", thrd_csgn),
					new NameValuePair("express_no", thrd_exprnum),
					new NameValuePair("weight", thrd_weight),
					new NameValuePair("token", thrd_token),
					new NameValuePair("code", MD5.GetMD5Code(thrd_csgn + thrd_exprnum+ thrd_weight + thrd_token)) };

			method.setRequestHeader("Content-Type",
					"application/x-www-form-urlencoded;charset=UTF-8");
			method.setRequestBody(param);

			try {
				// Execute the method.
				statusCode = client.executeMethod(method);

				if (statusCode == HttpStatus.SC_OK) {
					ins = method.getResponseBodyAsStream();
					byte[] b = new byte[1024];
					int r_len = 0;
					while ((r_len = ins.read(b)) > 0) {
						sb.append(new String(b, 0, r_len, method
								.getResponseCharSet()));
					}
					result = sb.toString();

					try {
						submitrslt = JSONObject.fromString(result);
						if (submitrslt != null) {
							String status = submitrslt.getString("status");
							String msg = submitrslt.optString("msg");
							if (status.equals("ok")) {
								showMessage(AppData.PROMPT_SUCCESS, msg, true);
								String str = thrd_exprnum + "   "	+ thrd_weight;
								mHandler.obtainMessage(MESSAGE_UPDATE_WEIGHT_LISTVIEW, -1, -1, str).sendToTarget();
								
							} else {
								statusCode = HttpStatus.SC_METHOD_FAILURE;
								showMessage(AppData.PROMPT_WARN, msg, true);
							}
						}
					} catch (Exception e) {
						statusCode = HttpStatus.SC_METHOD_FAILURE;
						showMessage(AppData.PROMPT_WARN, "入库称重信息异常，请重新采集提交。",
								false);
						e.printStackTrace();
					}
				} else {
					showMessage(AppData.PROMPT_WARN, "入库称重提交请求失败，返回码：" + statusCode, false);
					System.err.println("Response Code: " + statusCode);
				}

			} catch (HttpException e) {
				System.err.println("Fatal protocol violation: "
						+ e.getMessage());
			} catch (IOException e) {
				System.err.println("Fatal transport error: " + e.getMessage());
			} finally {
				method.releaseConnection();
				if (ins != null) {
					ins.close();
				}
			}

			return statusCode;
		}
	}
}
