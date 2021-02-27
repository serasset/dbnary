package org.getalp.dbnary.tools;

import java.util.ArrayList;
import java.util.stream.Stream;

/**
 * Created by serasset on 07/01/15.
 */
public class ArrayMatrix<T> {

  private ArrayList<ArrayList<T>> matrix = new ArrayList<>();

  public void set(int nrow, int ncol, T val) {
    if (matrix.size() <= nrow) {
      for (int k = matrix.size(); k <= nrow; k++) {
        matrix.add(new ArrayList<T>());
      }
    }
    ArrayList<T> row = matrix.get(nrow);
    if (row.size() <= ncol) {
      for (int k = row.size(); k <= ncol; k++) {
        row.add(null);
      }
    }
    row.set(ncol, val);
  }

  public T get(int nrow, int ncol) {
    if (matrix.size() <= nrow) {
      return null;
    }
    ArrayList<T> row = matrix.get(nrow);
    if (row.size() <= ncol) {
      return null;
    }
    return row.get(ncol);
  }

  public int nlines() {
    return matrix.size();
  }

  public int ncolumns() {
    return matrix.stream().map(l -> l.size()).max(Integer::compareTo).orElse(0);
  }
}
