package com.skillsforge.accountfeeds.outputmodels;

import com.skillsforge.accountfeeds.config.ProgramState;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.Contract;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import static com.skillsforge.accountfeeds.config.LogLevel.WARN;

/**
 * @author aw1459
 * @date 27-May-2017
 */
@SuppressWarnings({"FieldNotUsedInToString", "TypeMayBeWeakened"})
public class OutputUser {

  @Nonnull
  public static final Comparator<? super OutputUser> CSV_SORTER =
      (left, right) -> left.getSortString().compareToIgnoreCase(right.getSortString());

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
      state.log(WARN, "UserGroup mapping of '%s' -> '%s' is specified more than once.",
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
        state.log(WARN, "UserRelationship mapping '%s'-[%s]->'%s' is specified more than once.",
            userId, rel.getRoleAliasLeft(), rel.getUserIdRight());
        return;
      }
    } else {
      relationshipRoleToUserIds.put(rel.getRoleAliasLeft(), new TreeSet<>());
    }
    relationshipRoleToUserIds.get(rel.getRoleAliasLeft()).add(rel.getUserIdRight());
    userRelationships.add(rel);
  }

  @Nonnull
  public Set<String> getMetadataKeys() {
    return metadata.keySet();
  }

  @Nonnull
  @Contract(pure = true)
  private String getSortString() {
    return userId + ',' + userName + ',' + email;
  }

  @Nonnull
  public String getCsvRow(@Nonnull final Collection<String> metadataHeaders) {
    return StringEscapeUtils.escapeCsv(userId) + ',' +
           StringEscapeUtils.escapeCsv(userName) + ',' +
           StringEscapeUtils.escapeCsv(email) + ',' +
           StringEscapeUtils.escapeCsv(title) + ',' +
           StringEscapeUtils.escapeCsv(forename) + ',' +
           StringEscapeUtils.escapeCsv(surname) + ',' +
           disabled + ',' +
           archived + ',' +
           metadataHeaders.stream()
               .map(s -> StringEscapeUtils.escapeCsv(metadata.getOrDefault(s, "")))
               .collect(Collectors.joining(","));
  }

  @Nonnull
  public Stream<OutputUserGroup> getGroups() {
    return userGroups.stream();
  }

  @Nonnull
  public Stream<OutputUserRelationship> getRelationshipsHeld() {
    return userRelationships.stream();
  }
}
