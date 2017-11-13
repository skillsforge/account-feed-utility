package com.skillsforge.accountfeeds.config;

import org.jetbrains.annotations.Contract;

import java.io.PrintStream;

import javax.annotation.Nonnull;

/**
 * @author aw1459
 * @date 28-Jun-2017
 */
@SuppressWarnings("BooleanParameter")
public class LogLine {
  @Nonnull
  private final LogLevel level;

  @Nonnull
  private final String errorString;

  private final boolean lintable;

  public LogLine(
      @Nonnull final LogLevel lvl,
      @Nonnull final String str) {

    errorString = str;
    level = lvl;
    lintable = false;
  }

  public LogLine(
      @Nonnull final LogLevel lvl,
      final boolean lintable,
      @Nonnull final String str) {

    errorString = str;
    level = lvl;
    this.lintable = lintable;
  }

  public LogLine(
      @Nonnull final LogLevel lvl,
      @Nonnull final String fmt,
      @Nonnull final Object... args) {

    errorString = String.format(fmt, args);
    level = lvl;
    this.lintable = false;
  }

  public LogLine(
      @Nonnull final LogLevel lvl,
      final boolean lintable,
      @Nonnull final String fmt,
      @Nonnull final Object... args) {

    errorString = String.format(fmt, args);
    level = lvl;
    this.lintable = lintable;
  }

  public void outputLogLine(@Nonnull final PrintStream stream) {
    //noinspection resource
    stream.printf("[%s%s] %s\n", level.name(), lintable ? "-LINTABLE" : "", errorString);
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
