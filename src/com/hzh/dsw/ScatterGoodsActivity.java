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

import com.hzh.dsw.ScatterGoodsActivity.SetResultThread;
import com.zkc.Service.CaptureService;
import com.zkc.pc700.helper.SerialPort;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class ScatterGoodsActivity extends Activity implements CaptureService.ICallback {
	private static final String TAG = "ScatterGoodsActivity";
	public static final int MESSAGE_UPDATE_CONSIGNEE_LISTVIEW = 1001;
	public static final int MESSAGE_SHOW_SCAN_MSG = 1002;	
	public static final int MESSAGE_UPDATE_WAYBILL_LISTVIEW = 1003;
	public static final int MESSAGE_UPDATE_EXPRNUM_LISTVIEW = 1004;
	public static final int MESSAGE_SET_SCAN_DECLNUM = 1005;
	public static final int MESSAGE_INSERT_DATA_SCATTER = 1006;
	
	JSONObject csgnrslt = null, submitrslt = null;

	EditText etdeclnum,etcsgn, etweight, etexprnum;
	TextView tvcptcnt;
	private Button btnMsg,btnConfirm,btnSearch;
	
	String declaration_no = "";
	String customer_id = "";
	String customer_name = "";
	String waybill_id = "";
	String waybill_no = "";
	String weight = "0.00";
	String express_no = "";
	String token;
	
	ArrayList<String> arrayList = new ArrayList<String>();
	
	int customer_index = 0, waybill_index = 0;
	int scan_flag = 0;
	
	private ArrayAdapter<String> mCsgnArrayAdapter;
	private ArrayAdapter<String> mWbillArrayAdapter;
	private ArrayAdapter<String> mExprnumArrayAdapter;

	boolean isSetCsgn = false;
	private Spinner spiwbill = null;
	private int count=0;

	private long lastTime,curTime;

	SetResultThread resultThread = null;
	private byte[] choosedData = new byte[] { 0x07, (byte) 0xC6, 0x04, 0x08,
			0x00, (byte) 0x9C, 0x0A, (byte) 0xFE, (byte) 0x81 };// 修改一维波特率115200
	private byte[] choosedData2 = new byte[] { 0x07, (byte) 0xC6, 0x04, 0x08,
			0x00, (byte) 0x9C, 0x0A, (byte) 0xFE, (byte) 0x81 };// 修改二维波特率115200
	// 启动service 方式2
	private String choosed_serial = "/dev/ttyMT0";// 串口号

	private void bindService() {
		Intent intent = new Intent(ScatterGoodsActivity.this, CaptureService.class);
		Log.i(TAG, "bindService()");
		bindService(intent, conn, ScatterGoodsActivity.BIND_AUTO_CREATE);
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
		setContentView(R.layout.activity_scattergoods);
		token = AppData.getInstance().getToken();
		
		tvcptcnt = (TextView) findViewById(R.id.cptcnt);		
		etdeclnum = (EditText) findViewById(R.id.etdeclnum);
		etcsgn = (EditText) findViewById(R.id.etcsgn);
		etweight = (EditText) findViewById(R.id.etweight);
		etexprnum = (EditText) findViewById(R.id.etexprnum);		
		btnMsg = (Button) findViewById(R.id.btnMsg);
		
		btnSearch = (Button) findViewById(R.id.btnSearch);
		btnSearch.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(etdeclnum.getText().toString().length()<=0){
					Log.e("etdeclnum.getText().toString()", etdeclnum.getText().toString());
//					onScanned(etexprnum.getText().toString());
//				}else{
//					Toast.makeText(getApplicationContext(), "快递单号不能为空！", Toast.LENGTH_LONG).show();
					showMessage(AppData.PROMPT_WARN, "报关单号不能为空！", true);
					return;
				}
				scan_flag |= 1;
				declaration_no = etdeclnum.getText().toString();
				new Thread() {
					public void run() {
						try {
							if (PostConsigneeRequest() == HttpStatus.SC_OK) {
								String status = csgnrslt.getString("status");
								if (status.equals("ok")) {
									mHandler.obtainMessage(MESSAGE_UPDATE_WAYBILL_LISTVIEW,
											customer_index, -1, csgnrslt).sendToTarget();
								}
							}
						} catch (HttpException e) {
							System.err.println("Fatal protocol violation: "
									+ e.getMessage());
						} catch (IOException e) {
							System.err.println("Fatal transport error: "
									+ e.getMessage());
						}
					}
				}.start();
			}
		});
		btnConfirm = (Button) findViewById(R.id.btnConfirm);
		btnConfirm.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(etexprnum.getText().toString().length()<=0){
					Log.e("etexprnum.getText().toString()", etexprnum.getText().toString());
//					onScanned(etexprnum.getText().toString());
//				}else{
//					Toast.makeText(getApplicationContext(), "快递单号不能为空！", Toast.LENGTH_LONG).show();
					showMessage(AppData.PROMPT_WARN, "快递单号不能为空！", true);
					return;
				}
				
				new Thread() {
					public void run() {
						try {
							if (PostScanDataRequest() == HttpStatus.SC_OK) {	
								mHandler.obtainMessage(MESSAGE_UPDATE_EXPRNUM_LISTVIEW,
										-1, -1, submitrslt).sendToTarget();								
							}
						} catch (HttpException e) {
							System.err.println("Fatal protocol violation: "
									+ e.getMessage());
						} catch (IOException e) {
							System.err.println("Fatal transport error: "
									+ e.getMessage());
						}
					}
				}.start();
			}
		});
		
		// 控件初始化		
		etweight.setText((CharSequence) weight);
//		etweight.setEnabled(false);
		etexprnum.setEnabled(false);
		
		AppData.getInstance().setEtwght(etweight);
		AppData.getInstance().setEtscode(etexprnum);

		CaptureService.callback = ScatterGoodsActivity.this;
	
		spiwbill = (Spinner) super.findViewById(R.id.selwaybill);
		spiwbill.setPrompt("请选择运单");
		mWbillArrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item);
		mWbillArrayAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spiwbill.setAdapter(mWbillArrayAdapter);
		spiwbill.setOnItemSelectedListener(mWaybillClickListener);

		mExprnumArrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item);
		ListView exprnumListView = (ListView) findViewById(R.id.lvexprnum);
		exprnumListView.setAdapter(mExprnumArrayAdapter);

		AppData.getInstance().setScanOpen(false);
		AppData.getInstance().setStartOpen(false);
		CaptureService.isCloseRead = false;
/*		
		if (getIntent().getData() != null) {
			// 监听是否获得扫描数据
			resultThread = new SetResultThread();
			resultThread.start();
		}
*/		
		if (CaptureService.serialPort == null) {
			// 开启电源
			CaptureService.scanGpio.openPower();
			try {
				// 连接串口
				CaptureService.serialPort = new SerialPort(choosed_serial,
						9600, 0);
				// 设置串口波特率
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
				CaptureService.serialPort = new SerialPort(choosed_serial,
						115200, 0);
				CaptureService.isChoosed = true;
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		etexprnum.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub
				express_no = etexprnum.getText().toString();

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
			}
		});
/*
		Button btn_ok = (Button) findViewById(R.id.btnok);
		btn_ok.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {

				new Thread() {
					public void run() {
						try {
							if (PostScanDataRequest() == HttpStatus.SC_OK) {
								mHandler.obtainMessage(
										MESSAGE_UPDATE_EXPRNUM_LISTVIEW, -1,
										-1, submitrslt).sendToTarget();
							}
						} catch (HttpException e) {
							System.err.println("Fatal protocol violation: "
									+ e.getMessage());
						} catch (IOException e) {
							System.err.println("Fatal transport error: "
									+ e.getMessage());
						}
					}
				}.start();
				
			}
		});
*/
		Button btn_goback = (Button) findViewById(R.id.btngoback);
		btn_goback.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
    			AppData.getInstance().finishActivity(ScatterGoodsActivity.this, "确认返回吗？", "确定", "取消");     
			}
		});
	}
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		if (keyCode == 4) {
			AppData.getInstance().finishActivity(ScatterGoodsActivity.this, "确认返回吗？", "确定", "取消");  			
			return true;
		}
		else if (keyCode == 66 || keyCode == 136 || keyCode == 135) {
			
			curTime=System.currentTimeMillis();
			if((curTime-lastTime)>2000){
				lastTime=curTime;
				
				etcsgn.setFocusable(false);					
				etcsgn.setEnabled(false);	
				
				AppData.getInstance().setStartOpen(true);
				//ScatterGoodsActivity.isStartOpen = true;
				CaptureService.isCloseRead = true;
				// 打开扫描激光
				Intent newIntent = new Intent(getApplicationContext(),
						CaptureService.class);
				newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startService(newIntent);
			}
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

	private OnItemSelectedListener mWaybillClickListener = new OnItemSelectedListener() {

		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {
			try {
				if (parent.getCount() <= 0)
					return;

				waybill_index = pos;
				if (csgnrslt != null) {
					JSONArray userarr = csgnrslt.getJSONArray("user");
					JSONObject jsonuser;

					jsonuser = userarr.getJSONObject(customer_index);
					JSONArray waybill_id_arr = jsonuser
							.getJSONArray("waybill_id");
					waybill_id = waybill_id_arr.getString(waybill_index);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void onNothingSelected(AdapterView<?> parent) {
			// Another interface callback
		}
	};

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_UPDATE_WAYBILL_LISTVIEW: {
				try {
					JSONObject jsonlist = csgnrslt.getJSONObject("list");
					waybill_no = jsonlist.getString("waybill_no");
					customer_name = jsonlist.getString("consignee");
					waybill_id = jsonlist.getString("waybill_id");
					etcsgn.setText(customer_name);
					mWbillArrayAdapter.clear();
					mWbillArrayAdapter.add(waybill_no);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}
			case MESSAGE_UPDATE_EXPRNUM_LISTVIEW: {
				try {
					express_no = etexprnum.getText().toString();				

					String status = submitrslt.getString("status");
					if (status.equals("ok")) {
						showMessage(AppData.PROMPT_SUCCESS, "国内分发成功。",true);
					} else if (status.equals("dup")) {
						showMessage(AppData.PROMPT_QUERY, "重复分发。",true);
					} else{
						showMessage(AppData.PROMPT_WARN, "分发失败。",true);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}
			case MESSAGE_SET_SCAN_DECLNUM: {
				try {
					etexprnum.setFocusable(true);
					etexprnum.setText((CharSequence) (String)(msg.obj));
					showMessage(AppData.PROMPT_SUCCESS, "快递单号："+(CharSequence) (String)(msg.obj),true);
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
			case MESSAGE_INSERT_DATA_SCATTER: {
				try {
					mExprnumArrayAdapter.insert(msg.obj.toString(), 0);
					count++;
					tvcptcnt.setText("国内分发(" + count + ")");
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}	
			}
		}
	};
	/**
	 * 发送返回值线程
	 * 
	 * @author zkc-soft2
	 * 
	 */
/*	
	public class SetResultThread extends Thread {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			while (!interrupted()) {
				if (et_code.getText().toString().trim().length() > 0) {
					// 将扫描数据返回值传送
					Intent intent = getIntent();
					intent.putExtra("code", et_code.getText().toString().trim());
					setResult(201, intent);
					finish();
				}
			}
		}

	}
*/
	public class SetResultThread extends Thread {

		@Override
		public void run() {
			// TODO Auto-generated method stub

			while (!this.isInterrupted()) {
				try{
					String scnCode=AppData.getInstance().getScncode();
					if (scnCode.isEmpty()) {
						Thread.sleep(10);
					}
					else{
						onScanned(scnCode);
						AppData.getInstance().setScncode("");					
					}
				}catch(InterruptedException ie){
					ie.printStackTrace();
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
			CaptureService.isChoosed = true;
		}

		CaptureService.isCloseRead = false;
		CaptureService.scanGpio.closeScan();
		// 关闭电源
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
			CaptureService.isChoosed = true;
		}
		CaptureService.isCloseRead = false;
		CaptureService.scanGpio.closeScan();
		// 关闭电源
		CaptureService.scanGpio.closePower();
		Log.v("onDestroy", "close");
		//System.out.println("onDestroy close");
		super.onDestroy();
	}
	
	public void onScanned(String paramString) {

		String flagstr=paramString.substring(0, 2);
		if(flagstr.equals("13")||flagstr.equals("23")||flagstr.equals("53")||flagstr.equals("86")||flagstr.equals("HR")||flagstr.equals("CA"))
		{
			declaration_no = paramString;

			etdeclnum.setFocusable(true);
			etdeclnum.setText(declaration_no);
			
			//调取声音
			showMessage(AppData.PROMPT_SUCCESS, "报关单号："+declaration_no, true);
			mExprnumArrayAdapter.clear();
			count=0;
			tvcptcnt.setText("国内分发(" + count + ")");//应该附上该收货人（customer_name）的包裹数量
			
			scan_flag |= 1;
			
			declaration_no = etdeclnum.getText().toString();
			
			new Thread() {
				public void run() {
					try {
						if (PostConsigneeRequest() == HttpStatus.SC_OK) {
							String status = csgnrslt.getString("status");
							if (status.equals("ok")) {
								mHandler.obtainMessage(MESSAGE_UPDATE_WAYBILL_LISTVIEW,
										customer_index, -1, csgnrslt).sendToTarget();
							}
						}
					} catch (HttpException e) {
						System.err.println("Fatal protocol violation: "
								+ e.getMessage());
					} catch (IOException e) {
						System.err.println("Fatal transport error: "
								+ e.getMessage());
					}
				}
			}.start();
			
		}
		else{

			express_no = paramString;
			mHandler.obtainMessage(MESSAGE_SET_SCAN_DECLNUM,
					-1, -1, paramString).sendToTarget();
			
			scan_flag |= 2;
		}

		if(scan_flag !=3)return;
		scan_flag = 1;
		
		new Thread() {
			public void run() {
				try {
					if (PostScanDataRequest() == HttpStatus.SC_OK) {
						mHandler.obtainMessage(MESSAGE_UPDATE_EXPRNUM_LISTVIEW,
								-1, -1, submitrslt).sendToTarget();
					}
				} catch (HttpException e) {
					System.err.println("Fatal protocol violation: "
							+ e.getMessage());
				} catch (IOException e) {
					System.err.println("Fatal transport error: "
							+ e.getMessage());
				}
			}
		}.start(); 
	}

	public int PostConsigneeRequest() throws HttpException, IOException {
		HttpClient client = new HttpClient();
		StringBuilder sb = new StringBuilder();
		InputStream ins = null;
		int statusCode = HttpStatus.SC_METHOD_FAILURE;
		String result;

		PostMethod method = new PostMethod(
				"http://transport.360-express.com/index.php?route=api/waybill/getwaybill");

		NameValuePair[] param = {
				new NameValuePair("declaration_no", declaration_no),
				new NameValuePair("token", token),
				new NameValuePair("code", MD5.GetMD5Code(declaration_no + token)) };

		method.setRequestHeader("Content-Type",
				"application/x-www-form-urlencoded;charset=UTF-8");
		method.setRequestBody(param);

		try {
			// Execute the method.
			statusCode = client.executeMethod(method);

			//System.out.println(statusCode);
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
					csgnrslt = JSONObject.fromString(result);
						//csgnrslt = {"msg":"该报关单号不存在","status":"no"}
						//csgnrslt = {"msg":"运单读取成功","status":"ok","list":{"waybill_id":"1318","waybill_no":"471480761645","consignee":"CE-CESHIDE"}}
					if (csgnrslt != null) {
						String status = csgnrslt.getString("status");
						String msg = csgnrslt.getString("msg");
						
						if (status.equals("ok")) {
							// showMessage(AppData.PROMPT_QUERY, "收货人信息提交成功。");
//							JSONObject jsonlist = csgnrslt.getJSONObject("list");
//							String waybill_no2 = jsonlist.getString("waybill_no");
//							String consignee2 = jsonlist.getString("consignee");
//							String waybill_id2 = jsonlist.getString("waybill_id");
						} else {
							statusCode = HttpStatus.SC_METHOD_FAILURE;
							showMessage(AppData.PROMPT_ERROR, msg,true);
							// showMessage(AppData.PROMPT_WARN, "收货人信息提交失败。");
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					statusCode = HttpStatus.SC_METHOD_FAILURE;
					// showMessage(AppData.PROMPT_WARN, "收货人信息提交异常。");
				}

			} else {
				System.err.println("Response Code: " + statusCode);
			}

			isSetCsgn = false;

		} catch (HttpException e) {
			System.err.println("Fatal protocol violation: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("Fatal transport error: " + e.getMessage());
		} finally {
			method.releaseConnection();
			if (ins != null) {
				ins.close();
			}
		}

		return statusCode;
		// return HttpStatus.SC_OK;
	}

	public int PostScanDataRequest() throws HttpException, IOException {
		HttpClient client = new HttpClient();
		StringBuilder sb = new StringBuilder();
		InputStream ins = null;
		int statusCode = HttpStatus.SC_METHOD_FAILURE;
		String result;

		try {
			customer_name = etcsgn.getText().toString();
			if(customer_name.isEmpty()){
				statusCode = HttpStatus.SC_METHOD_FAILURE;
				showMessage(AppData.PROMPT_WARN, "请先检索合适的收货人", false);
				return statusCode;
			}			

			customer_name = etcsgn.getText().toString();
			if(spiwbill.getCount()>0){
				waybill_no = spiwbill.getSelectedItem().toString();				
			}else{
				waybill_no = "";
				waybill_id = "";
			}
			weight = etweight.getText().toString();
			express_no = etexprnum.getText().toString();

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		/*int i, j;
		try {
			if (csgnrslt != null) {
				JSONArray userarr = csgnrslt.getJSONArray("user");
				JSONObject jsonuser;
				for (i = 0; i < userarr.length(); i++) {
					jsonuser = userarr.getJSONObject(i);
					String username = jsonuser.getString("username");
					if (username.equals(customer_name)) {
						customer_id = jsonuser.getString("customer_id");

						if(!waybill_no.equals("新建运单")){
							JSONArray waybill_id_arr = jsonuser.getJSONArray("waybill_id");
							JSONArray waybill_no_arr = jsonuser.getJSONArray("waybill_no");					
							for (j = 0; j < waybill_no_arr.length(); j++) {
								if(waybill_no.equals(waybill_no_arr.getString(j))){
									waybill_id = waybill_id_arr.getString(j);
									break;
								}
							}
						}
						else{
							waybill_no = "";
							waybill_id = "";
						}
						break;
					}
				}
				if(i>=userarr.length()){
					statusCode = HttpStatus.SC_METHOD_FAILURE;
					showMessage(AppData.PROMPT_WARN, "请从收货人列表中选择合适的收货人", true);
					return statusCode;
				}				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}*/
		
		BigDecimal flw = new BigDecimal(weight);
		weight = flw.setScale(2,BigDecimal.ROUND_HALF_UP).toString();
		
		PostMethod method = new PostMethod(
				"http://transport.360-express.com/index.php?route=api/waybill/express");

		NameValuePair[] param = {
//				new NameValuePair("consignee_id", customer_id),
				new NameValuePair("waybill_id", waybill_id),
				new NameValuePair("weight", weight),
				new NameValuePair("express_no", express_no),
				new NameValuePair("token", token),
				new NameValuePair("code", MD5.GetMD5Code(customer_id
						+ waybill_id + weight + express_no + token)) };

		method.setRequestHeader("Content-Type",
				"application/x-www-form-urlencoded;charset=UTF-8");
		method.setRequestBody(param);

		try {
			// Execute the method.
			statusCode = client.executeMethod(method);

			//System.out.println(statusCode);
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
						String msg = submitrslt.getString("msg");
						if (status.equals("ok")) {					
							
							String str = express_no + "   "	+ weight;
							mHandler.obtainMessage(MESSAGE_INSERT_DATA_SCATTER, -1, -1, str).sendToTarget();	
							showMessage(AppData.PROMPT_SUCCESS, msg, true);
						} else {
							statusCode = HttpStatus.SC_METHOD_FAILURE;
							if (status.equals("dup")) {
								showMessage(AppData.PROMPT_WARN, msg, true);
							} else {
								showMessage(AppData.PROMPT_ERROR, msg, true);
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					statusCode = HttpStatus.SC_METHOD_FAILURE;
					showMessage(AppData.PROMPT_WARN, "国内分发信息异常，请重新采集提交。", false);
				}
			} else {
				System.err.println("Response Code: " + statusCode);
			}

		} catch (HttpException e) {
			System.err.println("Fatal protocol violation: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("Fatal transport error: " + e.getMessage());
		} finally {
			method.releaseConnection();
			if (ins != null) {
				ins.close();
			}
		}

		return statusCode;
		// return HttpStatus.SC_OK;
	}
}
