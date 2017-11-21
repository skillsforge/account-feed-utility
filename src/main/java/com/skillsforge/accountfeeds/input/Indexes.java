package com.skillsforge.accountfeeds.input;

import com.skillsforge.accountfeeds.config.OrganisationParameters;
import com.skillsforge.accountfeeds.config.ProgramState;
import com.skillsforge.accountfeeds.inputmodels.InputGroup;
import com.skillsforge.accountfeeds.inputmodels.InputUser;
import com.skillsforge.accountfeeds.outputmodels.OutputGroup;
import com.skillsforge.accountfeeds.outputmodels.OutputUser;

import org.jetbrains.annotations.Contract;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.skillsforge.accountfeeds.config.LogLevel.ERROR;
import static com.skillsforge.accountfeeds.config.LogLevel.INFO;
import static com.skillsforge.accountfeeds.config.LogLevel.WARN;

/**
 * @author aw1459
 * @date 28-May-2017
 */
@SuppressWarnings({"TypeMayBeWeakened", "ClassWithTooManyFields"})
public class Indexes {

  @Nonnull
  private final Map<String, OutputUser> compiledUsersByUserId = new HashMap<>();
  @Nonnull
  private final Map<String, OutputGroup> compiledGroupsByGroupAlias = new HashMap<>();
  @Nonnull
  private final Map<String, InputUser> usersByUserIdLowerCase = new HashMap<>();
  @Nonnull
  private final Map<String, InputUser> usersByUserId = new HashMap<>();
  @Nonnull
  private final Map<String, InputUser> usersByUsername = new HashMap<>();
  @Nonnull
  private final Map<String, InputUser> usersByEmail = new HashMap<>();
  @Nonnull
  private final Map<String, InputGroup> groupsByAlias = new HashMap<>();
  @Nonnull
  private final Map<String, InputGroup> groupsByAliasUpperCase = new HashMap<>();
  @Nonnull
  private final Map<String, InputGroup> groupsByName = new HashMap<>();
  @Nonnull
  private final Set<String> rolesForGroups = new HashSet<>();
  @Nonnull
  private final Set<String> rolesForGroupsUpperCase = new HashSet<>();
  @Nonnull
  private final Set<String> rolesForRelationships = new HashSet<>();
  @Nonnull
  private final Set<String> rolesForRelationshipsUpperCase = new HashSet<>();

  @Nonnull
  private final OrganisationParameters orgParams;

  public Indexes(
      @Nonnull final OrganisationParameters orgParams,
      @Nonnull final ProgramState state,
      @Nonnull final Iterable<InputUser> users,
      @Nonnull final Iterable<InputGroup> groups,
      @Nonnull final Iterable<OutputUser> compiledUsers,
      @Nonnull final Iterable<OutputGroup> compiledGroups) {

    this.orgParams = orgParams;
    orgParams.getGroupRoles().forEach(role -> {
      rolesForGroups.add(role.trim());
      rolesForGroupsUpperCase.add(role.trim().toUpperCase());
    });
    orgParams.getRelationshipRoles().forEach(role -> {
      rolesForRelationships.add(role.trim());
      rolesForRelationshipsUpperCase.add(role.trim().toUpperCase());
    });
    compiledUsers.forEach(user -> compiledUsersByUserId.putIfAbsent(user.getUserId(), user));
    compiledGroups.forEach(
        group -> compiledGroupsByGroupAlias.putIfAbsent(group.getGroupAlias(), group));

    buildUserIndexes(state, users);

    buildGroupIndexes(state, groups);
  }

  private void buildGroupIndexes(
      @Nonnull final ProgramState state,
      @Nonnull final Iterable<InputGroup> groups) {

    state.log(INFO, "Building Group indexes:");

    for (final InputGroup group : groups) {
      final String groupAlias = group.getGroupAlias();
      if (groupAlias == null) {
        state.log(ERROR, "A group with no GroupAlias was encountered: '%s'.",
            group.toString());
      } else {
        if (groupsByAliasUpperCase.containsKey(groupAlias.trim().toUpperCase())) {
          state.log(ERROR, "There is more than one group with the GroupAlias '%s'.",
              groupAlias.trim());
        } else {
          groupsByAlias.put(groupAlias.trim(), group);
          groupsByAliasUpperCase.put(groupAlias.trim().toUpperCase(), group);
        }
      }
      final String groupName = group.getGroupName();
      if (groupName == null) {
        state.log(WARN, "A group with no GroupName was encountered: '%s'.",
            group.toString());
      } else {
        if (groupsByName.containsKey(groupName.trim())) {
          if (orgParams.getTargetVersion() >= 5_010_000_006L) {
            state.log(WARN, "There is more than one group with the GroupName '%s'.",
                groupName.trim());
          } else {
            state.log(ERROR,
                "There is more than one group with the GroupName '%s'.  Invokes a bug fixed in "
                + "5.10.0-BETA-6.",
                groupName.trim());
          }
        } else {
          groupsByName.put(groupName.trim(), group);
        }
      }
    }

    state.log(INFO, "+ Built Group indexes (%d by GroupAlias, %d by GroupName)",
        groupsByAlias.size(), groupsByName.size());
  }

  private void buildUserIndexes(
      @Nonnull final ProgramState state,
      @Nonnull final Iterable<InputUser> users) {

    state.log(INFO, "Building User indexes:");

    for (final InputUser user : users) {
      final String userId = user.getUserId();
      if (userId == null) {
        state.log(ERROR, "A user with no UserID was encountered: '%s'.", user.toString());
      } else if (!userId.trim().isEmpty()) {
        if (usersByUserIdLowerCase.containsKey(userId.trim().toLowerCase())) {
          state.log(ERROR, "There is more than one user with the UserID '%s'.",
              userId.trim());
        } else {
          usersByUserIdLowerCase.put(userId.trim().toLowerCase(), user);
          usersByUserId.put(userId.trim(), user);
        }
      }
      final String username = user.getUsername();
      if (username == null) {
        state.log(ERROR, "A user with no Username was encountered: '%s'.", user.toString());
      } else if (!username.trim().isEmpty()) {
        final String lowerCaseUsername = username.toLowerCase().trim();
        if (!lowerCaseUsername.equals(username.trim())) {
          state.log(WARN, "The Username '%s' will be lower-cased when uploaded.", username);
        }
        if (usersByUsername.containsKey(lowerCaseUsername)) {
          state.log(ERROR, "There is more than one user with the username '%s'.",
              lowerCaseUsername);
        } else {
          usersByUsername.put(lowerCaseUsername, user);
        }
      }
      final String email = user.getEmail();
      if (email == null) {
        state.log(ERROR, "A user with no Email address was encountered: '%s'.",
            user.toString());
      } else if (!email.trim().isEmpty()) {
        if (usersByEmail.containsKey(email)) {
          state.log(WARN, "There is more than one user with the email address '%s'.",
              email);
        } else {
          usersByEmail.put(email.trim(), user);
        }
      }
    }

    state.log(INFO, "+ Built User indexes (%d by UserId, %d by Username, %d by Email)",
        usersByUserId.size(), usersByUsername.size(), usersByEmail.size());
  }

  @Nullable
  @Contract(pure = true, value = "null -> null")
  public OutputUser getCompiledUserByUserId(
      @Nullable final String userId) {

    if (userId == null) {
      return null;
    }
    return compiledUsersByUserId.get(userId);
  }

  @Nullable
  @Contract(pure = true, value = "null -> null")
  public OutputGroup getCompiledGroupByGroupAlias(
      @Nullable final String groupAlias) {

    if (groupAlias == null) {
      return null;
    }
    return compiledGroupsByGroupAlias.get(groupAlias);
  }

  @Nullable
  @Contract(pure = true)
  public InputUser getUserByUserIdIgnoreCase(
      @Nonnull final String userId) {

    return usersByUserIdLowerCase.get(userId.trim().toLowerCase());
  }

  @Contract(pure = true)
  public boolean userIdHasMismatchedCase(
      @Nonnull final String userId) {

    return !usersByUserId.containsKey(userId.trim())
           && usersByUserIdLowerCase.containsKey(userId.trim().toLowerCase());
  }

  @Nullable
  @Contract(pure = true)
  public InputGroup getGroupByAliasIgnoreCase(
      @Nonnull final String groupAlias) {

    return groupsByAliasUpperCase.get(groupAlias.trim().toUpperCase());
  }

  @Contract(pure = true)
  public boolean groupAliasHasMismatchedCase(
      @Nonnull final String groupAlias) {

    return !groupsByAlias.containsKey(groupAlias.trim())
           && groupsByAliasUpperCase.containsKey(groupAlias.trim().toUpperCase());
  }

  @Contract(pure = true)
  public boolean rolesForRelationshipsContainsIgnoreCase(
      @Nonnull final String roleName) {

    return rolesForRelationshipsUpperCase.contains(roleName.trim().toUpperCase());
  }

  @Contract(pure = true)
  public boolean rolesForRelationshipsHasMismatchedCase(
      @Nonnull final String roleName) {

    return !rolesForRelationships.contains(roleName.trim())
           && rolesForRelationshipsUpperCase.contains(roleName.trim().toUpperCase());
  }

  @Nullable
  @Contract(pure = true)
  public String correctRelationshipRoleCase(
      @Nonnull final String roleName) {

    for (final String role : rolesForRelationships) {
      if (role.trim().equalsIgnoreCase(roleName.trim())) {
        return role;
      }
    }
    return null;
  }

  @Contract(pure = true)
  public boolean rolesForGroupsContainsIgnoreCase(
      @Nonnull final String roleName) {

    return rolesForGroupsUpperCase.contains(roleName.trim().toUpperCase());
  }

  @Contract(pure = true)
  public boolean rolesForGroupsHasMismatchedCase(
      @Nonnull final String roleName) {

    return !rolesForGroups.contains(roleName.trim()) && rolesForGroupsUpperCase.contains(
        roleName.trim().toUpperCase());
  }

  @Nullable
  @Contract(pure = true)
  public String correctGroupRoleCase(
      @Nonnull final String roleName) {

    for (final String role : rolesForGroups) {
      if (role.trim().equalsIgnoreCase(roleName.trim())) {
        return role;
      }
    }
    return null;
  }
}
