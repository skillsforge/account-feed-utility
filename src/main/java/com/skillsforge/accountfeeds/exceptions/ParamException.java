package com.skillsforge.accountfeeds.exceptions;

/**
 * @author alexw
 * @date 22-Nov-2016
 */
@SuppressWarnings({"SerializableHasSerializationMethods", "DeserializableClassInSecureContext"})
public class ParamException extends Exception {

  private static final long serialVersionUID = -5771123188104328456L;

  public ParamException(final String s) {
    super(s);
  }
}
