package com.skillsforge.accountfeeds.config;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author aw1459
 * @date 26-May-2017
 */
public class ProgramState {

  private static final String ENV_SF_TOKEN = "SF_TOKEN";

  private static final Options checkOptions = new Options();
  private static final Options lintOptions = new Options();
  private static final Options uploadOptions = new Options();

  static {
    final Option optUsers =
        Option.builder().longOpt(PropKey.USERS_FILENAME.argName()).hasArg().build();
    final Option optUserGroups =
        Option.builder().longOpt(PropKey.USER_GROUPS_FILENAME.argName()).hasArg().build();
    final Option optUserRel =
        Option.builder().longOpt(PropKey.USER_RELATIONSHIPS_FILENAME.argName()).hasArg().build();
    final Option optGroups =
        Option.builder().longOpt(PropKey.GROUPS_FILENAME.argName()).hasArg().build();
    final Option optGroupRoles =
        Option.builder().longOpt(PropKey.GROUP_ROLES_FILENAME.argName()).hasArg().build();
    final Option optSourceDir =
        Option.builder("s").longOpt(PropKey.SOURCE_DIR.argName()).hasArg().required().build();
    final Option optDestDir =
        Option.builder("d").longOpt(PropKey.DEST_DIR.argName()).hasArg().required().build();
    final Option optStateFilename =
        Option.builder("p").longOpt(PropKey.STATE_FILENAME.argName()).hasArg().build();
    final Option optOutputLog =
        Option.builder("o").longOpt(PropKey.OUTPUT_LOG.argName()).hasArg().build();
    final Option optUrl =
        Option.builder("u").longOpt(PropKey.URL.argName()).hasArg().required().build();
    final Option optToken =
        Option.builder("t").longOpt(PropKey.TOKEN.argName()).hasArg().build();

    checkOptions.addOption(optUsers)
        .addOption(optUserGroups)
        .addOption(optUserRel)
        .addOption(optGroups)
        .addOption(optGroupRoles)
        .addOption(optSourceDir)
        .addOption(optStateFilename)
        .addOption(optOutputLog);

    lintOptions.addOption(optUsers)
        .addOption(optUserGroups)
        .addOption(optUserRel)
        .addOption(optGroups)
        .addOption(optGroupRoles)
        .addOption(optSourceDir)
        .addOption(optDestDir)
        .addOption(optStateFilename)
        .addOption(optOutputLog);

    uploadOptions
        .addOption(optUsers)
        .addOption(optUserGroups)
        .addOption(optUserRel)
        .addOption(optGroups)
        .addOption(optGroupRoles)
        .addOption(optSourceDir)
        .addOption(optOutputLog)
        .addOption(optUrl)
        .addOption(optToken);
  }

  @Nonnull
  private final Map<PropKey, String> properties = new EnumMap<>(PropKey.class);
  @Nonnull
  private final Map<FileKey, File> files = new EnumMap<>(FileKey.class);

  @Nonnull
  private ProgramMode programMode = ProgramMode.INVALID;
  @Nonnull
  private PrintStream outputLogStream = System.out;

  public ProgramState(final String[] programArgs) {

    // Presume the user doesn't know how to call the program
    if (programArgs.length == 0) {
      showUsage();
      return;
    }

    // The first parameter should be a mode string.
    try {
      programMode = ProgramMode.valueOf(programArgs[0].trim().toUpperCase());
    } catch (IllegalArgumentException ignored) {
      System.err.printf("[ERROR] %s is not a valid mode.\n\n"
                        + "Run the following command for information on how to use this utility.\n"
                        + "  java -jar account-feed-utility-<version>.jar help\n\n",
          programArgs[0]);
      return;
    }

    final Options optionsForMode;
    switch (programMode) {
      case HELP:
        showUsage();
        return;
      case CHECK:
        optionsForMode = checkOptions;
        break;
      case LINT:
        optionsForMode = lintOptions;
        break;
      case UPLOAD:
        optionsForMode = uploadOptions;
        break;
      default:
        programMode = ProgramMode.INVALID;
        return;
    }

    // Read off the command-line args using the Apache cli library.
    final CommandLineParser parser = new DefaultParser();
    final CommandLine args;
    final String[] remainingArgs = Arrays.copyOfRange(programArgs, 1, programArgs.length);
    try {
      args = parser.parse(optionsForMode, remainingArgs);
    } catch (ParseException e) {
      System.err.printf("[ERROR] Processing arguments:\n  %s\n\n"
                        + "Run the following command for information on how to use this utility.\n"
                        + "  java -jar account-feed-utility-<version>.jar help\n\n",
          e.getLocalizedMessage());
      programMode = ProgramMode.INVALID;
      return;
    }

    // For each possible argument, store it to a properties map:
    for (final PropKey key : PropKey.values()) {
      properties.put(key, args.getOptionValue(key.argName()));
    }

    // Set up the defaults for the properties:
    properties.putIfAbsent(PropKey.USERS_FILENAME, "Users.csv");
    properties.putIfAbsent(PropKey.USER_GROUPS_FILENAME, "UserGroups.csv");
    properties.putIfAbsent(PropKey.USER_RELATIONSHIPS_FILENAME, "UserRelationships.csv");
    properties.putIfAbsent(PropKey.GROUPS_FILENAME, "Groups.csv");
    properties.putIfAbsent(PropKey.GROUP_ROLES_FILENAME, "GroupRoles.csv");
    properties.putIfAbsent(PropKey.TOKEN, System.getenv(ENV_SF_TOKEN));

    // Open all the necessary files:
    for (final FileKey key : FileKey.values()) {
      files.put(key, openFileWithAccessCheck(key));
    }

    final File outputLogFile = files.get(FileKey.LOG);
    try {
      outputLogStream =
          (outputLogFile == null) ? System.out : new PrintStream(outputLogFile, "UTF-8");
    } catch (FileNotFoundException e) {
      System.err.printf("[ERROR] Could not open output log file:\n  %s\n\n",
          e.getLocalizedMessage());
      programMode = ProgramMode.INVALID;
    } catch (UnsupportedEncodingException e) {
      System.err.printf("[ERROR] Could set UTF-8 encoding on output log file:\n  %s\n\n",
          e.getLocalizedMessage());
      programMode = ProgramMode.INVALID;
    }
  }

  @Nullable
  private File openFileWithAccessCheck(final FileKey fileKey) {
    if (fileKey == null) {
      return null;
    }

    final String parentDir = properties.get(fileKey.getParentPathProp());
    final String filename = properties.get(fileKey.getFilePathProp());

    if (filename == null) {
      return null;
    }
    if ((fileKey.getParentPathProp() != null) && (parentDir == null)) {
      return null;
    }

    final File file = new File(parentDir, filename);
    if ((fileKey.getAccessType() == AccessType.WRITE_FILE) && !hasAccess(fileKey.getAccessType(),
        file)) {
      try {
        //noinspection ResultOfMethodCallIgnored
        file.createNewFile();
      } catch (IOException ioe) {
        System.err.printf("[ERROR] Could not create: %s (%s%s): %s.\n",
            fileKey.getFilePathProp().getFileDescription(),
            (parentDir == null) ? "" : (parentDir + '/'),
            filename,
            ioe.getLocalizedMessage());
        programMode = ProgramMode.INVALID;
        return null;
      }
    }

    if (!hasAccess(fileKey.getAccessType(), file)) {
      System.err.printf("[ERROR] Could not %s: %s (%s%s).\n",
          fileKey.getAccessType(),
          fileKey.getFilePathProp().getFileDescription(),
          (parentDir == null) ? "" : (parentDir + '/'),
          filename);
      programMode = ProgramMode.INVALID;
      return null;
    }
    return file;
  }

  private static boolean hasAccess(@Nonnull final AccessType accessType, @Nonnull final File file) {
    switch (accessType) {
      case READ_DIR:
        return file.isDirectory() && file.canRead();
      case READ_FILE:
        return file.isFile() && file.canRead();
      case WRITE_DIR:
        return file.isDirectory() && file.canWrite();
      case WRITE_FILE:
        return file.isFile() && file.canWrite();
    }
    return false;
  }

  private static void showUsage() {
    System.err.print(
        "Usage:\n\n"
        + "\tjava -jar account-feed-utility-<version>.jar <mode> [option[, option[, ...]]]\n\n"
        + "Where mode is one of:\n\n"
        + "  help              Show this message.\n"
        + '\n'
        + "  check             Load all feed files and verify their syntax and contents.\n"
        + '\n'
        + "    -s --source-dir=<path>    The directory containing a set of 'well-named' files.\n"
        + "    -p --state-file=<path>    Path to the instance-specific 'state' file.\n"
        + "    -o --output-log=<path>    File to log problems to (defaults to stdout).\n"
        + '\n'
        + "  lint              Attempt to fix any correctable syntax errors and make the files\n"
        + "                    ready to upload to a SkillsForge instance.\n"
        + '\n'
        + "    -s --source-dir=<path>    The directory containing a set of 'well-named' files.\n"
        + "    -d --dest-dir=<path>      The directory where linted files are deposited.\n"
        + "    -p --state-file=<path>    Path to the instance-specific 'state' file.\n"
        + "    -o --output-log=<path>    File to log problems to (defaults to stdout).\n"
        + '\n'
        + "  upload            Begin an account sync on the specified SkillsForge instance.\n"
        + '\n'
        + "    -s --source-dir=<path>    The directory containing a set of 'well-named' files.\n"
        + "    -o --output-log=<path>    File to log problems to (defaults to stdout).\n"
        + "    -u --url=<url>            Address of the SkillsForge account upload application.\n"
        + "    -t --token=<token>        Pre-shared token to authenticate the account upload.\n"
        + "                              If not specified, the SF_TOKEN environment var is used.\n"
        + "\n\n"
        + "The following options apply to all modes:\n"
        + "    --users-filename=<name>               Alternate filename of Users.csv\n"
        + "    --user-groups-filename=<name>         Alternate filename of UserGroups.csv\n"
        + "    --user-relationships-filename=<name>  Alternate filename of UserRelationships.csv\n"
        + "    --groups-filename=<name>              Alternate filename of Groups.csv\n"
        + "    --group-roles-filename=<name>         Alternate filename of GroupRoles.csv\n"
        + '\n');
  }

  @Nonnull
  public PrintStream getOutputLogStream() {
    return outputLogStream;
  }

  @Nonnull
  public Map<FileKey, File> getFiles() {
    return Collections.unmodifiableMap(files);
  }

  public File getFile(FileKey key) {
    return files.get(key);
  }

  @Nonnull
  public ProgramMode getProgramMode() {
    return programMode;
  }

  public void setProgramMode(@Nonnull final ProgramMode programMode) {
    this.programMode = programMode;
  }

  @Nonnull
  public Map<PropKey, String> getProperties() {
    return Collections.unmodifiableMap(properties);
  }

  public String getProperty(PropKey key) {
    return properties.get(key);
  }
}
