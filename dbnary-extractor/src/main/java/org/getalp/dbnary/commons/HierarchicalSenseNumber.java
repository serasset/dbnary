package org.getalp.dbnary.commons;

import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;

public class HierarchicalSenseNumber {

  private static final int MAX_DEPTH = 10;
  private final int[] senseNumbers = new int[MAX_DEPTH];
  private int currentDepth;

  public HierarchicalSenseNumber() {
    senseNumbers[0] = 0;
    currentDepth = 1;
  }

  public void increment(int deflevel) {
    if (deflevel == 0) {
      currentDepth = 1;
      senseNumbers[1] = 0;
      return;
    }
    // increment the definition level if necessary
    for (int i = currentDepth; i < deflevel; i++) {
      senseNumbers[i] = 0;
    }
    currentDepth = deflevel;
    senseNumbers[deflevel - 1]++;
  }

  public String formatWithModel(String format) {
    format = StringUtils.rightPad(format, 10, 'N');
    StringBuilder r = new StringBuilder(formatNum(senseNumbers[0], format.charAt(0)));
    for (int i = 1; i < currentDepth; i++) {
      r.append(".").append(formatNum(senseNumbers[i], format.charAt(i)));
    }
    return r.toString();
  }

  private String formatNum(int num, char format) {
    switch (format) {
      case 'n':
      case 'N':
        return String.valueOf(num);
      case 'a':
        return toAlpha(num).toLowerCase();
      case 'A':
        return toAlpha(num);
      case 'i':
        return toRoman(num).toLowerCase();
      case 'I':
        return toRoman(num);
      default:
        return "_";
    }
  }

  @Override
  public String toString() {
    return formatWithModel("");
  }

  // Utility function for roman conversion (taken from
  // https://stackoverflow.com/questions/12967896/converting-integers-to-roman-numerals-java)
  private final static TreeMap<Integer, String> map = new TreeMap<Integer, String>();

  static {

    map.put(1000, "M");
    map.put(900, "CM");
    map.put(500, "D");
    map.put(400, "CD");
    map.put(100, "C");
    map.put(90, "XC");
    map.put(50, "L");
    map.put(40, "XL");
    map.put(10, "X");
    map.put(9, "IX");
    map.put(5, "V");
    map.put(4, "IV");
    map.put(1, "I");

  }

  private static String toRoman(int number) {
    if (number == 0) {
      return "";
    }
    int l = map.floorKey(number);
    if (number == l) {
      return map.get(number);
    }
    return map.get(l) + toRoman(number - l);
  }

  private static String toAlpha(int number) {
    StringBuilder r = new StringBuilder();
    if (number == 0) {
      return "";
    } else {
      while (number > 0) {
        number--;
        r.append((char) ('A' + (number % 26)));
        number = number / 26;
      }
      return r.reverse().toString();
    }
  }
}
