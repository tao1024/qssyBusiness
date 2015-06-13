package com.hzh.dsw;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

import net.sf.json.JSONObject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Message;

public class QSYDataManager {

	private DatabaseHelper database = null;
	private SQLiteDatabase db = null;

	public class DatabaseHelper extends SQLiteOpenHelper {

		private static final String DB_NAME = "qsydata.db"; // 数据库名称
		private static final int version = 1; // 数据库版本

		public DatabaseHelper(Context context) {
			super(context, DB_NAME, null, version);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			String sql = "create table customer(customer_id varchar(16) primary key, customer_name varchar(20) )";
			db.execSQL(sql);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
		}
	}

	public QSYDataManager(Context context) {
		database = new DatabaseHelper(context);
		db = database.getWritableDatabase();
	}

	public void insertCustomer(String customer_id, String customer_name) {
		ContentValues cv = new ContentValues();
		cv.put("customer_id", customer_id);
		cv.put("customer_name", customer_name);
		db.insert("customer", null, cv);
	}



	public void exitQSYDataManager()
	{
		db.close();
		database = null;
	}
	
	public void showMessage(int msgType, String infomsg, boolean isSound) {
		try {
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

	public String getcustomers(String tbn){
		String items="";		
		try {
			Cursor c = db.query(tbn,null,null,null,null,null,null);//查询并获得游标
			if(c.moveToFirst()){//判断游标是否为空
				items = "{'tablename':'"+tbn+"','records':[";
			    for(int i=0;i<c.getCount();i++){
			        //c.move(i);//移动到指定记录  不能获取最后一条记录
			        if(tbn.equals("customer")){
				        String customer_id = c.getString(c.getColumnIndex("customer_id"));
				        String customer_name = c.getString(c.getColumnIndex("customer_name"));
				        items += "{"+"'customer_id':'"+customer_id+"','customer_name':'"+customer_name+"'},";						        	
			        }
			    	c.moveToNext();
			    }
				items=items.substring(0, items.length()-1);
				items += "]}";
				items = items.replace('\'', '\"');
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return items;
	}
	
	public void submitdata(String tablename) {
		new Thread(tablename) {
			String tbn = this.getName();
			public void run() {
				try {
					HttpClient client = new HttpClient();
					StringBuilder sb = new StringBuilder();
					InputStream ins = null;
					int statusCode = HttpStatus.SC_METHOD_FAILURE;
					String items="", result;
					String token = AppData.getInstance().getToken();
					JSONObject submitrslt = null;

					Cursor c = db.query(tbn,null,null,null,null,null,null);//查询并获得游标
					if(c.moveToFirst()){//判断游标是否为空
						items = "{tablename:"+tbn+",records:[";
					    for(int i=0;i<c.getCount();i++){
					        c.move(i);//移动到指定记录
					        if(tbn.equals("customer")){
						        String customer_id = c.getString(c.getColumnIndex("customer_id"));
						        String customer_name = c.getString(c.getColumnIndex("customer_name"));
						        items += "{"+"customer_id:"+customer_id+",customer_name:"+customer_name+"},";						        	
					        }							        
					    }
						items=items.substring(0, items.length()-1);
						items += "]}";
					}
					if(items.isEmpty()){
						return ;
					}
					
					PostMethod method = new PostMethod(
							"http://transport.360-express.com/index.php?route=api/waybill/batch");

					NameValuePair[] param = {
							new NameValuePair("items", items),
							new NameValuePair("token", token),
							new NameValuePair("code", MD5.GetMD5Code(items
									+ token)) };

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
								String msg = submitrslt.optString("msg");
								if (submitrslt != null) {
									String status = submitrslt
											.getString("status");
									if (status.equals("ok")) {
										showMessage(AppData.PROMPT_SUCCESS,msg, false);	
										db.delete(tbn, null, null);										
									} else {
										statusCode = HttpStatus.SC_METHOD_FAILURE;
										showMessage(AppData.PROMPT_WARN,msg, false);
									}
								}
							} catch (Exception e) {
								statusCode = HttpStatus.SC_METHOD_FAILURE;
								showMessage(AppData.PROMPT_WARN, tbn+"信息批量提交异常。", false);
								e.printStackTrace();
							}
						} else {
							System.err.println("Response Code: " + statusCode);
						}

					} catch (HttpException e) {
						System.err.println("Fatal protocol violation: "
								+ e.getMessage());
					} catch (IOException e) {
						System.err.println("Fatal transport error: "
								+ e.getMessage());
					} finally {
						method.releaseConnection();
						if (ins != null) {
							ins.close();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
}
