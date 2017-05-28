package com.skillsforge.accountfeeds.outputmodels;

import javax.annotation.Nonnull;

/**
 * @author aw1459
 * @date 27-May-2017
 */
public class OutputGroupRole {

  @Nonnull
  private final String groupAlias;
  @Nonnull
  private final String roleAlias;

  public OutputGroupRole(@Nonnull final String groupAlias, @Nonnull final String roleAlias) {
    this.groupAlias = groupAlias;
    this.roleAlias = roleAlias;
  }

  @Nonnull
  public String getGroupAlias() {
    return groupAlias;
  }

  @Nonnull
  public String getRoleAlias() {
    return roleAlias;
  }

  @Override
  public String toString() {
    return String.format("Group->Role['%s'->'%s']", groupAlias, roleAlias);
  }
}
