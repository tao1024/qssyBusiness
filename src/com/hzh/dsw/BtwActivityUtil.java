package com.hzh.dsw;

import android.bluetooth.BluetoothAdapter;
import android.widget.Toast;

public class BtwActivityUtil {
	public static final int BT_NOT_AVAILABLE = 1;
	public static final int BT_READ_WEIGHT_FINISH = 2;
	
	private BluetoothAdapter mBluetoothAdapter = null;
	
	public int ConnectBTandReadWeight() {
		try{
			// Get local Bluetooth adapter
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		}catch (Exception e) {
			e.printStackTrace();
		}	
		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			return BT_NOT_AVAILABLE;
		}
		
		return BT_READ_WEIGHT_FINISH;
	}
}
