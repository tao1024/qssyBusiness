package com.hzh.dsw;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class DialogActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_activity);

		Button dialog3 = (Button) findViewById(R.id.crtdlgbtn);
		dialog3.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				AlertDialog.Builder builder = new Builder(DialogActivity.this);
				builder.setTitle("����");
				builder.setView(new EditText(DialogActivity.this));
				builder.setPositiveButton("ȷ��", null);
				builder.setNegativeButton("ȡ��", null);
				builder.setIcon(android.R.drawable.ic_dialog_info);
				builder.setMessage("����Ϣ��");
				builder.show();
			}
		});
	}
}
