package com.skillsforge.accountfeeds.config;

import com.skillsforge.accountfeeds.input.Patterns;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.skillsforge.accountfeeds.config.LogLevel.ERROR;
import static com.skillsforge.accountfeeds.config.LogLevel.INFO;
import static com.skillsforge.accountfeeds.config.LogLevel.WARN;

/**
 * @author aw1459
 * @date 27-May-2017
 */
@SuppressWarnings({
    "TypeMayBeWeakened",
    "ClassWithTooManyFields",
    "MethodWithMultipleLoops",
    "OverlyComplexMethod"
    , "MethodWithMoreThanThreeNegations"
    , "OverlyLongMethod"
})
public class OrganisationParameters {
  @Nonnull
  public static final String ENV_SF_TOKEN = "SF_TOKEN";

  private static final long MINIMUM_SUPPORTED_TARGET_VERSION = 5_009_012_000L;

  private static final int STATE_CONFIG_VERSION = 1;

  @Nonnull
  private static final Set<String> defaultGroupRoles =
      new HashSet<>(Arrays.asList("ROLE_PGR", "ROLE_EM_PROVIDER", "ROLE_EM_FACILITATOR"));
  @Nonnull
  private static final Set<String> defaultRelationshipRoles =
      new HashSet<>(Arrays.asList("ROLE_GRADADMIN", "ROLE_SUPERVISOR"));
  @Nonnull
  private final Map<String, Long> headcountLimits = new HashMap<>();
  @Nonnull
  private final Map<String, Map<String, Long>> minimumRequiredRelationships = new HashMap<>();
  @Nonnull
  private final Map<String, Map<String, Long>> maximumRequiredRelationships = new HashMap<>();
  @Nonnull
  private final Set<String> groupRoles = new HashSet<>();
  @Nonnull
  private final Set<String> relationshipRoles = new HashSet<>();
  @Nonnull
  private final Map<PropKey, String> uploadParams = new EnumMap<>(PropKey.class);
  @Nonnull
  private final Map<String, Pattern> metadataPatternMap = new HashMap<>();
  private int targetVersionMajor = 5;
  private int targetVersionMinor = 9;
  private int targetVersionRevision = 12;
  private int targetVersionBetaLevel = 0;
  private long targetVersion = 5_009_012_000L;
  @Nonnull
  private String organisationName = "a Default SkillsForge Instance";
  @Nonnull
  private Patterns patterns = new Patterns(targetVersion);

  public OrganisationParameters(
      @Nonnull final ProgramState state) {

    final File stateFile = state.getFile(FileKey.STATE_FILE);
    if (stateFile == null) {
      groupRoles.addAll(defaultGroupRoles);
      relationshipRoles.addAll(defaultRelationshipRoles);
      state.log(null, INFO,
          "Not using any organisation-specific information - targeting %s running "
          + "SkillsForge version %d.%d.%d.\n",
          organisationName, targetVersionMajor, targetVersionMinor,
          targetVersionRevision);
      return;
    }

    try (InputStream stateStream = new FileInputStream(stateFile)) {
      final JSONObject stateConfig = new JSONObject(new JSONTokener(stateStream));

      final int stateFileVersion = stateConfig.getInt("stateFileVersion");
      if (stateFileVersion != STATE_CONFIG_VERSION) {
        state.log("OP.1", ERROR,
            "The state file (%s) is designed for a different version of this utility: "
            + "found version %d, require version %d.\n",
            stateFile.getPath(), stateFileVersion, STATE_CONFIG_VERSION);
        state.setFatalErrorEncountered();
        return;
      }

      final JSONObject sfTargetVersion = stateConfig.getJSONObject("sfTargetVersion");
      targetVersionMajor = sfTargetVersion.getInt("major");
      targetVersionMinor = sfTargetVersion.getInt("minor");
      targetVersionRevision = sfTargetVersion.getInt("revision");
      targetVersionBetaLevel = sfTargetVersion.getInt("betaLevel");
      //noinspection OverlyComplexArithmeticExpression
      targetVersion =
          (((((targetVersionMajor * 1000L)
              + targetVersionMinor) * 1000L)
            + targetVersionRevision) * 1000L)
          + targetVersionBetaLevel;
      if (targetVersion < MINIMUM_SUPPORTED_TARGET_VERSION) {
        state.log("OP.2", WARN,
            "The version of the SkillsForge instance this configuration targets "
            + "(v%d.%d.%d-%d) is older than this utility can provide for.\n",
            targetVersionMajor, targetVersionMinor, targetVersionRevision, targetVersionBetaLevel);
      }

      patterns = new Patterns(targetVersion);

      organisationName = stateConfig.getString("organisationName");

      if (stateConfig.has("headcountLimits")) {
        final JSONObject headcountLimitsObject =
            stateConfig.getJSONObject("headcountLimits");

        headcountLimitsObject
            .keys()
            .forEachRemaining(
                key -> this.headcountLimits.putIfAbsent(key, headcountLimitsObject.getLong(key)));
      }

      if (stateConfig.has("minimumRequiredRelationships")) {
        final JSONObject minRequiredRelsObject =
            stateConfig.getJSONObject("minimumRequiredRelationships");

        minimumRequiredRelationships.putAll(
            minRequiredRelsObject.keySet().stream().collect(Collectors.toMap(
                subjectRole -> subjectRole,
                subjectRole -> {
                  final JSONObject holderRoleCounts =
                      minRequiredRelsObject.getJSONObject(subjectRole);

                  return holderRoleCounts.keySet().stream().collect(Collectors.toMap(
                      holderRole -> holderRole,
                      holderRoleCounts::getLong
                  ));
                }
            ))
        );
      }
      if (stateConfig.has("maximumRequiredRelationships")) {
        final JSONObject maxRequiredRelsObject =
            stateConfig.getJSONObject("maximumRequiredRelationships");

        maximumRequiredRelationships.putAll(
            maxRequiredRelsObject.keySet().stream().collect(Collectors.toMap(
                subjectRole -> subjectRole,
                subjectRole -> {
                  final JSONObject holderRoleCounts =
                      maxRequiredRelsObject.getJSONObject(subjectRole);

                  return holderRoleCounts.keySet().stream().collect(Collectors.toMap(
                      holderRole -> holderRole,
                      holderRoleCounts::getLong
                  ));
                }
            ))
        );
      }

      final JSONArray jsonRelationshipRoles = stateConfig.getJSONArray("relationshipRoles");
      final JSONArray jsonGroupRoles = stateConfig.getJSONArray("groupRoles");
      final int relRoleCount = jsonRelationshipRoles.length();
      for (int index = 0; index < relRoleCount; index++) {
        relationshipRoles.add(jsonRelationshipRoles.getString(index));
      }
      final int groupRoleCount = jsonGroupRoles.length();
      for (int index = 0; index < groupRoleCount; index++) {
        groupRoles.add(jsonGroupRoles.getString(index));
      }

      if (stateConfig.has("upload")) {
        final JSONObject uploadParamObject = stateConfig.getJSONObject("upload");

        final String urlStateFile = uploadParamObject.getString("url");
        final String urlCmdLine = state.getProperty(PropKey.URL);
        uploadParams.put(PropKey.URL, coalesce(urlCmdLine, urlStateFile));

        final String tokenStateFile = uploadParamObject.optString("token", null);
        final String tokenCmdLine = state.getProperty(PropKey.TOKEN);
        //noinspection CallToSystemGetenv
        final String tokenSysEnv = System.getenv(ENV_SF_TOKEN);
        uploadParams.put(PropKey.TOKEN,
            coalesce(tokenCmdLine, tokenStateFile, tokenSysEnv, "Invalid Token"));

        final String orgAliasStateFile = uploadParamObject.getString("orgAlias");
        final String orgAliasCmdLine = state.getProperty(PropKey.ORG_ALIAS);
        uploadParams.put(PropKey.ORG_ALIAS, coalesce(orgAliasCmdLine, orgAliasStateFile));

        final String feedIdStateFile = uploadParamObject.getString("feedId");
        final String feedIdCmdLine = state.getProperty(PropKey.FEED_ID);
        uploadParams.put(PropKey.FEED_ID, coalesce(feedIdCmdLine, feedIdStateFile));

        final JSONArray emailStateFileArray = uploadParamObject.getJSONArray("emailRecipients");
        final int length = emailStateFileArray.length();
        final String[] emailStateFileList = new String[length];
        for (int i = 0; i < length; i++) {
          emailStateFileList[i] = emailStateFileArray.getString(i);
        }
        final String emailStateFile = String.join(",", emailStateFileList);
        final String emailCmdLine = state.getProperty(PropKey.EMAIL_LIST);
        uploadParams.put(PropKey.EMAIL_LIST, coalesce(emailCmdLine, emailStateFile));

        final String emailSubjectStateFile = uploadParamObject.optString("emailSubject", null);
        final String emailSubjectCmdLine = state.getProperty(PropKey.EMAIL_SUBJECT);
        final String emailSubjectToUse = coalesce(emailSubjectCmdLine, emailSubjectStateFile);
        if (emailSubjectToUse != null) {
          uploadParams.put(PropKey.EMAIL_SUBJECT, emailSubjectToUse);
        }

        final Boolean usernameStateFile = uploadParamObject.has("allowUsernameChanges")
                                          ? uploadParamObject.getBoolean("allowUsernameChanges")
                                          : null;
        final Boolean usernameCmdLine = (state.getProperty(PropKey.USERNAME_CHANGES) == null)
                                        ? null
                                        : true;
        final Boolean usernameChangeToUse = coalesce(usernameCmdLine, usernameStateFile);
        if ((usernameChangeToUse != null) && usernameChangeToUse) {
          uploadParams.put(PropKey.USERNAME_CHANGES, "on");
        }

        final String accountExpiryStateFile =
            uploadParamObject.has("accountExpiryDays")
            ? String.valueOf(uploadParamObject.getLong("accountExpiryDays"))
            : null;
        final String accountExpiryCmdLine = state.getProperty(PropKey.URL);
        final String accountExpiryToUse = coalesce(accountExpiryCmdLine, accountExpiryStateFile);
        if (accountExpiryToUse != null) {
          uploadParams.put(PropKey.ACCOUNT_EXPIRE_DELAY,
              String.valueOf(Long.parseUnsignedLong(accountExpiryToUse)));
        }

        final String relExpiryStateFile =
            uploadParamObject.has("relationshipExpiryDays")
            ? String.valueOf(uploadParamObject.getLong("relationshipExpiryDays"))
            : null;
        final String relExpiryCmdLine = state.getProperty(PropKey.URL);
        final String relExpiryToUse = coalesce(relExpiryCmdLine, relExpiryStateFile);
        if (relExpiryToUse != null) {
          uploadParams.put(PropKey.RELATIONSHIP_EXPIRE_DELAY,
              String.valueOf(Long.parseUnsignedLong(relExpiryToUse)));
        }
      }

      if (stateConfig.has("metadataPatterns")) {
        // If a User has a metadata key not in this map, it is accepted without question.
        // If the metadata key *is* in this map, a warning is generated if it doesn't match the
        // provided pattern.
        final JSONObject metadataPatterns = stateConfig.getJSONObject("metadataPatterns");
        metadataPatterns
            .keys()
            .forEachRemaining(
                key -> metadataPatternMap.put(
                    key.toLowerCase(),
                    Pattern.compile(metadataPatterns.getString(key))
                )
            );
      }
    } catch (@NotNull IOException | JSONException e) {
      state.log("OP.3", ERROR, "Problem reading state file (%s): %s.", stateFile.getPath(),
          e.getLocalizedMessage());
      state.setFatalErrorEncountered();
      return;
    }

    state.log(null, INFO, "Using state file provided for: "
                          + "%s targeting SkillsForge version %d.%d.%d-%d.\n",
        organisationName, targetVersionMajor, targetVersionMinor,
        targetVersionRevision, targetVersionBetaLevel);
  }

  @SafeVarargs
  @Nullable
  @Contract(pure = true)
  private static <T> T coalesce(
      @Nonnull final T... objects) {

    for (@Nullable final T o : objects) {
      if (o != null) {
        return o;
      }
    }
    return null;
  }

  @Nonnull
  public Map<String, Long> getHeadcountLimits() {
    return Collections.unmodifiableMap(headcountLimits);
  }

  @Nonnull
  public Map<String, Map<String, Long>> getMinimumRequiredRelationships() {
    return Collections.unmodifiableMap(minimumRequiredRelationships);
  }

  @Nonnull
  public Map<String, Map<String, Long>> getMaximumRequiredRelationships() {
    return Collections.unmodifiableMap(maximumRequiredRelationships);
  }

  @Nonnull
  @Contract(pure = true)
  public Set<String> getGroupRoles() {
    return Collections.unmodifiableSet(groupRoles);
  }

  @Nonnull
  @Contract(pure = true)
  public Set<String> getRelationshipRoles() {
    return Collections.unmodifiableSet(relationshipRoles);
  }

  @Contract(pure = true)
  public int getTargetVersionMajor() {
    return targetVersionMajor;
  }

  @Contract(pure = true)
  public int getTargetVersionMinor() {
    return targetVersionMinor;
  }

  @Contract(pure = true)
  public int getTargetVersionRevision() {
    return targetVersionRevision;
  }

  @Contract(pure = true)
  public int getTargetVersionBetaLevel() {
    return targetVersionBetaLevel;
  }

  @Contract(pure = true)
  public long getTargetVersion() {
    return targetVersion;
  }

  @Nonnull
  @Contract(pure = true)
  public String getOrganisationName() {
    return organisationName;
  }

  @Nullable
  @Contract(pure = true)
  public Pattern getMetadataPattern(
      @Nonnull final String key) {

    return metadataPatternMap.get(key.toLowerCase());
  }

  @Nonnull
  public Patterns getPatterns() {
    return patterns;
  }

  @Nonnull
  public Map<PropKey, String> getUploadParams() {
    return Collections.unmodifiableMap(uploadParams);
  }
}
