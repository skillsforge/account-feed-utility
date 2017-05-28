package com.skillsforge.accountfeeds.inputmodels;

import com.skillsforge.accountfeeds.config.ProgramState;
import com.skillsforge.accountfeeds.input.Indexes;

import org.jetbrains.annotations.Contract;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author aw1459
 * @date 28-May-2017
 */
public final class CommonMethods {

  private CommonMethods() {
  }

  @Nullable
  @Contract(pure = true)
  static String validateGroupAlias(@Nullable final String oGroupAlias,
      @Nonnull final Indexes indexes, @Nonnull final ProgramState state,
      @Nonnull final Object inputObject) {
    if ((oGroupAlias == null) || oGroupAlias.trim().isEmpty()) {
      state.getOutputLogStream()
          .printf(
              "[ERROR] The GroupAlias column is blank in %s - must be filled with a GroupAlias.\n",
              inputObject.toString());
      return null;
    }
    if (!oGroupAlias.trim().equals(oGroupAlias)) {
      state.getOutputLogStream()
          .printf(
              "[WARNING-LINTABLE] The GroupAlias in %s will be trimmed of whitespace when "
              + "uploaded.\n", inputObject.toString());
    }
    final InputGroup group = indexes.getGroupByAliasIgnoreCase(oGroupAlias);
    if (group == null) {
      state.getOutputLogStream()
          .printf("[ERROR] The GroupAlias (%s) in %s does not exist in the Groups file.\n",
              oGroupAlias, inputObject.toString());
      return null;
    }
    if (indexes.groupAliasHasMismatchedCase(oGroupAlias)) {
      state.getOutputLogStream()
          .printf(
              "[ERROR-LINTABLE] The GroupAlias (%s) in %s is different in case to its "
              + "definition in the Groups file.  Will proceed with spelling from Groups file.\n",
              oGroupAlias, inputObject.toString());
      return group.getGroupAlias();
    }
    return oGroupAlias.trim();
  }

  @Nullable
  @Contract(pure = true)
  static String validateUserId(@Nullable final String oUserId, @Nonnull final Indexes indexes,
      @Nonnull final ProgramState state, @Nonnull final Object inputObject,
      @Nonnull final String desc) {
    if ((oUserId == null) || oUserId.trim().isEmpty()) {
      state.getOutputLogStream()
          .printf(
              "[ERROR] The %sUserID column is blank in %s - both must be filled with a UserID.\n",
              desc, inputObject.toString());
      return null;
    }
    if (!oUserId.trim().equals(oUserId)) {
      state.getOutputLogStream()
          .printf(
              "[WARNING-LINTABLE] The %sUserID in %s will be trimmed of whitespace when uploaded"
              + ".\n", desc, inputObject.toString());
    }
    final InputUser user = indexes.getUserByUserIdIgnoreCase(oUserId);
    if (user == null) {
      state.getOutputLogStream()
          .printf("[ERROR] The %sUserID (%s) in %s does not exist in the Users file.\n",
              desc, oUserId, inputObject.toString());
      return null;
    }
    if (indexes.userIdHasMismatchedCase(oUserId)) {
      state.getOutputLogStream()
          .printf(
              "[ERROR-LINTABLE] The %sUserID (%s) in %s is different in case to its "
              + "definition in the Users file.  Will proceed with spelling from Users file.\n",
              desc, oUserId, inputObject.toString());
      return user.getUserId();
    }
    return oUserId.trim();
  }

  @Nullable
  @Contract(pure = true, value = "null,_,_,_,null,_ -> null;"
                                 + "null,_,_,_,!null,_ -> !null;"
                                 + "!null,_,_,_,_,!null -> !null")
  static String validateTrueFalse(@Nullable final String field, @Nonnull final ProgramState state,
      @Nonnull final Object inputObject, @Nonnull final String name,
      @Nullable final String defaultIfNotSpecified, @Nullable final String defaultIfInvalid) {
    if ((field == null) || field.trim().isEmpty()) {
      state.getOutputLogStream()
          .printf(
              "[ERROR-LINTABLE] '%s' not specified in %s - must be true or false.\n",
              name, inputObject.toString());
      return defaultIfNotSpecified;
    }
    if (!"true".equalsIgnoreCase(field) && !"false".equalsIgnoreCase(field)) {
      state.getOutputLogStream()
          .printf(
              "[ERROR-LINTABLE] '%s' invalid in %s - must be true or false.  Will "
              + "proceed as if false.\n", name, inputObject.toString());
      return defaultIfInvalid;
    } else {
      return "true".equalsIgnoreCase(field) ? "true" : "false";
    }
  }

  @Nullable
  @Contract(value = "null,_,_,_,_ -> null", pure = true)
  static String validateMandatory(@Nullable final String field,
      @Nonnull final Function<String, Boolean> matcher, @Nonnull final String name,
      @Nonnull final ProgramState state, @Nonnull final Object inputObject) {
    if ((field == null) || field.trim().isEmpty()) {
      state.getOutputLogStream()
          .printf("[ERROR] The %s in %s is blank.\n", name, inputObject.toString());
      return null;
    }
    if (!matcher.apply(field)) {
      state.getOutputLogStream()
          .printf("[ERROR] The %s (%s) in %s has invalid characters or is badly formatted.\n",
              name, field, inputObject.toString());
      return null;
    }
    if (!field.trim().equals(field)) {
      state.getOutputLogStream()
          .printf(
              "[WARNING-LINTABLE] The %s (%s) in %s is surrounded by whitespace which will be"
              + " trimmed when uploaded.\n", name, field, inputObject.toString());
    }
    return field.trim();
  }

  @Nonnull
  @Contract(pure = true)
  static String validateNonMandatory(@Nullable final String field,
      @Nonnull final Function<String, Boolean> matcher, @Nonnull final String name,
      @Nonnull final ProgramState state, @Nonnull final Object inputObject,
      final boolean warnIfMissing) {
    if ((field == null) || field.trim().isEmpty()) {
      if (warnIfMissing) {
        state.getOutputLogStream()
            .printf("[WARNING] The %s in %s is blank.\n", name, inputObject.toString());
      }
      return "";
    }
    if (!matcher.apply(field)) {
      state.getOutputLogStream()
          .printf(
              "[WARNING-LINTABLE] The %s (%s) in %s has invalid characters or is badly formatted."
              + "  Will proceed using a blank %s.\n",
              name, field, inputObject.toString(), name);
      if (warnIfMissing) {
        state.getOutputLogStream()
            .printf("[WARNING] The %s in %s has been made blank.\n", name, inputObject.toString());
      }
      return "";
    }
    if (!field.trim().equals(field)) {
      state.getOutputLogStream()
          .printf(
              "[WARNING-LINTABLE] The %s (%s) in %s is surrounded by whitespace which will be"
              + " trimmed when uploaded.\n", name, field, inputObject.toString());
    }
    return field.trim();
  }
}
