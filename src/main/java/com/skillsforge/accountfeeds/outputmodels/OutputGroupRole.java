package com.skillsforge.accountfeeds.outputmodels;

import org.apache.commons.lang3.StringEscapeUtils;
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
  private final String roleAlias;

  public OutputGroupRole(@Nonnull final String groupAlias, @Nonnull final String roleAlias) {
    this.groupAlias = groupAlias;
    this.roleAlias = roleAlias;
  }

  @Nonnull
  @Contract(pure = true)
  public String getGroupAlias() {
    return groupAlias;
  }

  @Nonnull
  @Contract(pure = true)
  public String getRoleAlias() {
    return roleAlias;
  }

  @Override
  @Contract(pure = true)
  public String toString() {
    return String.format("Group->Role['%s'->'%s']", groupAlias, roleAlias);
  }

  @Nonnull
  @Contract(pure = true)
  private String getSortString() {
    return groupAlias + ',' + roleAlias;
  }

  @Nonnull
  @Contract(pure = true)
  public String getCsvRow() {
    return StringEscapeUtils.escapeCsv(groupAlias) + ',' +
           StringEscapeUtils.escapeCsv(roleAlias);
  }
}
