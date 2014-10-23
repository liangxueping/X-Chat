package com.xchat.smack;

import com.xchat.exception.XChatException;

public interface Smack {
	public boolean login(String account, String password) throws XChatException;

	public boolean logout();

	public boolean isAuthenticated();

	public void addRosterItem(String user, String alias, String group)
			throws XChatException;

	public void removeRosterItem(String user) throws XChatException;

	public void renameRosterItem(String user, String newName)
			throws XChatException;

	public void moveRosterItemToGroup(String user, String group)
			throws XChatException;

	public void renameRosterGroup(String group, String newGroup);

	public void requestAuthorizationForRosterItem(String user);

	public void addRosterGroup(String group);

	public void setStatusFromConfig();

	public void sendMessage(String user, String message);

	public void sendServerPing();

	public String getNameForJID(String jid);
}
