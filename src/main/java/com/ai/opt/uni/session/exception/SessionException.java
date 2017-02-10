package com.ai.opt.uni.session.exception;

/**
 * 异常类封装
 * Date: 2017年2月9日 <br>
 * Copyright (c) 2017 asiainfo.com <br>
 * 
 * @author
 */
public class SessionException extends RuntimeException {
	
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
