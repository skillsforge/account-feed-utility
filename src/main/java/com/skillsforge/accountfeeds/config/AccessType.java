package com.skillsforge.accountfeeds.config;

import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;

/**
 * @author aw1459
 * @date 26-May-2017
 */
enum AccessType {
  READ_FILE("read the file"),
  WRITE_FILE("write to the file"),
  READ_DIR("access the directory"),
  WRITE_DIR("write within the directory");

  private final String displayString;

  AccessType(final String displayText) {
    this.displayString = displayText;
  }

  @Override
  @Nonnull
  @Contract(pure = true)
  public String toString() {
    return this.displayString;
  }
}
