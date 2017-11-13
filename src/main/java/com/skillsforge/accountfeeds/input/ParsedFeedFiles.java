package com.skillsforge.accountfeeds.input;

import com.skillsforge.accountfeeds.config.FileKey;
import com.skillsforge.accountfeeds.config.OrganisationParameters;
import com.skillsforge.accountfeeds.config.ProgramMode;
import com.skillsforge.accountfeeds.config.ProgramState;
import com.skillsforge.accountfeeds.exceptions.ParamException;
import com.skillsforge.accountfeeds.inputmodels.InputGroup;
import com.skillsforge.accountfeeds.inputmodels.InputGroupRole;
import com.skillsforge.accountfeeds.inputmodels.InputUser;
import com.skillsforge.accountfeeds.inputmodels.InputUserGroup;
import com.skillsforge.accountfeeds.inputmodels.InputUserRelationship;

import org.jetbrains.annotations.Contract;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import static com.skillsforge.accountfeeds.config.LogLevel.ERROR;
import static com.skillsforge.accountfeeds.config.LogLevel.INFO;
import static com.skillsforge.accountfeeds.config.LogLevel.WARN;

/**
 * @author aw1459
 * @date 27-May-2017
 */
@SuppressWarnings({"TypeMayBeWeakened", "ClassWithTooManyFields"})
public class ParsedFeedFiles {

  @Nonnull
  private static final Charset UTF8 = StandardCharsets.UTF_8;
  @Nonnull
  private static final String[] USERS_HEADERS_V5 =
      {"UserID", "Username", "Email", "Title", "Forename", "Surname", "Disabled", "Archived"};
  @Nonnull
  private static final String[] GROUPS_HEADERS_V5 =
      {"GroupAlias", "GroupName", "GroupDescription", "Delete"};
  @Nonnull
  private static final String[] USER_GROUPS_HEADERS_V5 = {"UserID", "GroupAlias"};
  @Nonnull
  private static final String[] USER_RELATIONSHIPS_HEADERS_V5 =
      {"UserIDLeft", "UserIDRight", "RoleAliasLeft", "RoleAliasRight", "Delete"};
  @Nonnull
  private static final String[] GROUP_ROLES_HEADERS_V5 = {"GroupAlias", "RoleAlias"};

  @Nonnull
  private static final Pattern RE_USERS_METAHEADER_VALID_V5 = Pattern.compile("^[a-zA-Z0-9-_.]+$");
  @Nonnull
  private static final Pattern RE_USERS_METAHEADER_DISALLOWED_V5 =
      Pattern.compile("^(usernameChange.*)|(syncTimestamp.*)$");

  @Nonnull
  private final ProgramState state;
  @Nonnull
  private final OrganisationParameters orgParams;
  @Nonnull
  private final List<List<String>> users = new LinkedList<>();
  @Nonnull
  private final List<List<String>> userGroups = new LinkedList<>();
  @Nonnull
  private final List<List<String>> userRelationships = new LinkedList<>();
  @Nonnull
  private final List<List<String>> groups = new LinkedList<>();
  @Nonnull
  private final List<List<String>> groupRoles = new LinkedList<>();

  // This is initialised as part of checkLayout(), so the CHECK phase needs to run before this is
  // accessed.
  @Nonnull
  private String metadataKeyCsvString = "";

  public ParsedFeedFiles(
      @Nonnull final ProgramState state,
      @Nonnull final OrganisationParameters orgParams) {

    this.state = state;
    this.orgParams = orgParams;

    readInFile(FileKey.INPUT_USERS, users);
    readInFile(FileKey.INPUT_USER_GROUPS, userGroups);
    readInFile(FileKey.INPUT_USER_RELATIONSHIPS, userRelationships);
    readInFile(FileKey.INPUT_GROUPS, groups);
    readInFile(FileKey.INPUT_GROUP_ROLES, groupRoles);
  }

  private void readInFile(
      @Nonnull final FileKey fileKey,
      @Nonnull final Collection<List<String>> multiList) {

    final File file = state.getFile(fileKey);
    if (file != null) {
      try (
          final CsvReader csvReader = new CsvReader(
              new InputStreamReader(new FileInputStream(file), UTF8),
              state
          )
      ) {

        multiList.addAll(csvReader.readFile());
      } catch (IOException e) {
        state.log(ERROR, "Problem encountered whilst accessing file: %s: %s.", file.getPath(),
            e.getLocalizedMessage());
        state.setFatalErrorEncountered();
      }
    }
  }

  @Nonnull
  @Contract(pure = true)
  public static List<String> getUsersHeaders() {
    return new LinkedList<>(Arrays.asList(USERS_HEADERS_V5));
  }

  @Nonnull
  @Contract(pure = true)
  public static List<String> getGroupsHeaders() {
    return new LinkedList<>(Arrays.asList(GROUPS_HEADERS_V5));
  }

  @Nonnull
  @Contract(pure = true)
  public static List<String> getUserGroupsHeaders() {
    return new LinkedList<>(Arrays.asList(USER_GROUPS_HEADERS_V5));
  }

  @Nonnull
  @Contract(pure = true)
  public static List<String> getUserRelationshipsHeaders() {
    return new LinkedList<>(Arrays.asList(USER_RELATIONSHIPS_HEADERS_V5));
  }

  @Nonnull
  @Contract(pure = true)
  public static List<String> getGroupRolesHeaders() {
    return new LinkedList<>(Arrays.asList(GROUP_ROLES_HEADERS_V5));
  }

  public void checkLayout() {
    state.log(INFO, "Checking syntax and layout of individual files:\n");

    checkUsersLayout();
    checkGenericLayout(groups, GROUPS_HEADERS_V5, "Groups");
    checkGenericLayout(userGroups, USER_GROUPS_HEADERS_V5, "UserGroups");
    checkGenericLayout(userRelationships, USER_RELATIONSHIPS_HEADERS_V5, "UserRelationships");
    checkGenericLayout(groupRoles, GROUP_ROLES_HEADERS_V5, "GroupRoles");
  }

  private void checkGenericLayout(
      @Nonnull final List<List<String>> multiList,
      @Nonnull final String[] headers,
      @Nonnull final String fileType) {

    if (multiList.isEmpty()) {
      state.log(INFO, "%s file: is blank.  No assessment of this file will take place.", fileType);
      return;
    }

    final List<String> headerLine = multiList.iterator().next();

    checkHeader(headerLine, headers, fileType, false);
    checkBody(multiList, headerLine.size(), fileType);

    state.log(INFO, "Completed checking %s file.", fileType);
  }

  private void checkUsersLayout() {
    if (users.isEmpty()) {
      state.log(INFO, "Users file: is blank.  No assessment of this file will take place.");
      return;
    }

    final List<String> header = users.iterator().next();
    checkHeader(header, USERS_HEADERS_V5, "Users", true);

    // There must be at least one metadata column, due to a bug in the sync servlet.
    final int headerCount = header.size();
    if (headerCount < (USERS_HEADERS_V5.length + 1)) {
      state.log(ERROR, "Users file: has too few columns - expected eight standard columns"
                       + " and at least one metadata column.");
    }

    final List<String> metadataHeaders = header.subList(8, header.size());
    this.metadataKeyCsvString = String.join(",", metadataHeaders);
    state.log(INFO, "Users file: Metadata columns are: %s.", metadataHeaders.toString());

    final Set<String> metadataHeadersSet = new HashSet<>(metadataHeaders);
    if (metadataHeaders.size() != metadataHeadersSet.size()) {
      state.log(WARN, "Users file: Some metadata columns have duplicate names - which column is"
                      + " actually used is not determinate.");
    }

    for (final String headerName : metadataHeaders) {
      if (!RE_USERS_METAHEADER_VALID_V5.matcher(headerName).matches()) {
        state.log(WARN, "Users file: Metadata column name '%s' contains invalid characters.",
            headerName);
      }
      if (RE_USERS_METAHEADER_DISALLOWED_V5.matcher(headerName).matches()) {
        state.log(WARN, "Users file: Metadata column name '%s' contains a reserved word.",
            headerName);
      }
    }

    checkBody(users, headerCount, "Users");

    state.log(INFO, "Completed checking Users file.");
  }

  private void checkHeader(
      @Nonnull final List<String> headerLine,
      @Nonnull final String[] headers,
      @Nonnull final String fileType,
      final boolean hasMetadata) {

    final int correctHeaderCount = headers.length;
    final int userProvidedHeaderCount = headerLine.size();

    if (userProvidedHeaderCount < correctHeaderCount) {
      state.log(ERROR, "%s file: This file has too few columns: expected " +
                       (hasMetadata ? "at least " : "") + "%d but found %d.",
          fileType, correctHeaderCount, userProvidedHeaderCount);
      return;
    }
    if (!hasMetadata && (userProvidedHeaderCount > correctHeaderCount)) {
      state.log(ERROR, "%s file: This file has too many columns: expected %d but found %d.",
          fileType, correctHeaderCount, userProvidedHeaderCount);
      return;
    }

    for (int i = 0; i < correctHeaderCount; i++) {
      final String headerName = headerLine.get(i);
      if (headers[i].equals(headerName)) {
        continue;
      }
      if (headers[i].equalsIgnoreCase(headerName)) {
        state.log(ERROR, "%s file: The column name '%s' is case-sensitive - it should be '%s'.",
            fileType, headerName, headers[i]);
        continue;
      }
      state.log(ERROR, "%s file: The column name '%s' should not appear in the position it does - "
                       + "'%s' should appear here instead.", fileType, headerName, headers[i]);
    }
  }

  @SuppressWarnings("MethodWithMultipleLoops")
  private void checkBody(
      @Nonnull final List<List<String>> multiList,
      final int headerCount,
      @Nonnull final String fileType) {

    int lineNum = 0;
    for (final List<String> line : multiList) {
      lineNum++;
      if (lineNum == 1) {
        continue;
      }
      if (line.isEmpty()) {
        if (state.getProgramMode() != ProgramMode.LINT) {
          state.log(WARN, "%s file: Line %d is blank.", fileType, lineNum);
        }
        continue;
      }
      if (line.size() != headerCount) {
        state.log(ERROR, "%s file: Line %d (%s) has the wrong number of columns - expected %d, "
                         + "but found %d.",
            fileType, lineNum, line.get(0), headerCount, line.size());
      }

      int colNum = 0;
      for (final String column : line) {
        colNum++;
        if (!column.equals(column.trim())) {
          state.log(WARN, "%s file: Line %d (%s) Column %d (%s) begins or ends with whitespace - "
                          + "this will be trimmed when uploaded.",
              fileType, lineNum, line.get(0), colNum, column);
        }
      }
    }
  }

  @Nonnull
  public Collection<InputUser> generateUserModels() {
    state.log(INFO, "Building InputUser objects:");

    final Collection<InputUser> objects = new HashSet<>();
    final List<String> headerLine = users.iterator().next();
    final List<String> metadataHeaders = headerLine.subList(8, headerLine.size());

    for (final List<String> line : users.subList(1, users.size())) {
      if (!line.isEmpty()) {
        objects.add(new InputUser(state, orgParams, line, metadataHeaders));
      }
    }

    state.log(INFO, "+ Built %d InputUser object(s).", objects.size());
    return objects;
  }

  @Nonnull
  public Collection<InputGroup> generateGroupModels() {
    state.log(INFO, "Building InputGroup objects:");

    final Collection<InputGroup> objects = new HashSet<>();

    for (final List<String> line : groups.subList(1, groups.size())) {
      if (!line.isEmpty()) {
        objects.add(new InputGroup(state, line, orgParams));
      }
    }

    state.log(INFO, "+ Built %d InputGroup object(s).", objects.size());
    return objects;
  }

  @Nonnull
  public Collection<InputGroupRole> generateGroupRoleModels() {
    state.log(INFO, "Building InputGroupRole objects:");

    final Collection<InputGroupRole> objects = new HashSet<>();

    for (final List<String> line : groupRoles.subList(1, groupRoles.size())) {
      if (!line.isEmpty()) {
        objects.add(new InputGroupRole(state, line));
      }
    }

    state.log(INFO, "+ Built %d InputGroupRole object(s).", objects.size());
    return objects;
  }

  @Nonnull
  public Collection<InputUserGroup> generateUserGroupModels() {
    state.log(INFO, "Building InputUserGroup objects:");

    final Collection<InputUserGroup> objects = new HashSet<>();

    for (final List<String> line : userGroups.subList(1, userGroups.size())) {
      if (!line.isEmpty()) {
        objects.add(new InputUserGroup(state, line));
      }
    }

    state.log(INFO, "+ Built %d InputUserGroup object(s).", objects.size());
    return objects;
  }

  @Nonnull
  public Collection<InputUserRelationship> generateUserRelationshipModels() {
    state.log(INFO, "Building InputUserRelationship objects:");

    final Collection<InputUserRelationship> objects = new HashSet<>();

    for (final List<String> line : userRelationships.subList(1, userRelationships.size())) {
      if (!line.isEmpty()) {
        objects.add(new InputUserRelationship(state, line));
      }
    }

    state.log(INFO, "+ Built %d InputUserRelationship object(s).", objects.size());
    return objects;
  }

  @Nonnull
  public String getMetadataKeyCsvString() throws ParamException {
    if (metadataKeyCsvString.isEmpty()) {
      throw new ParamException("Metadata Headers haven't been initialised yet.");
    }
    return metadataKeyCsvString;
  }
}
