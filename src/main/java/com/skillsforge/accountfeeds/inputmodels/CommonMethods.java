package com.skillsforge.accountfeeds.inputmodels;

import com.skillsforge.accountfeeds.config.ProgramState;
import com.skillsforge.accountfeeds.input.Indexes;

import org.jetbrains.annotations.Contract;

import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.skillsforge.accountfeeds.config.LogLevel.ERROR;
import static com.skillsforge.accountfeeds.config.LogLevel.WARN;

/**
 * @author aw1459
 * @date 28-May-2017
 */
@SuppressWarnings("UtilityClass")
public final class CommonMethods {

  private CommonMethods() {
  }

  @Nullable
  @Contract(pure = true)
  static String getFieldFromLine(
      @Nonnull final List<String> line,
      final int indexFrom0) {

    return (line.size() > indexFrom0)
           ? line.get(indexFrom0)
           : null;
  }

  @Contract(pure = true)
  static boolean containsNewlineOrDoubleQuote(@Nonnull final String... fields) {
    for (String field : fields) {
      if (field.contains("\n") || field.contains("\"")) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  @Contract(pure = true, value = "null,_,_,_ -> null")
  static String validateGroupAlias(
      @Nullable final String oGroupAlias,
      @Nonnull final Indexes indexes,
      @Nonnull final ProgramState state,
      @Nonnull final Object inputObject) {

    if ((oGroupAlias == null) || oGroupAlias.trim().isEmpty()) {
      state.log("CM.vga.1",
          ERROR, "The GroupAlias column is blank - it must be filled with a GroupAlias: %s",
          inputObject.toString());
      return null;
    }
    if (!oGroupAlias.trim().equals(oGroupAlias)) {
      state.log("CM.vga.2", ERROR, true,
          "[WARNING-LINTABLE] The GroupAlias will be trimmed of whitespace when "
          + "uploaded: %s", inputObject.toString());
    }
    final InputGroup group = indexes.getGroupByAliasIgnoreCase(oGroupAlias);
    if (group == null) {
      state.log("CM.vga.3", ERROR, "The GroupAlias (%s) does not exist in the Groups file: %s",
          oGroupAlias, inputObject.toString());
      return null;
    }
    if (indexes.groupAliasHasMismatchedCase(oGroupAlias)) {
      state.log("CM.vga.4", ERROR, true,
          "[ERROR-LINTABLE] The GroupAlias (%s) is different in case to its "
          + "definition in the Groups file: %s.  Will proceed with spelling from Groups file.",
          oGroupAlias, inputObject.toString());
      return group.getGroupAlias();
    }
    return oGroupAlias.trim();
  }

  @Nullable
  @Contract(pure = true, value = "null,_,_,_,_ -> null")
  static String validateUserId(
      @Nullable final String oUserId,
      @Nonnull final Indexes indexes,
      @Nonnull final ProgramState state,
      @Nonnull final Object inputObject,
      @Nonnull final String desc) {

    if ((oUserId == null) || oUserId.trim().isEmpty()) {
      state.log("CM.vui.1",
          ERROR, "The %sUserID column is blank - it must contain a UserID: %s",
          desc, inputObject.toString());
      return null;
    }
    if (!oUserId.trim().equals(oUserId)) {
      state.log("CM.vui.2", WARN, true,
          "The %sUserID will be trimmed of whitespace when uploaded: %s"
          + ".\n", desc, inputObject.toString());
    }
    final InputUser user = indexes.getUserByUserIdIgnoreCase(oUserId);
    if (user == null) {
      state.log("CM.vui.3", ERROR, "The %sUserID (%s) does not exist in the Users file: %s",
          desc, oUserId, inputObject.toString());
      return null;
    }
    if (indexes.userIdHasMismatchedCase(oUserId)) {
      state.log("CM.vui.4", ERROR, true,
          "The %sUserID (%s) is different in case to its "
          + "definition in the Users file: %s.  Will proceed with spelling from Users file.",
          desc, oUserId, inputObject.toString());
      return user.getUserId();
    }
    return oUserId.trim();
  }

  @Nonnull
  @Contract(pure = true)
  static String validateTrueFalse(
      @Nullable final String field,
      @Nonnull final ProgramState state,
      @Nonnull final Object inputObject,
      @Nonnull final String name) {

    if ((field == null) || field.trim().isEmpty()) {
      state.log("CM.vtf.1", ERROR, true,
          "'%s' not specified - must be true or false: %s",
          name, inputObject.toString());
      return "false"; // Default when not specified
    }
    if (!"true".equalsIgnoreCase(field) && !"false".equalsIgnoreCase(field)) {
      state.log("CM.vtf.1", ERROR, true,
          "'%s' invalid - must be true or false.  Will "
          + "proceed as if false: %s", name, inputObject.toString());
      return "false"; // Default if invalid
    } else {
      return "true".equalsIgnoreCase(field) ? "true" : "false";
    }
  }

  @Nullable
  @Contract(value = "null,_,_,_,_ -> null", pure = true)
  static String validateMandatory(
      @Nullable final String field,
      @Nonnull final Function<String, Boolean> matcher,
      @Nonnull final String name,
      @Nonnull final ProgramState state,
      @Nonnull final Object inputObject) {

    if ((field == null) || field.trim().isEmpty()) {
      state.log("CM.vm.1", ERROR, "%s is blank: %s", name, inputObject.toString());
      return null;
    }
    if (!matcher.apply(field)) {
      state.log("CM.vm.2", ERROR, "%s (%s) has invalid characters or is badly formatted: %s",
          name, field, inputObject.toString());
      return null;
    }
    if (!field.trim().equals(field)) {
      state.log("CM.vm.3", WARN, true,
          "%s (%s) is surrounded by whitespace which will be"
          + " trimmed when uploaded: %s", name, field, inputObject.toString());
    }
    return field.trim();
  }

  @Nonnull
  @Contract(pure = true)
  static String validateNonMandatory(
      @Nullable final String field,
      @Nonnull final Function<String, Boolean> matcher,
      @Nonnull final String name,
      @Nonnull final ProgramState state,
      @Nonnull final Object inputObject,
      final boolean warnIfMissing) {

    if ((field == null) || field.trim().isEmpty()) {
      if (warnIfMissing) {
        state.log("CM.vnm.1", WARN, "%s is blank: %s.", name, inputObject.toString());
      }
      return "";
    }
    if (!matcher.apply(field)) {
      state.log("CM.vnm.2", WARN, true,
          "%s (%s) has invalid characters or is badly formatted: %s"
          + "  Will proceed using a blank %s.",
          name, field, inputObject.toString(), name);
      if (warnIfMissing) {
        state.log("CM.vnm.3", WARN, "%s has been made blank: %s", name, inputObject.toString());
      }
      return "";
    }
    if (!field.trim().equals(field)) {
      state.log("CM.vnm.4", WARN, true,
          "%s (%s) is surrounded by whitespace which will be"
          + " trimmed when uploaded: %s.", name, field, inputObject.toString());
    }
    return field.trim();
  }
}
