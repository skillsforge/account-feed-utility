package com.skillsforge.accountfeeds.exceptions;

/**
 * @author alexw
 * @date 22-Nov-2016
 */
@SuppressWarnings({"SerializableHasSerializationMethods", "DeserializableClassInSecureContext"})
public class CsvCheckedException extends Exception {
  private static final long serialVersionUID = 3296754079400623101L;

  public CsvCheckedException() {
  }

  public CsvCheckedException(final String s) {
    super(s);
  }

  public CsvCheckedException(final String s, final Throwable throwable) {
    super(s, throwable);
  }

  public CsvCheckedException(final Throwable throwable) {
    super(throwable);
  }
}
