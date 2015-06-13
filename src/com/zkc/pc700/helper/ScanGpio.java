package com.zkc.pc700.helper;

import java.io.IOException;

import com.zkc.io.EmGpio;

public class ScanGpio {
	EmGpio	gpio = new EmGpio();
	// ���Ӵ���
	SerialPort serialPort = null;
	//�򿪵�Դ
	public void openPower(){
		try {
			if (true == gpio.gpioInit()) {
				//��Դ����
			gpio.setGpioOutput(111);
			gpio.setGpioDataLow(111);
			Thread.sleep(100);
			//��Դ����
			gpio.setGpioOutput(111);
			gpio.setGpioDataHigh(111);
			Thread.sleep(100);
			}
			gpio.gpioUnInit();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	public void closePower(){
		try {
			if (true == gpio.gpioInit()) {
				//��Դ����
			gpio.setGpioOutput(111);
			gpio.setGpioDataLow(111);
			Thread.sleep(100);
			gpio.setGpioInput(111);	 
			}
			gpio.gpioUnInit();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	 //��ɨ��
	public void openScan(){
		// ����ɨ��
		try {
			if (true == gpio.gpioInit()) {
				gpio.setGpioOutput(110);
				gpio.setGpioDataHigh(110);
				Thread.sleep(100);
				gpio.setGpioDataLow(110);
			}
			gpio.gpioUnInit();
		} catch (Exception e) {
			// TODO: handle exception
		}
		
	}
	 //�ر�ɨ��
	public void closeScan(){
		// ����ɨ��
		try {
			if (true == gpio.gpioInit()) {
				gpio.setGpioOutput(110);
				gpio.setGpioDataHigh(110);			 
			}
			gpio.gpioUnInit();
		} catch (Exception e) {
			// TODO: handle exception
		}
		
	}
}
