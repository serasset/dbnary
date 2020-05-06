package org.getalp.dbnary.wiki;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.getalp.dbnary.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WikiTool {

  static Logger log = LoggerFactory.getLogger(WikiTool.class);


  /**
   * @param argsString the String containing all the args (the part of a template contained after
   *        the first pipe).
   * @return a Map associating each argument name with its value.
   * @deprecated Parse the args of a Template, e.g., parses a string like xxx=yyy|zzz=ttt It can
   *             handle nested parentheses, e.g., xxx=yyy|zzz={{aaa=bbb|ccc=ddd}}|kkk=hhh and
   *             xxx=yyy|zzz=[[aaa|bbb|ccc]]|kkk=hhh.
   */
  @Deprecated
  public static Map<String, String> parseArgs(String argsString) {
    HashMap<String, String> argsMap = new HashMap<String, String>();
    if (null == argsString || "" == argsString) {
      return argsMap;
    }

    ArrayList<String> argsArray = splitUnlessInTemplateOrLink(argsString, '|');

    // then consider each argument argString
    // split each element of argsArray (i.e. each argument arg) by "=" (unless "=" is contained in a
    // wiki template or link)
    // into argsMap, the returned map
    int n = 1; // number for positional args.
    String argString;
    for (int h = 0; h < argsArray.size(); h++) {// iterate over all arguments in argsArray
      argString = argsArray.get(h); // an argument in argsArray
      ArrayList<Pair> templatesAndLinksLocation = locateEnclosedString(argString, "{{", "}}");
      templatesAndLinksLocation.addAll(locateEnclosedString(argString, "[[", "]]"));
      int j = 0;
      while (j < argString.length()) {// iterate over characters in string argString
        if (argString.charAt(j) == '=') {
          Pair p = new Pair(j, j + 1);
          if (templatesAndLinksLocation.size() == 0
              || !(p.containedIn(templatesAndLinksLocation))) {
            if (j == argString.length() - 1) {
              argsMap.put(argString.substring(0, j).trim(), "");
            } else {
              argsMap.put(argString.substring(0, j).trim(),
                  argString.substring(j + 1, argString.length()).trim());
            }
            break;
          }
        }
        j++;
      }
      if (j == argString.length()) {// "=" not found in argument argString
        argsMap.put("" + n, argString);
        n++;
      }
    }
    return argsMap;
  }

  static Pattern htmlRefElement = Pattern.compile("(<ref(?:\\s[^>]*|\\s*)>)|(</ref>)");

  // WARN: not synchronized !
  public static String removeReferencesIn(String definition) {
    StringBuffer def = new StringBuffer();
    Matcher m = htmlRefElement.matcher(definition);
    boolean mute = false;
    int previousPos = 0;
    while (m.find()) {
      if (null != m.group(1) && m.group().endsWith("/>")) {
        // A opening/closing element
        if (!mute) {
          def.append(definition.substring(previousPos, m.start()));
        }
      } else if (null != m.group(1)) {
        // An opening element
        if (!mute) {
          def.append(definition.substring(previousPos, m.start()));
        }
        mute = true;
      } else if (null != m.group(2)) {
        // a closing element
        if (!mute) {
          def.append(definition.substring(previousPos, m.start()));
        }
        mute = false;
      }
      previousPos = m.end();
    }
    if (!mute) {
      def.append(definition.substring(previousPos, definition.length()));
    }
    return def.toString();
  }

  public static String removeTablesIn(String s) {
    String toreturn = "";
    for (Pair p : WikiTool.locateEnclosedString(s, "{|", "|}")) {
      toreturn = toreturn + s.substring(0, p.start) + s.substring(p.end, s.length());
    }
    if (toreturn.equals("")) {
      return s;
    } else {
      return toreturn;
    }
  }

  // REMOVE TEXT WITHIN PARENTHESES UNLESS PARENTHESES FALL INSIDE A WIKI LINK OR A WIKI TEMPLATE
  // This function is only used by /eng/Etymology.java
  public static String removeTextWithinParenthesesIn(String s) {
    // locate templates {{}} and links [[]]
    ArrayList<Pair> templatesAndLinksLocations = locateEnclosedString(s, "{{", "}}");
    templatesAndLinksLocations.addAll(locateEnclosedString(s, "[[", "]]"));
    // locate parentheses ()
    ArrayList<Pair> parenthesesLocations = locateEnclosedString(s, "(", ")");
    // ignore location of parentheses if they fall inside a link or a template
    int parenthesesLocationsLength = parenthesesLocations.size();
    for (int i = 0; i < parenthesesLocationsLength; i++) {
      Pair p = parenthesesLocations.get(parenthesesLocationsLength - i - 1);
      // check if parentheses are inside links [[ () ]]
      if (!p.containedIn(templatesAndLinksLocations)) {
        log.debug("Removing string {} in Etymology section", s.substring(p.start, p.end));
        s = s.substring(0, p.start) + s.substring(p.end, s.length());
      }
    }
    return s;
  }

  /**
   * This function locates the start and end position of two symbols (enclosingStringStart and
   * enclosingStringEnd) in input String s. It can handle nested symbols e.g.,
   * locateEnclosedString("string {{at}}","{{","}}") returns (7,13) e.g.,
   * locateEnclosedString("string {{at {{position}} }}","{{","}}") returns (7,27)
   *
   * @param s the string to be parsed, this function returns the position of the second parameter
   *        enclosingStringStart and the position of the third parameter enclosingStringEnd in
   *        string s
   * @param enclosingStringStart this function returns the position of the String
   *        enclosingStringStart in String s
   * @param enclosingStringEnd this function returns the position of the String enclosingStringEn in
   *        String s
   * @return an ArrayList with the start and ens positions of the enclosing Strings in input String
   *         s
   */
  public static ArrayList<Pair> locateEnclosedString(String s, String enclosingStringStart,
      String enclosingStringEnd) {
    int eSS = enclosingStringStart.length();
    int eSE = enclosingStringEnd.length();
    int numberOfEnclosings = 0, start = -1, end = -1;
    ArrayList<Pair> toreturn = new ArrayList<Pair>();
    for (int i = 0; i + eSE <= s.length(); i++) {
      if (i + eSS + eSE <= s.length()) {
        if (s.substring(i, i + eSS).equals(enclosingStringStart)) {
          numberOfEnclosings++;
          if (start == -1) {
            start = i;
          }
          i += eSS - 1;
        }
      }
      if (s.substring(i, i + eSE).equals(enclosingStringEnd)) {
        numberOfEnclosings--;
        if (numberOfEnclosings == 0 && start != -1) {
          end = i + eSE;
          toreturn.add(new Pair(start, end));
          start = -1;// initialize start
        }
        i += eSE - 1;
      }
    }
    return toreturn;
  }

  /**
   * @param s String
   * @param c character
   * @return an ArrayList of String This function takes as input a String s and a character or
   *         separator c, and splits s into an ArrayList of Strings using separator c, unless
   *         character c falls inside a wiki link or a wiki template
   */
  public static ArrayList<String> splitUnlessInTemplateOrLink(String s, char c) {
    // locate wiki templates and links in input string
    ArrayList<Pair> templatesAndLinksLocation = locateEnclosedString(s, "{{", "}}");
    templatesAndLinksLocation.addAll(locateEnclosedString(s, "[[", "]]"));

    ArrayList<String> a = new ArrayList<String>();
    int i = 0, j = 0;// iterate over characters in string s
    while (j < s.length() - 1) {
      if (s.charAt(j) == c) {
        Pair p = new Pair(j, j + 1);
        if (templatesAndLinksLocation.size() == 0
            || (!(p.containedIn(templatesAndLinksLocation)))) {
          a.add(s.substring(i, j).trim());
          i = j + 1;
        }
      }
      j++;
    }
    if (j == s.length() - 1) {
      if (s.charAt(j) == c) {
        a.add(s.substring(i, j).trim());
      } else { // includes case: argsString is a single character
        a.add(s.substring(i, j + 1).trim());
      }
    }
    return a;
  }


  public static String toParameterString(Map<String, String> parameterMap) {
    StringBuffer buf = new StringBuffer();
    for (Map.Entry<String, String> stringStringEntry : parameterMap.entrySet()) {
      buf.append(stringStringEntry.getKey()).append("=").append(stringStringEntry.getValue())
          .append("|");
    }
    if (buf.length() > 0) {
      buf.delete(buf.length() - 1, buf.length());
    }
    return buf.toString();
  }
}
