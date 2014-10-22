package com.xchat.smack;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import android.content.ContentResolver;
import android.util.Log;

import com.way.exception.XXException;
import com.xchat.service.XXService;
import com.xchat.utils.L;
import com.xchat.utils.PreferenceConstants;
import com.xchat.utils.PreferenceUtils;

public class SmackImpl implements Smack {

	private static final int PACKET_TIMEOUT = 30000;
	private ConnectionConfiguration mXMPPConfig;
	private XMPPConnection mXMPPConnection;
	private XXService mService;
	private final ContentResolver mContentResolver;

	public SmackImpl(XXService service) {
		String customServer = PreferenceUtils.getPrefString(service,
				PreferenceConstants.CUSTOM_SERVER, "");
		int port = PreferenceUtils.getPrefInt(service,
				PreferenceConstants.PORT, PreferenceConstants.DEFAULT_PORT_INT);
		String server = PreferenceUtils.getPrefString(service, PreferenceConstants.Server, PreferenceConstants.GMAIL_SERVER);
		boolean smackdebug = PreferenceUtils.getPrefBoolean(service,
				PreferenceConstants.SMACKDEBUG, false);
		boolean requireSsl = PreferenceUtils.getPrefBoolean(service,
				PreferenceConstants.REQUIRE_TLS, false);
		if (customServer.length() > 0
				|| port != PreferenceConstants.DEFAULT_PORT_INT)
			this.mXMPPConfig = new ConnectionConfiguration(customServer, port,
					server);
		else
			this.mXMPPConfig = new ConnectionConfiguration(server); // use SRV

		this.mXMPPConfig.setReconnectionAllowed(false);
		this.mXMPPConfig.setSendPresence(false);
		this.mXMPPConfig.setCompressionEnabled(false); // disable for now
		this.mXMPPConfig.setDebuggerEnabled(smackdebug);
		if (requireSsl)
			this.mXMPPConfig.setSecurityMode(ConnectionConfiguration.SecurityMode.required);

		this.mXMPPConnection = new XMPPConnection(mXMPPConfig);
		this.mService = service;
		mContentResolver = service.getContentResolver();
	}
	
	@Override
	public boolean login(String account, String password) throws XXException {
		try {
			if (mXMPPConnection.isConnected()) {
				try {
					mXMPPConnection.disconnect();
				} catch (Exception e) {
					L.d("conn.disconnect() failed: " + e);
				}
			}
			SmackConfiguration.setPacketReplyTimeout(PACKET_TIMEOUT);
			SmackConfiguration.setKeepAliveInterval(-1);
			SmackConfiguration.setDefaultPingInterval(0);
			//registerRosterListener();// 监听联系人动态变化
			mXMPPConnection.connect();
			if (!mXMPPConnection.isConnected()) {
				throw new XXException("SMACK connect failed without exception!");
			}
			mXMPPConnection.addConnectionListener(new ConnectionListener() {
				public void connectionClosedOnError(Exception e) {
					//mService.postConnectionFailed(e.getMessage());
				}

				public void connectionClosed() {
				}

				public void reconnectingIn(int seconds) {
				}

				public void reconnectionFailed(Exception e) {
				}

				public void reconnectionSuccessful() {
				}
			});
			//initServiceDiscovery();// 与服务器交互消息监听,发送消息需要回执，判断是否发送成功
			// SMACK auto-logins if we were authenticated before
			if (!mXMPPConnection.isAuthenticated()) {
				String ressource = PreferenceUtils.getPrefString(mService, PreferenceConstants.RESSOURCE, "XX");
				mXMPPConnection.login(account, password, ressource);
			}
			setStatusFromConfig();// 更新在线状态

		} catch (XMPPException e) {
			throw new XXException(e.getLocalizedMessage(),
					e.getWrappedThrowable());
		} catch (Exception e) {
			// actually we just care for IllegalState or NullPointer or XMPPEx.
			L.e(SmackImpl.class, "login(): " + Log.getStackTraceString(e));
			throw new XXException(e.getLocalizedMessage(), e.getCause());
		}
		//registerAllListener();// 注册监听其他的事件，比如新消息
		return mXMPPConnection.isAuthenticated();
	}

	@Override
	public boolean logout() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAuthenticated() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void addRosterItem(String user, String alias, String group)
			throws XXException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeRosterItem(String user) throws XXException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void renameRosterItem(String user, String newName)
			throws XXException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void moveRosterItemToGroup(String user, String group)
			throws XXException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void renameRosterGroup(String group, String newGroup) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void requestAuthorizationForRosterItem(String user) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addRosterGroup(String group) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setStatusFromConfig() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendMessage(String user, String message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendServerPing() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getNameForJID(String jid) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
