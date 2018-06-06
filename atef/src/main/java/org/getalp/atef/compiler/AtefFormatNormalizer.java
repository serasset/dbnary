package org.getalp.atef.compiler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AtefFormatNormalizer {

  public static final String firstLinePatternString = "^([^ ]{1,8})\\s*01==(.*)$";
  public static Pattern firstLinePattern = Pattern.compile(firstLinePatternString);

  public static final String nextLinePatternString = "^[^ ]{1,8}\\s*\\d\\d\\s*(.*)$";
  public static Pattern nextLinePattern = Pattern.compile(nextLinePatternString);

  public static final Matcher firstLineMatcher = firstLinePattern.matcher("");
  public static final Matcher nextLineMatcher = nextLinePattern.matcher("");

  protected static String normalizeLine(String originalLine) {
    if (null == originalLine)
      return null;

    Matcher firstLineMatcher = firstLinePattern.matcher(originalLine);
    if (firstLineMatcher.matches()) {
      return new StringBuilder().append(firstLineMatcher.group(1)).append(" ==")
          .append(firstLineMatcher.group(2)).toString();
    }

    Matcher nextLineMatcher = nextLinePattern.matcher(originalLine);
    if (nextLineMatcher.matches()) {
      return nextLineMatcher.group(1);
    }

    System.err.println("Unexpected line format in " + originalLine);
    return originalLine;
  }



  public static String getNormalizedString(BufferedReader original) {
    StringWriter res = new StringWriter();
    original.lines().map(AtefFormatNormalizer::normalizeLine).forEach(s -> {
      res.write(s);
      res.write("\n");
    });

    return res.toString();
  }

  protected static void protectedWriteLine(String line, Writer out) {
    try {
      out.write(line);
      out.write(System.lineSeparator());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public static void normalizeStreams(Reader from, Writer to) {
    List<String> list = new ArrayList<>();
    try (BufferedReader br = new BufferedReader(from)) {
      list = br.lines().map(AtefFormatNormalizer::normalizeLine).collect(Collectors.toList());
    } catch (IOException e) {
      e.printStackTrace();
    }
    list.forEach(s -> protectedWriteLine(s, to));
  }

  public static Reader normalizedReader(Reader rdr) {
    StringWriter w = new StringWriter();
    normalizeStreams(rdr, w);
    return new StringReader(w.toString());
  }



}
