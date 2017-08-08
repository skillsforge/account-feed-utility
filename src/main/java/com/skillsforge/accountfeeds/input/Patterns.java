package com.skillsforge.accountfeeds.input;

import org.jetbrains.annotations.Contract;

import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author aw1459
 * @date 28-May-2017
 */
@SuppressWarnings("MethodMayBeStatic")
public final class Patterns {

  private static final Pattern USER_ID_REGEX_V5 = Pattern.compile("\\A[\\p{Alnum}'-:@_.]+\\z");

  private static final Pattern USERNAME_REGEX_V5 = Pattern.compile("\\A[\\p{L}\\p{Graph}]+\\z");

  private static final Pattern EMAIL_REGEX_V5_9_AND_BELOW =
      Pattern.compile("\\A[\\p{L}\\p{Graph}&&[^@]]+@[\\p{L}\\p{Graph}&&[^@]]+\\z");
  private static final Pattern EMAIL_REGEX_V5_10_BETA_5 =
      Pattern.compile(
          "\\A"
          // Prevent '.' appearing at beginning, end, or more than one in sequence, in local part.
          + "[a-zA-Z0-9!#$%&'*+\\-/=?^_`{|}~](\\.?[a-zA-Z0-9!#$%&'*+\\-/=?^_`{|}~]+)*"
          + '@'
          // Domain part has smaller range of permissible characters, but same '.' requirement.
          + "[a-zA-Z0-9\\-]+(\\.[a-zA-Z0-9\\-]+)*"
          + "\\z");

  private static final Pattern NAME_REGEX_V5_9_AND_BELOW =
      Pattern.compile("[\\p{L}\\p{Graph} ]+");
  private static final Pattern NAME_REGEX_V5_10_BETA_5 =
      Pattern.compile("\\A[\\p{L}\\p{Graph} ]+\\z");

  private final long targetVersion;

  public Patterns(final long targetVersion) {
    this.targetVersion = targetVersion;
  }

  @Contract(value = "null -> false", pure = true)
  public boolean isValidGroupAlias(@Nullable final CharSequence groupAlias) {
    return isValidUserId(groupAlias);
  }

  @Contract(value = "null -> false", pure = true)
  public boolean isValidUserId(@Nullable final CharSequence userId) {
    if (userId == null) {
      return false;
    }
    return USER_ID_REGEX_V5.matcher(userId).matches();
  }

  @SuppressWarnings("unused")
  @Contract(value = "_ -> true", pure = true)
  public boolean isAlwaysValid(@Nonnull final CharSequence unused) {
    return true;
  }

  @Contract(value = "null -> false", pure = true)
  public boolean isValidUsername(@Nullable final CharSequence username) {
    if (username == null) {
      return false;
    }
    return USERNAME_REGEX_V5.matcher(username).matches();
  }

  @Contract(value = "null -> false", pure = true)
  public boolean isValidEmail(@Nullable final CharSequence email) {
    if (email == null) {
      return false;
    }
    return (targetVersion >= 5_010_000_005L)
           ? EMAIL_REGEX_V5_10_BETA_5.matcher(email).matches()
           : EMAIL_REGEX_V5_9_AND_BELOW.matcher(email).matches();
  }

  @Contract(value = "null -> false", pure = true)
  public boolean isValidName(@Nullable final CharSequence name) {
    if (name == null) {
      return false;
    }
    return (targetVersion >= 5_010_000_005L)
           ? NAME_REGEX_V5_10_BETA_5.matcher(name).matches()
           : NAME_REGEX_V5_9_AND_BELOW.matcher(name).matches();
  }
}
