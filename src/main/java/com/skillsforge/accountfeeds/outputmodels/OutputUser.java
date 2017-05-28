package com.skillsforge.accountfeeds.outputmodels;

import com.skillsforge.accountfeeds.config.ProgramState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nonnull;

/**
 * @author aw1459
 * @date 27-May-2017
 */
@SuppressWarnings({"FieldNotUsedInToString", "TypeMayBeWeakened"})
public class OutputUser {

  @Nonnull
  private final String userId;
  @Nonnull
  private final String userName;
  @Nonnull
  private final String email;
  @Nonnull
  private final String title;
  @Nonnull
  private final String forename;
  @Nonnull
  private final String surname;
  @SuppressWarnings("NegativelyNamedBooleanVariable")
  private final boolean disabled;
  private final boolean archived;
  @Nonnull
  private final Map<String, String> metadata = new HashMap<>();
  @Nonnull
  private final Set<OutputUserGroup> userGroups = new HashSet<>();
  @Nonnull
  private final Set<String> groupNames = new HashSet<>();
  @Nonnull
  private final Set<OutputUserRelationship> userRelationships = new HashSet<>();
  @Nonnull
  private final Map<String, Set<String>> relationshipRoleToUserIds = new HashMap<>();

  @SuppressWarnings({"BooleanParameter", "NegativelyNamedBooleanVariable"})
  public OutputUser(@Nonnull final String userId, @Nonnull final String userName,
      @Nonnull final String email, @Nonnull final String title, @Nonnull final String forename,
      @Nonnull final String surname, final boolean disabled, final boolean archived,
      @Nonnull final Map<String, String> metadata) {
    this.userId = userId;
    this.userName = userName;
    this.email = email;
    this.title = title;
    this.forename = forename;
    this.surname = surname;
    this.disabled = disabled;
    this.archived = archived;
    this.metadata.putAll(metadata);
  }

  @Nonnull
  public String getUserId() {
    return userId;
  }

  @Override
  public String toString() {
    return String.format("User['%s','%s','%s','%s','%s','%s','%s','%s',meta=%s,group=%s,rel=%s]",
        userId, userName, email, title, forename, surname, disabled ? "disabled" : "enabled",
        archived ? "archived" : "active", metadata.toString(), userGroups.toString(),
        userRelationships.toString());
  }

  public void addGroup(@Nonnull  final ProgramState state,
      @Nonnull final OutputUserGroup userGroup) {
    if (groupNames.contains(userGroup.getGroupAlias())) {
      state.getOutputLogStream()
          .printf("[WARNING] UserGroup mapping of '%s' -> '%s' is specified more than once.\n",
              userId, userGroup.getGroupAlias());
      return;
    }
    groupNames.add(userGroup.getGroupAlias());
    userGroups.add(userGroup);
  }

  public void addRelationshipHeldOverAnotherUser(@Nonnull final ProgramState state,
      @Nonnull final OutputUserRelationship rel) {
    if (relationshipRoleToUserIds.containsKey(rel.getRoleAliasLeft())) {
      if (relationshipRoleToUserIds.get(rel.getRoleAliasLeft()).contains(rel.getUserIdRight())) {
        state.getOutputLogStream()
            .printf(
                "[WARNING] UserRelationship mapping '%s'-[%s]->'%s' is specified more than once.\n",
                userId, rel.getRoleAliasLeft(), rel.getUserIdRight());
        return;
      }
    } else {
      relationshipRoleToUserIds.put(rel.getRoleAliasLeft(), new TreeSet<>());
    }
    relationshipRoleToUserIds.get(rel.getRoleAliasLeft()).add(rel.getUserIdRight());
    userRelationships.add(rel);
  }
}
