package com.skillsforge.accountfeeds.inputmodels;

import com.skillsforge.accountfeeds.config.ProgramState;
import com.skillsforge.accountfeeds.input.Indexes;
import com.skillsforge.accountfeeds.outputmodels.OutputGroupRole;

import org.jetbrains.annotations.Contract;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.skillsforge.accountfeeds.config.LogLevel.ERROR;

/**
 * @author aw1459
 * @date 27-May-2017
 */
public class InputGroupRole {
  @SuppressWarnings("FieldNotUsedInToString")
  @Nonnull
  private final ProgramState state;
  @Nullable
  private final String groupAlias;
  @Nullable
  private final String roleAlias;

  public InputGroupRole(
      @Nonnull final ProgramState state,
      @Nonnull final List<String> line) {

    this.state = state;

    if (line.size() < 2) {
      state.log("IGR.1", ERROR, "InputGroupRole is incomplete as CSV line does not contain enough "
                                + "columns: %s", line.toString());
    }
    if (line.size() > 2) {
      state.log("IGR.2", ERROR, "InputGroupRole CSV line contains too many columns: %s",
          line.toString());
    }

    groupAlias = CommonMethods.getFieldFromLine(line, 0);
    roleAlias = CommonMethods.getFieldFromLine(line, 1);

    if (CommonMethods.containsNewlineOrDoubleQuote(
        groupAlias, roleAlias)) {
      state.log("IGR.3", ERROR,
          "A field on this GroupRole CSV line contains either a double-quote or a "
          + "newline character - these are not supported by the target version: %s",
          line.toString());
    }
  }

  @Nullable
  @Contract(pure = true)
  public OutputGroupRole validateAllFields(
      @Nonnull final Indexes indexes) {

    final String oGroupAlias = CommonMethods.validateGroupAlias(groupAlias, indexes, state, this);
    final String oRoleAlias = validateGroupRole(roleAlias, indexes);

    if ((oGroupAlias == null) || (oRoleAlias == null)) {
      return null;
    }

    return new OutputGroupRole(oGroupAlias, oRoleAlias);
  }

  @Nullable
  @Contract(pure = true, value = "null,_ -> null")
  private String validateGroupRole(
      @Nullable final String oRoleAlias,
      @Nonnull final Indexes indexes) {

    if (oRoleAlias == null) {
      state.log("IGR.vgr.1", ERROR, "The role alias column is blank - this must be filled with a "
                                    + "valid group-role: %s", this.toString());
      return null;
    }
    if (!indexes.rolesForGroupsContainsIgnoreCase(oRoleAlias)) {
      state.log("IGR.vgr.2", ERROR, "The role alias column (%s) is not a valid group-role: %s",
          oRoleAlias, this.toString());
      return null;
    }
    if (indexes.rolesForGroupsHasMismatchedCase(oRoleAlias)) {
      state.log("IGR.vgr.3", ERROR, "The role alias column (%s) is different in case from the "
                                    + "defined group-role.  Will attempt to proceed with the "
                                    + "defined role spelling: %s",
          oRoleAlias, this.toString());
      return indexes.correctGroupRoleCase(oRoleAlias);
    }
    return oRoleAlias;
  }

  @Override
  @Nonnull
  @Contract(pure = true)
  public String toString() {
    return String.format("GroupRole['%s','%s']", groupAlias, roleAlias);
  }
}
