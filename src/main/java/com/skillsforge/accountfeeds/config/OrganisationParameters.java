package com.skillsforge.accountfeeds.config;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.skillsforge.accountfeeds.config.LogLevel.ERROR;
import static com.skillsforge.accountfeeds.config.LogLevel.INFO;
import static com.skillsforge.accountfeeds.config.LogLevel.WARN;

/**
 * @author aw1459
 * @date 27-May-2017
 */
@SuppressWarnings("TypeMayBeWeakened")
public class OrganisationParameters {

  private static final long MINIMUM_SUPPORTED_TARGET_VERSION = 5_009_012L;
  private static final int STATE_CONFIG_VERSION = 1;

  private static final Set<String> defaultGroupRoles =
      new HashSet<>(Arrays.asList("ROLE_PGR", "ROLE_EM_PROVIDER", "ROLE_EM_FACILITATOR"));
  private static final Set<String> defaultRelationshipRoles =
      new HashSet<>(Arrays.asList("ROLE_GRADADMIN", "ROLE_SUPERVISOR"));
  @Nonnull
  private final Set<String> groupRoles = new HashSet<>();
  @Nonnull
  private final Set<String> relationshipRoles = new HashSet<>();
  @Nonnull
  private final Map<String, Pattern> metadataPatternMap = new HashMap<>();

  private int targetVersionMajor = 5;
  private int targetVersionMinor = 9;
  private int targetVersionRevision = 12;
  private long targetVersion = 5_009_012L;
  private String organisation = "DEFAULT";
  private String organisationName = "a Default SkillsForge Instance";

  public OrganisationParameters(@Nonnull final ProgramState state) {
    final File stateFile = state.getFile(FileKey.STATE_FILE);
    if (stateFile == null) {
      groupRoles.addAll(defaultGroupRoles);
      relationshipRoles.addAll(defaultRelationshipRoles);
      state.log(INFO, "Not using any organisation-specific information - targeting %s (%s) running "
                      + "SkillsForge version %d.%d.%d.\n",
          organisationName, organisation, targetVersionMajor, targetVersionMinor,
          targetVersionRevision);
      return;
    }

    try (InputStream stateStream = new FileInputStream(stateFile)) {
      final JSONObject stateConfig = new JSONObject(new JSONTokener(stateStream));

      final int stateFileVersion = stateConfig.getInt("stateFileVersion");
      if (stateFileVersion != STATE_CONFIG_VERSION) {
        state.log(ERROR, "The state file (%s) is designed for a different version of this utility: "
                         + "found version %d, require version %d.\n",
            stateFile.getPath(), stateFileVersion, STATE_CONFIG_VERSION);
        state.setFatalErrorEncountered();
        return;
      }

      final JSONObject sfTargetVersion = stateConfig.getJSONObject("sfTargetVersion");
      targetVersionMajor = sfTargetVersion.getInt("major");
      targetVersionMinor = sfTargetVersion.getInt("minor");
      targetVersionRevision = sfTargetVersion.getInt("revision");
      targetVersion =
          (((targetVersionMajor * 1000L) + targetVersionMinor) * 1000L) + targetVersionRevision;
      if (targetVersion < MINIMUM_SUPPORTED_TARGET_VERSION) {
        state.log(WARN, "The version of the SkillsForge instance this configuration targets "
                        + "(v%d.%d.%d) is older than this utility can provide for.\n",
            targetVersionMajor, targetVersionMinor, targetVersionRevision);
      }

      organisation = stateConfig.getString("organisation");
      organisationName = stateConfig.getString("organisationName");

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
    } catch (IOException | JSONException e) {
      state.log(ERROR, "Problem reading state file (%s): %s.", stateFile.getPath(),
          e.getLocalizedMessage());
      state.setFatalErrorEncountered();
      return;
    }

    state.log(INFO, "Using state file provided for: "
                    + "%s (%s) targeting SkillsForge version %d.%d.%d.\n",
        organisationName, organisation, targetVersionMajor, targetVersionMinor,
        targetVersionRevision);
  }

  @Nonnull
  public Set<String> getGroupRoles() {
    return Collections.unmodifiableSet(groupRoles);
  }

  @Nonnull
  public Set<String> getRelationshipRoles() {
    return Collections.unmodifiableSet(relationshipRoles);
  }

  public int getTargetVersionMajor() {
    return targetVersionMajor;
  }

  public int getTargetVersionMinor() {
    return targetVersionMinor;
  }

  public int getTargetVersionRevision() {
    return targetVersionRevision;
  }

  public long getTargetVersion() {
    return targetVersion;
  }

  public String getOrganisation() {
    return organisation;
  }

  public String getOrganisationName() {
    return organisationName;
  }

  @Nullable
  public Pattern getMetadataPattern(@Nonnull final String key) {
    return metadataPatternMap.get(key.toLowerCase());
  }
}
