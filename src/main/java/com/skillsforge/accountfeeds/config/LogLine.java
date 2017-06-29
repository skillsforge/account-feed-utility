package com.skillsforge.accountfeeds.config;

import java.io.PrintStream;

import javax.annotation.Nonnull;

/**
 * @author aw1459
 * @date 28-Jun-2017
 */
public class LogLine {
  @Nonnull
  private final LogLevel level;

  @Nonnull
  private final String errorString;

  private final boolean lintable;

  public LogLine(@Nonnull final LogLevel lvl, @Nonnull final String str) {
    errorString = str;
    level = lvl;
    lintable = false;
  }

  public LogLine(@Nonnull final LogLevel lvl, final boolean lintable, @Nonnull final String str) {
    errorString = str;
    level = lvl;
    this.lintable = lintable;
  }

  public LogLine(@Nonnull final LogLevel lvl, @Nonnull final String fmt, final Object... args) {
    errorString = String.format(fmt, args);
    level = lvl;
    this.lintable = false;
  }

  public LogLine(@Nonnull final LogLevel lvl, final boolean lintable, @Nonnull final String fmt,
      final Object... args) {
    errorString = String.format(fmt, args);
    level = lvl;
    this.lintable = lintable;
  }

  public void outputLogLine(final PrintStream stream) {

    stream.printf("[%s%s] %s\n", level.name(), lintable ? "-LINTABLE" : "", errorString);
  }

  public boolean isError() {
    return level == LogLevel.ERROR;
  }

  public boolean isWarning() {
    return level == LogLevel.WARN;
  }
}
