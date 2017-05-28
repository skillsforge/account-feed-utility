package com.skillsforge.accountfeeds.outputmodels;

import com.skillsforge.accountfeeds.config.ProgramState;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

/**
 * @author aw1459
 * @date 27-May-2017
 */
@SuppressWarnings("FieldNotUsedInToString")
public class OutputGroup {

  @Nonnull
  private final String groupAlias;
  @Nonnull
  private final String groupName;
  @Nonnull
  private final String groupDescription;
  private final boolean delete;
  @Nonnull
  private final Set<OutputGroupRole> groupRoles = new HashSet<>();
  @Nonnull
  private final Set<String> roleNames = new HashSet<>();

  @SuppressWarnings("BooleanParameter")
  public OutputGroup(@Nonnull final ProgramState state,
      @Nonnull final String groupAlias, @Nonnull final String groupName,
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
      state.getOutputLogStream()
          .printf("[WARNING] GroupRole mapping of '%s' -> '%s' is specified more than once.\n",
              groupAlias, groupRole.getRoleAlias());
      return;
    }
    roleNames.add(groupRole.getRoleAlias());
    groupRoles.add(groupRole);
  }
}
