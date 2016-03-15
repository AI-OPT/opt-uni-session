package com.ai.opt.uni.session.exception;

public class SessionException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2773791782981818136L;

	public SessionException() {
	}

	public SessionException(String message) {
		super(message);
	}

	public SessionException(String message, Throwable cause) {
		super(message, cause);
	}

	public SessionException(Throwable cause) {
		super(cause);
	}
}
