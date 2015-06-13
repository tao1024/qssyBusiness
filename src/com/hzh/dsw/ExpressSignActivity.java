package com.hzh.dsw;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpMethodParams;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class ExpressSignActivity extends Activity {

	public static final int MESSAGE_UPDATE_CONSIGNEE_LISTVIEW = 1;
	public static final int MESSAGE_UPDATE_WAYBILL_LISTVIEW = 2;
	public static final int MESSAGE_UPDATE_EXPRSIGN_LISTVIEW = 3;

    private static final int NONE = 0;
    private static final int PHOTO_GRAPH = 1;// 拍照
    private static final int PHOTO_ZOOM = 2; // 缩放
    private static final int PHOTO_RESULT = 3;// 结果
    private static final String IMAGE_UNSPECIFIED = "image/*";	
	
	JSONObject csgnrslt = null, submitrslt = null;

	EditText etcsgn;
	TextView tvcptcnt;
	
	String customer_id = "";
	String customer_name = "";
	String waybill_id = "";
	String waybill_no = "";
	String token;
	String imgfile;
	
	int customer_index = 0, waybill_index = 0;

	private ArrayAdapter<String> mCsgnArrayAdapter;
	private ArrayAdapter<String> mWbillArrayAdapter;
	private ArrayAdapter<String> mExprsignArrayAdapter;

	boolean isSetCsgn = false;
	private Spinner spiwbill = null;
	private AutoCompleteTextView atcsgn = null;
	private ImageView ivExprsign = null;
	private byte[] exprsignimg = null;
	private int count=0;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 设置布局
		setContentView(R.layout.activity_exprsign);
		token = AppData.getInstance().getToken();
		
		tvcptcnt = (TextView) findViewById(R.id.cptcnt);		
		etcsgn = (EditText) findViewById(R.id.etcsgn);
		ivExprsign = (ImageView) findViewById(R.id.imagesign);
		
		// 控件初始化

		mCsgnArrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line);
		atcsgn = (AutoCompleteTextView) findViewById(R.id.selcsgn);
		// 设置Adapter
		atcsgn.setAdapter(mCsgnArrayAdapter);
		atcsgn.setOnItemSelectedListener(mConsigneeClickListener);

		spiwbill = (Spinner) super.findViewById(R.id.selwaybill);
		spiwbill.setPrompt("请选择运单");
		mWbillArrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item);
		mWbillArrayAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spiwbill.setAdapter(mWbillArrayAdapter);
		spiwbill.setOnItemSelectedListener(mWaybillClickListener);

		mExprsignArrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item);
		ListView exprsignListView = (ListView) findViewById(R.id.lvexprsign);
		exprsignListView.setAdapter(mExprsignArrayAdapter);

		etcsgn.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub
				if (isSetCsgn)return;

				customer_name = etcsgn.getText().toString();
				String str = atcsgn.getText().toString();
				if(str.equals(customer_name))return;
				
				new Thread() {
					public void run() {
						try {
							if (PostConsigneeRequest() == HttpStatus.SC_OK) {
								String status = csgnrslt.getString("status");
								if (status.equals("ok")) {
									mHandler.obtainMessage(
											MESSAGE_UPDATE_CONSIGNEE_LISTVIEW,
											-1, -1, csgnrslt).sendToTarget();
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

		atcsgn.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub
				int i,pos=0;
				String sinfo=atcsgn.getText().toString();
				
				try {

					for(i=0;i<mCsgnArrayAdapter.getCount();i++)
					{
						String str=mCsgnArrayAdapter.getItem(i);
						if(str.equals(sinfo)){
							pos=i;break;
						}
					}
					if(i>=mCsgnArrayAdapter.getCount())return;
					
					customer_index = pos;
					etcsgn.setText(sinfo);
					
					if (csgnrslt != null) {
						JSONArray userarr = csgnrslt.getJSONArray("user");
						JSONObject jsonuser;

						jsonuser = userarr.getJSONObject(customer_index);
						customer_id = jsonuser.getString("customer_id");

						mHandler.obtainMessage(MESSAGE_UPDATE_WAYBILL_LISTVIEW,
								customer_index, -1, csgnrslt).sendToTarget();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}					
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
		
		ivExprsign.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				Date date = new Date();
				SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss",Locale.CHINA); // 格式化时间
				String filename = format.format(date) + ".jpg";
				File fileFolder = new File(Environment.getExternalStorageDirectory()
						+ "/qsybtw/");
				if (!fileFolder.exists()) { // 如果目录不存在，则创建一个名为"finger"的目录
					fileFolder.mkdir();
				}				
				//打开系统照相机拍照
				Uri uri = Uri.fromFile(new File(fileFolder,filename));
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);              
                startActivityForResult(intent, PHOTO_GRAPH);
			}
		});
		
		Button btn_ok = (Button) findViewById(R.id.btnok);
		btn_ok.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {

				new Thread() {
					public void run() {
						try {
							if (PostScanDataRequest() == HttpStatus.SC_OK) {
								mHandler.obtainMessage(
										MESSAGE_UPDATE_EXPRSIGN_LISTVIEW, -1,
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

		Button btn_goback = (Button) findViewById(R.id.btngoback);
		btn_goback.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
    			AppData.getInstance().finishActivity(ExpressSignActivity.this, "确认返回吗？", "确定", "取消");     
			}
		});
	}
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		if (keyCode == 4) {
			AppData.getInstance().finishActivity(ExpressSignActivity.this, "确认返回吗？", "确定", "取消");  			
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 拍照
        if (requestCode == PHOTO_GRAPH) {
        	try{
        		if(data!=null){
		            Bundle extras = data.getExtras();
		            if (extras != null) {
		                Bitmap photo = extras.getParcelable("data");
		                saveBitmap(photo);

		                ByteArrayOutputStream stream = new ByteArrayOutputStream();
						photo.compress(Bitmap.CompressFormat.JPEG, 75, stream);
						exprsignimg = stream.toByteArray();

						ivExprsign.setImageBitmap(photo);
		                return;
		            }
        		}
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
		// 处理结果
		if (requestCode == PHOTO_RESULT) {
			try {
				if (data != null) {
					Bundle extras = data.getExtras();
					if (extras != null) {
						Bitmap photo = extras.getParcelable("data");
						ByteArrayOutputStream stream = new ByteArrayOutputStream();
						photo.compress(Bitmap.CompressFormat.JPEG, 75, stream);
						ivExprsign.setImageBitmap(photo);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}       
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void startPhotoZoom(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setType(IMAGE_UNSPECIFIED);         
        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // outputX outputY 是裁剪图片宽高
        intent.putExtra("outputX", 400);
        intent.putExtra("outputY", 400);
        startActivityForResult(intent, PHOTO_RESULT);
    }

	public File saveToSDCard(byte[] data) throws IOException {
		Date date = new Date();
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss",Locale.CHINA); // 格式化时间
		String filename = format.format(date) + ".jpg";
		File fileFolder = new File(Environment.getExternalStorageDirectory()
				+ "/qsybtw/");
		if (!fileFolder.exists()) { // 如果目录不存在，则创建一个名为"finger"的目录
			fileFolder.mkdir();
		}
		imgfile = fileFolder + "/" + filename;
		
		File jpgFile = new File(fileFolder, filename);
		FileOutputStream outputStream = new FileOutputStream(jpgFile); // 文件输出流
		outputStream.write(data); // 写入sd卡中
		outputStream.close(); // 关闭输出流
		return jpgFile;
	}


	public void saveBitmap(Bitmap bm)
	{
		Date date = new Date();
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss",Locale.CHINA); // 格式化时间
		String filename = format.format(date) + ".png";
		File fileFolder = new File(Environment.getExternalStorageDirectory()
				+ "/qsybtw/");
		if (!fileFolder.exists()) { // 如果目录不存在，则创建一个名为"finger"的目录
			fileFolder.mkdir();
		}
		imgfile = fileFolder + "/" + filename;
		
		File jpgFile = new File(fileFolder, filename);
		try {
			FileOutputStream out = new FileOutputStream(jpgFile);
			//bm.compress(Bitmap.CompressFormat.PNG, 90, out);
			bm.compress(Bitmap.CompressFormat.PNG, 0, out);
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void showMessage(int msgType, String infomsg, boolean isSound) {
		try {
			if (isSound) {
				if (msgType == AppData.PROMPT_SUCCESS) {

				} else if (msgType == AppData.PROMPT_QUERY) {

				} else {

				}

			}
			Message msg = AppData.getInstance().getmHandler()
					.obtainMessage(AppData.MESSAGE_TOAST);
			Bundle bundle = new Bundle();
			bundle.putString(AppData.TOAST, infomsg);
			msg.setData(bundle);
			AppData.getInstance().getmHandler().sendMessage(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private OnItemSelectedListener mConsigneeClickListener = new OnItemSelectedListener() {

		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {
			try {
				if (parent.getCount() <= 0)
					return;

				isSetCsgn = true;

				customer_index = pos;

				if (csgnrslt != null) {
					JSONArray userarr = csgnrslt.getJSONArray("user");
					JSONObject jsonuser;

					jsonuser = userarr.getJSONObject(customer_index);
					customer_id = jsonuser.getString("customer_id");

					mHandler.obtainMessage(MESSAGE_UPDATE_WAYBILL_LISTVIEW,
							customer_index, -1, csgnrslt).sendToTarget();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void onNothingSelected(AdapterView<?> parent) {
			// Another interface callback
		}
	};

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
			case MESSAGE_UPDATE_CONSIGNEE_LISTVIEW: {
				try {
					JSONArray userarr = csgnrslt.getJSONArray("user");
					JSONObject jsonuser;
					int i;
				
					mCsgnArrayAdapter.clear();
					for (i = 0; i < userarr.length(); i++) {
						jsonuser = userarr.getJSONObject(i);
						String username = jsonuser.getString("username");
						mCsgnArrayAdapter.add(username);
					}			
					mWbillArrayAdapter.clear();

					atcsgn.setText(etcsgn.getText().toString());
					atcsgn.setDropDownHorizontalOffset(60);
					atcsgn.setDropDownVerticalOffset(49);
					atcsgn.showDropDown();	
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}
			case MESSAGE_UPDATE_WAYBILL_LISTVIEW: {
				try {
					JSONArray userarr = csgnrslt.getJSONArray("user");
					JSONObject jsonuser;
					int i;

					mWbillArrayAdapter.clear();
					jsonuser = userarr.getJSONObject(msg.arg1);
					JSONArray waybill_no_arr = jsonuser
							.getJSONArray("waybill_no");
					for (i = 0; i < waybill_no_arr.length(); i++) {
						String waybill_no = waybill_no_arr.getString(i);
						mWbillArrayAdapter.add(waybill_no);
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}
			case MESSAGE_UPDATE_EXPRSIGN_LISTVIEW: {
				try {
					waybill_no = spiwbill.getSelectedItem().toString();	
					String rtnstr;
					String status = submitrslt.getString("status");
					if (status.equals("ok")) {
						rtnstr = "拍照上传成功";
					} else{
						rtnstr = "拍照上传失败";
					}
					
					mExprsignArrayAdapter.insert(waybill_no + "   " + rtnstr,0);
					tvcptcnt.setText("快递签收(" + count + ")");
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}
			}
		}
	};

	public int PostConsigneeRequest() throws HttpException, IOException {
		HttpClient client = new HttpClient();
		StringBuilder sb = new StringBuilder();
		InputStream ins = null;
		int statusCode = HttpStatus.SC_METHOD_FAILURE;
		String result;

		PostMethod method = new PostMethod(
				"http://transport.360-express.com/index.php?route=api/customer/search");

		NameValuePair[] param = {
				new NameValuePair("consignee", customer_name),
				new NameValuePair("waybill_status", "transported"),
				new NameValuePair("token", token),
				new NameValuePair("code", MD5.GetMD5Code(customer_name + token)) };

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
					if (csgnrslt != null) {
						String status = csgnrslt.getString("status");
						if (status.equals("ok")) {
							// showMessage(AppData.PROMPT_QUERY, "收货人信息提交成功。");
						} else {
							statusCode = HttpStatus.SC_METHOD_FAILURE;
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
			
			customer_name = atcsgn.getText().toString();
			if(spiwbill.getCount()>0){
				waybill_no = spiwbill.getSelectedItem().toString();				
			}else{
				waybill_no = "";
				waybill_id = "";		

				statusCode = HttpStatus.SC_METHOD_FAILURE;
				showMessage(AppData.PROMPT_WARN, "该收货人没有运单！", false);
				return statusCode;	
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		int i, j;
		try {
			if (csgnrslt != null) {
				JSONArray userarr = csgnrslt.getJSONArray("user");
				JSONObject jsonuser;
				for (i = 0; i < userarr.length(); i++) {
					jsonuser = userarr.getJSONObject(i);
					String username = jsonuser.getString("username");
					if (username.equals(customer_name)) {
						customer_id = jsonuser.getString("customer_id");

						JSONArray waybill_id_arr = jsonuser.getJSONArray("waybill_id");
						JSONArray waybill_no_arr = jsonuser.getJSONArray("waybill_no");					
						for (j = 0; j < waybill_no_arr.length(); j++) {
							if(waybill_no.equals(waybill_no_arr.getString(j))){
								waybill_id = waybill_id_arr.getString(j);
								break;
							}
						}
						if(j>=waybill_no_arr.length()){
							waybill_no = "";
							waybill_id = "";
							
							statusCode = HttpStatus.SC_METHOD_FAILURE;
							showMessage(AppData.PROMPT_WARN, "该运单没有对应的id！", false);
							return statusCode;								
						}
						break;
					}
				}
				if(i>=userarr.length()){
					statusCode = HttpStatus.SC_METHOD_FAILURE;
					showMessage(AppData.PROMPT_WARN, "请从收货人列表中选择合适的收货人", false);
					return statusCode;
				}				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
				
		PostMethod method = new PostMethod(
				"http://transport.360-express.com/index.php?route=api/waybill/sign");

		File targetFile=new File(imgfile);
		Part[] parts = { new StringPart("consignee_id", customer_id), 
				new StringPart("waybill_id", waybill_id),
				new FilePart("file", targetFile),
				new StringPart("token", token),
				new StringPart("code", MD5.GetMD5Code(customer_id
						+ waybill_id + token)) };
		method.setRequestEntity(new MultipartRequestEntity(parts, method.getParams()));
		client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
		 
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
							//showMessage(AppData.PROMPT_SUCCESS,	msg,false);
							count ++;
						} else {
							statusCode = HttpStatus.SC_METHOD_FAILURE;
							showMessage(AppData.PROMPT_WARN, msg, false);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					statusCode = HttpStatus.SC_METHOD_FAILURE;
					showMessage(AppData.PROMPT_WARN, "快递签收信息异常，请重新采集提交。", false);
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
