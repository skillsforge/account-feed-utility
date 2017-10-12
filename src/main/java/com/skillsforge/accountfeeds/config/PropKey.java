package com.skillsforge.accountfeeds.config;

import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
  TOKEN("token", null),
  ORG_ALIAS("org-alias", null),
  FEED_ID("feed-id", null),
  EMAIL_LIST("email-to", null),
  EMAIL_SUBJECT("email-subject", null),
  USERNAME_CHANGES("allow-username-changes", null),
  ACCOUNT_EXPIRE_DELAY("account-expiry-days", null),
  RELATIONSHIP_EXPIRE_DELAY("relationship-expiry-days", null);

  @Nonnull
  private final String argName;
  @Nullable
  private final String fileDescription;

  PropKey(@Nonnull final String cmdlineArg, @Nullable final String fileDesc) {
    this.argName = cmdlineArg;
    this.fileDescription = fileDesc;
  }

  @Nonnull
  @Contract(pure = true)
  public String argName() {
    return argName;
  }

  @Nullable
  @Contract(pure = true)
  public String getFileDescription() {
    return fileDescription;
  }
}
