package com.skillsforge.accountfeeds.inputmodels;

import com.skillsforge.accountfeeds.config.ProgramState;
import com.skillsforge.accountfeeds.input.Indexes;
import com.skillsforge.accountfeeds.outputmodels.OutputGroupRole;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author aw1459
 * @date 27-May-2017
 */
public class InputGroupRole {
  // {"GroupAlias", "RoleAlias"};
  @SuppressWarnings("FieldNotUsedInToString")
  @Nonnull
  private final ProgramState state;
  @Nullable
  private final String groupAlias;
  @Nullable
  private final String roleAlias;

  @SuppressWarnings("TypeMayBeWeakened")
  public InputGroupRole(@Nonnull final ProgramState state, @Nonnull final List<String> line) {
    this.state = state;

    final int lineSize = line.size();

    if (lineSize < 2) {
      state.getOutputLogStream()
          .printf(
              "[ERROR] InputGroupRole is incomplete as CSV line (%s) does not contain enough "
              + "columns.\n",
              line.toString());
    }
    if (lineSize > 2) {
      state.getOutputLogStream()
          .printf("[ERROR] InputGroupRole CSV line (%s) contains too many columns.\n",
              line.toString());
    }

    groupAlias = (lineSize > 0) ? line.get(0) : null;
    roleAlias = (lineSize > 1) ? line.get(1) : null;
  }

  @Nullable
  public OutputGroupRole validateAllFields(@Nonnull final Indexes indexes) {
    final String oGroupAlias = CommonMethods.validateGroupAlias(groupAlias, indexes, state, this);
    final String oRoleAlias = validateGroupRole(roleAlias, indexes);

    if ((oGroupAlias == null) || (oRoleAlias == null)) {
      return null;
    }

    return new OutputGroupRole(oGroupAlias, oRoleAlias);
  }

  @Nullable
  private String validateGroupRole(@Nullable final String oRoleAlias,
      @Nonnull final Indexes indexes) {
    if (oRoleAlias == null) {
      state.getOutputLogStream()
          .printf(
              "[ERROR] The role alias column is blank in %s - this must be filled with a "
              + "valid group-role.\n", this.toString());
      return null;
    }
    if (!indexes.rolesForGroupsContainsIgnoreCase(oRoleAlias)) {
      state.getOutputLogStream()
          .printf(
              "[ERROR] The role alias column (%s) in %s is not a valid group-role.\n",
              oRoleAlias, this.toString());
      return null;
    }
    if (indexes.rolesForGroupsHasMismatchedCase(oRoleAlias)) {
      state.getOutputLogStream()
          .printf(
              "[ERROR] The role alias column (%s) in %s is different in case from the "
              + "defined group-role.  Will attempt to proceed with the defined role spelling.\n",
              oRoleAlias, this.toString());
      return indexes.correctGroupRoleCase(oRoleAlias);
    }
    return oRoleAlias;
  }

  @Override
  public String toString() {
    return String.format("GroupRole['%s','%s']", groupAlias, roleAlias);
  }
}
