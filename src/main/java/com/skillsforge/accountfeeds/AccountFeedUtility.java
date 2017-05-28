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
public class AccountFeedUtility {

  @Nonnull
  private static final Collection<OutputUser> compiledUsers = new HashSet<>();
  @Nonnull
  private static final Collection<OutputGroup> compiledGroups = new HashSet<>();
  private static ProgramState state;
  private static ParsedFeedFiles feedFiles;
  private static OrganisationParameters orgParams;
  private static Indexes indexes;

  public static void main(final String[] args) {

    state = new ProgramState(args);
    if (state.getProgramMode() == ProgramMode.INVALID) {
      System.err.print("Problems were encountered whilst starting up - exiting.\n\n");
      System.exit(1);
      return;
    }

    // Read in the state file
    orgParams = new OrganisationParameters(state);
    if (state.getProgramMode() == ProgramMode.INVALID) {
      state.getOutputLogStream()
          .printf(
              "Problems were encountered whilst reading the organisational state file - exiting"
              + ".\n\n");
      System.exit(1);
      return;
    }

    if ((state.getProgramMode() == ProgramMode.CHECK)
        || (state.getProgramMode() == ProgramMode.LINT)) {

      feedFiles = new ParsedFeedFiles(state, orgParams);
      if (state.getProgramMode() == ProgramMode.INVALID) {
        state.getOutputLogStream()
            .printf("Problems were encountered whilst parsing the input files - exiting.\n\n");
        System.exit(1);
        return;
      }

      check();
    }
    if (state.getProgramMode() == ProgramMode.LINT) {
      lint();
    }

    if (state.getProgramMode() == ProgramMode.UPLOAD) {
      upload();
    }
  }

  @SuppressWarnings("StaticVariableUsedBeforeInitialization")
  private static void check() {

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
    indexes = new Indexes(orgParams, state, users, groups, compiledUsers, compiledGroups);

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
}
