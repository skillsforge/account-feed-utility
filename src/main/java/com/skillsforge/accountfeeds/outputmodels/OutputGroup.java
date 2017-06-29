package com.skillsforge.accountfeeds.outputmodels;

import com.skillsforge.accountfeeds.config.ProgramState;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.Contract;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import static com.skillsforge.accountfeeds.config.LogLevel.WARN;

/**
 * @author aw1459
 * @date 27-May-2017
 */
@SuppressWarnings("FieldNotUsedInToString")
public class OutputGroup {

  @Nonnull
  public static final Comparator<? super OutputGroup> CSV_SORTER =
      (left, right) -> left.getSortString().compareToIgnoreCase(right.getSortString());

  @Nonnull
  private final String groupAlias;
  @Nonnull
  private final String groupName;
  @Nonnull
  private final String groupDescription;
  private final boolean delete;
  @Nonnull
  private final Collection<OutputGroupRole> groupRoles = new HashSet<>();
  @Nonnull
  private final Collection<String> roleNames = new HashSet<>();

  @SuppressWarnings("BooleanParameter")
  public OutputGroup(@Nonnull final String groupAlias, @Nonnull final String groupName,
      @Nonnull final String groupDescription, final boolean delete) {
    this.groupAlias = groupAlias;
    this.groupName = groupName;
    this.groupDescription = groupDescription;
    this.delete = delete;
  }

  @Nonnull
  public String getGroupAlias() {
    return groupAlias;
  }

  @Override
  public String toString() {
    return String.format("Group['%s','%s','%s','%s',roles=%s]", groupAlias, groupName,
        groupDescription, delete ? "true" : "false", groupRoles.toString());
  }

  public void addRole(@Nonnull final ProgramState state, @Nonnull final OutputGroupRole groupRole) {
    if (roleNames.contains(groupRole.getRoleAlias())) {
      state.log(WARN, "GroupRole mapping of '%s' -> '%s' is specified more than once.",
          groupAlias, groupRole.getRoleAlias());
      return;
    }
    roleNames.add(groupRole.getRoleAlias());
    groupRoles.add(groupRole);
  }

  @Contract(pure = true)
  @Nonnull
  private String getSortString() {
    return groupAlias + ',' + groupName;
  }

  @Nonnull
  public String getCsvRow() {
    return StringEscapeUtils.escapeCsv(groupAlias) + ',' +
           StringEscapeUtils.escapeCsv(groupName) + ',' +
           StringEscapeUtils.escapeCsv(groupDescription) + ',' +
           delete;
  }

  @Nonnull
  public Stream<OutputGroupRole> getRoles() {
    return groupRoles.stream();
  }
}
