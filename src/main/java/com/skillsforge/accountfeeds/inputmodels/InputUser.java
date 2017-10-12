package com.skillsforge.accountfeeds.inputmodels;

import com.skillsforge.accountfeeds.config.OrganisationParameters;
import com.skillsforge.accountfeeds.config.ProgramState;
import com.skillsforge.accountfeeds.input.Patterns;
import com.skillsforge.accountfeeds.outputmodels.OutputUser;

import org.jetbrains.annotations.Contract;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.skillsforge.accountfeeds.config.LogLevel.ERROR;
import static com.skillsforge.accountfeeds.config.LogLevel.WARN;

/**
 * @author aw1459
 * @date 27-May-2017
 */
public class InputUser {

  @SuppressWarnings("FieldNotUsedInToString")
  @Nonnull
  private final ProgramState state;
  @SuppressWarnings("FieldNotUsedInToString")
  @Nonnull
  private final OrganisationParameters orgParams;
  @SuppressWarnings("FieldNotUsedInToString")

  @Nonnull
  private final Map<String, String> metaData = new HashMap<>();
  @Nullable
  private final String userId;
  @Nullable
  private final String username;
  @Nullable
  private final String email;
  @Nullable
  private final String title;
  @Nullable
  private final String forename;
  @Nullable
  private final String surname;
  @Nullable
  private final String archived;
  @Nullable
  private final String disabled;

  public InputUser(@Nonnull final ProgramState state,
      @Nonnull final OrganisationParameters orgParams,
      @Nonnull final List<String> line,
      @Nonnull final List<String> metadataHeaders) {
    this.state = state;
    this.orgParams = orgParams;

    if (line.size() < (metadataHeaders.size() + 8)) {
      state.log(ERROR, "InputUser is incomplete as CSV line (%s) does not contain enough columns.",
          line.toString());
    }
    if (line.size() > (metadataHeaders.size() + 8)) {
      state.log(ERROR, "Users CSV line (%s) contains too many columns.", line.get(0));
    }

    userId = CommonMethods.getFieldFromLine(line, 0);
    username = CommonMethods.getFieldFromLine(line, 1);
    email = CommonMethods.getFieldFromLine(line, 2);
    title = CommonMethods.getFieldFromLine(line, 3);
    forename = CommonMethods.getFieldFromLine(line, 4);
    surname = CommonMethods.getFieldFromLine(line, 5);
    archived = CommonMethods.getFieldFromLine(line, 6);
    disabled = CommonMethods.getFieldFromLine(line, 7);

    if (line.size() > 8) {
      final List<String> metaValues = line.subList(8, line.size());
      final int metaCount = Integer.min(metaValues.size(), metadataHeaders.size());

      for (int valNum = 0; valNum < metaCount; valNum++) {
        metaData.put(metadataHeaders.get(valNum), metaValues.get(valNum));
      }
    }
  }

  @Nullable
  @Contract(pure = true)
  public String getUserId() {
    return userId;
  }

  @Nullable
  @Contract(pure = true)
  public String getUsername() {
    return username;
  }

  @Nullable
  @Contract(pure = true)
  public String getEmail() {
    return email;
  }

  @Nullable
  @Contract(pure = true)
  public OutputUser validateAllFields() {
    final Patterns patterns = orgParams.getPatterns();
    final String oUserId =
        CommonMethods.validateMandatory(userId, patterns::isValidUserId, "UserID", state, this);
    final String oUsername =
        CommonMethods.validateMandatory(username, patterns::isValidUsername, "Username", state,
            this);
    final String oEmail =
        CommonMethods.validateMandatory(email, patterns::isValidEmail, "Email", state, this);
    final String oTitle =
        CommonMethods.validateNonMandatory(title, patterns::isValidName, "Title", state, this,
            true);
    final String oForename =
        CommonMethods.validateNonMandatory(forename, patterns::isValidName, "Forename", state, this,
            true);
    final String oSurname =
        CommonMethods.validateMandatory(surname, patterns::isValidName, "Surname", state, this);
    final String oDisabled =
        CommonMethods.validateTrueFalse(disabled, state, this, "Disabled");
    final String oArchived =
        CommonMethods.validateTrueFalse(archived, state, this, "Archived");

    metaData.forEach((key, value) -> {
      final Pattern pattern = orgParams.getMetadataPattern(key);
      if ((pattern != null) && !pattern.matcher(value).matches()) {
        state.log(WARN, "User '%s' has unexpected or badly formatted metadata: '%s' -> '%s'.",
            oUserId, key, value);
      }
    });

    //noinspection OverlyComplexBooleanExpression
    if ((oUserId == null)
        || (oUsername == null)
        || (oEmail == null)
        || (oSurname == null)) {
      return null;
    }

    return new OutputUser(oUserId, oUsername, oEmail, oTitle, oForename, oSurname,
        "true".equalsIgnoreCase(oDisabled), "true".equalsIgnoreCase(oArchived), metaData);
  }

  @Override
  @Nonnull
  @Contract(pure = true)
  public String toString() {
    return String.format("User['%s','%s','%s','%s','%s','%s','%s','%s',meta=%s]", userId,
        username, email, title, forename, surname, archived, disabled, metaData.toString());
  }
}
