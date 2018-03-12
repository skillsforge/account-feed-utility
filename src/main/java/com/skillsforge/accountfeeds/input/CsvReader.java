package com.skillsforge.accountfeeds.input;

import com.skillsforge.accountfeeds.config.ProgramState;
import com.skillsforge.accountfeeds.exceptions.CsvCheckedException;

import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.Contract;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.skillsforge.accountfeeds.config.LogLevel.ERROR;
import static com.skillsforge.accountfeeds.config.LogLevel.WARN;

/**
 * @author alexw
 * @date 25-Nov-2016
 */
public class CsvReader extends BufferedReader {
  @Nonnull
  private static final Pattern QUOTE_MATCHER = Pattern.compile("\\A\"(.*)\"\\z");
  @Nonnull
  private final ProgramState state;

  private int lineNum = 1;

  public CsvReader(@Nonnull final Reader reader, @Nonnull final ProgramState state) {
    super(reader);
    this.state = state;
  }

  @Nonnull
  @Contract(pure = true)
  public List<List<String>> readFile() throws IOException {
    final List<List<String>> fullFile = new LinkedList<>();

    List<String> thisLine = null;
    do {
      try {
        thisLine = parseLine();
      } catch (CsvCheckedException e) {
        state.log("CR.rf.1", WARN, "Skipping line %d due to CSV parsing exception: %s", lineNum,
            e.getLocalizedMessage());
        continue;
      }
      if (thisLine != null) {
        fullFile.add(thisLine);
      }
    } while (thisLine != null);

    return Collections.unmodifiableList(new ArrayList<>(fullFile));
  }

  @SuppressWarnings({
      "OverlyComplexMethod",
      "OverlyLongMethod",
      "MethodWithMultipleLoops",
      "NumericCastThatLosesPrecision"
  })
  @Nullable
  public List<String> parseLine() throws IOException, CsvCheckedException {

    final List<String> result = new LinkedList<>();
    CsvStates stateMachine = CsvStates.START_OF_FIELD;

    int fieldNum = 1;
    boolean startOfLine = true;
    final StringBuilder thisField = new StringBuilder();
    do {
      int nextRead = this.read();
      char nextChar = (char) nextRead;
      if (nextChar == '\r') {
        continue;
      }
      while (nextChar == '#') {
        this.readLine();
        nextRead = this.read();
        nextChar = (char) nextRead;
      }
      if ((nextRead == -1) && startOfLine) {
        return null;
      }

      //noinspection SwitchStatementDensity
      switch (stateMachine) {
        case START_OF_FIELD:
          if ((nextRead == -1) || (nextChar == '\n')) {
            lineNum++;
            result.add(thisField.toString());
            return startOfLine ? Collections.emptyList() : Collections.unmodifiableList(result);
          }
          startOfLine = false;

          if (nextChar == ',') {
            result.add("");
          } else if (nextChar == '\"') {
            thisField.append('\"');
            stateMachine = CsvStates.LEXING_QUOTED_FIELD;
          } else {
            thisField.append(nextChar);
            stateMachine = CsvStates.LEXING_UNQUOTED_FIELD;
          }
          break;

        case LEXING_UNQUOTED_FIELD:
          if ((nextRead == -1) || (nextChar == '\n')) {
            lineNum++;
            result.add(StringEscapeUtils.unescapeCsv(thisField.toString()));
            return Collections.unmodifiableList(result);
          }
          if (nextChar == ',') {
            stateMachine = CsvStates.START_OF_FIELD;
            result.add(StringEscapeUtils.unescapeCsv(thisField.toString()));
            thisField.setLength(0);
            fieldNum++;
          } else if (nextChar == '\"') {
            state.log("CR.pl.1", ERROR,
                "CSV (Line %d, Field %d): Unescaped quotation mark or leading characters "
                + "before quoted field.\n", lineNum, fieldNum);
            throw new CsvCheckedException("[ERROR] CSV (Line " + lineNum + ", Field " + fieldNum
                                          + "): Unescaped quotation mark or leading character "
                                          + "before quoted field.");
          } else {
            thisField.append(nextChar);
          }
          break;

        case LEXING_QUOTED_FIELD:
          if (nextRead == -1) {
            state.log("CR.pl.2", ERROR, "CSV (Line %d, Field %d): Unterminated quoted field.",
                lineNum, fieldNum);
            throw new CsvCheckedException("[ERROR] CSV (Line " + lineNum + ", Field " + fieldNum
                                          + "): Unterminated quoted field.");
          }
          if (nextChar == '\n') {
            lineNum++;
/*
            state.log(null, INFO,
                "CSV (Line %d, Field %d): Quoted field contains newline - was this "
                + "intentional?",
                lineNum, fieldNum);
*/
          } else if (nextChar == '\"') {
            thisField.append('\"');
            stateMachine = CsvStates.ENDING_QUOTED_FIELD;
          } else {
            thisField.append(nextChar);
          }
          break;

        case ENDING_QUOTED_FIELD:
          if ((nextRead == -1) || (nextChar == '\n')) {
            lineNum++;
            result.add(stripEnclosingQuotes(StringEscapeUtils.unescapeCsv(thisField.toString())));
            return Collections.unmodifiableList(result);
          }
          if (nextChar == ',') {
            stateMachine = CsvStates.START_OF_FIELD;
            result.add(stripEnclosingQuotes(StringEscapeUtils.unescapeCsv(thisField.toString())));
            thisField.setLength(0);
            fieldNum++;
          } else if (nextChar == '\"') {
            thisField.append('\"');
            stateMachine = CsvStates.LEXING_QUOTED_FIELD;
          } else {
            state.log("CR.pl.3", ERROR,
                "CSV (Line %d, Field %d): Unescaped quotation mark or trailing character "
                + "after quoted field.",
                lineNum, fieldNum);
            throw new CsvCheckedException("[ERROR] CSV (Line " + lineNum + ", Field " + fieldNum
                                          + "): Unescaped quotation mark or trailing character "
                                          + "after quoted field.");
          }
          break;
      }
    } while (true);
  }

  public static String stripEnclosingQuotes(@Nonnull final CharSequence quotedString) {
    return QUOTE_MATCHER.matcher(quotedString).replaceAll("$1");
  }

  private enum CsvStates {
    START_OF_FIELD,
    LEXING_UNQUOTED_FIELD,
    LEXING_QUOTED_FIELD,
    ENDING_QUOTED_FIELD
  }
}
