package com.hzh.dsw;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

public class InputDialog extends Dialog {
	private Context context;
	private EditText editText;
	private Button button_sure;
	private Button button_cancel;
	public OnSureClickListener mListener;

	public InputDialog(Context context) {
		super(context);
		this.context = context;
	}

	public InputDialog(Context context, OnSureClickListener listener)
	{
		super(context);
		mListener = listener;
	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.project_edit_dialog);
		setView();
	}

	private void setView() {
		button_sure = (Button) findViewById(R.id.button_project_dialog_sure);
		button_cancel = (Button) findViewById(R.id.button_project_dialog_cancel);
		editText = (EditText) findViewById(R.id.edit_project_new_name);

		button_sure.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mListener.getText(editText.getText().toString());
				dismiss();
			}
		});
		button_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//mListener.getText(editText.getText().toString());// 在Button监听事件中实现这一方法
				dismiss();
			}
		});
	}

	public interface OnSureClickListener {
		void getText(String string); // 声明获取EditText中数据的接口
	}
}
