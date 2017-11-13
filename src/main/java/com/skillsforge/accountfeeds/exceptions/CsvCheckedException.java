package com.skillsforge.accountfeeds.exceptions;

/**
 * @author alexw
 * @date 22-Nov-2016
 */
@SuppressWarnings({"SerializableHasSerializationMethods", "DeserializableClassInSecureContext"})
public class CsvCheckedException extends Exception {

  private static final long serialVersionUID = 4563570790316136077L;

  public CsvCheckedException(final String s) {
    super(s);
  }
}
