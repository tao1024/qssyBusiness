package com.hzh.dsw;

import net.sf.json.JSONArray;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

public class WorkSpaceActivity extends Activity {
	
	private Object lock = new Object();

	private boolean enWeightfee=false;
	private boolean enCollect=false;
	private	boolean enCheck=false;
	private	boolean enScatter=false;
	private boolean enExprsign=false;
	private boolean btPaired=false;
	
    @Override  
    protected void onCreate(Bundle savedInstanceState) {   
        super.onCreate(savedInstanceState);   
        //设置布局   
        setContentView(R.layout.activity_workspace);  

        btPaired = AppData.getInstance().isbPaired();
        ImageButton btn_weightfee=(ImageButton)findViewById(R.id.weightfee);        
        ImageButton btn_collect=(ImageButton)findViewById(R.id.collectgoods);
        ImageButton btn_checkorder=(ImageButton)findViewById(R.id.checkorder);
        ImageButton btn_scatter=(ImageButton)findViewById(R.id.scattergoods);
        ImageButton btn_exprsign=(ImageButton)findViewById(R.id.exprsign);
        
        JSONArray parr=AppData.getInstance().getPermissionarr();
        if(parr!=null)
        {
	        int i,count=parr.length();
	        for(i=0;i<count;i++)
	        {
	        	String pmstr=parr.getString(i);
	        	if(pmstr.equals("waybill_weight")){
	        		enWeightfee = true;
	        		continue;
	        	}
	        	if(pmstr.equals("waybill_collect")){
	        		enCollect = true;
	        		continue;
	        	}
	        	if(pmstr.equals("waybill_check")){
	        		enCheck = true;
	        		continue;
	        	}
	        	if(pmstr.equals("waybill_dispatch")){
	        		enScatter = true;
	        		continue;
	        	}
	        	if(pmstr.equals("waybill_sign")){
	        		enExprsign = true;
	        		continue;
	        	}          	
	        }
        }
        //入库称重
        btn_weightfee.setOnClickListener(new OnClickListener(){
    		public void onClick(View arg0) {

    			if(!checkPermission(enWeightfee))return; 			
				Intent intent = new Intent(WorkSpaceActivity.this, WeightFeeActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				//启动Activity
				startActivityForResult(intent,RESULT_OK);
    		}
        });
        
        //收货
        btn_collect.setOnClickListener(new OnClickListener(){
    		public void onClick(View arg0) {

    			if(!checkPermission(enCollect))return; 			
				Intent intent = new Intent(WorkSpaceActivity.this, CollectGoodsActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				//启动Activity
				startActivityForResult(intent,RESULT_OK);
    		}
        });
        
        //核单
        btn_checkorder.setOnClickListener(new OnClickListener(){
    		public void onClick(View arg0) {

    			if(!checkPermission(enCheck))return; 
				Intent intent = new Intent(WorkSpaceActivity.this, CheckOrderActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				//启动Activity
				startActivityForResult(intent,RESULT_OK);
    		}
        });
        
        //国内分发
        btn_scatter.setOnClickListener(new OnClickListener(){
    		public void onClick(View arg0) {
    			
    			if(!checkPermission(enScatter))return; 
				Intent intent = new Intent(WorkSpaceActivity.this, ScatterGoodsActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				//启动Activity
				startActivityForResult(intent,RESULT_OK);
    		}
        });
        //快递签收
        btn_exprsign.setOnClickListener(new OnClickListener(){
    		public void onClick(View arg0) {
    			
    			if(!checkPermission(enExprsign))return; 
				Intent intent = new Intent(WorkSpaceActivity.this, ExpressSignActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				//启动Activity
				startActivityForResult(intent,RESULT_OK);
    		}
        });
        
        //蓝牙配对
        ImageButton btn_btpair=(ImageButton)findViewById(R.id.btpair);
        btn_btpair.setOnClickListener(new OnClickListener(){
    		public void onClick(View arg0) {
    			
    			if (!AppData.getInstance().getBtAdapter().isEnabled()) {
    				Intent enableIntent = new Intent(
    						BluetoothAdapter.ACTION_REQUEST_ENABLE);
    				startActivityForResult(enableIntent, AppData.REQUEST_ENABLE_BT);
    			}	
    			else{
    				AppData.getInstance().newBtService();
    				new Thread() {
    					public void run() {
		    				while (!AppData.getInstance().isbAccepting()) {
		    					synchronized (this) {
		    						try {
		    							wait(1000);
		    						} catch (InterruptedException e) {
		    							System.err.println("WaitError: " + e.getMessage());
		    						}
		    					}
		    				}
		    				
		    				Intent serverIntent = new Intent(WorkSpaceActivity.this, DeviceListActivity.class);
		    				startActivityForResult(serverIntent, AppData.REQUEST_CONNECT_DEVICE);		    				
    					}
    				}.start();
    			}
    		}
        });
        
        //注销
        ImageButton btn_logout=(ImageButton)findViewById(R.id.logout);
        btn_logout.setOnClickListener(new OnClickListener(){
    		public void onClick(View arg0) {
    			AppData.getInstance().finishActivity(WorkSpaceActivity.this, "确认注销吗？", "确定", "取消");              
    		}
        });
        
		Toast.makeText(getApplicationContext(), "系统正在初始化，请稍侯...",
				Toast.LENGTH_LONG).show();
/*		
        try{
	        QSYDataManager sqlmgr = new QSYDataManager(getApplicationContext());
	        sqlmgr.insertCustomer("5393736983", "TANG");   
	        sqlmgr.insertCustomer("5393736995", "ZHEN");   
	        sqlmgr.insertCustomer("5387426947", "SHUN");
			sqlmgr.exitQSYDataManager();
	    }catch(Exception e){
	    	e.printStackTrace();
	    }
*/        
        btn_btpair.performClick();
    }
    
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		if (keyCode == 4) {
			AppData.getInstance().finishActivity(WorkSpaceActivity.this, "确认返回吗？", "确定", "取消");  			
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
    private boolean checkPermission(boolean bPerm){
    	
		if(!btPaired){
			Message msg = AppData.getInstance().getmHandler()
					.obtainMessage(AppData.MESSAGE_TOAST);
			Bundle bundle = new Bundle();
			bundle.putString(AppData.TOAST, "未与蓝牙秤蓝牙配对");
			msg.setData(bundle);
			AppData.getInstance().getmHandler().sendMessage(msg);  
			return false;
		}
		if(!bPerm){
			Message msg = AppData.getInstance().getmHandler()
					.obtainMessage(AppData.MESSAGE_TOAST);
			Bundle bundle = new Bundle();
			bundle.putString(AppData.TOAST, "无权限");
			msg.setData(bundle);
			AppData.getInstance().getmHandler().sendMessage(msg);
			return false;
		}
		return true;
    }
    
	private void ensureDiscoverable() {
		if (AppData.D)Log.d(this.getClass().getCanonicalName(), "ensure discoverable");

		if (AppData.getInstance().getBtAdapter().getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}    
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (AppData.D)Log.d(this.getClass().getCanonicalName(), "onActivityResult " + resultCode);
		switch (requestCode) {
		case AppData.REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Get the BLuetoothDevice object
				BluetoothDevice device = AppData.getInstance().getBtAdapter()
						.getRemoteDevice(address);
				// Attempt to connect to the device
				AppData.getInstance().getBtService().connect(device);
				
				btPaired = true;
				AppData.getInstance().setbPaired(btPaired);			
			}
			break;
		case AppData.REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
				AppData.getInstance().newBtService();
			} else {
				// User did not enable Bluetooth or an error occured
				if (AppData.D)Log.d(this.getClass().getCanonicalName(), "BT not enabled");
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
			/*
			String sendCommand = StringHexUtils.toHexString("g");
			// sendCommand+="0x01";
			// sendCommand+="0x18";
			// sendCommand+=StringHexUtils.encode("l72");
			bluetoothService.write(sendCommand.getBytes());
			*/
			return true;
		}
		return false;
	}    
}
