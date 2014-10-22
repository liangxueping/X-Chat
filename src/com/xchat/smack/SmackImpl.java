package com.xchat.smack;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;

import android.content.ContentResolver;

import com.way.exception.XXException;
import com.xchat.service.XXService;
import com.xchat.utils.PreferenceConstants;
import com.xchat.utils.PreferenceUtils;

public class SmackImpl implements Smack {

	private ConnectionConfiguration mXMPPConfig;
	private XMPPConnection mXMPPConnection;
	private XXService mService;
	private final ContentResolver mContentResolver;

	public SmackImpl(XXService service) {
		String customServer = PreferenceUtils.getPrefString(service,
				PreferenceConstants.CUSTOM_SERVER, "");
		int port = PreferenceUtils.getPrefInt(service,
				PreferenceConstants.PORT, PreferenceConstants.DEFAULT_PORT_INT);
		String server = PreferenceUtils.getPrefString(service,
				PreferenceConstants.Server, PreferenceConstants.GMAIL_SERVER);
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
		// TODO Auto-generated method stub
		return false;
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
