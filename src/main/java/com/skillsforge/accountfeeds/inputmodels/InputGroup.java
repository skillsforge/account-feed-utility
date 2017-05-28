package com.skillsforge.accountfeeds.inputmodels;

import com.skillsforge.accountfeeds.config.ProgramState;
import com.skillsforge.accountfeeds.input.Patterns;
import com.skillsforge.accountfeeds.outputmodels.OutputGroup;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author aw1459
 * @date 27-May-2017
 */
public class InputGroup {

  @SuppressWarnings("FieldNotUsedInToString")
  @Nonnull
  private final ProgramState state;
  @Nullable
  private String groupAlias;
  @Nullable
  private String groupName;
  @Nullable
  private String groupDescription;
  @Nullable
  private String delete;

  @SuppressWarnings("TypeMayBeWeakened")
  public InputGroup(@Nonnull final ProgramState state, @Nonnull final List<String> line) {
    this.state = state;

    final int lineSize = line.size();

    if (lineSize < 4) {
      state.getOutputLogStream()
          .printf(
              "[ERROR] InputGroup is incomplete as CSV line (%s) does not contain enough columns"
              + ".\n",
              line.toString());
    }
    if (lineSize > 4) {
      state.getOutputLogStream()
          .printf("[ERROR] InputGroup CSV line (%s) contains too many columns.\n", line.toString());
    }

    groupAlias = (lineSize > 0) ? line.get(0) : null;
    groupName = (lineSize > 1) ? line.get(1) : null;
    groupDescription = (lineSize > 2) ? line.get(2) : null;
    delete = (lineSize > 3) ? line.get(3) : null;
  }

  @Nullable
  public String getGroupAlias() {
    return groupAlias;
  }

  @Nullable
  public String getGroupName() {
    return groupName;
  }

  @Nullable
  public OutputGroup validateAllFields() {
    final String oGroupAlias =
        CommonMethods.validateMandatory(groupAlias, Patterns::isValidGroupAlias, "GroupAlias",
            state, this);
    final String oGroupName =
        CommonMethods.validateNonMandatory(groupName, Patterns::isAlwaysValid, "GroupName", state,
            this, true);
    final String oGroupDescription =
        CommonMethods.validateNonMandatory(groupDescription, Patterns::isAlwaysValid,
            "GroupDescription", state, this, false);
    final String oDelete =
        CommonMethods.validateTrueFalse(delete, state, this,
            "Delete", "false", "false");

    if (oGroupAlias == null) {
      return null;
    }

    if ("true".equals(oDelete)) {
      state.getOutputLogStream()
          .printf(
              "[WARNING] The Delete field in %s is set to true - deletion of groups has not yet "
              + "been implemented, and this will likely not have the desired effect.\n",
              this.toString());
    }

    return new OutputGroup(state, oGroupAlias, oGroupName, oGroupDescription,
        "true".equals(oDelete));
  }

  @Override
  public String toString() {
    return String.format("Group['%s','%s','%s','%s']", groupAlias, groupName, groupDescription,
        delete);
  }

}
