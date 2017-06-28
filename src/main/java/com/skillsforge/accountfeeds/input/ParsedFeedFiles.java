package com.skillsforge.accountfeeds.input;

import com.skillsforge.accountfeeds.config.FileKey;
import com.skillsforge.accountfeeds.config.OrganisationParameters;
import com.skillsforge.accountfeeds.config.ProgramMode;
import com.skillsforge.accountfeeds.config.ProgramState;
import com.skillsforge.accountfeeds.inputmodels.InputGroup;
import com.skillsforge.accountfeeds.inputmodels.InputGroupRole;
import com.skillsforge.accountfeeds.inputmodels.InputUser;
import com.skillsforge.accountfeeds.inputmodels.InputUserGroup;
import com.skillsforge.accountfeeds.inputmodels.InputUserRelationship;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

/**
 * @author aw1459
 * @date 27-May-2017
 */
@SuppressWarnings("TypeMayBeWeakened")
public class ParsedFeedFiles {

  private static final Charset UTF8 = Charset.forName("UTF-8");
  private static final String[] USERS_HEADERS_V5 =
      {"UserID", "Username", "Email", "Title", "Forename", "Surname", "Disabled", "Archived"};
  private static final String[] GROUPS_HEADERS_V5 =
      {"GroupAlias", "GroupName", "GroupDescription", "Delete"};
  private static final String[] USER_GROUPS_HEADERS_V5 = {"UserID", "GroupAlias"};
  private static final String[] USER_RELATIONSHIPS_HEADERS_V5 =
      {"UserIDLeft", "UserIDRight", "RoleAliasLeft", "RoleAliasRight", "Delete"};
  private static final String[] GROUP_ROLES_HEADERS_V5 = {"GroupAlias", "RoleAlias"};

  private static final Pattern RE_USERS_METAHEADER_VALID_V5 = Pattern.compile("^[a-zA-Z0-9-_.]+$");
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

  public ParsedFeedFiles(@Nonnull final ProgramState state,
      @Nonnull final OrganisationParameters orgParams) {
    this.state = state;
    this.orgParams = orgParams;

    readInFile(FileKey.INPUT_USERS, users);
    readInFile(FileKey.INPUT_USER_GROUPS, userGroups);
    readInFile(FileKey.INPUT_USER_RELATIONSHIPS, userRelationships);
    readInFile(FileKey.INPUT_GROUPS, groups);
    readInFile(FileKey.INPUT_GROUP_ROLES, groupRoles);
  }

  private void readInFile(@Nonnull final FileKey fileKey,
      @Nonnull final Collection<List<String>> multiList) {

    final File file = state.getFile(fileKey);
    if (file != null) {
      try (final CsvReader csvReader = new CsvReader(
          new InputStreamReader(new FileInputStream(file), UTF8))) {

        multiList.addAll(csvReader.readFile(System.out));
      } catch (IOException e) {
        state.getOutputLogStream()
            .printf("[ERROR] Problem encountered whilst accessing file: %s: %s.\n", file.getPath(),
                e.getLocalizedMessage());
        state.setFatalErrorEncountered();
      }
    }
  }

  public void checkLayout() {
    state.getOutputLogStream().printf("[INFO] Checking syntax and layout of individual files:\n\n");

    checkUsersLayout();
    checkGenericLayout(groups, GROUPS_HEADERS_V5, "Groups");
    checkGenericLayout(userGroups, USER_GROUPS_HEADERS_V5, "UserGroups");
    checkGenericLayout(userRelationships, USER_RELATIONSHIPS_HEADERS_V5, "UserRelationships");
    checkGenericLayout(groupRoles, GROUP_ROLES_HEADERS_V5, "GroupRoles");
  }

  private void checkGenericLayout(@Nonnull final List<List<String>> multiList,
      @Nonnull final String[] headers, @Nonnull final String fileType) {
    if (multiList.isEmpty()) {
      state.getOutputLogStream()
          .printf("[INFO] %s file: is blank.  No assessment of this file will take place.\n",
              fileType);
      return;
    }

    final List<String> headerLine = multiList.iterator().next();

    checkHeader(headerLine, headers, fileType, false);
    checkBody(multiList, headerLine.size(), fileType);

    state.getOutputLogStream().printf("[INFO] Completed checking %s file.\n", fileType);
  }

  private void checkUsersLayout() {
    if (users.isEmpty()) {
      state.getOutputLogStream()
          .printf("[INFO] Users file: is blank.  No assessment of this file will take place.\n");
      return;
    }

    final List<String> header = users.iterator().next();
    checkHeader(header, USERS_HEADERS_V5, "Users", true);

    // There must be at least one metadata column, due to a bug in the sync servlet.
    final int headerCount = header.size();
    if (headerCount < (USERS_HEADERS_V5.length + 1)) {
      state.getOutputLogStream()
          .printf(
              "[ERROR] Users file: has too few columns - expected at least eight standard columns"
              + " and one metadata column.\n");
    }

    final List<String> metadataHeaders = header.subList(8, header.size());
    state.getOutputLogStream()
        .printf("[INFO] Users file: Metadata columns are: %s.\n", metadataHeaders.toString());

    final Set<String> metadataHeadersSet = new HashSet<>(metadataHeaders);
    if (metadataHeaders.size() != metadataHeadersSet.size()) {
      state.getOutputLogStream()
          .printf(
              "[WARNING] Users file: Some metadata columns have duplicate names - which column is"
              + " actually used is not determinate.\n");
    }

    for (final String headerName : metadataHeaders) {
      if (!RE_USERS_METAHEADER_VALID_V5.matcher(headerName).matches()) {
        state.getOutputLogStream()
            .printf(
                "[WARNING] Users file: Metadata column name '%s' contains invalid characters.\n",
                headerName);
      }
      if (RE_USERS_METAHEADER_DISALLOWED_V5.matcher(headerName).matches()) {
        state.getOutputLogStream()
            .printf(
                "[WARNING] Users file: Metadata column name '%s' contains a reserved word.\n",
                headerName);
      }
    }

    checkBody(users, headerCount, "Users");

    state.getOutputLogStream().print("[INFO] Completed checking Users file.\n");
  }

  private void checkHeader(@Nonnull final List<String> headerLine,
      @Nonnull final String[] headers, @Nonnull final String fileType, final boolean hasMetadata) {

    final int correctHeaderCount = headers.length;
    final int userProvidedHeaderCount = headerLine.size();

    if (userProvidedHeaderCount < correctHeaderCount) {
      state.getOutputLogStream()
          .printf(
              "[ERROR] %s file: This file has too few columns - expected " +
              (hasMetadata ? "at least " : "") + "%d but found %d.\n",
              fileType, correctHeaderCount, userProvidedHeaderCount);
      return;
    }
    if (!hasMetadata && (userProvidedHeaderCount > correctHeaderCount)) {
      state.getOutputLogStream()
          .printf(
              "[ERROR] %s file: This file has too many columns - expected %d but found %d.\n",
              fileType, correctHeaderCount, userProvidedHeaderCount);
      return;
    }

    for (int i = 0; i < correctHeaderCount; i++) {
      final String headerName = headerLine.get(i);
      if (headers[i].equals(headerName)) {
        continue;
      }
      if (headers[i].equalsIgnoreCase(headerName)) {
        state.getOutputLogStream()
            .printf(
                "[ERROR] %s file: The column name '%s' is case-sensitive - it should be '%s'.\n",
                fileType, headerName, headers[i]);
        continue;
      }
      state.getOutputLogStream()
          .printf(
              "[ERROR] %s file: The column name '%s' should not appear in the position it does - "
              + "'%s' should appear here instead.", fileType, headerName, headers[i]);
    }
  }

  private void checkBody(@Nonnull final List<List<String>> multiList, final int headerCount,
      @Nonnull final String fileType) {
    int lineNum = 0;
    for (final List<String> line : multiList) {
      lineNum++;
      if (lineNum == 1) {
        continue;
      }
      if (line.isEmpty()) {
        if (state.getProgramMode() != ProgramMode.LINT) {
          state.getOutputLogStream()
              .printf("[WARNING] %s file: Line %d is blank.\n", fileType, lineNum);
        }
        continue;
      }
      if (line.size() != headerCount) {
        state.getOutputLogStream()
            .printf(
                "[ERROR] %s file: Line %d (%s) has the wrong number of columns - expected %d, "
                + "but found %d.\n",
                fileType, lineNum, line.get(0), headerCount, line.size());
      }

      int colNum = 0;
      for (final String column : line) {
        colNum++;
        if (!column.equals(column.trim())) {
          state.getOutputLogStream()
              .printf(
                  "[WARNING] %s file: Line %d (%s) Column %d (%s) begins or ends with whitespace - "
                  + "this will be trimmed when uploaded.\n",
                  fileType, lineNum, line.get(0), colNum, column);
        }
      }
    }
  }

  public Collection<InputUser> generateUserModels() {
    state.getOutputLogStream().printf("[INFO] Building InputUser objects:\n");

    final Collection<InputUser> objects = new HashSet<>();
    final List<String> headerLine = users.iterator().next();
    final List<String> metadataHeaders = headerLine.subList(8, headerLine.size());

    for (final List<String> line : users.subList(1, users.size())) {
      if (!line.isEmpty()) {
        objects.add(new InputUser(state, orgParams, line, metadataHeaders));
      }
    }

    state.getOutputLogStream().printf("[INFO] + Built %d InputUser object(s).\n", objects.size());
    return objects;
  }

  public Collection<InputGroup> generateGroupModels() {
    state.getOutputLogStream().printf("[INFO] Building InputGroup objects:\n");

    final Collection<InputGroup> objects = new HashSet<>();

    for (final List<String> line : groups.subList(1, groups.size())) {
      if (!line.isEmpty()) {
        objects.add(new InputGroup(state, line));
      }
    }

    state.getOutputLogStream().printf("[INFO] + Built %d InputGroup object(s).\n", objects.size());
    return objects;
  }

  public Collection<InputGroupRole> generateGroupRoleModels() {
    state.getOutputLogStream().printf("[INFO] Building InputGroupRole objects:\n");

    final Collection<InputGroupRole> objects = new HashSet<>();

    for (final List<String> line : groupRoles.subList(1, groupRoles.size())) {
      if (!line.isEmpty()) {
        objects.add(new InputGroupRole(state, line));
      }
    }

    state.getOutputLogStream()
        .printf("[INFO] + Built %d InputGroupRole object(s).\n", objects.size());
    return objects;
  }

  public Collection<InputUserGroup> generateUserGroupModels() {
    state.getOutputLogStream().printf("[INFO] Building InputUserGroup objects:\n");

    final Collection<InputUserGroup> objects = new HashSet<>();

    for (final List<String> line : userGroups.subList(1, userGroups.size())) {
      if (!line.isEmpty()) {
        objects.add(new InputUserGroup(state, line));
      }
    }

    state.getOutputLogStream()
        .printf("[INFO] + Built %d InputUserGroup object(s).\n", objects.size());
    return objects;
  }

  public Collection<InputUserRelationship> generateUserRelationshipModels() {
    state.getOutputLogStream().printf("[INFO] Building InputUserRelationship objects:\n");

    final Collection<InputUserRelationship> objects = new HashSet<>();

    for (final List<String> line : userRelationships.subList(1, userRelationships.size())) {
      if (!line.isEmpty()) {
        objects.add(new InputUserRelationship(state, line));
      }
    }

    state.getOutputLogStream()
        .printf("[INFO] + Built %d InputUserRelationship object(s).\n", objects.size());
    return objects;
  }
}
