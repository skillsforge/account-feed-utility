package com.skillsforge.accountfeeds;

/**
 * @author aw1459
 * @date 28-Jun-2017
 */
public final class AccountFeedUtility {
  private AccountFeedUtility() {
  }

  public static void main(final String[] args) {

    final MainProgram result = new MainProgram(args);
    System.exit(result.getExitCode());
  }
}
