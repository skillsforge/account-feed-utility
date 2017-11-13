package com.skillsforge.accountfeeds.outputmodels;

import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.Contract;

import java.util.Comparator;

import javax.annotation.Nonnull;

/**
 * @author aw1459
 * @date 27-May-2017
 */
@SuppressWarnings("BooleanParameter")
public class OutputUserRelationship {

  @Nonnull
  public static final Comparator<? super OutputUserRelationship> CSV_SORTER =
      (left, right) -> left.getSortString().compareToIgnoreCase(right.getSortString());

  @Nonnull
  private final String userIdLeft;
  @Nonnull
  private final String userIdRight;
  @Nonnull
  private final String roleAliasLeft;
  @Nonnull
  private final String roleAliasRight;
  private final boolean delete;

  public OutputUserRelationship(
      @Nonnull final String userIdLeft,
      @Nonnull final String userIdRight,
      @Nonnull final String roleAliasLeft,
      @Nonnull final String roleAliasRight,
      final boolean delete) {

    this.userIdLeft = userIdLeft;
    this.userIdRight = userIdRight;
    this.roleAliasLeft = roleAliasLeft;
    this.roleAliasRight = roleAliasRight;
    this.delete = delete;
  }

  @Nonnull
  @Contract(pure = true)
  public String getUserIdLeft() {
    return userIdLeft;
  }

  @Nonnull
  @Contract(pure = true)
  public String getUserIdRight() {
    return userIdRight;
  }

  @Nonnull
  @Contract(pure = true)
  public String getRoleAliasLeft() {
    return roleAliasLeft;
  }

  @Override
  @Contract(pure = true)
  public String toString() {
    return String.format("User->Relationship['%s'-[%s]->'%s','%s','%s']", userIdLeft, roleAliasLeft,
        userIdRight, delete ? "delete" : "active", roleAliasRight);
  }

  @Nonnull
  @Contract(pure = true)
  private String getSortString() {
    return userIdLeft + roleAliasLeft + userIdRight;
  }

  @Nonnull
  @Contract(pure = true)
  public String getCsvRow() {
    return StringEscapeUtils.escapeCsv(userIdLeft) + ',' +
           StringEscapeUtils.escapeCsv(userIdRight) + ',' +
           StringEscapeUtils.escapeCsv(roleAliasLeft) + ',' +
           StringEscapeUtils.escapeCsv(roleAliasRight) + ',' +
           "false";
  }
}
