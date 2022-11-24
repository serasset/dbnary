package org.getalp.dbnary.commons;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;


class HierarchicalSenseNumberTest {

  @Test
  public void testHierarchicalSenseNumber() {
    HierarchicalSenseNumber num = new HierarchicalSenseNumber();
    num.increment(1);
    assertEquals("1", num.toString());
    num.increment(1);
    assertEquals("2", num.toString());
    num.increment(2);
    assertEquals("2.1", num.toString());
    num.increment(2);
    assertEquals("2.2", num.toString());
    num.increment(3);
    assertEquals("2.2.1", num.toString());
    num.increment(3);
    assertEquals("2.2.2", num.toString());
    num.increment(2);
    assertEquals("2.3", num.toString());
    num.increment(2);
    assertEquals("2.4", num.toString());
    num.increment(3);
    assertEquals("2.4.1", num.toString());
    num.increment(3);
    assertEquals("2.4.2", num.toString());
    num.increment(1);
    assertEquals("3", num.toString());
    num.increment(5);
    assertEquals("3.0.0.0.1", num.toString());
  }

  @Test
  public void testHierarchicalSenseNumberWithFormat() {
    String format = "naiiiiiiii";
    HierarchicalSenseNumber num = new HierarchicalSenseNumber();
    num.increment(1);
    assertEquals("1", num.formatWithModel(format));
    num.increment(1);
    assertEquals("2", num.formatWithModel(format));
    num.increment(2);
    assertEquals("2.a", num.formatWithModel(format));
    num.increment(2);
    assertEquals("2.b", num.formatWithModel(format));
    num.increment(3);
    assertEquals("2.b.i", num.formatWithModel(format));
    num.increment(3);
    assertEquals("2.b.ii", num.formatWithModel(format));
    num.increment(2);
    assertEquals("2.c", num.formatWithModel(format));
    num.increment(2);
    assertEquals("2.d", num.formatWithModel(format));
    num.increment(3);
    assertEquals("2.d.i", num.formatWithModel(format));
    num.increment(3);
    assertEquals("2.d.ii", num.formatWithModel(format));
    num.increment(1);
    assertEquals("3", num.formatWithModel(format));
    num.increment(5);
    assertEquals("3....i", num.formatWithModel(format));
  }

  @Test
  public void testHighHierarchicalSenseNumber() {
    String format = "naiiiiiiii";
    HierarchicalSenseNumber num = new HierarchicalSenseNumber();
    for (int i = 0; i < 100; i++) {
      num.increment(1);
    }
    assertEquals("100", num.formatWithModel(format));
    num.increment(2);
    assertEquals("100.a", num.formatWithModel(format));
    for (int i = 0; i < 25; i++) {
      num.increment(2);
    }
    assertEquals("100.z", num.formatWithModel(format));
    num.increment(2);
    assertEquals("100.aa", num.formatWithModel(format));
    num.increment(2);
    assertEquals("100.ab", num.formatWithModel(format));
    for (int i = 0; i < 26; i++) {
      num.increment(3);
    }
    assertEquals("100.ab.xxvi", num.formatWithModel(format));
    num.increment(3);
    assertEquals("100.ab.xxvii", num.formatWithModel(format));
    num.increment(3);
    num.increment(3);
    assertEquals("100.ab.xxix", num.formatWithModel(format));
  }
}
