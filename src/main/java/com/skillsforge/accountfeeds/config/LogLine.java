package com.skillsforge.accountfeeds.config;

import org.jetbrains.annotations.Contract;

import java.io.PrintStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author aw1459
 * @date 28-Jun-2017
 */
@SuppressWarnings("BooleanParameter")
public class LogLine {
  @Nullable
  private final String errorCode;
  @Nonnull
  private final LogLevel level;
  @Nonnull
  private final String errorString;

  private final boolean lintable;

  public LogLine(
      @Nullable final String code,
      @Nonnull final LogLevel lvl,
      @Nonnull final String str) {

    errorCode = code;
    errorString = str;
    level = lvl;
    lintable = false;
  }

  public LogLine(
      @Nullable final String code,
      @Nonnull final LogLevel lvl,
      final boolean lintable,
      @Nonnull final String str) {

    errorCode = code;
    errorString = str;
    level = lvl;
    this.lintable = lintable;
  }

  public LogLine(
      @Nullable final String code,
      @Nonnull final LogLevel lvl,
      @Nonnull final String fmt,
      @Nonnull final Object... args) {

    errorCode = code;
    errorString = String.format(fmt, args);
    level = lvl;
    this.lintable = false;
  }

  public LogLine(
      @Nullable final String code,
      @Nonnull final LogLevel lvl,
      final boolean lintable,
      @Nonnull final String fmt,
      @Nonnull final Object... args) {

    errorCode = code;
    errorString = String.format(fmt, args);
    level = lvl;
    this.lintable = lintable;
  }

  public void outputLogLine(@Nonnull final PrintStream stream) {
    //noinspection resource
    stream.printf("[%s%s%s] %s\n",
        level.name(),
        lintable ? "-LINTABLE" : "",
        (errorCode == null) ? "" : ("::" + errorCode),
        errorString);
  }

  @Contract(pure = true)
  public boolean isError() {
    return level == LogLevel.ERROR;
  }

  @Contract(pure = true)
  public boolean isWarning() {
    return level == LogLevel.WARN;
  }
}
