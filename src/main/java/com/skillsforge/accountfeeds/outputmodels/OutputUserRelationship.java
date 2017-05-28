package com.skillsforge.accountfeeds.outputmodels;

import javax.annotation.Nonnull;

/**
 * @author aw1459
 * @date 27-May-2017
 */
public class OutputUserRelationship {

  @Nonnull
  private final String userIdLeft;
  @Nonnull
  private final String userIdRight;
  @Nonnull
  private final String roleAliasLeft;
  @Nonnull
  private final String roleAliasRight;
  private final boolean delete;

  @SuppressWarnings("BooleanParameter")
  public OutputUserRelationship(@Nonnull final String userIdLeft, @Nonnull final String userIdRight,
      @Nonnull final String roleAliasLeft, @Nonnull final String roleAliasRight,
      final boolean delete) {
    this.userIdLeft = userIdLeft;
    this.userIdRight = userIdRight;
    this.roleAliasLeft = roleAliasLeft;
    this.roleAliasRight = roleAliasRight;
    this.delete = delete;
  }

  @Nonnull
  public String getUserIdLeft() {
    return userIdLeft;
  }

  @Nonnull
  public String getUserIdRight() {
    return userIdRight;
  }

  @Nonnull
  public String getRoleAliasLeft() {
    return roleAliasLeft;
  }

  @Nonnull
  public String getRoleAliasRight() {
    return roleAliasRight;
  }

  public boolean isDelete() {
    return delete;
  }

  @Override
  public String toString() {
    return String.format("User->Relationship['%s'-[%s]->'%s','%s','%s']", userIdLeft, roleAliasLeft,
        userIdRight, delete ? "delete" : "active", roleAliasRight);
  }
}
