package org.getalp.dbnary.morphology;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.getalp.dbnary.tools.ArrayMatrix;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TableExtractor extends HtmlTableHandler {

  protected Set<Element> alreadyParsedTables = new HashSet<>();
  protected String currentEntry = null;
  private final Logger log = LoggerFactory.getLogger(TableExtractor.class);

  /**
   * returns the inflection data that correspond to current celle context
   * <p>
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

  public InflectedFormSet parseHTML(String htmlCode, String pagename) {
    currentEntry = pagename;
    Document doc = Jsoup.parse(htmlCode);

    // for debug : show the body of the HTML
    // log.trace("parseTables for template {} returns body {}", declinationTemplateCall,
    // doc.body().toString());
    InflectedFormSet forms = new InflectedFormSet();

    // expandWikiCode for Adjective-Flextables now returns headers for the degree info
    // (i.e. Positiv, Komparativ, Superlativ) in h4 !

    Elements elts = doc.select("h2, h3, h4, table");
    LinkedList<String> h2Context = new LinkedList<>();
    LinkedList<String> h3Context = new LinkedList<>();
    LinkedList<String> h4Context = new LinkedList<>();
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
          log.debug("Ignoring already parsed table in {}", currentEntry);
          continue;
        }
        if (elt.id().equalsIgnoreCase("toc")) {
          continue;
        }
        forms.addAll(parseTable(elt, h4Context));
      }
    }
    currentEntry = null;
    return forms;
  }

  protected Collection<? extends String> decodeH2Context(String text) {
    return new LinkedList<>();
  }

  protected InflectedFormSet parseTable(Element table, List<String> globalContext) {

    InflectedFormSet forms = new InflectedFormSet();
    alreadyParsedTables.add(table);

    ArrayMatrix<Element> explodedTable = explodeTable(table);

    for (int i = 0; i < explodedTable.nlines(); i++) {
      for (int j = 0; j < explodedTable.ncolumns(); j++) {
        Element cell = explodedTable.get(i, j);
        if (null == cell)
          continue;
        if (isHeaderCell(cell))
          continue;
        if (isNormalCell(cell) && shouldProcessCell(cell)) {
          List<String> context = getRowAndColumnContext(i, j, explodedTable);
          // prepend global context
          context.addAll(0, globalContext);
          if (cell.select("table").isEmpty()) {
            // No nested table in current cell.
            handleSimpleCell(cell, context, forms);
          } else {
            handleNestedTables(cell, context, forms);
          }
        }
      }
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

  protected boolean isNormalCell(Element cell) {
    return cell.tagName().equalsIgnoreCase("td");
  }

  protected boolean isHeaderCell(Element cell) {
    return cell.tagName().equalsIgnoreCase("th");
  }

  protected List<String> getRowAndColumnContext(int nrow, int ncol,
      ArrayMatrix<Element> columnHeaders) {
    LinkedList<String> res = new LinkedList<>();
    for (int i = 0; i < nrow; i++) {
      addToContext(columnHeaders, i, ncol, res);
    }
    for (int i = 0; i < ncol; i++) {
      addToContext(columnHeaders, nrow, i, res);
    }
    return res;
  }

  protected void addToContext(ArrayMatrix<Element> columnHeaders, int i, int j, List<String> res) {
    Element cell = columnHeaders.get(i, j);
    String header;
    if (null != cell && isHeaderCell(cell) && (header = cell.text()).trim().length() != 0) {
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
    Set<String> forms = new HashSet<>();
    Elements anchors = cell.select("a, strong");

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
      cellText = cellText.replaceAll("</?div.*?>", "");

      String[] atomicForms = cellText.split("[,;]");
      for (String atomicForm : atomicForms) {
        String trimmedText = atomicForm.trim();
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
    forms = forms.stream().map(s -> s.replaceAll("&nbsp;", " ")).collect(Collectors.toSet());
    for (String form : forms) {
      if (form.contains(",")) {
        log.trace("Comma found in morphological value: {}", form);
      }
    }
    return forms;
  }

}
