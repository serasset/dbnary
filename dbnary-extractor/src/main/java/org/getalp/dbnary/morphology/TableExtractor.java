package org.getalp.dbnary.morphology;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.getalp.dbnary.tools.ArrayMatrix;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TableExtractor {

  protected Set<Element> alreadyParsedTables = new HashSet<Element>();
  protected String currentEntry;
  private Logger log = LoggerFactory.getLogger(TableExtractor.class);

  public TableExtractor(String currentEntry) {
    this.currentEntry = currentEntry;
  }

  /**
   * returns the inflection data that correspond to current celle context
   *
   * The cell context is a list of String that corresponds to all column and row headers + section
   * headers in which the cell appears.
   *
   * @param context a list of Strings that represent the celle context
   * @return The InflexionData corresponding to the context
   */
  protected abstract List<? extends InflectionData> getInflectionDataFromCellContext(
      List<String> context);

  /**
   * returns true if the current H2 element should be ignore while extracting morphological tables
   *
   * @param elt the H2 Header Element
   * @return true iff the section should be ignored
   */
  protected boolean shouldIgnoreCurrentH2(Element elt) {
    return false;
  }

  public InflectedFormSet parseHTML(String htmlCode) {
    Document doc = Jsoup.parse(htmlCode);
    if (null == doc) {
      return null;
    }

    // for debug : show the body of the HTML
    // log.trace("parseTables for template {} returns body {}", declinationTemplateCall,
    // doc.body().toString());
    InflectedFormSet forms = new InflectedFormSet();

    // expandWikiCode for Adjective-Flextables now returns headers for the degree info
    // (i.e. Positiv, Komparativ, Superlativ) in h4 !

    Elements elts = doc.select("h2, h3, h4, table");
    LinkedList<String> h2Context = new LinkedList<String>();
    LinkedList<String> h3Context = new LinkedList<String>();
    LinkedList<String> h4Context = new LinkedList<String>();
    // emptying the alreadyParsedTables for the current parse
    alreadyParsedTables.clear();
    boolean processCurrentH2sSection = true;
    for (Element elt : elts) {

      // log.debug(" parseTables: elt.tagName = {} text = {}", elt.tagName(), elt.text());
      if (elt.tagName().equalsIgnoreCase("h4")) {
        h4Context.clear();
        h4Context.addAll(h3Context);
        h4Context.add(elt.text());
      } else if (elt.tagName().equalsIgnoreCase("h3")) {
        h3Context.clear();
        h4Context.clear();
        h3Context.addAll(h2Context);
        h3Context.add(elt.text());

      } else if (elt.tagName().equalsIgnoreCase("h2")) {
        if (shouldIgnoreCurrentH2(elt)) {
          processCurrentH2sSection = false;
          log.debug("Breaking flexion parse due to header {} --in-- {}", elt.text(), currentEntry);
          // return forms;
        } else {
          processCurrentH2sSection = true;
          h2Context.clear();
          h3Context.clear();
          h4Context.clear();
          h2Context.addAll(decodeH2Context(elt.text()));
        }
      } else {
        if (!processCurrentH2sSection) {
          continue;
        }
        if (alreadyParsedTables.contains(elt)) {
          log.debug("Ignoring already parsed table {} in {}", elt.html(), currentEntry);
          continue;
        }
        if (elt.id().equalsIgnoreCase("toc")) {
          continue;
        }
        forms.addAll(parseTable(elt, h4Context));
      }
    }
    return forms;
  }

  protected Collection<? extends String> decodeH2Context(String text) {
    return new LinkedList<String>();
  }

  protected InflectedFormSet parseTable(Element table, List<String> globalContext) {
    InflectedFormSet forms = new InflectedFormSet();
    alreadyParsedTables.add(table);
    Elements rows = table.select("tr");
    ArrayMatrix<Element> columnHeaders = new ArrayMatrix<>();
    int nrow = 0;
    for (Element row : rows) {
      // Filter out rows that belong to nested tables
      if (!isInCurrentTable(table, row)) {
        continue;
      }

      String rowbgcolor = getBackgroundColor(row);
      int ncol = 0;
      for (Element cell : row.children()) {
        // transmit row background color to cell as it is useful to decide if it is an header cell
        // or not.
        if (rowbgcolor != null && cell.attr("bgcolor").length() == 0) {
          cell.attr("bgcolor", rowbgcolor);
        }
        if (isHeaderCell(cell)) {
          // Advance if current column spans from a previous row
          while (columnHeaders.get(nrow, ncol) != null) {
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
              columnHeaders.set(nrow + j, ncol, cell);
            }
            ncol++;
          }
        } else if (isNormalCell(cell)) {
          while (columnHeaders.get(nrow, ncol) != null) {
            ncol++;
          }
          String colspan = cell.attr("colspan");
          String rowspan = cell.attr("rowspan");
          int cspan =
              (null != colspan && colspan.trim().length() != 0) ? Integer.parseInt(colspan) : 1;
          int rspan =
              (null != rowspan && rowspan.trim().length() != 0) ? Integer.parseInt(rowspan) : 1;
          if (rspan != 1) {
            log.debug("Non null rowspan in data cell ({},{}) for {}", nrow, ncol, currentEntry);
          }

          for (int i = 0; i < cspan; i++) {
            if (shouldProcessCell(cell)) {
              List<String> context = getRowAndColumnContext(nrow, ncol, columnHeaders);
              // prepend global context
              context.addAll(0, globalContext);
              if (cell.select("table").isEmpty()) {
                // No nested table in current cell.
                // only get inflection for cells without bgcolour!
                handleSimpleCell(cell, context, forms);
              } else {
                handleNestedTables(cell, context, forms);
              }
            }
            ncol++;
          }
        } else {
          log.debug("Row child \"{}\"is not a cell in {}", cell.tagName(), currentEntry);
        }
      }
      nrow++;
    }
    return forms;
  }

  protected boolean shouldProcessCell(Element cell) {
    return true;
  }

  protected void handleSimpleCell(Element cell, List<String> context, InflectedFormSet forms) {
    if (cell.attr("bgcolor").isEmpty()) {
      List<? extends InflectionData> inflections = getInflectionDataFromCellContext(context);
      if (null != inflections) {
        forms.add(inflections, getInflectedForms(cell));
      }
    }
  }

  protected void handleNestedTables(Element cell, List<String> context, InflectedFormSet forms) {
    // handle tables that are nested in cells
    Elements tables = cell.select("table");
    for (Element nestedTable : tables) {
      if (alreadyParsedTables.contains(nestedTable)) {
        log.debug("Ignoring already parsed nested table in {}", currentEntry);
        continue;
      }
      if (nestedTable.id().equalsIgnoreCase("toc")) {
        continue;
      }
      forms.addAll(parseTable(nestedTable, context));
    }
  }

  private boolean isInCurrentTable(Element table, Element row) {
    Elements parents = row.parents();
    for (Element parent : parents) {
      if (parent.tagName().equalsIgnoreCase("table")) {
        return parent == table;
      }
    }
    return false;
  }

  protected String getBackgroundColor(Element row) {
    String bgcolor = row.attr("bgcolor");
    if (null == bgcolor || bgcolor.equals("")) {
      String style = row.attr("style");
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

  protected boolean isNormalCell(Element cell) {
    return cell.tagName().equalsIgnoreCase("td");
  }

  protected boolean isHeaderCell(Element cell) {
    return cell.tagName().equalsIgnoreCase("th");
  }

  protected List<String> getRowAndColumnContext(int nrow, int ncol,
      ArrayMatrix<Element> columnHeaders) {
    LinkedList<String> res = new LinkedList<String>();
    for (int i = 0; i < nrow; i++) {
      addToContext(columnHeaders, i, ncol, res);
    }
    for (int i = 0; i < ncol; i++) {
      addToContext(columnHeaders, nrow, i, res);
    }
    return res;
  }

  protected void addToContext(ArrayMatrix<Element> columnHeaders, int i, int j, List<String> res) {
    Element headerCell = columnHeaders.get(i, j);
    String header;
    if (null != headerCell && (header = headerCell.text()) != null && header.trim().length() != 0) {
      res.add(header);
    }
  }

  /**
   * Extract wordforms from table cell<br>
   * Splits cell content by &lt;br\&gt; or comma and removes HTML formatting
   *
   * @param cell the current cell in the inflection table
   * @return Set of wordforms (Strings) from this cell
   */
  protected Set<String> getInflectedForms(Element cell) {
    // there are cells with <br> and commas to separate different values: split them
    // get rid of spurious html-formatting (<nbsp> <small> <i> etc.)
    Set<String> forms = new HashSet<String>();
    Elements anchors = cell.select("a");

    if (anchors.isEmpty()) {
      String cellText = cell.html();
      // check for <br>
      Elements linebreaks = cell.select("br");
      if (!linebreaks.isEmpty()) {
        log.debug("cell contains <br> : {}", cell.html());
        // replace <br> by ","
        cellText = cellText.replaceAll("<br/?>", ",");
      }
      cellText = cellText.replaceAll("&nbsp;", " ");
      cellText = cellText.replaceAll("</?small>", "");
      cellText = cellText.replaceAll("</?i>", "");
      cellText = cellText.replaceAll("</?strong.*?>", "");
      cellText = cellText.replaceAll("</?span.*?>", "");
      cellText = cellText.replaceAll("</?b.*?>", "");

      String[] atomicForms = cellText.split("[,;]");
      for (int i = 0; i < atomicForms.length; i++) {
        String trimmedText = atomicForms[i].trim();
        // log.debug(" was split into : {}", trimmedText);
        if (!trimmedText.isEmpty()) {
          forms.add(trimmedText);
        }
      }
    } else {
      for (Element anchor : anchors) {
        forms.add(anchor.text());
      }
    }
    for (String form : forms) {
      if (form.contains(",")) {
        log.trace("Comma found in morphological value: {}", form);
      }
    }
    return forms;
  }

}
