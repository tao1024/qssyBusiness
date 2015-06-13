package com.hzh.dsw;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
/**
 * 程序启动时显示的第一个Activity
 */
public class AndroidLoginActivity extends Activity {
	//用户名文本编辑框
	private EditText username;
	//密码文本编辑框
	private EditText password;
	//登录按钮
	private Button login;
	//定义Intent对象,用来连接两个Activity
	private Intent intent;
	//重写的方法，启动一个Activity时，此方法会自动调用
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置布局
        setContentView(R.layout.login_dialog);
        //得到登录按钮对象
        login = (Button)findViewById(R.id.userlogin);
        //给登录按钮设置监听器
        login.setOnClickListener(ocl);
        login.setBackgroundColor(Color.MAGENTA);
    }
    //创建登录按钮监听器对象
    OnClickListener ocl = new OnClickListener(){
		public void onClick(View arg0) {
			//得到用户名和密码的编辑框
			username = (EditText)findViewById(R.id.username);
			password = (EditText)findViewById(R.id.password);
			//判断用户输入的用户名和密码是否与设置的值相同,必须要有toString()
			if("rhfbtw".equals(username.getText().toString())&& 
					"123456".equals(password.getText().toString())){
				//System.out.println("你点击了按钮");
				//创建Intent对象，传入源Activity和目的Activity的类对象
				intent = new Intent(AndroidLoginActivity.this, SecondActivity.class);
				//启动Activity
				startActivityForResult(intent,0);
				
			}else{
				//登录信息错误，通过Toast显示提示信息
				Toast.makeText(AndroidLoginActivity.this,"用户登录信息错误" , Toast.LENGTH_SHORT).show();
			}
		}
    };
    
    @Override 
    protected void onActivityResult(int requestCode, int resultCode, Intent data) { 
        // TODO Auto-generated method stub 
        super.onActivityResult(requestCode, resultCode, data); 
        if(resultCode == RESULT_OK){ 
        	finish();
        } 
    }   
}