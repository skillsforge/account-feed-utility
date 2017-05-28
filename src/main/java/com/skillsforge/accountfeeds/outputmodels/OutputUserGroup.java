package com.skillsforge.accountfeeds.outputmodels;

import javax.annotation.Nonnull;

/**
 * @author aw1459
 * @date 27-May-2017
 */
public class OutputUserGroup {

  @Nonnull
  private final String userId;
  @Nonnull
  private final String groupAlias;

  public OutputUserGroup(@Nonnull final String userId, @Nonnull final String groupAlias) {
    this.userId = userId;
    this.groupAlias = groupAlias;
  }

  @Nonnull
  public String getUserId() {
    return userId;
  }

  @Nonnull
  public String getGroupAlias() {
    return groupAlias;
  }

  @Override
  public String toString() {
    return String.format("User->Group['%s'->'%s']", userId, groupAlias);
  }
}
