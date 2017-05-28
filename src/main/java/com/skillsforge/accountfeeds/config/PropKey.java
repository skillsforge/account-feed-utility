package com.skillsforge.accountfeeds.config;

/**
 * @author aw1459
 * @date 26-May-2017
 */
public enum PropKey {

  USERS_FILENAME("users-filename", "Users"),
  USER_GROUPS_FILENAME("user-groups-filename", "UserGroups"),
  USER_RELATIONSHIPS_FILENAME("user-relationships-filename", "UserRelationships"),
  GROUPS_FILENAME("groups-filename", "Groups"),
  GROUP_ROLES_FILENAME("group-roles-filename", "GroupRoles"),
  SOURCE_DIR("source-dir", "Source Directory"),
  DEST_DIR("dest-dir", "Destination Directory"),
  STATE_FILENAME("state-filename", "State File"),
  OUTPUT_LOG("output-log", "Output Log"),
  URL("url", null),
  TOKEN("token", null);

  private final String argName;
  private final String fileDescription;

  PropKey(final String cmdlineArg, String fileDesc) {
    this.argName = cmdlineArg;
    this.fileDescription = fileDesc;
  }

  public String argName() {
    return argName;
  }

  public String getFileDescription() {
    return fileDescription;
  }
}
