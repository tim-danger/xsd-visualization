package com.dangersoft.mapping;

public class LimitExceededException extends Exception {
	private static final long serialVersionUID = -1795971498332381300L;

	public LimitExceededException(String message) {
		super(message);
	}
}
