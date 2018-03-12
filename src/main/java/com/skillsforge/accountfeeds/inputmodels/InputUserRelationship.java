package com.skillsforge.accountfeeds.inputmodels;

import com.skillsforge.accountfeeds.config.ProgramState;
import com.skillsforge.accountfeeds.input.Indexes;
import com.skillsforge.accountfeeds.outputmodels.OutputUserRelationship;

import org.jetbrains.annotations.Contract;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.skillsforge.accountfeeds.config.LogLevel.ERROR;

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

  public InputUserRelationship(
      @Nonnull final ProgramState state,
      @Nonnull final List<String> line) {

    this.state = state;

    if (line.size() < 5) {
      state.log("IUR.1", ERROR, "InputUserRelationship is incomplete as CSV line does not contain "
                                + "enough columns: %s",
          line.toString());
    }
    if (line.size() > 5) {
      state.log("IUR.2", ERROR, "InputUserRelationship CSV line contains too many columns: %s",
          line.toString());
    }

    userIdLeft = CommonMethods.getFieldFromLine(line, 0);
    userIdRight = CommonMethods.getFieldFromLine(line, 1);
    roleAliasLeft = CommonMethods.getFieldFromLine(line, 2);
    roleAliasRight = CommonMethods.getFieldFromLine(line, 3);
    delete = CommonMethods.getFieldFromLine(line, 4);

    if (CommonMethods.containsNewlineOrDoubleQuote(
        userIdLeft, userIdRight, roleAliasLeft, roleAliasRight, delete)) {
      state.log("IUR.3", ERROR,
          "A field on this UserRelationship CSV line contains either a double-quote or a "
          + "newline character - these are not supported by the target version:  %s",
          line.toString());
    }
  }

  @Nullable
  @Contract(pure = true)
  public OutputUserRelationship validateAllFields(
      @Nonnull final Indexes indexes) {

    final String oDelete =
        CommonMethods.validateTrueFalse(delete, state, this, "Delete");
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
      state.log("IUR.vaf.1",
          ERROR, "The holder/left and subject/right UserID columns held the same value "
                 + "- you cannot hold a relationship over yourself: %s\n",
          this.toString());
      return null;
    }

    return new OutputUserRelationship(oUserIdLeft, oUserIdRight, oRoleAlias, "",
        "true".equals(oDelete));
  }

  @Nullable
  @Contract(pure = true, value = "null,_ -> null")
  private String validateHolderRole(
      @Nullable final String oRoleAlias,
      @Nonnull final Indexes indexes) {

    if ((oRoleAlias == null) || oRoleAlias.trim().isEmpty()) {
      state.log("IUR.vhr.1", ERROR,
          "The (left) role alias column is blank - this must be filled with a "
          + "valid relationship-role: %s", this.toString());
      return null;
    }
    if (!indexes.rolesForRelationshipsContainsIgnoreCase(oRoleAlias)) {
      state.log("IUR.vhr.2", ERROR,
          "The (left) role alias column (%s) is not a valid relationship-role: %s",
          oRoleAlias, this.toString());
      return null;
    }
    if (indexes.rolesForRelationshipsHasMismatchedCase(oRoleAlias)) {
      state.log("IUR.vhr.3", ERROR, true,
          "The left role alias column (%s) is different in case from the"
          + " defined relationship-role.  Will attempt to proceed with the defined role "
          + "spelling: %s", oRoleAlias, this.toString());
      return indexes.correctRelationshipRoleCase(oRoleAlias);
    }
    return oRoleAlias;
  }

  @Override
  @Nonnull
  @Contract(pure = true)
  public String toString() {
    return String.format("UserRelationship['%s','%s','%s','%s','%s']", userIdLeft, userIdRight,
        roleAliasLeft, roleAliasRight, delete);
  }
}
