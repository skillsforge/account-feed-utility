package com.skillsforge.accountfeeds;

import javax.annotation.Nonnull;

/**
 * @author aw1459
 * @date 28-Jun-2017
 */
@SuppressWarnings({"UtilityClass", "CallToSystemExit"})
public final class AccountFeedUtility {
  private AccountFeedUtility() {
  }

  public static void main(@Nonnull final String[] args) {

    final MainProgram result = new MainProgram(args);
    System.exit(result.getExitCode());
  }
}
