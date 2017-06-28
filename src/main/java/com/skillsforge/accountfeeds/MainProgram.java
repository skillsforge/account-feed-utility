package com.skillsforge.accountfeeds;

import com.skillsforge.accountfeeds.config.OrganisationParameters;
import com.skillsforge.accountfeeds.config.ProgramMode;
import com.skillsforge.accountfeeds.config.ProgramState;
import com.skillsforge.accountfeeds.input.Indexes;
import com.skillsforge.accountfeeds.input.ParsedFeedFiles;
import com.skillsforge.accountfeeds.inputmodels.InputGroup;
import com.skillsforge.accountfeeds.inputmodels.InputGroupRole;
import com.skillsforge.accountfeeds.inputmodels.InputUser;
import com.skillsforge.accountfeeds.inputmodels.InputUserGroup;
import com.skillsforge.accountfeeds.inputmodels.InputUserRelationship;
import com.skillsforge.accountfeeds.outputmodels.OutputGroup;
import com.skillsforge.accountfeeds.outputmodels.OutputGroupRole;
import com.skillsforge.accountfeeds.outputmodels.OutputUser;
import com.skillsforge.accountfeeds.outputmodels.OutputUserGroup;
import com.skillsforge.accountfeeds.outputmodels.OutputUserRelationship;

import java.util.Collection;
import java.util.HashSet;

import javax.annotation.Nonnull;

/**
 * @author aw1459
 * @date 26-May-2017
 */
public class MainProgram {

  private int exitCode = 0;

  public MainProgram(final String[] args) {

    // A state object that describes the parameters that the program is running under.  Mostly
    // immutable, apart from other classes can indicate that a fatal problem has occurred, and the
    // program should exit at the next convenient opportunity.
    @Nonnull final ProgramState state = new ProgramState(args);
    if (state.hasFatalErrorBeenEncountered()) {
      System.err.print("Problems were encountered whilst starting up - exiting.\n\n");
      exitCode = 1;
      return;
    }

    // Configuration specific to the current organisation.  Defines the available group and
    // relationship roles that a feed could assign users into, as well as version information for
    // the
    // organisation's current SkillsForge instance, and rules about their user metadata.  Immutable.
    @Nonnull final OrganisationParameters orgParams = new OrganisationParameters(state);
    if (state.hasFatalErrorBeenEncountered()) {
      state.getOutputLogStream()
          .printf(
              "Problems were encountered whilst reading the organisational state file - exiting"
              + ".\n\n");
      exitCode = 1;
      return;
    }

    // A container structure for the input csv files, after having been parsed into lines and
    // fields.  The files will be checked for obvious CSV errors whilst constructing this object.
    // This class exposes methods for checking the SF-specific format of the files.
    @Nonnull final ParsedFeedFiles feedFiles = new ParsedFeedFiles(state, orgParams);

    if (state.hasFatalErrorBeenEncountered()) {
      state.getOutputLogStream()
          .printf("Problems were encountered whilst parsing the input files - exiting.\n\n");
      exitCode = 1;
      return;
    }

    // Sets of appropriately linted objects, suitable for re-creating a "mint-condition" feed from.
    @Nonnull final Collection<OutputUser> compiledUsers = new HashSet<>();
    @Nonnull final Collection<OutputGroup> compiledGroups = new HashSet<>();

    // For every mode, run the full sanity check.
    check(state, orgParams, feedFiles, compiledUsers, compiledGroups);

    if (state.getProgramMode() == ProgramMode.LINT) {
      lint();
    }

    if (state.getProgramMode() == ProgramMode.UPLOAD) {
      upload();
    }
  }

  private static void check(@Nonnull final ProgramState state,
      @Nonnull final OrganisationParameters orgParams,
      @Nonnull final ParsedFeedFiles feedFiles,
      @Nonnull final Collection<OutputUser> compiledUsers,
      @Nonnull final Collection<OutputGroup> compiledGroups) {

    // Sanity check the syntactic contents of each file (e.g. number of columns).
    feedFiles.checkLayout();

    // Build objects
    state.getOutputLogStream().printf("\n[INFO] Building objects:\n\n");
    final Collection<InputUser> users = feedFiles.generateUserModels();
    final Collection<InputGroup> groups = feedFiles.generateGroupModels();
    final Collection<InputUserGroup> userGroups = feedFiles.generateUserGroupModels();
    final Collection<InputUserRelationship> userRelationships =
        feedFiles.generateUserRelationshipModels();
    final Collection<InputGroupRole> groupRoles = feedFiles.generateGroupRoleModels();

    // Sanity check the semantics of each object.
    for (final InputUser user : users) {
      final OutputUser outputUser = user.validateAllFields();
      if (outputUser != null) {
        compiledUsers.add(outputUser);
      }
    }
    for (final InputGroup group : groups) {
      final OutputGroup outputGroup = group.validateAllFields();
      if (outputGroup != null) {
        compiledGroups.add(outputGroup);
      }
    }

    // Build indexes against the objects, and check for missing primary keys whilst doing so.
    state.getOutputLogStream().printf("\n[INFO] Building object indexes:\n\n");
    final Indexes indexes =
        new Indexes(orgParams, state, users, groups, compiledUsers, compiledGroups);

    // Build output objects
    state.getOutputLogStream().printf("\n[INFO] Validating final feed objects:\n\n");
    for (final InputGroupRole groupRole : groupRoles) {
      final OutputGroupRole newGroupRole = groupRole.validateAllFields(indexes);

      if (newGroupRole != null) {
        final OutputGroup grp = indexes.getCompiledGroupByGroupAlias(newGroupRole.getGroupAlias());
        if (grp != null) {
          grp.addRole(state, newGroupRole);
        }
      }
    }
    for (final InputUserGroup userGroup : userGroups) {
      final OutputUserGroup newUserGroup = userGroup.validateAllFields(indexes);

      if (newUserGroup != null) {
        final OutputUser usr = indexes.getCompiledUserByUserId(newUserGroup.getUserId());
        if (usr != null) {
          usr.addGroup(state, newUserGroup);
        }
      }
    }
    for (final InputUserRelationship userRelationship : userRelationships) {
      final OutputUserRelationship newUserRel = userRelationship.validateAllFields(indexes);

      if (newUserRel != null) {
        final OutputUser usr = indexes.getCompiledUserByUserId(newUserRel.getUserIdLeft());
        if (usr != null) {
          usr.addRelationshipHeldOverAnotherUser(state, newUserRel);
        }
      }
    }
    state.getOutputLogStream().printf("\n[INFO] + Validated all final feed objects.\n\n");
  }

  private static void lint() {

  }

  private static void upload() {

  }

  public int getExitCode() {
    return exitCode;
  }
}
