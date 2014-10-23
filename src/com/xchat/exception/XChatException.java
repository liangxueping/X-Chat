package com.xchat.exception;

public class XChatException extends Exception {
	private static final long serialVersionUID = 1L;

	public XChatException(String message) {
		super(message);
	}

	public XChatException(String message, Throwable cause) {
		super(message, cause);
	}
}
