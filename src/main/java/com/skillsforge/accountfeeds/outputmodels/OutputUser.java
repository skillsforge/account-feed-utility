package com.skillsforge.accountfeeds.outputmodels;

import com.skillsforge.accountfeeds.config.ProgramState;

import org.apache.commons.text.StringEscapeUtils;
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
@SuppressWarnings({
    "FieldNotUsedInToString",
    "TypeMayBeWeakened",
    "ClassWithTooManyFields",
    "NegativelyNamedBooleanVariable"
    , "BooleanParameter"
})
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
  private final boolean disabled;
  private final boolean archived;
  @Nonnull
  private final Map<String, String> metadata = new HashMap<>();
  @Nonnull
  private final Set<OutputUserGroup> userGroups = new HashSet<>();
  @Nonnull
  private final Set<String> groupNames = new HashSet<>();
  @Nonnull
  private final Set<OutputUserRelationship> userRelationshipsHeld = new HashSet<>();
  @Nonnull
  private final Set<OutputUserRelationship> userRelationshipsSubject = new HashSet<>();
  @Nonnull
  private final Map<String, Set<String>> userRelationshipsHeldOverOtherUsers = new HashMap<>();
  @Nonnull
  private final Map<String, Set<String>> userRelationshipsThisUserIsASubjectOf = new HashMap<>();

  public OutputUser(
      @Nonnull final String userId,
      @Nonnull final String userName,
      @Nonnull final String email,
      @Nonnull final String title,
      @Nonnull final String forename,
      @Nonnull final String surname,
      final boolean disabled,
      final boolean archived,
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

  public boolean isArchived() {
    return archived;
  }

  @Nonnull
  @Contract(pure = true)
  public String getUserId() {
    return userId;
  }

  @Override
  @Nonnull
  @Contract(pure = true)
  public String toString() {
    return String.format("User['%s','%s','%s','%s','%s','%s','%s','%s',meta=%s,group=%s,"
                         + "relHeld=%s,relSubj=%s]",
        userId, userName, email, title, forename, surname, disabled ? "disabled" : "enabled",
        archived ? "archived" : "active", metadata.toString(), userGroups.toString(),
        userRelationshipsHeld.toString(),
        userRelationshipsSubject.toString());
  }

  public void addGroup(
      @Nonnull final ProgramState state,
      @Nonnull final OutputUserGroup userGroup) {

    if (groupNames.contains(userGroup.getGroupAlias())) {
      state.log("OU.ag.1", WARN, "UserGroup mapping is specified more than once: '%s' -> '%s'",
          userId, userGroup.getGroupAlias());
      return;
    }
    groupNames.add(userGroup.getGroupAlias());
    userGroups.add(userGroup);
  }

  public void addRelationshipHeldOverAnotherUser(
      @Nonnull final ProgramState state,
      @Nonnull final OutputUserRelationship rel) {

    if (userRelationshipsHeldOverOtherUsers.containsKey(rel.getRoleAliasLeft())) {
      if (userRelationshipsHeldOverOtherUsers
          .get(rel.getRoleAliasLeft())
          .contains(rel.getUserIdRight())) {
        state.log("OU.arhoau.1", WARN,
            "UserRelationship mapping is specified more than once: '%s'-[%s]->'%s'",
            userId, rel.getRoleAliasLeft(), rel.getUserIdRight());
        return;
      }
    } else {
      userRelationshipsHeldOverOtherUsers.put(rel.getRoleAliasLeft(), new TreeSet<>());
    }
    userRelationshipsHeldOverOtherUsers.get(rel.getRoleAliasLeft()).add(rel.getUserIdRight());
    userRelationshipsHeld.add(rel);
  }

  public void addRelationshipThisUserIsASubjectOf(
      @Nonnull final OutputUserRelationship rel) {

    if (userRelationshipsThisUserIsASubjectOf.containsKey(rel.getRoleAliasLeft())) {
      if (userRelationshipsThisUserIsASubjectOf
          .get(rel.getRoleAliasLeft())
          .contains(rel.getUserIdRight())) {
        return;
      }
    } else {
      userRelationshipsThisUserIsASubjectOf.put(rel.getRoleAliasLeft(), new TreeSet<>());
    }
    userRelationshipsThisUserIsASubjectOf.get(rel.getRoleAliasLeft()).add(rel.getUserIdRight());
    userRelationshipsSubject.add(rel);
  }

  @Nonnull
  @Contract(pure = true)
  public Stream<String> getMetadataKeys() {
    return metadata.keySet().stream();
  }

  @Nonnull
  @Contract(pure = true)
  private String getSortString() {
    return userId + ',' + userName + ',' + email;
  }

  @Nonnull
  @Contract(pure = true)
  public String getCsvRow(
      @Nonnull final Collection<String> metadataHeaders) {

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
  @Contract(pure = true)
  public Stream<OutputUserGroup> getGroups() {
    return userGroups.stream();
  }

  @Nonnull
  @Contract(pure = true)
  public Stream<OutputUserRelationship> getRelationshipsHeld() {
    return userRelationshipsHeld.stream();
  }

  @Nonnull
  @Contract(pure = true)
  public Stream<OutputUserRelationship> getRelationshipsSubject() {
    return userRelationshipsSubject.stream();
  }
}
