package org.getalp.dbnary.tools;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class StringDistanceTest {

  @Test
  public void suffixTest() {
    String s = "toto";
    String t = "totos";

    String change = StringDistance.suffixChange(s, t);

    assertEquals("-+s", change);
  }

  @Test
  public void simpleTest() {
    String s = "toto";
    String t = "totos";

    int[][] d = StringDistance.levenstein(s, t);

    printDistanceMatrix(s, t, d);

    StringBuffer c = new StringBuffer();
    String r = StringDistance.searchPath(s, t, d, c, true);
    System.out.println(c);
    assertEquals(r, t);
  }

  private void printDistanceMatrix(String s, String t, int[][] d) {
    s = s + ' ';
    t = t + ' ';
    System.out.print("  ");
    for (int j = 0; j < d[0].length; j++) {
      System.out.print(t.charAt(j) + ",");
    }
    System.out.println();
    for (int i = 0; i < d.length; i++) {
      System.out.print(s.charAt(i) + ":");
      for (int j = 0; j < d[i].length; j++) {
        System.out.print(d[i][j] + ",");
      }
      System.out.println();
    }

  }
}
