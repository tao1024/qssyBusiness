package com.hzh.dsw;

import java.lang.reflect.Field;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.login_activity);

		showWaiterAuthorizationDialog();
	}

	// ��ʾ�Ի���
	public void showWaiterAuthorizationDialog() {

		// LayoutInflater��������layout�ļ����µ�xml�����ļ�������ʵ����
		LayoutInflater factory = LayoutInflater.from(LoginActivity.this);
		// ��activity_login�еĿؼ�������View��
		final View textEntryView = factory.inflate(R.layout.login_activity,
				null);

		// ��LoginActivity�еĿؼ���ʾ�ڶԻ�����
		new AlertDialog.Builder(LoginActivity.this)
		// �Ի���ı���
				.setTitle("��½")
				// �趨��ʾ��View
				.setView(textEntryView)
				// �Ի����еġ���½����ť�ĵ���¼�
				.setPositiveButton("��½", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

						// ��ȡ�û�����ġ��û������������롱
						// ע�⣺textEntryView.findViewById����Ҫ����Ϊ����factory.inflate(R.layout.activity_login,
						// null)��ҳ�沼�ָ�ֵ����textEntryView��
						final EditText etUserName = (EditText) textEntryView
								.findViewById(R.id.etuserName);
						final EditText etPassword = (EditText) textEntryView
								.findViewById(R.id.etPWD);

						// ��ҳ��������л�õġ��û������������롱תΪ�ַ���
						String userName = etUserName.getText().toString()
								.trim();
						String password = etPassword.getText().toString()
								.trim();

						// ����Ϊֹ�Ѿ�������ַ��͵��û����������ˣ����������Ǹ����Լ�����������д������
						// ������һ���򵥵Ĳ��ԣ��ٶ�������û��������붼��1�������������ҳ�棨OperationActivity��
						if (userName.equals("1") && password.equals("1")) {
							// ��ת��OperationActivity
							Intent intent = new Intent();
							intent.setClass(LoginActivity.this,	DialogActivity.class);
							startActivity(intent);
							// �رյ�ǰҳ��
							LoginActivity.this.finish();

						} else {
							Toast.makeText(LoginActivity.this, "������û�������",
									Toast.LENGTH_SHORT).show();

							try {
								// ע��˴���ͨ�����䣬�޸�Դ�������е��ֶ�mShowingΪtrue��ϵͳ����Ϊ�Ի����
								// �Ӷ�����dismiss()
								Field field = dialog.getClass().getSuperclass()
										.getDeclaredField("mShowing");
								field.setAccessible(true);
								field.set(dialog, false);
								dialog.dismiss();

							} catch (Exception e) {

							}
						}
					}
				})
				// �Ի���ġ��˳��������¼�
				.setNegativeButton("�˳�", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						LoginActivity.this.finish();
					}
				})
				// ����dialog�Ƿ�Ϊģ̬��false��ʾģ̬��true��ʾ��ģ̬
				.setCancelable(false)
				// �Ի���Ĵ�������ʾ
				.create().show();
	}
}
