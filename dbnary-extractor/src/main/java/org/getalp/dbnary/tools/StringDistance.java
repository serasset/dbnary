package org.getalp.dbnary.tools;

import java.util.function.BiFunction;

public class StringDistance {

  /**
   * return the suffixal change from s to t
   * 
   * @param s the source String to be changed
   * @param t the target String result of the change
   * @return the change as a String of form -xxx+yyy where t = s where suffix xxx is stripped out
   *         and suffix yyy is appended to the resulting base
   */
  public static String suffixChange(String s, String t) {
    int i = 0;
    int l = Math.min(s.length(), t.length());
    while (i < l && s.charAt(i) == t.charAt(i)) {
      i++;
    }
    String deletedSuffix = s.substring(i);
    String appendedSuffix = t.substring(i);

    StringBuilder buf = new StringBuilder();
    return buf.append("-").append(deletedSuffix).append("+").append(appendedSuffix).toString();
  }



  static private int min(int a, int b, int c) {
    int min = (a <= b) ? a : b;
    return (min <= c) ? min : c;
  }

  // *****************************
  // Compute Levenshtein distance
  // *****************************

  public static int[][] distanceMatrix(String s, String t,
      BiFunction<Character, Character, Integer> cost) {

    int n = s.length();
    int m = t.length();
    int atomicCost; // cost of one change

    int[][] d = new int[n + 1][m + 1];

    if (n == 0 || m == 0)
      return d;

    // initialize edges of distance matrix
    for (int i = 0; i <= n; i++)
      d[i][0] = i;
    for (int j = 0; j <= m; j++)
      d[0][j] = j;

    for (int i = 1; i <= n; i++) {

      char cs = s.charAt(i - 1);

      for (int j = 1; j <= m; j++) {
        char ct = t.charAt(j - 1);
        atomicCost = cost.apply(cs, ct);
        d[i][j] = min(d[i - 1][j] + cost.apply('\0', ct), d[i][j - 1] + cost.apply(cs, '\0'),
            d[i - 1][j - 1] + atomicCost);

      }
    }
    return d;
  }


  /**
   * Compute the cost for an edition.
   * 
   * @param c1
   * @param c2
   * @return
   */
  private static int levensteinCost(char c1, char c2) {
    if (c1 == c2)
      return 0;
    else
      return 1;
  }

  public static int[][] levenstein(String s, String t) {
    return distanceMatrix(s, t, StringDistance::levensteinCost);
  }


  static String searchPath(String s, String t, int[][] d, StringBuffer operations, boolean debug) {

    StringBuffer result = new StringBuffer(s);

    int n = d.length;
    int m = d[0].length;

    int x = n - 1;
    int y = m - 1;
    boolean changed = false;
    while (true) {
      if (debug && changed)
        System.out.println("result " + result.toString());

      if (d[x][y] == 0)
        break;
      if (y > 0 && x > 0 && d[x - 1][y - 1] < d[x][y]) {
        if (debug)
          System.out.println("min d[x-1][y-1] " + d[x - 1][y - 1] + " d[x][y] " + d[x][y] + " rep "
              + s.charAt(x - 1) + " with " + t.charAt(y - 1) + " at " + (x - 1));

        operations.append('R').append(Character.toString((char) ((int) x - 1)))
            .append(s.charAt(x - 1)).append(t.charAt(y - 1));
        if (debug)
          result.setCharAt(x - 1, t.charAt(y - 1));
        y--;
        x--;
        changed = true;
        continue;
      }
      if (y > 0 && d[x][y - 1] < d[x][y]) {
        if (debug)
          System.out.println("min d[x][y-1] " + d[x][y - 1] + "  d[x][y] " + d[x][y] + " ins "
              + t.charAt(y - 1) + " at " + (x));
        operations.append('I').append(Character.toString((char) ((int) x))).append(t.charAt(y - 1));
        if (debug)
          result.insert(x, t.charAt(y - 1));
        y--;
        changed = true;
        continue;
      }
      if (x > 0 && d[x - 1][y] < d[x][y]) {
        if (debug)
          System.out.println("min d[x-1][y] " + d[x - 1][y] + " d[x][y] " + d[x][y] + " del "
              + s.charAt(x - 1) + " at " + (x - 1));
        operations.append('D').append(Character.toString((char) ((int) x - 1)))
            .append(s.charAt(x - 1));
        if (debug)
          result.deleteCharAt(x - 1);
        x--;
        changed = true;
        continue;
      }
      changed = false;
      if (x > 0 && y > 0 && d[x - 1][y - 1] == d[x][y]) {
        x--;
        y--;
        continue;
      }
      if (x > 0 && d[x - 1][y] == d[x][y]) {
        x--;
        continue;
      }
      if (y > 0 && d[x][y - 1] == d[x][y]) {
        y--;
        continue;
      }

    }
    if (debug)
      return result.toString();
    else
      return null;
  }

  /**
   * @param form
   * @param operation
   * @param c
   * @return
   */
  public static String changeSimple(String form, String operation, int c) {

    if (operation.equals("0"))
      return form;

    if (operation.charAt(0) == 'I') {
      StringBuffer f = new StringBuffer(form);
      if (f.length() <= c) {
        // DB.println("fail insert ");
        return form;
      }
      f.insert(c + 1, operation.charAt(1));
      return f.toString();
    }
    if (operation.charAt(0) == 'R') {
      StringBuffer f = new StringBuffer(form);
      // if (f.length()<=c) f.append(' ');
      if (f.length() <= c) {
        // DB.println("fail replace ");
        return form;
      }
      f.setCharAt(c, operation.charAt(2));
      return f.toString();
    }

    if (operation.charAt(0) == 'D') {
      StringBuffer f = new StringBuffer(form);
      f.delete(c, c + 1);// .append(' ');
      return f.toString();
    }
    return form;
  }



  /**
   * @param o
   * @return
   */
  public static String simple(String o) {
    StringBuffer s = new StringBuffer(o);
    s.delete(1, 2);
    return s.toString();
  }


}
