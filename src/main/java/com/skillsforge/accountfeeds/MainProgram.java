package com.skillsforge.accountfeeds;

import com.skillsforge.accountfeeds.config.FileKey;
import com.skillsforge.accountfeeds.config.OrganisationParameters;
import com.skillsforge.accountfeeds.config.ProgramMode;
import com.skillsforge.accountfeeds.config.ProgramState;
import com.skillsforge.accountfeeds.config.PropKey;
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

import org.jetbrains.annotations.Contract;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import static com.skillsforge.accountfeeds.config.LogLevel.ERROR;
import static com.skillsforge.accountfeeds.config.LogLevel.INFO;
import static com.skillsforge.accountfeeds.config.LogLevel.WARN;

/**
 * @author aw1459
 * @date 26-May-2017
 */
public class MainProgram {

  @Nonnull
  private static final String CR_LF = "\r\n";

  private int exitCode = 0;

  public MainProgram(final String[] args) {

    // A state object that describes the parameters that the program is running under.  Mostly
    // immutable, apart from other classes can indicate that a fatal problem has occurred, and the
    // program should exit at the next convenient opportunity.
    @Nonnull final ProgramState state = new ProgramState(args);
    if (state.hasFatalErrorBeenEncountered()) {
      state.log(ERROR, "Problems were encountered whilst starting up - exiting.\n");
      state.renderLog();
      exitCode = 1;
      return;
    }
    if (state.getProgramMode() == ProgramMode.HELP) {
      exitCode = 1;
      return;
    }

    // Configuration specific to the current organisation.  Defines the available group and
    // relationship roles that a feed could assign users into, as well as version information for
    // the
    // organisation's current SkillsForge instance, and rules about their user metadata.  Immutable.
    @Nonnull final OrganisationParameters orgParams = new OrganisationParameters(state);
    if (state.hasFatalErrorBeenEncountered()) {
      state.log(ERROR,
          "Problems were encountered whilst reading the organisational state file - exiting.\n");
      state.renderLog();
      exitCode = 1;
      return;
    }

    // A container structure for the input csv files, after having been parsed into lines and
    // fields.  The files will be checked for obvious CSV errors whilst constructing this object.
    // This class exposes methods for checking the SF-specific format of the files.
    @Nonnull final ParsedFeedFiles feedFiles = new ParsedFeedFiles(state, orgParams);

    if (state.hasFatalErrorBeenEncountered()) {
      state.log(ERROR, "Problems were encountered whilst parsing the input files - exiting.\n");
      state.renderLog();
      exitCode = 1;
      return;
    }

    // Sets of appropriately linted objects, suitable for re-creating a "mint-condition" feed from.
    @Nonnull final Collection<OutputUser> compiledUsers = new HashSet<>();
    @Nonnull final Collection<OutputGroup> compiledGroups = new HashSet<>();

    // For every mode, run the full sanity check.
    check(state, orgParams, feedFiles, compiledUsers, compiledGroups);

    if (state.getProgramMode() == ProgramMode.LINT) {
      lint(state, compiledUsers, compiledGroups);
    }

    if (state.getProgramMode() == ProgramMode.UPLOAD) {
      upload(state, orgParams);
    }

    state.renderLog();
  }

  @SuppressWarnings({
      "MethodWithMoreThanThreeNegations",
      "OverlyComplexMethod",
      "OverlyCoupledMethod"
  })
  private static void check(@Nonnull final ProgramState state,
      @Nonnull final OrganisationParameters orgParams,
      @Nonnull final ParsedFeedFiles feedFiles,
      @Nonnull final Collection<OutputUser> compiledUsers,
      @Nonnull final Collection<OutputGroup> compiledGroups) {

    /*
    Plan:
     - check the syntax of each file is sensible w.r.t. the organisation and the file header.
     - build "input" objects (one per non-duplicate line in each file)
     - build "output" objects (one per user and one per group) - these are "clean" objects, that can
         be used if a linted output is requested.
     - create indexes into the output objects to try to locate duplicates and also to allow later
         steps to more reliably locate objects based on whatever not-quite-right data they use.
     - iterate through each input "link" object and locate either the output user or output group it
         refers to; then validate the input link and add it to that user or group where possible.
     - count the number of distinct users picking up headcount roles for reporting purposes.
     - check each subject role has the minimum and maximum number of required relationships.
     */

    // Sanity check the syntactic contents of each file (e.g. number of columns).
    feedFiles.checkLayout();

    // Build objects
    state.log(INFO, "\n\nBuilding objects:\n=================\n");
    final Collection<InputUser> users = feedFiles.generateUserModels();
    final Collection<InputGroup> groups = feedFiles.generateGroupModels();
    final Collection<InputUserGroup> userGroups = feedFiles.generateUserGroupModels();
    final Collection<InputUserRelationship> userRelationships =
        feedFiles.generateUserRelationshipModels();
    final Collection<InputGroupRole> groupRoles = feedFiles.generateGroupRoleModels();

    // Sanity check the semantics of each input object, and build output objects.
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
    state.log(INFO, "+ All objects built.\n");

    // Build indexes against the objects, and check for missing primary keys whilst doing so.
    state.log(INFO, "\n\nBuilding object indexes:\n========================\n");
    final Indexes indexes =
        new Indexes(orgParams, state, users, groups, compiledUsers, compiledGroups);
    state.log(INFO, "+ All indexes built.\n");

    // Build link objects
    state.log(INFO, "\n\nValidating output objects:\n==========================\n");
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
        final OutputUser holder = indexes.getCompiledUserByUserId(newUserRel.getUserIdLeft());
        if (holder != null) {
          holder.addRelationshipHeldOverAnotherUser(state, newUserRel);
        }
        final OutputUser subject = indexes.getCompiledUserByUserId(newUserRel.getUserIdRight());
        if (subject != null) {
          subject.addRelationshipThisUserIsASubjectOf(newUserRel);
        }
      }
    }
    state.log(INFO, "+ Validated all final feed objects.\n");

    // Check headcounts
    state.log(INFO, "\n\nChecking headcounts:\n==========================\n"
                    + "NOTE: These do not consider manually created accounts or accounts in\n"
                    + "their grace period, so these numbers will be an under-estimate.\n");
    for (final String headcountRole : orgParams.getHeadcountLimits().keySet()) {
      final long limit = orgParams.getHeadcountLimits().get(headcountRole);
      final long headcount = compiledUsers.stream()
          .filter(user -> doesUserHaveRole(user, headcountRole, compiledGroups))
          .count();

      if (headcount > limit) {
        state.log(WARN, "HEADCOUNT EXCEEDED for '%s': using %d of %d licences.",
            headcountRole, headcount, limit);
      } else {
        state.log(INFO, "Headcount for '%s': using %d of %d licences.",
            headcountRole, headcount, limit);
      }
    }
    state.log(INFO, "+ All headcounts checked.\n");

    state.log(INFO, "\n\nValidating relationships:\n==========================\n");

    // Check minimum relationship users.
    orgParams.getMinimumRequiredRelationships()
        .forEach((userRole, minRelMap) ->
            compiledUsers.stream()
                .filter(user -> doesUserHaveRole(user, userRole, compiledGroups))
                .forEach(user ->
                    minRelMap.forEach(
                        (roleName, minExpected) -> {
                          final long count = user.getRelationshipsSubject()
                              .filter(rel -> rel.getRoleAliasLeft().equals(roleName))
                              .count();

                          if (count < minExpected) {
                            state.log(WARN, "User with ID %s has too few %s relationships - "
                                            + "expected at least %d but user has %d.",
                                user.getUserId(), roleName, minExpected, count);
                          }
                        }
                    )
                )

        );

    // Check maximum relationship users.
    orgParams.getMaximumRequiredRelationships()
        .forEach((userRole, maxRelMap) ->
            compiledUsers.stream()
                .filter(user -> doesUserHaveRole(user, userRole, compiledGroups))
                .forEach(user ->
                    maxRelMap.forEach(
                        (roleName, maxExpected) -> {
                          final long count = user.getRelationshipsSubject()
                              .filter(rel -> rel.getRoleAliasLeft().equals(roleName))
                              .count();

                          if (count > maxExpected) {
                            state.log(WARN, "User with ID %s has too many %s relationships - "
                                            + "expected at most %d but user has %d.",
                                user.getUserId(), roleName, maxExpected, count);
                          }
                        }
                    )
                )

        );
    state.log(INFO, "+ All relationships checked.\n");
  }

  @Contract(pure = true)
  private static boolean doesUserHaveRole(
      @Nonnull final OutputUser user,
      @Nonnull final String roleName,
      @Nonnull final Collection<OutputGroup> compiledGroups) {

    if (user.isArchived()) {
      // Archived users have no roles.
      return false;
    }

    return "all users".equalsIgnoreCase(roleName)

           || user.getGroups()
               .map(OutputUserGroup::getGroupAlias)
               .flatMap(groupAlias -> compiledGroups.stream()
                   .filter(group -> group.getGroupAlias().equals(groupAlias))
                   .flatMap(OutputGroup::getRoles)
                   .map(OutputGroupRole::getRoleName)
               )
               .anyMatch(roleName::equals)

           || user.getRelationshipsHeld()
               .map(OutputUserRelationship::getRoleAliasLeft)
               .anyMatch(roleName::equals);
  }

  private static void lint(@Nonnull final ProgramState state,
      @Nonnull final Collection<OutputUser> compiledUsers,
      @Nonnull final Collection<OutputGroup> compiledGroups) {

    state.log(INFO, "\n\nOutputting linted objects:\n==========================\n");

    /*
    Plan:
     - iterate through each user and group, outputting these to their associated files.
     - iterate through each user's relationships, each user's groups and each group's roles,
         outputting these to their associated files.
     */

    // Users file
    final Collection<String> usersFileHeader = ParsedFeedFiles.getUsersHeaders();
    final Collection<String> metadataHeaders =
        compiledUsers.stream()
            .flatMap(OutputUser::getMetadataKeys)
            .distinct()
            .sorted(String::compareToIgnoreCase)
            .collect(Collectors.toList());
    usersFileHeader.addAll(metadataHeaders);

    writeOutToFile(state, usersFileHeader, FileKey.OUTPUT_USERS,
        output -> compiledUsers.stream()
            .sorted(OutputUser.CSV_SORTER)
            .map(user -> user.getCsvRow(metadataHeaders))
            .forEach(output::println)
    );
    if (state.hasFatalErrorBeenEncountered()) {
      return;
    }

    // Groups file
    writeOutToFile(state, ParsedFeedFiles.getGroupsHeaders(), FileKey.OUTPUT_GROUPS,
        output -> compiledGroups.stream()
            .sorted(OutputGroup.CSV_SORTER)
            .map(OutputGroup::getCsvRow)
            .forEach(output::println)
    );
    if (state.hasFatalErrorBeenEncountered()) {
      return;
    }

    // UserGroups file
    writeOutToFile(state, ParsedFeedFiles.getUserGroupsHeaders(), FileKey.OUTPUT_USER_GROUPS,
        output -> compiledUsers.stream()
            .flatMap(OutputUser::getGroups)
            .sorted(OutputUserGroup.CSV_SORTER)
            .map(OutputUserGroup::getCsvRow)
            .forEach(output::println)
    );
    if (state.hasFatalErrorBeenEncountered()) {
      return;
    }

    // UserRelationships file
    writeOutToFile(state, ParsedFeedFiles.getUserRelationshipsHeaders(),
        FileKey.OUTPUT_USER_RELATIONSHIPS,
        output -> compiledUsers.stream()
            .flatMap(OutputUser::getRelationshipsHeld)
            .sorted(OutputUserRelationship.CSV_SORTER)
            .map(OutputUserRelationship::getCsvRow)
            .forEach(output::println)
    );
    if (state.hasFatalErrorBeenEncountered()) {
      return;
    }

    // GroupRoles file
    writeOutToFile(state, ParsedFeedFiles.getGroupRolesHeaders(),
        FileKey.OUTPUT_GROUP_ROLES,
        output -> compiledGroups.stream()
            .flatMap(OutputGroup::getRoles)
            .sorted(OutputGroupRole.CSV_SORTER)
            .map(OutputGroupRole::getCsvRow)
            .forEach(output::println)
    );
    if (state.hasFatalErrorBeenEncountered()) {
      return;
    }

    state.log(INFO, "+ Output all linted objects.\n");
  }

  private static void writeOutToFile(@Nonnull final ProgramState state,
      @Nonnull final Collection<String> headers, @Nonnull final FileKey fileType,
      @Nonnull final Consumer<PrintStream> csvLineOutputConsumer) {

    state.log(INFO, "Writing new %s file...", fileType.getFileDescription());
    final File outputFile = state.getFile(fileType);
    if (outputFile == null) {
      throw new AssertionError("The " + fileType.getFileDescription() + " file was null.");
    }

    try (final PrintStream output = new PrintStream(outputFile, "UTF-8")) {
      output.println(headers.stream().collect(Collectors.joining(",")));

      csvLineOutputConsumer.accept(output);

      output.flush();
    } catch (UnsupportedEncodingException e) {
      state.log(ERROR, "Could not set the file encoding of the output %s file to UTF-8: %s",
          fileType.getFileDescription(), e.getLocalizedMessage());
      state.setFatalErrorEncountered();
      return;
    } catch (FileNotFoundException e) {
      state.log(ERROR, "Could not locate the output %s file (%s): %s",
          fileType.getFileDescription(), outputFile.getPath(), e.getLocalizedMessage());
      state.setFatalErrorEncountered();
      return;
    }

    state.log(INFO, " ... %s file written successfully.", fileType.getFileDescription());
  }

  private static void upload(@Nonnull final ProgramState state,
      @Nonnull final OrganisationParameters orgParams) {

    final String token = state.getProperty(PropKey.TOKEN);
    final String url = state.getProperty(PropKey.URL);

    if (url == null) {
      state.log(ERROR, "The URL to upload the feed files to was not specified.");
      state.setFatalErrorEncountered();
    }
    if (token == null) {
      state.log(ERROR,
          "The token for authenticating with the SkillsForge instance was not specified.  This "
          + "can be specified either on the command line, or within the '%s' environment variable.",
          ProgramState.ENV_SF_TOKEN);
      state.setFatalErrorEncountered();
    }
    if (state.hasFatalErrorBeenEncountered()) {
      return;
    }

    assert url != null;
    assert token != null;

    final URLConnection urlConnection;
    try {
      urlConnection = new URL(url).openConnection();
    } catch (IOException e) {
      state.log(ERROR, "Could not open connection to URL: %s\n  %s", url, e.getLocalizedMessage());
      state.setFatalErrorEncountered();
      return;
    }
    urlConnection.setDoOutput(true);
    final String multipartBoundary = "next-file-boundary-" + UUID.randomUUID();
    urlConnection.setRequestProperty("Content-Type",
        "multipart/form-data; charset=utf-8; boundary=" + multipartBoundary);

    final File usersFile = state.getFile(FileKey.INPUT_USERS);
    if (usersFile == null) {
      state.log(ERROR, "No Users.csv file was specified.");
      state.setFatalErrorEncountered();
      return;
    }

    try (OutputStream outputStream = urlConnection.getOutputStream();
         PrintWriter httpOut = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"))) {
      httpOut.append("--").append(multipartBoundary).append(CR_LF);
      httpOut.append("Content-Disposition: form-data; name=\"param\"").append(CR_LF);
      httpOut.append("Content-Type: text/plain; charset=UTF-8").append(CR_LF);
      httpOut.append(CR_LF).append("").append(CR_LF).flush();
      // TODO: actually finish this.
    } catch (UnsupportedEncodingException e) {

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Contract(pure = true)
  public int getExitCode() {
    return exitCode;
  }
}
