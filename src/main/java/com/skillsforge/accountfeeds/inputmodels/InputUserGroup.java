package com.skillsforge.accountfeeds.inputmodels;

import com.skillsforge.accountfeeds.config.ProgramState;
import com.skillsforge.accountfeeds.input.Indexes;
import com.skillsforge.accountfeeds.outputmodels.OutputUserGroup;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.skillsforge.accountfeeds.config.LogLevel.ERROR;

/**
 * @author aw1459
 * @date 27-May-2017
 */
public class InputUserGroup {
  // {"GroupAlias", "RoleAlias"};
  @SuppressWarnings("FieldNotUsedInToString")
  @Nonnull
  private final ProgramState state;
  @Nullable
  private final String userId;
  @Nullable
  private final String groupAlias;

  @SuppressWarnings("TypeMayBeWeakened")
  public InputUserGroup(@Nonnull final ProgramState state, @Nonnull final List<String> line) {
    this.state = state;

    final int lineSize = line.size();

    if (lineSize < 2) {
      state.log(ERROR, "InputUserGroup is incomplete as CSV line (%s) does not contain enough "
                       + "columns.",
          line.toString());
    }
    if (lineSize > 2) {
      state.log(ERROR, "InputUserGroup CSV line (%s) contains too many columns.",
          line.toString());
    }

    userId = (lineSize > 0) ? line.get(0) : null;
    groupAlias = (lineSize > 1) ? line.get(1) : null;
  }

  @Nullable
  public OutputUserGroup validateAllFields(@Nonnull final Indexes indexes) {
    final String oUserId = CommonMethods.validateUserId(userId, indexes, state, this, "");
    final String oGroupAlias = CommonMethods.validateGroupAlias(groupAlias, indexes, state, this);
    if ((oUserId == null) || (oGroupAlias == null)) {
      return null;
    }

    return new OutputUserGroup(oUserId, oGroupAlias);
  }

  @Override
  public String toString() {
    return String.format("UserGroup['%s','%s']", userId, groupAlias);
  }
}
