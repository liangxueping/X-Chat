package com.xchat.activity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.way.exception.XXAdressMalformedException;
import com.xchat.service.IConnectionStatusCallback;
import com.xchat.service.XXService;
import com.xchat.utils.DialogUtil;
import com.xchat.utils.L;
import com.xchat.utils.PreferenceConstants;
import com.xchat.utils.PreferenceUtils;
import com.xchat.utils.T;
import com.xchat.utils.XMPPHelper;

@SuppressLint("HandlerLeak")
public class LoginActivity extends FragmentActivity implements IConnectionStatusCallback, TextWatcher {
	public static final String LOGIN_ACTION = "com.way.action.LOGIN";
	private Button mLoginBtn;
	private EditText mAccountEt;
	private EditText mPasswordEt;
	private Dialog mLoginDialog;
	private String mAccount;
	private String mPassword;
	private View mTipsViewRoot;
	private TextView mTipsTextView;
	private Animation mTipsAnimation;
	
	private XXService mXxService;

	ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mXxService = ((XXService.XXBinder) service).getService();
//			mXxService.registerConnectionStatusCallback(LoginActivity.this);
			// 开始连接xmpp服务器
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
//			mXxService.unRegisterConnectionStatusCallback();
			mXxService = null;
		}

	};

	private void unbindXMPPService() {
		try {
			unbindService(mServiceConnection);
			L.i(LoginActivity.class, "[SERVICE] Unbind");
		} catch (IllegalArgumentException e) {
			L.e(LoginActivity.class, "Service wasn't bound!");
		}
	}

	private void bindXMPPService() {
		L.i(LoginActivity.class, "[SERVICE] Unbind");
		Intent intent = new Intent(this, XXService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		bindXMPPService();
		
		setContentView(R.layout.loginpage);
		initView();
	}
	private void initView() {
		mTipsAnimation = AnimationUtils.loadAnimation(this, R.anim.connection);
		mTipsViewRoot = findViewById(R.id.login_help_view);
		mTipsTextView = (TextView) findViewById(R.id.pulldoor_close_tips);
		mAccountEt = (EditText) findViewById(R.id.account_input);
		mPasswordEt = (EditText) findViewById(R.id.password);
		mLoginBtn = (Button) findViewById(R.id.login);
		String account = PreferenceUtils.getPrefString(this, PreferenceConstants.ACCOUNT, "");
		String password = PreferenceUtils.getPrefString(this, PreferenceConstants.PASSWORD, "");
		if (!TextUtils.isEmpty(account)){
			mAccountEt.setText(account);
		}
		if (!TextUtils.isEmpty(password)){
			mPasswordEt.setText(password);
		}
		mAccountEt.addTextChangedListener(this);
		mLoginDialog = DialogUtil.getLoginDialog(this);
	}
	@Override
	protected void onResume() {
		super.onResume();
		if (TextUtils.equals(PreferenceUtils.getPrefString(this, PreferenceConstants.APP_VERSION, ""), getString(R.string.app_version)) 
				&& !TextUtils.isEmpty(PreferenceUtils.getPrefString(this, PreferenceConstants.ACCOUNT, ""))) {
			mTipsViewRoot.setVisibility(View.GONE);
		} else {
			mTipsViewRoot.setVisibility(View.VISIBLE);
			PreferenceUtils.setPrefString(this, PreferenceConstants.APP_VERSION, getString(R.string.app_version));
		}
		mTipsViewRoot.setVisibility(View.GONE);
		if (mTipsTextView != null && mTipsAnimation != null){
			mTipsTextView.startAnimation(mTipsAnimation);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mTipsTextView != null && mTipsAnimation != null){
			mTipsTextView.clearAnimation();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindXMPPService();
//		if (mLoginOutTimeProcess != null) {
//			mLoginOutTimeProcess.stop();
//			mLoginOutTimeProcess = null;
//		}
	}

	public void onLoginClick(View v) {
		mAccount = mAccountEt.getText().toString();
		mAccount = splitAndSaveServer(mAccount);
		mPassword = mPasswordEt.getText().toString();
		if (TextUtils.isEmpty(mAccount)) {
			T.showShort(this, R.string.null_account_prompt);
			return;
		}
		if (TextUtils.isEmpty(mPassword)) {
			T.showShort(this, R.string.password_input_prompt);
			return;
		}
		if (mLoginDialog != null && !mLoginDialog.isShowing()){
			mLoginDialog.show();
		}
		if (mXxService != null) {
			mXxService.Login(mAccount, mPassword);
		}
	}

	private String splitAndSaveServer(String account) {
		if (!account.contains("@")){
			return account;
		}
		String customServer = PreferenceUtils.getPrefString(this, PreferenceConstants.CUSTOM_SERVER, "");
		String[] res = account.split("@");
		String userName = res[0];
		String server = res[1];
		// check for gmail.com and other google hosted jabber accounts
		if ("gmail.com".equals(server) || "googlemail.com".equals(server)
				|| PreferenceConstants.GMAIL_SERVER.equals(customServer)) {
			// work around for gmail's incompatible jabber implementation:
			// send the whole JID as the login, connect to talk.google.com
			userName = account;
		}
		PreferenceUtils.setPrefString(this, PreferenceConstants.Server, server);
		return userName;
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		
	}

	@Override
	public void afterTextChanged(Editable s) {
		try {
			XMPPHelper.verifyJabberID(s);
			mLoginBtn.setEnabled(true);
			mAccountEt.setTextColor(Color.parseColor("#ff333333"));
		} catch (XXAdressMalformedException e) {
			mLoginBtn.setEnabled(false);
			mAccountEt.setTextColor(Color.RED);
		}
	}

	@Override
	public void connectionStatusChanged(int connectedState, String reason) {
		if (mLoginDialog != null && mLoginDialog.isShowing()){
			mLoginDialog.dismiss();
		}
//		if (mLoginOutTimeProcess != null && mLoginOutTimeProcess.running) {
//			mLoginOutTimeProcess.stop();
//			mLoginOutTimeProcess = null;
//		}
//		if (connectedState == XXService.CONNECTED) {
//			save2Preferences();
//			startActivity(new Intent(this, MainActivity.class));
//			finish();
//		} else if (connectedState == XXService.DISCONNECTED){
//			T.showLong(LoginActivity.this, getString(R.string.request_failed) + reason);
//		}
	}
}
