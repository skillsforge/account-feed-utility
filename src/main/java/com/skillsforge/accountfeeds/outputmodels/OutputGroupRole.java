package com.skillsforge.accountfeeds.outputmodels;

import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.Contract;

import java.util.Comparator;

import javax.annotation.Nonnull;

/**
 * @author aw1459
 * @date 27-May-2017
 */
public class OutputGroupRole {
  @Nonnull
  public static final Comparator<? super OutputGroupRole> CSV_SORTER =
      (left, right) -> left.getSortString().compareToIgnoreCase(right.getSortString());

  @Nonnull
  private final String groupAlias;
  @Nonnull
  private final String roleName;

  public OutputGroupRole(
      @Nonnull final String groupAlias,
      @Nonnull final String roleName) {

    this.groupAlias = groupAlias;
    this.roleName = roleName;
  }

  @Nonnull
  @Contract(pure = true)
  public String getGroupAlias() {
    return groupAlias;
  }

  @Nonnull
  @Contract(pure = true)
  public String getRoleName() {
    return roleName;
  }

  @Override
  @Contract(pure = true)
  public String toString() {
    return String.format("Group->Role['%s'->'%s']", groupAlias, roleName);
  }

  @Nonnull
  @Contract(pure = true)
  private String getSortString() {
    return groupAlias + ',' + roleName;
  }

  @Nonnull
  @Contract(pure = true)
  public String getCsvRow() {
    return StringEscapeUtils.escapeCsv(groupAlias) + ',' +
           StringEscapeUtils.escapeCsv(roleName);
  }
}
