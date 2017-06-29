package com.skillsforge.accountfeeds.outputmodels;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.Contract;

import java.util.Comparator;

import javax.annotation.Nonnull;

/**
 * @author aw1459
 * @date 27-May-2017
 */
public class OutputUserGroup {

  @Nonnull
  public static final Comparator<? super OutputUserGroup> CSV_SORTER =
      (left, right) -> left.getSortString().compareToIgnoreCase(right.getSortString());

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

  @Nonnull
  @Contract(pure = true)
  private String getSortString() {
    return userId + ',' + groupAlias;
  }

  public String getCsvRow() {
    return StringEscapeUtils.escapeCsv(userId) + ',' + StringEscapeUtils.escapeCsv(groupAlias);
  }
}
