package com.skillsforge.accountfeeds.exceptions;

/**
 * @author alexw
 * @date 22-Nov-2016
 */
@SuppressWarnings({"SerializableHasSerializationMethods", "DeserializableClassInSecureContext"})
public class ParamException extends Exception {

  private static final long serialVersionUID = -6795564220126911455L;

  public ParamException() {
  }

  public ParamException(final String s) {
    super(s);
  }

  public ParamException(final String s, final Throwable throwable) {
    super(s, throwable);
  }

  public ParamException(final Throwable throwable) {
    super(throwable);
  }
}
