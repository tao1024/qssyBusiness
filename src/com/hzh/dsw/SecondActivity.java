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
 * 登录成功后，显示的Activity  
 */  
public class SecondActivity extends Activity{   
    @Override  
    protected void onCreate(Bundle savedInstanceState) {   
        super.onCreate(savedInstanceState);   
        //设置布局   
        setContentView(R.layout.second);   
        
        Button btn_ok=(Button)findViewById(R.id.btn_ok);
        btn_ok.setBackgroundColor(Color.MAGENTA);
        btn_ok.setOnClickListener(new OnClickListener(){
    		public void onClick(View arg0) {
                //添加给第一个Activity的返回值，并设置resultCode 
                Intent intent = new Intent(); 
                setResult(RESULT_OK, intent); 
                finish(); 
    		}
        });
    }
}
