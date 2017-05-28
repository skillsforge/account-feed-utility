package com.skillsforge.accountfeeds.inputmodels;

import com.skillsforge.accountfeeds.config.ProgramState;
import com.skillsforge.accountfeeds.input.Indexes;
import com.skillsforge.accountfeeds.outputmodels.OutputUserRelationship;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author aw1459
 * @date 27-May-2017
 */
public class InputUserRelationship {
  @SuppressWarnings("FieldNotUsedInToString")
  @Nonnull
  private final ProgramState state;
  @Nullable
  private final String userIdLeft;
  @Nullable
  private final String userIdRight;
  @Nullable
  private final String roleAliasLeft;
  @Nullable
  private final String roleAliasRight;
  @Nullable
  private final String delete;

  @SuppressWarnings("TypeMayBeWeakened")
  public InputUserRelationship(@Nonnull final ProgramState state,
      @Nonnull final List<String> line) {
    this.state = state;

    final int lineSize = line.size();

    if (lineSize < 5) {
      state.getOutputLogStream()
          .printf(
              "[ERROR] InputUserRelationship is incomplete as CSV line (%s) does not contain "
              + "enough "
              + "columns.\n",
              line.toString());
    }
    if (lineSize > 5) {
      state.getOutputLogStream()
          .printf("[ERROR] InputUserRelationship CSV line (%s) contains too many columns.\n",
              line.toString());
    }

    userIdLeft = (lineSize > 0) ? line.get(0) : null;
    userIdRight = (lineSize > 1) ? line.get(1) : null;
    roleAliasLeft = (lineSize > 2) ? line.get(2) : null;
    roleAliasRight = (lineSize > 3) ? line.get(3) : null;
    delete = (lineSize > 4) ? line.get(4) : null;
  }

  @Nullable
  public OutputUserRelationship validateAllFields(@Nonnull final Indexes indexes) {
    final String oDelete =
        CommonMethods.validateTrueFalse(delete, state, this,
            "Delete", "false", "false");
    final String oUserIdLeft =
        CommonMethods.validateUserId(userIdLeft, indexes, state, this, "holder/left ");
    final String oUserIdRight =
        CommonMethods.validateUserId(userIdRight, indexes, state, this, "subject/right ");
    final String oRoleAlias = validateHolderRole(roleAliasLeft, indexes);

    //noinspection OverlyComplexBooleanExpression
    if ((oUserIdLeft == null)
        || (oUserIdRight == null)
        || (oRoleAlias == null)) {
      return null;
    }

    if (oUserIdLeft.equalsIgnoreCase(oUserIdRight)) {
      state.getOutputLogStream()
          .printf(
              "[ERROR] The holder/left and subject/right UserID columns held the same value "
              + "in %s - you cannot hold a relationship over yourself.\n",
              this.toString());
      return null;
    }

    return new OutputUserRelationship(oUserIdLeft, oUserIdRight, oRoleAlias, "",
        "true".equals(oDelete));
  }

  @Nullable
  private String validateHolderRole(@Nullable final String oRoleAlias,
      @Nonnull final Indexes indexes) {
    if ((oRoleAlias == null) || oRoleAlias.trim().isEmpty()) {
      state.getOutputLogStream()
          .printf(
              "[ERROR] The (left) role alias column is blank in %s - this must be filled with a "
              + "valid relationship-role.\n", this.toString());
      return null;
    }
    if (!indexes.rolesForRelationshipsContainsIgnoreCase(oRoleAlias)) {
      state.getOutputLogStream()
          .printf(
              "[ERROR] The (left) role alias column (%s) in %s is not a valid relationship-role.\n",
              oRoleAlias, this.toString());
      return null;
    }
    if (indexes.rolesForRelationshipsHasMismatchedCase(oRoleAlias)) {
      state.getOutputLogStream()
          .printf(
              "[ERROR-LINTABLE] The left role alias column (%s) in %s is different in case from the"
              + " defined relationship-role.  Will attempt to proceed with the defined role "
              + "spelling.\n", oRoleAlias, this.toString());
      return indexes.correctRelationshipRoleCase(oRoleAlias);
    }
    return oRoleAlias;
  }

  @Override
  public String toString() {
    return String.format("UserRelationship['%s','%s','%s','%s','%s']", userIdLeft, userIdRight,
        roleAliasLeft, roleAliasRight, delete);
  }
}
