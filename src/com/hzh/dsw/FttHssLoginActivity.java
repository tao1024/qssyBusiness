package com.hzh.dsw;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import net.sf.json.JSONObject;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import com.hzh.dsw.InputDialog.OnSureClickListener;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

//全时运手持机系统登录Activity
public class FttHssLoginActivity extends Activity {
	// Debugging
	private static final String TAG = "FttHssLoginActivity";

	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothService bluetoothService = null;
	private BluetoothServerSocket mmServerSocket = null;
	private BluetoothSocket mmSocket = null;
	private BluetoothDevice mmDevice = null;
	BluetoothSocket socket = null;
	private InputStream mmInStream = null;
	private OutputStream mmOutStream = null;
	private int mState;

	private Object lock = new Object();

	private EditText username = null;
	private EditText password = null;
	private String user;
	private String psw;
	String result;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (AppData.D)
			Log.i(TAG, "+++ ON CREATE +++");
		// 设置布局
		setContentView(R.layout.activity_main);

		username = (EditText) findViewById(R.id.edtuser);
		password = (EditText) findViewById(R.id.edtpsw);
		Button btn_login = (Button) findViewById(R.id.login);		
		
		//界面初始化
		username.setText("laodai");
		password.setText("111111");

		if (!AppData.getInstance().getBtAdapter().isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivity(enableIntent);
		}

		username.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				// TODO Auto-generated method stub
				if (arg1.getAction() == MotionEvent.ACTION_DOWN) {
					user = "";
					username.setText(user);
				} else if (arg1.getAction() == MotionEvent.ACTION_MOVE) {

				} else if (arg1.getAction() == MotionEvent.ACTION_UP) {

				}
				return false;
			}
		});
		password.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				// TODO Auto-generated method stub
				if (arg1.getAction() == MotionEvent.ACTION_DOWN) {
					psw = "";
					password.setText(psw);
				} else if (arg1.getAction() == MotionEvent.ACTION_MOVE) {

				} else if (arg1.getAction() == MotionEvent.ACTION_UP) {

				}
				return false;
			}
		});

		username.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				// TODO Auto-generated method stub
				if (!hasFocus) {
					if (username.getText().toString().length() == 0) {
						showMessage(AppData.PROMPT_WARN, "请输入用户账号！", false);
					}
				} else {
					username.selectAll();
				}
			}

		});
		password.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				// TODO Auto-generated method stub
				if (!hasFocus) {
					if (password.getText().toString().length() == 0) {
						showMessage(AppData.PROMPT_WARN, "请输入密码！", false);
					}
				} else {
					password.selectAll();
				}
			}

		});

		btn_login.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {

				// 判断用户输入的用户名和密码是否与设置的值相同,必须要有toString()
				user = username.getText().toString();
				psw = password.getText().toString();
				// 账号 密码：admin rhf4fdev

				new Thread() {
					public void run() {
						try {
							if (PostRequest() == HttpStatus.SC_OK) {
								Intent intent = new Intent();
								intent.setClass(FttHssLoginActivity.this,
										WorkSpaceActivity.class);
								intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
								startActivity(intent);
							} else {

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
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == 4) {
			ExitBtwSystem();
		}
		return super.onKeyDown(keyCode, event);
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
		return super.onKeyUp(keyCode, event);
	}

	protected void onDestroy() {
		super.onDestroy();
	}

	protected void onResume() {
		super.onResume();
	}
	protected void onPause() {
		super.onPause();
	}
	protected void onStop() {
		super.onStop();
	}
	
	private void ExitBtwSystem()
	{
		OnSureClickListener listener1 = new OnSureClickListener() {
			public void getText(String exitcode) {
				try {
					if(exitcode.equals(AppData.getInstance().getExitcode()))
					{
						synchronized (AppData.getInstance().getLock()) {
							AppData.getInstance().stopBtService();
							AppData.getInstance().exitApp();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		InputDialog d1 = new InputDialog(
				FttHssLoginActivity.this, listener1);
		d1.show();		
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

	public int PostRequest() throws HttpException, IOException {
		HttpClient client = new HttpClient();
		StringBuilder sb = new StringBuilder();
		InputStream ins = null;
		int statusCode = HttpStatus.SC_METHOD_FAILURE;
		JSONObject jsonObj = null;

		PostMethod method = new PostMethod(
				"http://transport.360-express.com/index.php?route=api/user/login");

		NameValuePair[] param = {
				new NameValuePair("user", user),
				new NameValuePair("psw", MD5.GetMD5Code(psw)),
				new NameValuePair("code", MD5.GetMD5Code(user
						+ MD5.GetMD5Code(psw)
						+ AppData.getInstance().getCodeflag())) };

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
					jsonObj = JSONObject.fromString(result);
					if (jsonObj != null) {
						String status = jsonObj.getString("status");
						if (status.equals("ok")) {
							String token = jsonObj.optString("token");
							String exitcode = jsonObj.optString("exitcode");
							
							AppData.getInstance().setToken(token);
							if(!exitcode.isEmpty()){
								AppData.getInstance().setExitcode(exitcode);
							}
							AppData.getInstance().setPermissionarr(
									jsonObj.getJSONArray("permission"));

							// showMessage(AppData.PROMPT_QUERY, "登录成功。");
						} else {
							statusCode = HttpStatus.SC_METHOD_FAILURE;
							showMessage(AppData.PROMPT_WARN, "登录失败：账号或密码错误。",
									false);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					statusCode = HttpStatus.SC_METHOD_FAILURE;
					showMessage(AppData.PROMPT_WARN, "登录信息异常，请重新登录。", false);
				}

			} else {
				System.err.println("Response Code: " + statusCode);
			}
		} catch (HttpException e) {
			System.err.println("Fatal protocol violation: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("Fatal transport error: " + e.getMessage());
		} finally {
			if (jsonObj == null) {
				statusCode = HttpStatus.SC_METHOD_FAILURE;
			}
			method.releaseConnection();
			if (ins != null) {
				ins.close();
			}
		}

		return statusCode;
	}
}
