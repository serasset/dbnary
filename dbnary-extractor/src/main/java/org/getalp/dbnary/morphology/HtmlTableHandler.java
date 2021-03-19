package org.getalp.dbnary.morphology;

import org.getalp.dbnary.tools.ArrayMatrix;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HtmlTableHandler {

  /**
   * Explode the HTMLTable into an array matrix where all cells are contained in Matrix cells. If a
   * cell spans several rows/columns, it is duplicated in the different Matric cell.
   *
   * @param table the HTML Table to be exploded
   * @return
   */
  public static ArrayMatrix<Element> explodeTable(Element table) {
    Elements rows = table.select("tr");
    ArrayMatrix<Element> result = new ArrayMatrix<>();
    int nrow = 0;
    for (Element row : rows) {
      // Filter out rows that belong to nested tables
      if (!isInCurrentTable(table, row)) {
        continue;
      }

      String rowbgcolor = getBackgroundColor(row);
      int ncol = 0;
      for (Element cell : row.children()) {
        // transmit row background color to cells as it is useful to decide if it is an header cell
        // or not.
        if (rowbgcolor != null && cell.attr("bgcolor").length() == 0) {
          cell.attr("bgcolor", rowbgcolor);
        }
        // Advance if current column spans from a previous row
        while (result.get(nrow, ncol) != null) {
          ncol++;
        }
        String colspan = cell.attr("colspan");
        String rowspan = cell.attr("rowspan");
        int cspan =
            (null != colspan && colspan.trim().length() != 0) ? Integer.parseInt(colspan) : 1;
        int rspan =
            (null != rowspan && rowspan.trim().length() != 0) ? Integer.parseInt(rowspan) : 1;

        for (int i = 0; i < cspan; i++) {
          for (int j = 0; j < rspan; j++) {
            result.set(nrow + j, ncol, cell);
          }
          ncol++;
        }
      }
      nrow++;
    }
    return result;
  }

  private static boolean isInCurrentTable(Element table, Element row) {
    Elements parents = row.parents();
    for (Element parent : parents) {
      if (parent.tagName().equalsIgnoreCase("table")) {
        return parent == table;
      }
    }
    return false;
  }

  public static String getBackgroundColor(Element element) {
    String bgcolor = element.attr("bgcolor");
    if (null == bgcolor || bgcolor.equals("")) {
      String style = element.attr("style");
      if (null != style && style.length() != 0) {
        int bgpos = style.indexOf("background:");
        if (bgpos != -1) {
          style = style.substring(bgpos + 11);
          bgcolor = style.split("[,;]")[0];
        } else {
          bgpos = style.indexOf("background-color:");
          if (bgpos != -1) {
            style = style.substring(bgpos + 17);
            bgcolor = style.split("[,;]")[0];
          }
        }
      }
    }
    if (bgcolor.equals("")) {
      bgcolor = null;
    }
    return bgcolor;
  }
}
