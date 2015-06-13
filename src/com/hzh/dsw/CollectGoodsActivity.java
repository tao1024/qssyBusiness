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

import com.hzh.dsw.ComboBox.ListViewItemClickListener;

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

public class CollectGoodsActivity extends Activity implements CaptureService.ICallback {
	private static final String TAG = "CollectGoodsActivity";
	
	public static final int MESSAGE_SET_SCAN_DECLNUM = 1;
	public static final int MESSAGE_SHOW_SCAN_MSG = 2;
	public static final int MESSAGE_INSERT_DATA = 3;
	
	JSONObject submitrslt = null;
	JSONObject jsoncustomer = null;

	private EditText etcsgn, etweight, etpcs, etdeclnum;
	private TextView tvcptcnt;
	private Button btnMsg;
	
	String customer_id = "";
	String customer_name = "";
	String product_id = "";
	String product_name = "衣服";
	String weight = "0.20";
	String pieces = "100";
	String declaration_no = "";
	String token;

	int product_index = 0;
	int scan_flag = 0;
	
	private ArrayAdapter<String> mDeclWgtArrayAdapter;
	
	ArrayList<String> arrayList = new ArrayList<String>();
	
	private ComboBox m_combobox;
	private String[] list_goods = {	"衣服","鞋","包","手表","饰品","文化用品","汽配","配件","衣服 鞋","鞋 帽","小衫 帽","衣服 眼镜","衣服 手表","工艺品","餐具","熨板","包装壳"};
	
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
		Intent intent = new Intent(CollectGoodsActivity.this, CaptureService.class);
		Log.i(TAG, "bindService()");
		bindService(intent, conn, CollectGoodsActivity.BIND_AUTO_CREATE);
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
		setContentView(R.layout.activity_collectgoods);
		token = AppData.getInstance().getToken();
		lastTime=System.currentTimeMillis();
		
		tvcptcnt = (TextView) findViewById(R.id.cptcnt);
		etcsgn = (EditText) findViewById(R.id.etcsgn);
		etweight = (EditText) findViewById(R.id.etweight);
		etpcs = (EditText) findViewById(R.id.etpieces);
		etdeclnum = (EditText) findViewById(R.id.etdeclnum);
//		etexprnum = (EditText) findViewById(R.id.etexprnum);	//快递单号	
		btnMsg = (Button) findViewById(R.id.btnMsg);
		
		// 控件初始化
		etweight.setText((CharSequence) weight);
		etpcs.setText((CharSequence) pieces);
		etcsgn.setEnabled(false);
		etweight.setEnabled(false);
		etdeclnum.setEnabled(false);
//		etexprnum.setEnabled(false);//快递单号	
		
		AppData.getInstance().setEtwght(etweight);
		AppData.getInstance().setEtscode(etdeclnum);
		
		CaptureService.callback = CollectGoodsActivity.this;
		
		mDeclWgtArrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item);
		ListView declwgtListView = (ListView) findViewById(R.id.lvdeclwgt);
		declwgtListView.setAdapter(mDeclWgtArrayAdapter);

	    m_combobox = (ComboBox)findViewById(R.id.id_combobox);
	    m_combobox.setData(list_goods);
	    m_combobox.selectItem(0);
	    
	    m_combobox.setListViewOnClickListener(new ListViewItemClickListener() {
			
			@Override
			public void onItemClick(int postion) {
				Log.i("CMM", "m_combobox:"+m_combobox.getText());
				product_name = m_combobox.getText();
			}
		});
/*
	    try{
		    QSYDataManager sqlmgr = new QSYDataManager(getApplicationContext());
		    String customers = sqlmgr.getcustomers("customer");
		    sqlmgr.exitQSYDataManager();
		    if(!customers.isEmpty()){
			    jsoncustomer = JSONObject.fromString(customers);
		    }
	    }catch(Exception e){
	    	e.printStackTrace();
	    }
*/	    
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
	
		etpcs.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				// TODO Auto-generated method stub
				if (arg1.getAction() == MotionEvent.ACTION_DOWN) {
					pieces = "";
					etpcs.setText(pieces);
				} else if (arg1.getAction() == MotionEvent.ACTION_MOVE) {

				} else if (arg1.getAction() == MotionEvent.ACTION_UP) {

				}
				return false;
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
										MESSAGE_UPDATE_WAYBILL_DECLNUM, -1, -1,
										submitrslt).sendToTarget();
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
    			AppData.getInstance().finishActivity(CollectGoodsActivity.this, "确认返回吗？", "确定", "取消");     
			}
		});
	}
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		if (keyCode == 4) {
			AppData.getInstance().finishActivity(CollectGoodsActivity.this, "确认返回吗？", "确定", "取消");  			
		}
		else if (keyCode == 66 || keyCode == 136 || keyCode == 135) {
/*			
			curTime=System.currentTimeMillis();
			if((curTime-lastTime)>2000){
				lastTime=curTime;
*/								
				etcsgn.setEnabled(false);	
				etdeclnum.setEnabled(false);
				m_combobox.setEnabled(false);
				
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
			case MESSAGE_SET_SCAN_DECLNUM: {
				try {
					etdeclnum.setFocusable(true);
					etdeclnum.setText((CharSequence) (String)(msg.obj));
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}
			case MESSAGE_SHOW_SCAN_MSG: {
				try {
					btnMsg.setText((String)(msg.obj));
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}	
			case MESSAGE_INSERT_DATA: {
				try {
					mDeclWgtArrayAdapter.insert(msg.obj.toString(), 0);
					count++;
					tvcptcnt.setText("海外集货(" + count + ")");
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}	
			}
		}
	};

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
				if (etdeclnum.getText().toString().trim().length() > 0) {
					// ��ɨ�����ݷ���ֵ����
					Intent intent = getIntent();
					intent.putExtra("code", etdeclnum.getText().toString().trim());
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
			customer_id = paramString;//CE11982
			customer_name = paramString;//CE11982
			
			try {
				int i;
				if (jsoncustomer != null) {
					customer_name = "";
					JSONArray recarr = jsoncustomer.getJSONArray("records");
					JSONObject jsonobj;				
					for (i = 0; i < recarr.length(); i++) {
						jsonobj = recarr.getJSONObject(i);
						String customerid = jsonobj.getString("customer_id");
						if(paramString.equals(customerid)){
							customer_name = jsonobj.getString("customer_name");
							break;
						}
					}
					if(i>=recarr.length()){
						showMessage(AppData.PROMPT_WARN, "无效收货人: " + paramString, false);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			etcsgn.setFocusable(true);
			etcsgn.setText(customer_name);
			
			//调取声音
			showMessage(AppData.PROMPT_SUCCESS, "收货人："+customer_name, true);
//			mDeclWgtArrayAdapter.insert(declaration_no + "   "	+ weight, 0);
			mDeclWgtArrayAdapter.clear();
			count=0;
			tvcptcnt.setText("海外集货(" + count + ")");//应该附上该收货人（customer_name）的包裹数量
													//或者重新开个接口，提交customer_name，返回包裹数量，再赋值

			scan_flag |= 1; //scan_flag = scan_flag|1;
		}
		else{
			etdeclnum.setEnabled(true);
			etdeclnum.setFocusable(true);
			etdeclnum.setText(paramString);	
			etdeclnum.setFocusable(false);
			etdeclnum.setEnabled(false);
			declaration_no = paramString;
			//调取声音
			showMessage(AppData.PROMPT_SUCCESS, "报关单号："+declaration_no, true);
			
			try{
				Thread.sleep(500);
			}catch(InterruptedException ire){
				ire.printStackTrace();
			}
			
			scan_flag |= 2; //scan_flag = scan_flag|2;
		}
		
		if(scan_flag !=3)return;
		scan_flag = 1;
		
		weight = etweight.getText().toString();
		if(Double.parseDouble(weight) < 0.01){
			showMessage(AppData.PROMPT_ERROR, "重量不允许为空！", true);
			return;
		}
		pieces = etpcs.getText().toString();
		product_name = m_combobox.getText();
		
		BigDecimal flw = new BigDecimal(weight);
		weight = flw.setScale(2,BigDecimal.ROUND_HALF_UP).toString();
		
		try{			
			SubmitThread scnThread = new SubmitThread(
					customer_id,
					product_name,
					weight,
					pieces,
					declaration_no,
					token
					);
			scnThread.start();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		m_combobox.setEnabled(true);		
	}

	private class SubmitThread extends Thread {
		private final String thrd_customer_id;
		private final String thrd_product_name;			
		private final String thrd_weight;
		private final String thrd_pieces;
		private final String thrd_declaration_no;
		private final String thrd_token;
				
		public SubmitThread(
			String cs_customer_id,
			String cs_product_name,				
			String cs_weight,
			String cs_pieces,
			String cs_declaration_no,
			String cs_token) {
			thrd_customer_id=cs_customer_id;
			thrd_product_name=cs_product_name;
			thrd_weight=cs_weight;
			thrd_pieces=cs_pieces;
			thrd_declaration_no=cs_declaration_no;
			thrd_token = cs_token;		
		}

		public void run() {
			setName("SubmitThread");
			try {
				long begint = System.currentTimeMillis();				
				PostScanDataRequest();
				long endt = System.currentTimeMillis();

				//Log.i(TAG,thrd_declaration_no+": "+(endt-begint));
//				showMessage(AppData.PROMPT_SUCCESS,	thrd_declaration_no+": " + (endt-begint), false);
				
			} catch (HttpException e) {
				System.err.println("Fatal protocol violation: "
						+ e.getMessage());
			} catch (IOException e) {
				System.err.println("Fatal transport error: "
						+ e.getMessage());
			}
		}
		
		public int PostScanDataRequest() throws HttpException, IOException {
			HttpClient client = new HttpClient();
			StringBuilder sb = new StringBuilder();
			InputStream ins = null;
			int statusCode = HttpStatus.SC_METHOD_FAILURE;
			String result;
			
			PostMethod method = new PostMethod(
					"http://transport.360-express.com/index.php?route=api/waybill/insert");

			NameValuePair[] param = {
					new NameValuePair("customer_no", thrd_customer_id),//CE10DU
					new NameValuePair("product_name", thrd_product_name),//衣服					
					new NameValuePair("weight", thrd_weight),//0.00
					new NameValuePair("num", thrd_pieces),// 件数
					new NameValuePair("declaration_no", thrd_declaration_no),//13328606
					new NameValuePair("token", thrd_token),
					new NameValuePair("code", MD5.GetMD5Code(thrd_customer_id + thrd_product_name + thrd_weight + thrd_pieces
							+ thrd_declaration_no + thrd_token)) };

			method.setRequestHeader("Content-Type",
					"application/x-www-form-urlencoded;charset=UTF-8");
			method.setRequestBody(param);
			//param = [name=customer_no, value=CE10DU, name=product_name, value=衣服, name=weight, value=0.00, name=num, value=1, name=declaration_no, value=13328606, name=token, value=884e4ecdcf587f31ed87184c56437a3b, name=code, value=d1b33cc55977caf2f0b3e79946ec7a08]

			try {
				// Execute the method.
				statusCode = client.executeMethod(method);//200
				//System.out.println(statusCode);
				
				if (statusCode == HttpStatus.SC_OK) {
					ins = method.getResponseBodyAsStream();
					byte[] b = new byte[1024];
					int r_len = 0;
					while ((r_len = ins.read(b)) > 0) {
						sb.append(new String(b, 0, r_len, method
								.getResponseCharSet()));
					}
					result = sb.toString();//result = {"status":"dup","msg":"\u62a5\u5173\u5355\u53f7\u91cd\u590d"}
											//result = {"status":"ok","msg":"\u6dfb\u52a0\u6210\u529f"}
					
					try {
						submitrslt = JSONObject.fromString(result);
						if (submitrslt != null) {
							String status = submitrslt.getString("status");//dup
							String msg = submitrslt.optString("msg");//msg = 报关单号重复/添加成功
							if (status.equals("ok")) {
								
								String str = thrd_declaration_no + "   "	+ thrd_weight;
								mHandler.obtainMessage(MESSAGE_INSERT_DATA, -1, -1, str).sendToTarget();
//								mDeclWgtArrayAdapter.insert(thrd_declaration_no + "   "	+ thrd_weight, 0);
//								count++;
//								tvcptcnt.setText("海外集货(" + count + ")");
								
								showMessage(AppData.PROMPT_SUCCESS, msg, true);
//								ListWatch();
								
								
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
						statusCode = HttpStatus.SC_METHOD_FAILURE;
						showMessage(AppData.PROMPT_ERROR, "集货信息异常，请重新采集提交。", true);
						e.printStackTrace();
					}
				} else {
					showMessage(AppData.PROMPT_ERROR, "集货信息提交请求失败，返回码：" + statusCode, true);
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
		}

		/*private void ListWatch(thrd_declaration_no,thrd_weight) {
			// TODO Auto-generated method stub
			mDeclWgtArrayAdapter.insert(thrd_declaration_no + "   "	+ thrd_weight, 0);
			count++;
			tvcptcnt.setText("海外集货(" + count + ")");
		}*/		
			
	}
}
