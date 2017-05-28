package com.skillsforge.accountfeeds.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author aw1459
 * @date 26-May-2017
 */
public enum FileKey {
  INPUT_USERS(PropKey.SOURCE_DIR, PropKey.USERS_FILENAME, AccessType.READ_FILE),
  INPUT_USER_GROUPS(PropKey.SOURCE_DIR, PropKey.USER_GROUPS_FILENAME, AccessType.READ_FILE),
  INPUT_USER_RELATIONSHIPS(PropKey.SOURCE_DIR, PropKey.USER_RELATIONSHIPS_FILENAME,
      AccessType.READ_FILE),
  INPUT_GROUPS(PropKey.SOURCE_DIR, PropKey.GROUPS_FILENAME, AccessType.READ_FILE),
  INPUT_GROUP_ROLES(PropKey.SOURCE_DIR, PropKey.GROUP_ROLES_FILENAME, AccessType.READ_FILE),

  OUTPUT_USERS(PropKey.DEST_DIR, PropKey.USERS_FILENAME, AccessType.WRITE_FILE),
  OUTPUT_USER_GROUPS(PropKey.DEST_DIR, PropKey.USER_GROUPS_FILENAME, AccessType.WRITE_FILE),
  OUTPUT_USER_RELATIONSHIPS(PropKey.DEST_DIR, PropKey.USER_RELATIONSHIPS_FILENAME,
      AccessType.WRITE_FILE),
  OUTPUT_GROUPS(PropKey.DEST_DIR, PropKey.GROUPS_FILENAME, AccessType.WRITE_FILE),
  OUTPUT_GROUP_ROLES(PropKey.DEST_DIR, PropKey.GROUP_ROLES_FILENAME, AccessType.WRITE_FILE),

  SOURCE_DIR(null, PropKey.SOURCE_DIR, AccessType.READ_DIR),
  DEST_DIR(null, PropKey.DEST_DIR, AccessType.READ_DIR),
  STATE_FILE(null, PropKey.STATE_FILENAME, AccessType.READ_FILE),
  LOG(null, PropKey.OUTPUT_LOG, AccessType.WRITE_FILE),;

  @Nonnull
  private final AccessType accessType;
  @Nonnull
  private final PropKey filePathProp;
  @Nullable
  private final PropKey parentPathProp;

  FileKey(@Nullable final PropKey parentPathProp, @Nonnull final PropKey filePathProp,
      @Nonnull final AccessType accessType) {
    this.parentPathProp = parentPathProp;
    this.filePathProp = filePathProp;
    this.accessType = accessType;
  }

  @Nullable
  public PropKey getParentPathProp() {
    return parentPathProp;
  }

  @Nonnull
  public PropKey getFilePathProp() {
    return filePathProp;
  }

  @Nonnull
  public AccessType getAccessType() {
    return accessType;
  }
}
