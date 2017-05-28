package com.skillsforge.accountfeeds.input;

import com.skillsforge.accountfeeds.exceptions.CsvCheckedException;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author alexw
 * @date 25-Nov-2016
 */
public class CsvReader extends BufferedReader {
  private int lineNum = 1;

  public CsvReader(@Nonnull final Reader reader) {
    super(reader);
  }

  @Nonnull
  public List<List<String>> readFile(@Nonnull final PrintStream outputStream) throws IOException {
    final List<List<String>> fullFile = new LinkedList<>();

    List<String> thisLine = null;
    do {
      try {
        thisLine = parseLine(outputStream);
      } catch (CsvCheckedException e) {
        outputStream.printf("[WARNING] Skipping line %d due to CSV parsing exception: %s", lineNum,
            e.getLocalizedMessage());
        continue;
      }
      if (thisLine != null) {
        fullFile.add(thisLine);
      }
    } while (thisLine != null);

    return Collections.unmodifiableList(new ArrayList<>(fullFile));
  }

/*
  // This doesn't work because you can get commas in the middle of values.  But it's a lovely
  example of a lambda, so I left it in :)

  private static final Pattern commentStripper = Pattern.compile("#.*$");
  private static List<List<String>> readFileFromReader(@Nonnull final BufferedReader reader) {
    return reader.lines()
        .map(line -> commentStripper.matcher(line).replaceAll(""))
        .map(String::trim)
        .filter(line -> !line.isEmpty())
        .map(line -> Arrays.stream(line.split(",")).parallel()
            .map(StringEscapeUtils::unescapeCsv)
            .map(String::trim)
            .collect(Collectors.toList()))
        .collect(Collectors.toList());
  }
*/

  /**
   * @return
   * @throws IOException
   * @throws CsvCheckedException
   */
  @Nullable
  public List<String> parseLine(@Nonnull final PrintStream outputStream)
      throws IOException, CsvCheckedException {

    final List<String> result = new LinkedList<>();
    CsvStates state = CsvStates.START_OF_FIELD;

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

      switch (state) {
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
            state = CsvStates.LEXING_QUOTED_FIELD;
          } else {
            thisField.append(nextChar);
            state = CsvStates.LEXING_UNQUOTED_FIELD;
          }
          break;
        case LEXING_UNQUOTED_FIELD:
          if ((nextRead == -1) || (nextChar == '\n')) {
            lineNum++;
            result.add(StringEscapeUtils.unescapeCsv(thisField.toString()));
            return Collections.unmodifiableList(result);
          }
          if (nextChar == ',') {
            state = CsvStates.START_OF_FIELD;
            result.add(StringEscapeUtils.unescapeCsv(thisField.toString()));
            thisField.setLength(0);
            fieldNum++;
          } else if (nextChar == '\"') {
            outputStream.printf(
                "[ERROR] CSV (Line %d, Field %d): Unescaped quotation mark or leading characters "
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
            outputStream.printf("[ERROR] CSV (Line %d, Field %d): Unterminated quoted field.",
                lineNum, fieldNum);
            throw new CsvCheckedException("[ERROR] CSV (Line " + lineNum + ", Field " + fieldNum
                                          + "): Unterminated quoted field.");
          }
          if (nextChar == '\n') {
            lineNum++;
            outputStream.printf(
                "[WARNING] CSV (Line %d, Field %d): Quoted field contains newline - was this "
                + "intentional?",
                lineNum, fieldNum);
          } else if (nextChar == '\"') {
            thisField.append('\"');
            state = CsvStates.ENDING_QUOTED_FIELD;
          } else {
            thisField.append(nextChar);
          }
          break;
        case ENDING_QUOTED_FIELD:
          if ((nextRead == -1) || (nextChar == '\n')) {
            lineNum++;
            result.add(StringEscapeUtils.unescapeCsv(thisField.toString()));
            return Collections.unmodifiableList(result);
          }
          if (nextChar == ',') {
            state = CsvStates.START_OF_FIELD;
            result.add(StringEscapeUtils.unescapeCsv(thisField.toString()));
            thisField.setLength(0);
            fieldNum++;
          } else if (nextChar == '\"') {
            thisField.append('\"');
            state = CsvStates.LEXING_QUOTED_FIELD;
          } else {
            outputStream.printf(
                "[ERROR] CSV (Line %d, Field %d): Unescaped quotation mark or trailing character "
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

  private enum CsvStates {
    START_OF_FIELD,
    LEXING_UNQUOTED_FIELD,
    LEXING_QUOTED_FIELD,
    ENDING_QUOTED_FIELD
  }
}
