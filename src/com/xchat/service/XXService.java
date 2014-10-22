package com.xchat.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class XXService extends Service{

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
		System.out.println("account:" + account);
	}
}
