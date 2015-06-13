package com.hzh.dsw;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;

import com.hzh.dsw.CheckOrderActivity.SetResultThread;
import com.zkc.Service.CaptureService;
import com.zkc.pc700.helper.SerialPort;

import android.app.Activity;
import android.content.ComponentName;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class CheckOrderActivity extends Activity implements CaptureService.ICallback {
	private static final String TAG = "CheckOrderActivity";
	public static final int MESSAGE_UPDATE_DECLNUM_LISTVIEW = 1;
	public static final int MESSAGE_SHOW_SCAN_MSG = 2;	
	public static final int MESSAGE_SET_SCAN_DECLNUM = 5;
	
	private ArrayAdapter<String> mDeclnumArrayAdapter;

	JSONObject submitrslt = null;

	EditText etdeclnum;
	TextView tvcptcnt;
	private Button btnMsg,btnConfirm;
	
	String declaration_no;
	String token;
	
	ArrayList<String> arrayList = new ArrayList<String>();
	
	private int count=0;
	SetResultThread resultThread = null;
	private byte[] choosedData = new byte[] { 0x07, (byte) 0xC6, 0x04, 0x08,
			0x00, (byte) 0x9C, 0x0A, (byte) 0xFE, (byte) 0x81 };// 修改一维波特率115200
	private byte[] choosedData2 = new byte[] { 0x07, (byte) 0xC6, 0x04, 0x08,
			0x00, (byte) 0x9C, 0x0A, (byte) 0xFE, (byte) 0x81 };// 修改二维波特率115200
	// 启动service 方式2
	private String choosed_serial = "/dev/ttyMT0";// 串口号

	private void bindService() {
		Intent intent = new Intent(CheckOrderActivity.this, CaptureService.class);
		Log.i(TAG, "bindService()");
		bindService(intent, conn, CheckOrderActivity.BIND_AUTO_CREATE);
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
		setContentView(R.layout.activity_checkorder);
		token = AppData.getInstance().getToken();

		tvcptcnt = (TextView) findViewById(R.id.cptcnt);
		etdeclnum = (EditText) findViewById(R.id.etdeclnum);
		btnMsg = (Button) findViewById(R.id.btnMsg);
		
		btnConfirm = (Button) findViewById(R.id.btnConfirm);
		btnConfirm.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				onScanned(etdeclnum.getText().toString());
			}
		});
		
		// 控件初始化
//		etdeclnum.setEnabled(false);//已修改为可编辑
		
		AppData.getInstance().setEtscode(etdeclnum);

		CaptureService.callback = CheckOrderActivity.this;
		
		mDeclnumArrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item);
		ListView declnumListView = (ListView) findViewById(R.id.lvdeclnum);
		declnumListView.setAdapter(mDeclnumArrayAdapter);

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
	    
		etdeclnum.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub
				/*
				 * declaration_no = etdeclnum.getText().toString();
				 * mDeclnumArrayAdapter.add(declaration_no);
				 */
				// Toast.makeText(CheckOrderActivity.this, "Changed--" +
				// declaration_no , Toast.LENGTH_SHORT).show();
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
		
		Button btn_goback = (Button) findViewById(R.id.btngoback);
		btn_goback.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
    			AppData.getInstance().finishActivity(CheckOrderActivity.this, "确认返回吗？", "确定", "取消");     
			}
		});
	}
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		if (keyCode == 4) {
			AppData.getInstance().finishActivity(CheckOrderActivity.this, "确认返回吗？", "确定", "取消");  			
			return true;
		}
		if (keyCode == 66 || keyCode == 136 || keyCode == 135) {
			AppData.getInstance().setStartOpen(true);
			//CheckOrderActivity.isStartOpen = true;
			CaptureService.isCloseRead = true;
			// 打开扫描激光
			Intent newIntent = new Intent(getApplicationContext(),
					CaptureService.class);
			newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startService(newIntent);

			return true;
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
				} else {
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
			case MESSAGE_UPDATE_DECLNUM_LISTVIEW: {
				try {
					String weight="",consignee="",number="",consignor="",product="";
					if (submitrslt != null) {
						String status = submitrslt.getString("status");
						String rtnmsg = submitrslt.optString("msg");
						btnMsg.setText(rtnmsg);
						
						if (status.equals("ok")) {
							mDeclnumArrayAdapter.clear();

							JSONArray recarr = submitrslt.getJSONArray("list");
							JSONObject jsonobj;				
							for (int i = 0; i < recarr.length(); i++) {
								jsonobj = recarr.getJSONObject(i);
								weight = jsonobj.optString("weight");
								consignee = jsonobj.optString("consignee");
								number = jsonobj.optString("declaration_no");
								consignor = jsonobj.optString("consignor");
								product = jsonobj.optString("consignee_name");//收货人中文名字
								
								if(weight==null||weight=="null")weight="0.00";
								if(consignee==null||consignee=="null")consignee="收货人";
								if(number==null||number=="null")number="返回单号";	
								
								mDeclnumArrayAdapter.insert(number + "   " + consignee + "   " + weight+ "   " +consignor+ "   " +product,0);								
							}							
						}
						tvcptcnt.setText("中转核单(" + count + ")");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}
			case MESSAGE_SET_SCAN_DECLNUM: {
				try {
					etdeclnum.setFocusable(true);
					etdeclnum.setText((CharSequence) (String)(msg.obj));
					// 播放声音
					showMessage(AppData.PROMPT_SUCCESS, "包裹编号："+(CharSequence) (String)(msg.obj),true);
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
		
		/*mHandler.obtainMessage(MESSAGE_SET_SCAN_DECLNUM,
				-1, -1, paramString).sendToTarget();	*/	
		etdeclnum.setFocusable(true);
		etdeclnum.setText(paramString);
		declaration_no = paramString;
		
		try{
			Thread.sleep(500);
		}catch(InterruptedException ire){
			ire.printStackTrace();
		}
		
		new Thread() {
			public void run() {
				try {
					if (PostScanDataRequest() == HttpStatus.SC_OK) {						
						mHandler.obtainMessage(MESSAGE_UPDATE_DECLNUM_LISTVIEW,
								-1, -1, submitrslt).sendToTarget();
						// 播放声音
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

	public int PostScanDataRequest() throws HttpException, IOException {
		HttpClient client = new HttpClient();
		StringBuilder sb = new StringBuilder();
		InputStream ins = null;
		int statusCode = HttpStatus.SC_METHOD_FAILURE;
		String result;

		try {
			if(declaration_no.isEmpty()){
				declaration_no = etdeclnum.getText().toString();
			
				if(declaration_no.isEmpty()){
					statusCode = HttpStatus.SC_METHOD_FAILURE;
					showMessage(AppData.PROMPT_WARN, "请先扫描报关单再提交", false);
					return statusCode;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		PostMethod method = new PostMethod(
				"http://transport.360-express.com/index.php?route=api/waybill/check");

		NameValuePair[] param = {
				new NameValuePair("number", declaration_no),
				new NameValuePair("token", token),
				new NameValuePair("code",
						MD5.GetMD5Code(declaration_no + token)) };

		method.setRequestHeader("Content-Type",
				"application/x-www-form-urlencoded;charset=UTF-8");
		method.setRequestBody(param);//param=[name=number, value=858585850031, name=token, value=217930991af48d7029453fc8f490d806, name=code, value=d712e0d58b8a85f4575926dd55998860]

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
					/*result = {"status":"ok",
						"msg":"\u9700\u8981\u62c6\u5305",
						"list":[
						        {"declaration_no":"13537636","consignee":"CE-XXQYZ","consignor":"XXQF","product_name":"\u8863\u670d","num":"1","weight":"5.10"},
						        {"declaration_no":"13537635","consignee":"CE-XXQYZ","consignor":"XXQF","product_name":"\u8863\u670d","num":"1","weight":"5.10"},
						        {"declaration_no":"13537634","consignee":"CE-XXQYZ","consignor":"XXQF","product_name":"\u8863\u670d","num":"1","weight":"4.80"},
						        {"declaration_no":"13537633","consignee":"CE-XXQYZ","consignor":"XXQF","product_name":"\u8863\u670d","num":"1","weight":"4.60"},
						        {"declaration_no":"13537637","consignee":"CE-XXQFF","consignor":"XXQF","product_name":"\u8863\u670d","num":"1","weight":"1.90"}]}*/
					/*submitrslt = {msg=需要拆包, 
							status=ok, 
							list=[
							      {"num":"1","consignor":"XXQF","weight":"5.10","consignee":"CE-XXQYZ","declaration_no":"13537636","product_name":"衣服"},
							      {"num":"1","consignor":"XXQF","weight":"5.10","consignee":"CE-XXQYZ","declaration_no":"13537635","product_name":"衣服"},
							      {"num":"1","consignor":"XXQF","weight":"4.80","consignee":"CE-XXQYZ","declaration_no":"13537634","product_name":"衣服"},
							      {"num":"1","consignor":"XXQF","weight":"4.60","consignee":"CE-XXQYZ","declaration_no":"13537633","product_name":"衣服"},
							      {"num":"1","consignor":"XXQF","weight":"1.90","consignee":"CE-XXQFF","declaration_no":"13537637","product_name":"衣服"}]}*/
					
					
					if (submitrslt != null) {
						String status = submitrslt.getString("status");
						if (status.equals("ok")) {
							showMessage(AppData.PROMPT_SUCCESS, "中转核单信息核单成功。",true);
							count ++;
						} else {
							statusCode = HttpStatus.SC_METHOD_FAILURE;
							if (status.equals("dup")){
								showMessage(AppData.PROMPT_QUERY, "中转核单信息重复。",true);
							}else{
								showMessage(AppData.PROMPT_WARN, "中转核单信息核单失败。",true);
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					statusCode = HttpStatus.SC_METHOD_FAILURE;
					showMessage(AppData.PROMPT_WARN, "中转核单信息异常，请重新核单。",true);
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
