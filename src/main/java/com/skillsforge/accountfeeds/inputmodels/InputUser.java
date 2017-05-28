package com.skillsforge.accountfeeds.inputmodels;

import com.skillsforge.accountfeeds.config.OrganisationParameters;
import com.skillsforge.accountfeeds.config.ProgramState;
import com.skillsforge.accountfeeds.input.Patterns;
import com.skillsforge.accountfeeds.outputmodels.OutputUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
  @Nonnull
  private final Map<String, String> metaData = new HashMap<>();
  @Nullable
  private String userId;
  @Nullable
  private String username;
  @Nullable
  private String email;
  @Nullable
  private String title;
  @Nullable
  private String forename;
  @Nullable
  private String surname;
  @Nullable
  private String archived;
  @Nullable
  private String disabled;

  @SuppressWarnings("TypeMayBeWeakened")
  public InputUser(@Nonnull final ProgramState state,
      @Nonnull final OrganisationParameters orgParams,
      @Nonnull final List<String> line,
      @Nonnull final List<String> metadataHeaders) {
    this.state = state;
    this.orgParams = orgParams;

    final int lineSize = line.size();

    if (lineSize < (metadataHeaders.size() + 8)) {
      state.getOutputLogStream()
          .printf(
              "[ERROR] InputUser is incomplete as CSV line (%s) does not contain enough columns.\n",
              line.toString());
    }
    if (lineSize > (metadataHeaders.size() + 8)) {
      state.getOutputLogStream()
          .printf("[ERROR] Users CSV line (%s) contains too many columns.\n", line.get(0));
    }

    userId = (lineSize > 0) ? line.get(0) : null;
    username = (lineSize > 1) ? line.get(1) : null;
    email = (lineSize > 2) ? line.get(2) : null;
    title = (lineSize > 3) ? line.get(3) : null;
    forename = (lineSize > 4) ? line.get(4) : null;
    surname = (lineSize > 5) ? line.get(5) : null;
    archived = (lineSize > 6) ? line.get(6) : null;
    disabled = (lineSize > 7) ? line.get(7) : null;

    if (lineSize > 8) {
      final List<String> metaValues = line.subList(8, line.size());
      final int metaCount = Integer.max(metaValues.size(), metadataHeaders.size());

      for (int valNum = 0; valNum < metaCount; valNum++) {
        metaData.put(metadataHeaders.get(valNum), metaValues.get(valNum));
      }
    }
  }

  @Nullable
  public String getUserId() {
    return userId;
  }

  @Nullable
  public String getUsername() {
    return username;
  }

  @Nullable
  public String getEmail() {
    return email;
  }

  @Nullable
  public OutputUser validateAllFields() {
    final String oUserId =
        CommonMethods.validateMandatory(userId, Patterns::isValidUserId, "UserID", state, this);
    final String oUsername =
        CommonMethods.validateMandatory(username, Patterns::isValidUsername, "Username", state,
            this);
    final String oEmail =
        CommonMethods.validateMandatory(email, Patterns::isValidEmail, "Email", state, this);
    final String oTitle =
        CommonMethods.validateNonMandatory(title, Patterns::isValidName, "Title", state, this,
            true);
    final String oForename =
        CommonMethods.validateNonMandatory(forename, Patterns::isValidName, "Forename", state, this,
            true);
    final String oSurname =
        CommonMethods.validateMandatory(surname, Patterns::isValidName, "Surname", state, this);
    final String oDisabled =
        CommonMethods.validateTrueFalse(disabled, state, this, "Disabled", "false", "false");
    final String oArchived =
        CommonMethods.validateTrueFalse(archived, state, this, "Archived", "false", "false");

    metaData.forEach((key, value) -> {
      final Pattern pattern = orgParams.getMetadataPattern(key);
      if ((pattern != null) && !pattern.matcher(value).matches()) {
        state.getOutputLogStream()
            .printf(
                "[WARNING] User '%s' has unexpected or badly formatted metadata: '%s' -> '%s'.\n",
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
  public String toString() {
    return String.format("User['%s','%s','%s','%s','%s','%s','%s','%s',meta=%s]", userId,
        username, email, title, forename, surname, archived, disabled, metaData.toString());
  }
}
