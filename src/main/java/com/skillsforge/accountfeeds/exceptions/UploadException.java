package com.skillsforge.accountfeeds.exceptions;

/**
 * @author alexw
 * @date 22-Nov-2016
 */
@SuppressWarnings({"SerializableHasSerializationMethods", "DeserializableClassInSecureContext"})
public class UploadException extends Exception {

  private static final long serialVersionUID = -4894366644007042040L;

  public UploadException(final String s) {
    super(s);
  }
}
