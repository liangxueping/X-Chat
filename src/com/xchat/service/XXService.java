package com.xchat.service;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.way.exception.XXException;
import com.xchat.smack.SmackImpl;
import com.xchat.utils.L;
import com.xchat.utils.NetUtil;

public class XXService extends BaseService{

	private Thread mConnectingThread;
	private SmackImpl mSmackable;
	
	private IBinder mBinder = new XXBinder();
	 
	public class XXBinder extends Binder {
		public XXService getService() {
			return XXService.this;
		}
	}
	
	@Override
    public IBinder onBind(Intent intent) {
		
        return mBinder;
    }
	
	// 登录
	public void Login(final String account, final String password) {
		if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE) {
			//connectionFailed(NETWORK_ERROR);
			return;
		}
		if (mConnectingThread != null) {
			L.i("a connection is still goign on!");
			return;
		}
		mConnectingThread = new Thread() {
			@Override
			public void run() {
				try {
					//postConnecting();
					mSmackable = new SmackImpl(XXService.this);
					if (mSmackable.login(account, password)) {
						// 登陆成功
						//postConnectionScuessed();
					} else {
						// 登陆失败
						//postConnectionFailed(LOGIN_FAILED);
					}
				} catch (XXException e) {
					String message = e.getLocalizedMessage();
					// 登陆失败
					if (e.getCause() != null)
						message += "\n" + e.getCause().getLocalizedMessage();
					//postConnectionFailed(message);
					L.i(XXService.class, "YaximXMPPException in doConnect():" + message);
					e.printStackTrace();
				} finally {
					if (mConnectingThread != null)
						synchronized (mConnectingThread) {
							mConnectingThread = null;
						}
				}
			}

		};
		mConnectingThread.start();
	}
}
