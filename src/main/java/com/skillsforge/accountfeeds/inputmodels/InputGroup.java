package com.skillsforge.accountfeeds.inputmodels;

import com.skillsforge.accountfeeds.config.OrganisationParameters;
import com.skillsforge.accountfeeds.config.ProgramState;
import com.skillsforge.accountfeeds.input.Patterns;
import com.skillsforge.accountfeeds.outputmodels.OutputGroup;

import org.jetbrains.annotations.Contract;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.skillsforge.accountfeeds.config.LogLevel.ERROR;
import static com.skillsforge.accountfeeds.config.LogLevel.WARN;

/**
 * @author aw1459
 * @date 27-May-2017
 */
public class InputGroup {
  @SuppressWarnings("FieldNotUsedInToString")
  @Nonnull
  private final ProgramState state;
  @SuppressWarnings("FieldNotUsedInToString")
  @Nonnull
  private final OrganisationParameters orgParams;

  @Nullable
  private final String groupAlias;
  @Nullable
  private final String groupName;
  @Nullable
  private final String groupDescription;
  @Nullable
  private final String delete;

  public InputGroup(
      @Nonnull final ProgramState state,
      @Nonnull final List<String> line,
      @Nonnull final OrganisationParameters orgParams) {

    this.state = state;

    if (line.size() < 4) {
      state.log(ERROR,
          "InputGroup is incomplete as CSV line (%s) does not contain enough columns.",
          line.toString());
    }
    if (line.size() > 4) {
      state.log(ERROR, "InputGroup CSV line (%s) contains too many columns.", line.toString());
    }

    groupAlias = CommonMethods.getFieldFromLine(line, 0);
    groupName = CommonMethods.getFieldFromLine(line, 1);
    groupDescription = CommonMethods.getFieldFromLine(line, 2);
    delete = CommonMethods.getFieldFromLine(line, 3);

    this.orgParams = orgParams;
  }

  @Nullable
  @Contract(pure = true)
  public String getGroupAlias() {
    return groupAlias;
  }

  @Nullable
  @Contract(pure = true)
  public String getGroupName() {
    return groupName;
  }

  @Nullable
  @Contract(pure = true)
  public OutputGroup validateAllFields() {
    final Patterns patterns = orgParams.getPatterns();
    final String oGroupAlias =
        CommonMethods.validateMandatory(groupAlias, patterns::isValidGroupAlias, "GroupAlias",
            state, this);
    final String oGroupName =
        CommonMethods.validateNonMandatory(groupName, patterns::isAlwaysValid, "GroupName", state,
            this, true);
    final String oGroupDescription =
        CommonMethods.validateNonMandatory(groupDescription, patterns::isAlwaysValid,
            "GroupDescription", state, this, false);
    final String oDelete =
        CommonMethods.validateTrueFalse(delete, state, this, "Delete");

    if (oGroupAlias == null) {
      return null;
    }

    if ("true".equals(oDelete)) {
      state.log(WARN,
          "The Delete field in %s is set to true - deletion of groups has not yet "
          + "been implemented, and this will likely not have the desired effect.",
          this.toString());
    }

    return new OutputGroup(oGroupAlias, oGroupName, oGroupDescription, "true".equals(oDelete));
  }

  @Override
  @Nonnull
  @Contract(pure = true)
  public String toString() {
    return String.format("Group['%s','%s','%s','%s']", groupAlias, groupName, groupDescription,
        delete);
  }
}
