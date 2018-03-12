package com.skillsforge.accountfeeds.inputmodels;

import com.skillsforge.accountfeeds.config.ProgramState;
import com.skillsforge.accountfeeds.input.Indexes;
import com.skillsforge.accountfeeds.outputmodels.OutputUserGroup;

import org.jetbrains.annotations.Contract;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.skillsforge.accountfeeds.config.LogLevel.ERROR;

/**
 * @author aw1459
 * @date 27-May-2017
 */
public class InputUserGroup {
  @SuppressWarnings("FieldNotUsedInToString")
  @Nonnull
  private final ProgramState state;

  @Nullable
  private final String userId;
  @Nullable
  private final String groupAlias;

  public InputUserGroup(
      @Nonnull final ProgramState state,
      @Nonnull final List<String> line) {

    this.state = state;

    if (line.size() < 2) {
      state.log("IUG.1", ERROR, "InputUserGroup is incomplete as CSV line does not contain enough "
                                + "columns: %s",
          line.toString());
    }
    if (line.size() > 2) {
      state.log("IUG.2", ERROR, "InputUserGroup CSV line contains too many columns: %s",
          line.toString());
    }

    userId = CommonMethods.getFieldFromLine(line, 0);
    groupAlias = CommonMethods.getFieldFromLine(line, 1);

    if (CommonMethods.containsNewlineOrDoubleQuote(
        userId, groupAlias)) {
      state.log("IUG.3", ERROR,
          "A field on this UserGroup CSV line contains either a double-quote or a "
          + "newline character - these are not supported by the target version: %s",
          line.toString());
    }
  }

  @Nullable
  public OutputUserGroup validateAllFields(
      @Nonnull final Indexes indexes) {

    final String oUserId = CommonMethods.validateUserId(userId, indexes, state, this, "");
    final String oGroupAlias = CommonMethods.validateGroupAlias(groupAlias, indexes, state, this);
    if ((oUserId == null) || (oGroupAlias == null)) {
      return null;
    }

    return new OutputUserGroup(oUserId, oGroupAlias);
  }

  @Override
  @Nonnull
  @Contract(pure = true)
  public String toString() {
    return String.format("UserGroup['%s','%s']", userId, groupAlias);
  }
}
