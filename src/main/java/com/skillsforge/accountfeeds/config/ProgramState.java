package com.skillsforge.accountfeeds.config;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jetbrains.annotations.Contract;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.skillsforge.accountfeeds.config.LogLevel.ERROR;

/**
 * @author aw1459
 * @date 26-May-2017
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr", "OverlyComplexMethod", "BooleanParameter"})
public class ProgramState {

  @Nonnull
  private static final Options checkOptions = new Options();
  @Nonnull
  private static final Options lintOptions = new Options();
  @Nonnull
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
        Option.builder("u").longOpt(PropKey.URL.argName()).hasArg().build();
    final Option optToken =
        Option.builder("t").longOpt(PropKey.TOKEN.argName()).hasArg().build();
    final Option optOrgAlias =
        Option.builder("a").longOpt(PropKey.ORG_ALIAS.argName()).hasArg().build();
    final Option optFeedId =
        Option.builder("i").longOpt(PropKey.FEED_ID.argName()).hasArg().build();
    final Option optEmailList =
        Option.builder("e").longOpt(PropKey.EMAIL_LIST.argName()).hasArg().build();
    final Option optEmailSubject =
        Option.builder("j").longOpt(PropKey.EMAIL_SUBJECT.argName()).hasArg().build();
    final Option optAccountUsernameChanges =
        Option.builder("c").longOpt(PropKey.USERNAME_CHANGES.argName()).build();
    final Option optAccountExpiry =
        Option.builder("x").longOpt(PropKey.ACCOUNT_EXPIRE_DELAY.argName()).hasArg().build();
    final Option optRelationshipExpiry =
        Option.builder("r").longOpt(PropKey.RELATIONSHIP_EXPIRE_DELAY.argName()).hasArg().build();

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
        .addOption(optStateFilename)
        .addOption(optOutputLog)
        .addOption(optUrl)
        .addOption(optToken)
        .addOption(optOrgAlias)
        .addOption(optFeedId)
        .addOption(optEmailList)
        .addOption(optEmailSubject)
        .addOption(optAccountUsernameChanges)
        .addOption(optAccountExpiry)
        .addOption(optRelationshipExpiry);
  }

  @Nonnull
  private final Map<PropKey, String> properties = new EnumMap<>(PropKey.class);
  @Nonnull
  private final Map<FileKey, File> files = new EnumMap<>(FileKey.class);
  @Nonnull
  private final PrintStream outputLogStream;
  @Nonnull
  private final Collection<LogLine> allLogLines = new LinkedList<>();
  @Nonnull
  private final Collection<LogLine> licenceLogLines = new LinkedList<>();
  @Nonnull
  private ProgramMode programMode = ProgramMode.HELP;
  private boolean fatalErrorEncountered = false;

  public ProgramState() {
    outputLogStream = System.out;
  }

  @SuppressWarnings({"OverlyLongMethod", "MethodWithMultipleLoops"})
  public ProgramState(
      @Nonnull final String[] programArgs) {

    // Presume the user doesn't know how to call the program
    if (programArgs.length == 0) {
      outputLogStream = System.out;
      showUsage();
      return;
    }

    // The first parameter should be a mode string.
    try {
      programMode = ProgramMode.valueOf(programArgs[0].trim().toUpperCase());
    } catch (IllegalArgumentException ignored) {
      outputLogStream = System.err;
      log(ERROR, "%s is not a valid mode.\n\n"
                 + "Run the following command for information on how to use this utility.\n"
                 + "  java -jar account-feed-utility-<version>.jar help\n",
          programArgs[0]);
      setFatalErrorEncountered();
      return;
    }

    final Options optionsForMode;
    switch (programMode) {
      case HELP:
        outputLogStream = System.out;
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
      //noinspection UnnecessaryDefault
      default:
        outputLogStream = System.err;
        setFatalErrorEncountered();
        return;
    }

    // Read off the command-line args using the Apache cli library.
    final CommandLineParser parser = new DefaultParser();
    final CommandLine args;
    final String[] remainingArgs = Arrays.copyOfRange(programArgs, 1, programArgs.length);
    try {
      args = parser.parse(optionsForMode, remainingArgs);
    } catch (ParseException e) {
      outputLogStream = System.err;
      log(ERROR, "Processing arguments:\n  %s\n\n"
                 + "Run the following command for information on how to use this utility.\n"
                 + "  java -jar account-feed-utility-<version>.jar help\n",
          e.getLocalizedMessage());
      setFatalErrorEncountered();
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

    // Open all the necessary files:
    for (final FileKey key : FileKey.values()) {
      files.put(key, openFileWithAccessCheck(key));
    }

    final File outputLogFile = files.get(FileKey.LOG);
    final PrintStream outputStream;
    try {
      outputStream = (outputLogFile == null) ? System.out : new PrintStream(outputLogFile, "UTF-8");
    } catch (FileNotFoundException e) {
      outputLogStream = System.err;
      log(ERROR, "Could not open output log file:\n  %s\n", e.getLocalizedMessage());
      setFatalErrorEncountered();
      return;
    } catch (UnsupportedEncodingException e) {
      outputLogStream = System.err;
      log(ERROR, "Could not set UTF-8 encoding on output log file:\n  %s\n",
          e.getLocalizedMessage());
      setFatalErrorEncountered();
      return;
    }

    outputLogStream = outputStream;
  }

  @Nullable
  @Contract(pure = true, value = "null -> null")
  private File openFileWithAccessCheck(
      @Nullable final FileKey fileKey) {

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

        log(ERROR, "Could not create: %s (%s%s): %s.\n",
            fileKey.getFilePathProp().getFileDescription(),
            (parentDir == null) ? "" : (parentDir + '/'),
            filename,
            ioe.getLocalizedMessage());
        setFatalErrorEncountered();
        return null;
      }
    }

    if (!hasAccess(fileKey.getAccessType(), file)) {
      log(ERROR, "Could not %s: %s (%s%s).\n",
          fileKey.getAccessType(),
          fileKey.getFilePathProp().getFileDescription(),
          (parentDir == null) ? "" : (parentDir + '/'),
          filename);
      setFatalErrorEncountered();
      return null;
    }
    return file;
  }

  @Contract(pure = true)
  private static boolean hasAccess(
      @Nonnull final AccessType accessType,
      @Nonnull final File file) {

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
        + "    -p --state-file=<path>    Path to the instance-specific 'state' file.\n"
        + "    -o --output-log=<path>    File to log problems to (defaults to stdout).\n\n"
        + ""
        + "    The following options override any options specified in the state file.\n"
        + "    -u --url=<url>            Address of the SkillsForge account upload application.\n"
        + "    -t --token=<token>        Pre-shared token to authenticate the account upload.\n"
        + "                              If not specified, the SF_TOKEN environment var is used.\n"
        + "    -a --org-alias=<org>      Organisation ID for newly created accounts.\n"
        + "    -i --feed-id=<id>                  Account feed identifier.\n"
        + "    -e --email-to=<addr[,addr[,...]]>  Report address list (must not contain spaces).\n"
        + "    -j --email-subject=<subject>       Subject for the report email - %RESULT% will\n"
        + "                                       be replaced with a summary.\n"
        + "    -c --allow-username-changes        Permits a user in a feed (identified by their\n"
        + "                                       user ID) to update their login username to a\n"
        + "                                       new value.\n"
        + "    -x --account-expiry-days      The number of days before an account not appearing \n"
        + "                                  in this feed is archived.\n"
        + "    -r --relationship-expiry-days The number of days before a relationship not within \n"
        + "                                  this feed is deleted.\n"
        + "\n\n"
        + "The following options apply to all modes:\n"
        + "    --users-filename=<name>               Alternate filename of Users.csv\n"
        + "    --user-groups-filename=<name>         Alternate filename of UserGroups.csv\n"
        + "    --user-relationships-filename=<name>  Alternate filename of UserRelationships.csv\n"
        + "    --groups-filename=<name>              Alternate filename of Groups.csv\n"
        + "    --group-roles-filename=<name>         Alternate filename of GroupRoles.csv\n"
        + '\n');
  }

  public final void setFatalErrorEncountered() {
    this.fatalErrorEncountered = true;
  }

  public final void log(
      @Nonnull final LogLevel lvl,
      @Nonnull final String fmt,
      final Object... args) {

    allLogLines.add(new LogLine(lvl, fmt, args));
  }

  public final void licenceLog(
      @Nonnull final LogLevel lvl,
      @Nonnull final String fmt,
      final Object... args) {

    licenceLogLines.add(new LogLine(lvl, fmt, args));
  }

  @Nullable
  @Contract(pure = true, value = "null -> null")
  public File getFile(
      @Nullable final FileKey key) {

    return files.get(key);
  }

  @Nonnull
  @Contract(pure = true)
  public ProgramMode getProgramMode() {
    return programMode;
  }

  @Contract(pure = true)
  public boolean hasFatalErrorBeenEncountered() {
    return this.fatalErrorEncountered;
  }

  @Nullable
  @Contract(pure = true, value = "null -> null")
  public String getProperty(
      @Nullable final PropKey key) {

    return properties.get(key);
  }

  public final void log(
      @Nonnull final LogLevel lvl,
      @Nonnull final String str) {

    allLogLines.add(new LogLine(lvl, str));
  }

  public final void log(
      @Nonnull final LogLevel lvl,
      final boolean lintable,
      @Nonnull final String fmt,
      final Object... args) {

    allLogLines.add(new LogLine(lvl, lintable, fmt, args));
  }

  @SuppressWarnings("resource")
  public void renderLog() {
    outputLogStream.printf("Feed Utility Results: Warnings: [%d], Errors: [%d]\n"
                           + "=========================================================\n\n",
        allLogLines.stream().filter(LogLine::isWarning).count(),
        allLogLines.stream().filter(LogLine::isError).count()
    );

    if (!licenceLogLines.isEmpty()) {
      outputLogStream.printf("Licencing Information:\n"
                             + "======================\n");
      licenceLogLines.forEach(logLine -> logLine.outputLogLine(outputLogStream));
    }

    if (!allLogLines.isEmpty()) {
      outputLogStream.printf("\nAccount Feed Utility Output:\n"
                             + "============================\n");
      allLogLines.forEach(logLine -> logLine.outputLogLine(outputLogStream));
    }
  }
}
