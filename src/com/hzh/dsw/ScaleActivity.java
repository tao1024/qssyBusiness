/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hzh.dsw;

import java.util.Timer;
import java.util.TimerTask;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ScaleActivity extends Activity {
	// Debugging
	private static final String TAG = "ScaleActivity";

	// Layout Views
	private TextView mTitle;
	private ListView mConversationView;
	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Array adapter for the conversation thread
	private ArrayAdapter<String> mConversationArrayAdapter;
	// String buffer for outgoing messages
	// private StringBuffer mOutStringBuffer;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private BluetoothService bluetoothService = null;

	private String numcode = "";
	private Object lock = new Object();
	private Timer mWriteBytesTimer=null;

	//用户名文本编辑框   
	private EditText username;   
	//密码文本编辑框   
	private EditText password;   
	//登录按钮   
	private Button login;   
	//定义Intent对象,用来连接两个Activity   
	private Intent intent;   
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (AppData.D)Log.e(this.getClass().getCanonicalName(), "+++ ON CREATE +++");

		// Set up the window layout
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		
		setContentView(R.layout.other);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.custom_title);

		// Set up the custom title
		mTitle = (TextView) findViewById(R.id.title_left_text);
		mTitle.setText(R.string.app_name);
		mTitle = (TextView) findViewById(R.id.title_right_text);

		try{
			// Get local Bluetooth adapter
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		}catch (Exception e) {
			e.printStackTrace();
		}	
		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		//startActivity(new Intent(ScaleActivity.this,AndroidLoginActivity.class));  
	}
	
	@Override
	public void onStart() {
		super.onStart();
		if (AppData.D)Log.e(TAG, "++ ON START ++");

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, AppData.REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		} else {
			if (bluetoothService == null)
				setupConnect();
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (AppData.D)Log.e(TAG, "+ ON RESUME +");

		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
		if (bluetoothService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (bluetoothService.getState() == AppData.STATE_NONE) {
				// Start the Bluetooth chat services
				bluetoothService.start();
			}
		}
	}

	private synchronized void setupConnect() {
		if (AppData.D)Log.d(TAG, "setupConnect()");

		// Initialize the array adapter for the conversation thread
		mConversationArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.message);

		mConversationView = (ListView) findViewById(R.id.in);
		mConversationView.setAdapter(mConversationArrayAdapter);
		// if(!mConversationArrayAdapter.isEmpty())
		// Log.i(TAG1, mConversationArrayAdapter.getItem(0) + "hello");

		// Initialize the BluetoothChatService to perform bluetooth connections
		bluetoothService = new BluetoothService(this, mHandler);

		// Initialize the buffer for outgoing messages
		// mOutStringBuffer = new StringBuffer("");
	}

	@Override
	public synchronized void onPause() {
		super.onPause();
		if (AppData.D)Log.e(TAG, "- ON PAUSE -");
	}

	@Override
	public void onStop() {
		super.onStop();
		if (AppData.D)Log.e(TAG, "-- ON STOP --");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Stop the Bluetooth chat services
		if (bluetoothService != null)
			bluetoothService.stop();
		if(mWriteBytesTimer!=null){
			mWriteBytesTimer.cancel();
		}
		if (AppData.D)Log.e(TAG, "--- ON DESTROY ---");
	}

	private void ensureDiscoverable() {
		if (AppData.D)Log.d(TAG, "ensure discoverable");
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	// The Handler that gets information back from the BluetoothService
	@SuppressLint("HandlerLeak") private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case AppData.MESSAGE_STATE_CHANGE:
				if (AppData.D)Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case AppData.STATE_CONNECTED:
					mTitle.setText(R.string.title_connected_to);
					mTitle.append(mConnectedDeviceName);
					mConversationArrayAdapter.clear();
					break;
				case AppData.STATE_CONNECTING:
					mTitle.setText(R.string.title_connecting);
					break;
				case AppData.STATE_LISTEN:
				case AppData.STATE_NONE:
					mTitle.setText(R.string.title_not_connected);
					break;
				}
				break;
			case AppData.MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer
				String writeMessage = new String(writeBuf);
				/*测试时打开，正式产品中关闭
				mConversationArrayAdapter.add("Sended:  " + writeMessage);
				*/
				break;
			case AppData.MESSAGE_READ:
				// MessageRead messageRead = new
				// MessageRead(mConversationArrayAdapter,msg);
				// messageRead.start();
				synchronized (lock) {
					byte[] readBuf = (byte[]) msg.obj;
					// construct a string from the valid bytes in the buffer
					// String message = new String(readBuf);
					// Toast.makeText(OtherActivity.this, "message="+message,
					// 1).show();
					if (AppData.D){
	//					String readMessage = new String(readBuf, 0, msg.arg1);
	//					Log.i("bt", "read message:"+readMessage);
						String stringHex =StringHexUtils.Bytes2HexString(readBuf);
						Log.i("bt", "string hex:"+stringHex);
						String result =StringHexUtils.hexStr2Str(stringHex);
						Log.i("bt", "string result:"+result);
						/*测试时打开，正式产品中关闭
						Toast.makeText(	ScaleActivity.this,
								"readMessage=" + StringHexUtils.decode(StringHexUtils.Bytes2HexString(readBuf)), Toast.LENGTH_SHORT).show();
						*/
					}
					//正式产品剑结果输出到TextView
					String rdstr=StringHexUtils.decode(StringHexUtils.Bytes2HexString(readBuf));
					String lastW,validW,befPoint,aftPoint,resultW;
					int pos1,pos2;
					
					try{
						/*
						String[] Weights=rdstr.split("\\+");
						lastW=Weights[Weights.length-1];
						pos1=0;
						while(lastW.charAt(pos1)=='0')pos1++;
						pos2=pos1;
						while(lastW.charAt(pos2)>='0' && lastW.charAt(pos2)<='9')pos2++;
						validW=lastW.substring(pos1, pos2);
						
						if(validW.length()>0)
						{
							if(validW.length()<=5)
							{
								befPoint="0";
								aftPoint=validW;
							}
							else
							{
								befPoint=validW.substring(0, validW.length()-5);
								aftPoint=validW.substring(validW.length()-5);
							}
							resultW=befPoint+"."+aftPoint;
							
							//Toast.makeText(getApplicationContext(),(CharSequence)resultW, Toast.LENGTH_LONG).show();
							TextView tv=(TextView)findViewById(R.id.weight);
							tv.setText((CharSequence)resultW);
							
						}
						*/
						TextView tv=(TextView)findViewById(R.id.weight);
						tv.setText((CharSequence)rdstr);
					}catch (Exception e) {
						e.printStackTrace();
					}				
				}

				break;
			case AppData.MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(AppData.DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				// 閾炬帴鎴愬姛鍚庡紑濮嬪彂閫佽幏鍙栫О閲嶄俊鍙�
/*
				if (null == mWriteBytesTimer) {
					mWriteBytesTimer = new Timer();
				}
				mWriteBytesTimer.schedule(new TimerTask() {

					@Override
					public void run() {
						if (null != bluetoothService) {
							bluetoothService.write(StringHexUtils.hexStr2Bytes(StringHexUtils.encode("g")));
						}
					}
				}, 10, 300);//测试500ms太慢，正式产品改为300ms
*/
				break;
			case AppData.MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(AppData.TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (AppData.D)Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case AppData.REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Get the BLuetoothDevice object
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(address);
				// Attempt to connect to the device
				bluetoothService.connect(device);
			}
			break;
		case AppData.REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
				setupConnect();
			} else {
				// User did not enable Bluetooth or an error occured
				if (AppData.D)Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.scan:
			// Launch the DeviceListActivity to see devices and do scan
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, AppData.REQUEST_CONNECT_DEVICE);
			return true;
		case R.id.discoverable:
			// Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		case R.id.getscale:
			String sendCommand = StringHexUtils.toHexString("g");
			// sendCommand+="0x01";
			// sendCommand+="0x18";
			// sendCommand+=StringHexUtils.encode("l72");
			bluetoothService.write(sendCommand.getBytes());
			return true;
		}
		return false;
	}

}