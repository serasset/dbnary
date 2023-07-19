package org.getalp.dbnary.morphology;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.getalp.dbnary.languages.fra.morphology.Utils;
import org.getalp.dbnary.tools.ArrayMatrix;
import org.getalp.model.ontolex.LexicalForm;
import org.getalp.model.ontolex.PhoneticRepresentation;
import org.getalp.model.ontolex.WrittenRepresentation;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RefactoredTableExtractor implements Cloneable {

  private final HashSet<Element> alreadyParsedTables;
  protected final String language;
  protected String entryName;
  protected List<String> globalContext;
  protected ArrayMatrix<Element> cells = null;
  protected ArrayMatrix<Set<LexicalForm>> results = null;

  private final Logger log = LoggerFactory.getLogger(RefactoredTableExtractor.class);

  public RefactoredTableExtractor(String entryName, String language, List<String> context) {
    this(entryName, language, context, new HashSet<>());
  }

  /* private constructor for nested table extraction */
  private RefactoredTableExtractor(String entryName, String language, List<String> context,
      HashSet<Element> alreadyParsedTables) {
    this.entryName = entryName;
    this.language = language;
    this.globalContext = context;
    this.alreadyParsedTables = alreadyParsedTables;
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    RefactoredTableExtractor clone = (RefactoredTableExtractor) super.clone();
    // These are mutable structures that will be reconstructed when necessary
    clone.results = null;
    clone.cells = null;
    return clone;
  }

  protected Element getCell(int i, int j) {
    if (null == cells)
      return null;
    else
      return cells.get(i, j);
  }

  protected Set<LexicalForm> getResult(int i, int j) {
    if (null == results)
      return null;
    else
      return results.get(i, j);
  }

  public Set<LexicalForm> parseTable(Element tableElement) {
    if (alreadyParsedTables.contains(tableElement))
      return new HashSet<>();
    alreadyParsedTables.add(tableElement);

    Set<LexicalForm> forms = new LinkedHashSet<>();

    cells = HtmlTableHandler.explodeTable(tableElement);
    results = new ArrayMatrix<>();

    for (int i = 0; i < cells.nlines(); i++) {
      for (int j = 0; j < cells.ncolumns(); j++) {
        Element cell = cells.get(i, j);
        if (null == cell)
          continue;
        if (isHeaderCell(cell))
          continue;
        if (shouldProcessCell(cell)) {
          List<String> cellContext = getRowAndColumnContext(i, j, cells);
          // prepend global context
          cellContext.addAll(0, globalContext);
          if (cell.select("table").isEmpty()) {
            // No nested table in current cell.
            Set<LexicalForm> lexForms = handleSimpleCell(i, j, cell, cellContext);
            results.set(i, j, lexForms);
            forms.addAll(lexForms);
          } else {
            Set<LexicalForm> lexForms = handleNestedTables(i, j, cell, cellContext);
            results.set(i, j, lexForms);
            forms.addAll(lexForms);
          }
        }
      }
    }
    return forms;
  }

  /**
   * true if the cell should be processed by the extractor. This is called for a normal cell and not
   * for a header cell, it allows specific subclasses to further filter out cells based on their
   * content.
   *
   * @param cell the td element to be examined
   * @return true if the cell should be processed
   */
  protected boolean shouldProcessCell(Element cell) {
    return true;
  }

  protected Set<LexicalForm> handleSimpleCell(int i, int j, Element cell, List<String> context) {
    // Do not process cell that have a background color (these are usually not valid values)
    if (cell.attr("bgcolor").isEmpty()) {
      return getLexicalFormsFromCell(i, j, cell, context);
    }
    return new HashSet<>();
  }

  protected Set<LexicalForm> handleNestedTables(int i, int j, Element cell, List<String> context) {
    // handle tables that are nested in cells
    Elements tables = cell.select("table");
    Set<LexicalForm> forms = new LinkedHashSet<>();
    for (Element nestedTable : tables) {
      if (alreadyParsedTables.contains(nestedTable)) {
        log.debug("Ignoring already parsed nested table in {}", entryName);
        continue;
      }
      if (nestedTable.id().equalsIgnoreCase("toc")) {
        continue;
      }
      try {
        RefactoredTableExtractor nestedExtractor = (RefactoredTableExtractor) this.clone();
        nestedExtractor.globalContext = new ArrayList<>(nestedExtractor.globalContext);
        nestedExtractor.globalContext.addAll(0, context);
        forms.addAll(nestedExtractor.parseTable(nestedTable));
      } catch (CloneNotSupportedException e) {
        log.error("Could not clone the Table Extractor for nested table extraction.");
        e.printStackTrace();
      }
    }
    return forms;
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

  protected boolean addToContext(ArrayMatrix<Element> columnHeaders, int i, int j,
      List<String> res) {
    Element cell = columnHeaders.get(i, j);
    String header;
    if (null != cell && isHeaderCell(cell) && (header = cell.text().trim()).length() != 0) {
      res.add(header);
      return true;
    }
    return false;
  }

  /**
   * returns the set of lexical forms that correspond to current cell and context
   * <p>
   * The context is a list of String that corresponds to all column and row headers + section
   * headers in which the cell appears.
   *
   * @param i the line number of the cell in the table
   * @param j the column number of the cell in the table
   * @param context a list of Strings that represent the celle context
   * @return The set of lexical forms corresponding to the context
   */
  protected Set<LexicalForm> getLexicalFormsFromCell(int i, int j, Element cell,
      List<String> context) {
    InflectionScheme infl = getInflectionSchemeFromContext(context);
    return getInflectedForms(cell, infl);
  }

  /**
   * returns the inflection that correspond to current cell context
   * <p>
   * The cell context is a list of String that corresponds to all column and row headers + section
   * headers in which the cell appears.
   *
   * @param context a list of Strings that represent the celle context
   * @return The set of lexical forms corresponding to the context
   */
  protected abstract InflectionScheme getInflectionSchemeFromContext(List<String> context);


  /**
   * Extract wordforms from table cell<br>
   * Splits cell content by &lt;br\&gt; or comma and removes HTML formatting
   *
   * @param cell the current cell in the inflection table
   * @param infl the inflection scheme corresponding to the current cell
   * @return Set of wordforms (Strings) from this cell
   */
  protected Set<LexicalForm> getInflectedForms(Element cell, InflectionScheme infl) {
    // there are cells with <br> and commas to separate different values: split them
    // get rid of spurious html-formatting (<nbsp> <small> <i> etc.)
    Set<LexicalForm> forms = new HashSet<>();
    // if the inflection is null, there is no inflected form to build
    if (null == infl)
      return forms;

    Elements elements;

    if (!(elements = cell.select("a, strong.selflink")).isEmpty()) {
      for (Element anchor : elements) {
        // Ignore links to pronunciation pages
        if (elementIsAValidForm(anchor)) {
          LexicalForm form = new LexicalForm(infl);
          form.addValue(new WrittenRepresentation(standardizeValue(anchor.text()), language));
          forms.add(form);
        }
      }
      Elements prons = cell.select("span.API");
      for (Element pron : prons) {
        String pronValue = Utils.standardizePronunciation(pron.text());
        if (pronValue.length() > 0)
          forms.forEach(f -> f.addValue(new PhoneticRepresentation(pronValue, language)));
      }
    } else {
      String cellText = cell.text();
      // check for <br>
      Elements linebreaks = cell.select("br");
      if (!linebreaks.isEmpty()) {
        log.debug("cell contains <br> : {}", cell.text());
        // replace <br> by ","
        cellText = cellText.replaceAll("<br/?>", ",");
      }

      String[] atomicForms = cellText.split("[,;]");
      for (String atomicForm : atomicForms) {
        String trimmedText = atomicForm.trim();
        // log.debug(" was split into : {}", trimmedText);
        if (!trimmedText.isEmpty()) {
          LexicalForm form = new LexicalForm(infl);
          form.addValue(new WrittenRepresentation(trimmedText, language));
          forms.add(form);
        }
      }
    }
    return forms;
  }

  protected boolean elementIsAValidForm(Element anchor) {
    return true;
  }

  protected String standardizeValue(String value) {
    value = value.replaceAll("&nbsp;", " ");
    value = value.replaceAll("</?small>", "");
    value = value.replaceAll("</?i>", "");
    value = value.replaceAll("</?strong.*?>", "");
    value = value.replaceAll("</?span.*?>", "");
    value = value.replaceAll("</?b.*?>", "");
    value = value.replaceAll("</?sup.*?>", "");
    return value.trim();
  }

}
