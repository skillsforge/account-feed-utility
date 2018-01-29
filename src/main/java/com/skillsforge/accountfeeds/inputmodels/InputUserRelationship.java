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
      state.log(ERROR, "InputUserRelationship is incomplete as CSV line (%s) does not contain "
                       + "enough columns.",
          line.toString());
    }
    if (line.size() > 5) {
      state.log(ERROR, "InputUserRelationship CSV line (%s) contains too many columns.",
          line.toString());
    }

    userIdLeft = CommonMethods.getFieldFromLine(line, 0);
    userIdRight = CommonMethods.getFieldFromLine(line, 1);
    roleAliasLeft = CommonMethods.getFieldFromLine(line, 2);
    roleAliasRight = CommonMethods.getFieldFromLine(line, 3);
    delete = CommonMethods.getFieldFromLine(line, 4);

    if (CommonMethods.containsNewlineOrDoubleQuote(
        userIdLeft, userIdRight, roleAliasLeft, roleAliasRight, delete)) {
      state.log(ERROR,
          "A field on this UserRelationship CSV line (%s) contains either a double-quote or a "
          + "newline character - these are not supported by the target version.", line.toString());
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
      state.log(
          ERROR, "The holder/left and subject/right UserID columns held the same value "
                 + "in %s - you cannot hold a relationship over yourself.\n",
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
      state.log(ERROR, "The (left) role alias column is blank in %s - this must be filled with a "
                       + "valid relationship-role.", this.toString());
      return null;
    }
    if (!indexes.rolesForRelationshipsContainsIgnoreCase(oRoleAlias)) {
      state.log(ERROR, "The (left) role alias column (%s) in %s is not a valid relationship-role.",
          oRoleAlias, this.toString());
      return null;
    }
    if (indexes.rolesForRelationshipsHasMismatchedCase(oRoleAlias)) {
      state.log(ERROR, true,
          "The left role alias column (%s) in %s is different in case from the"
          + " defined relationship-role.  Will attempt to proceed with the defined role "
          + "spelling.", oRoleAlias, this.toString());
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
