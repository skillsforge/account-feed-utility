package com.skillsforge.accountfeeds;

import com.skillsforge.accountfeeds.config.FileKey;
import com.skillsforge.accountfeeds.config.OrganisationParameters;
import com.skillsforge.accountfeeds.config.ProgramMode;
import com.skillsforge.accountfeeds.config.ProgramState;
import com.skillsforge.accountfeeds.config.PropKey;
import com.skillsforge.accountfeeds.exceptions.ParamException;
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

import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jetbrains.annotations.Contract;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.skillsforge.accountfeeds.config.LogLevel.ERROR;
import static com.skillsforge.accountfeeds.config.LogLevel.INFO;
import static com.skillsforge.accountfeeds.config.LogLevel.WARN;

/**
 * @author aw1459
 * @date 26-May-2017
 */
public class MainProgram {

  @Nonnull
  private static final Charset UTF8 = StandardCharsets.UTF_8;
  @Nonnull
  private static final ContentType CSV_CONTENT_TYPE = ContentType.create("text/csv", UTF8);
  @Nonnull
  private static final ContentType PARAM_CONTENT_TYPE = ContentType.create("text/plain", UTF8);

  private int exitCode = 0;

  public MainProgram(@Nonnull final String[] args) {

    // A state object that describes the parameters that the program is running under.  Mostly
    // immutable, apart from other classes can indicate that a fatal problem has occurred, and the
    // program should exit at the next convenient opportunity.
    final ProgramState state = new ProgramState(args);
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
    final OrganisationParameters orgParams = new OrganisationParameters(state);
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
    final ParsedFeedFiles feedFiles = new ParsedFeedFiles(state, orgParams);

    if (state.hasFatalErrorBeenEncountered()) {
      state.log(ERROR, "Problems were encountered whilst parsing the input files - exiting.\n");
      state.renderLog();
      exitCode = 1;
      return;
    }

    // Sets of appropriately linted objects, suitable for re-creating a "mint-condition" feed from.
    final Collection<OutputUser> compiledUsers = new HashSet<>();
    final Collection<OutputGroup> compiledGroups = new HashSet<>();

    // For every mode, run the full sanity check.
    check(state, orgParams, feedFiles, compiledUsers, compiledGroups);

    if (state.getProgramMode() == ProgramMode.LINT) {
      lint(state, compiledUsers, compiledGroups);
    }

    if (state.getProgramMode() == ProgramMode.UPLOAD) {
      try {
        upload(state, orgParams, feedFiles.getMetadataKeyCsvString());
      } catch (ParamException ignored) {
        state.log(ERROR, "Could not locate the metadata headers in use in the Users file.");
      }
    }

    state.renderLog();
  }

  @SuppressWarnings({
      "MethodWithMoreThanThreeNegations",
      "OverlyComplexMethod",
      "OverlyCoupledMethod",
      "OverlyLongMethod",
      "MethodWithMultipleLoops"
  })
  private static void check(
      @Nonnull final ProgramState state,
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
    state.licenceLog(INFO,
        "Checking headcounts:\n"
        + "  NOTE: These do not consider manually created accounts or accounts\n"
        + "  in their grace period, so these numbers will be an under-estimate.\n");
    for (final String headcountRole : orgParams.getHeadcountLimits().keySet()) {
      final long limit = orgParams.getHeadcountLimits().get(headcountRole);
      final long headcount = compiledUsers.stream()
          .filter(user -> doesUserHaveRole(user, headcountRole, compiledGroups))
          .count();

      if (headcount > limit) {
        state.licenceLog(WARN, "HEADCOUNT EXCEEDED for '%s': using %d of %d licences.",
            headcountRole, headcount, limit);
      } else {
        state.licenceLog(INFO, "Headcount for '%s': using %d of %d licences.",
            headcountRole, headcount, limit);
      }
    }
    state.licenceLog(INFO, "+ All headcounts checked.\n");

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

  private static void lint(
      @Nonnull final ProgramState state,
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

  private static void writeOutToFile(
      @Nonnull final ProgramState state,
      @Nonnull final Collection<String> headers,
      @Nonnull final FileKey fileType,
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

  private static void upload(
      @Nonnull final ProgramState state,
      @Nonnull final OrganisationParameters orgParams,
      @Nonnull final String metaKeyCsvList) {

    final Map<PropKey, String> uploadParams = orgParams.getUploadParams();

    final Map<String, String> fields = new HashMap<>();
    fields.put("j_orgAlias", uploadParams.get(PropKey.ORG_ALIAS));
    fields.put("orgAlias", uploadParams.get(PropKey.ORG_ALIAS));
    fields.put("owningFeed", uploadParams.get(PropKey.FEED_ID));
    fields.put("emailResult_recipientList", uploadParams.get(PropKey.EMAIL_LIST));
    fields.put("csvFile_Users_extra_metadata_keys", metaKeyCsvList);
    if (uploadParams.containsKey(PropKey.USERNAME_CHANGES)) {
      fields.put("allowUsernameChanges", "on");
    }
    if (uploadParams.containsKey(PropKey.EMAIL_SUBJECT)) {
      fields.put("emailResult_subject", uploadParams.get(PropKey.EMAIL_SUBJECT));
    }
    if (uploadParams.containsKey(PropKey.ACCOUNT_EXPIRE_DELAY)) {
      fields.put("archiveAccountsAfterDays", uploadParams.get(PropKey.ACCOUNT_EXPIRE_DELAY));
    }
    if (uploadParams.containsKey(PropKey.RELATIONSHIP_EXPIRE_DELAY)) {
      fields.put("relationshipDeleteAfterDays",
          uploadParams.get(PropKey.RELATIONSHIP_EXPIRE_DELAY));
    }

    final File usersFile = state.getFile(FileKey.INPUT_USERS);
    final File groupsFile = state.getFile(FileKey.INPUT_GROUPS);
    final File userRelationshipsFile = state.getFile(FileKey.INPUT_USER_RELATIONSHIPS);
    final File userGroupsFile = state.getFile(FileKey.INPUT_USER_GROUPS);
    final File groupRolesFile = state.getFile(FileKey.INPUT_GROUP_ROLES);

    if (anyNull(usersFile, groupsFile, userRelationshipsFile, userGroupsFile, groupRolesFile)) {
      state.log(ERROR, "All five CSV files must be specified and exist.");
      state.setFatalErrorEncountered();
      return;
    }
    assert usersFile != null;
    assert groupsFile != null;
    assert userRelationshipsFile != null;
    assert userGroupsFile != null;
    assert groupRolesFile != null;

    // This uses the FileBody class to write out each file.  It's probably not UTF-8 compliant...
    // See the monstrosity that is FileBody::writeTo to see how this works.

    final MultipartEntityBuilder entityBuilder = MultipartEntityBuilder
        .create()
        .setBoundary("------------------------boundary-" + UUID.randomUUID())
        .setCharset(UTF8)
        .addBinaryBody("csvFile_Users",
            usersFile, CSV_CONTENT_TYPE, "Users.csv")
        .addBinaryBody("csvFile_UserGroup",
            userGroupsFile, CSV_CONTENT_TYPE, "UserGroups.csv")
        .addBinaryBody("csvFile_UserRelationships",
            userRelationshipsFile, CSV_CONTENT_TYPE, "UserRelationships.csv")
        .addBinaryBody("csvFile_Groups",
            groupsFile, CSV_CONTENT_TYPE, "Groups.csv")
        .addBinaryBody("csvFile_GroupRole",
            groupRolesFile, CSV_CONTENT_TYPE, "GroupRoles.csv");
    fields.forEach((paramName, value) ->
        entityBuilder.addTextBody(paramName, value, PARAM_CONTENT_TYPE));

    final String url = uploadParams.get(PropKey.URL);
    if (url == null) {
      throw new AssertionError("URL was not specified.");
    }

    final HttpPost post = new HttpPost(url);
    post.setEntity(entityBuilder.build());
    post.setHeader("X-Auth-Token", uploadParams.get(PropKey.TOKEN));
    post.setHeader("User-Agent",
        String.format("account-feed-utility-1.0-SNAPSHOT for [%s] targeting [%d.%d.%d/%d]",
            orgParams.getOrganisationName(), orgParams.getTargetVersionMajor(),
            orgParams.getTargetVersionMinor(), orgParams.getTargetVersionRevision(),
            orgParams.getTargetVersionBetaLevel()));

    state.log(INFO, "\n\nBeginning upload to server.\n"
                    + "===========================\n\n");

    final RequestConfig config = RequestConfig.custom()
        .setConnectTimeout(10 * 1000)
        .setConnectionRequestTimeout(1000)
        .setSocketTimeout(10 * 1000)
        .setCircularRedirectsAllowed(false)
        .setContentCompressionEnabled(false)
        .setRedirectsEnabled(true)
        .build();

    final StatusLine statusLine;
    try (
        CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(config)
            .disableContentCompression()
            .build();
        CloseableHttpResponse response = client.execute(post)
    ) {
      statusLine = response.getStatusLine();
    } catch (IOException e) {
      state.log(ERROR, "Could not communicate with server: %s\n  %s", url, e.getLocalizedMessage());
      state.setFatalErrorEncountered();
      return;
    }

    state.log(INFO, "Completed uploading: %s", statusLine.toString());
  }

  @SafeVarargs
  @Contract(pure = true)
  private static <T> boolean anyNull(@Nonnull final T... objects) {
    for (@Nullable T o : objects) {
      if (o == null) {
        return true;
      }
    }
    return false;
  }

  @Contract(pure = true)
  public int getExitCode() {
    return exitCode;
  }
}
