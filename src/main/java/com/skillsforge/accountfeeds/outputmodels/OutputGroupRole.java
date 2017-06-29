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

  @Contract(pure = true)
  @Nonnull
  private String getSortString() {
    return groupAlias + ',' + roleAlias;
  }

  @Nonnull
  public String getCsvRow() {
    return StringEscapeUtils.escapeCsv(groupAlias) + ',' +
           StringEscapeUtils.escapeCsv(roleAlias);
  }
}
