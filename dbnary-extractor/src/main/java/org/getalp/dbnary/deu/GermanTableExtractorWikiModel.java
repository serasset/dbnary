package org.getalp.dbnary.deu;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

import info.bliki.wiki.template.ITemplateFunction;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.tools.ArrayMatrix;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class GermanTableExtractorWikiModel extends GermanDBnaryWikiModel {

  private Logger log = LoggerFactory.getLogger(GermanTableExtractorWikiModel.class);
  protected IWiktionaryDataHandler wdh; // TODO : only used to query the current entry name for
                                        // logging purposes.

  protected Set<Element> alreadyParsedTables = new HashSet<>();

  public GermanTableExtractorWikiModel(WiktionaryIndex wi, Locale locale, String imageBaseURL,
      String linkBaseURL, IWiktionaryDataHandler wdh) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.wdh = wdh;
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    if ("Flexlink".equals(templateName)) {
      // Just display the link name and drop the link...
      writer.append(parameterMap.get("1"));
    } else if ("Str subrev".equals(templateName)) {
      // subrev & rightc are extensively called and generate a very big Lua overhead
      writer.append(subrev(parameterMap));
    } else if ("Str rightc".equals(templateName)) {
      writer.append(rightc(parameterMap));
    } else if ("Str right".equals(templateName)) {
      writer.append(right(parameterMap));
    } else if ("Str len".equals(templateName)) {
      writer.append(Integer.toString(parameterMap.getOrDefault("1", "").length()));
    } else if ("Str crop".equals(templateName)) {
      writer.append(crop(parameterMap));
    } else if ("Literatur".equals(templateName)) {
      // nop : catch and ignore this template in the context of morphology tables.
    } else {
      // DONE: catch Str subc and Str subrev for drastic extraction time enhancement
      // log.trace("Expanding template call: {}", templateName);
      super.substituteTemplateCall(templateName, parameterMap, writer);
    }
  }

  private String crop(Map<String, String> parameterMap) {
    String s = parameterMap.getOrDefault("1", "");
    int cut = Integer.valueOf(parameterMap.getOrDefault("2", "0"));
    int end = s.length() - cut;
    if (0 <= end && end <= s.length()) {
      return s.substring(0, end);
    } else {
      return "";
    }
  }

  private String right(Map<String, String> parameterMap) {
    String text = parameterMap.getOrDefault("1", "").trim();
    String arg2 = parameterMap.get("2");
    int start = (null == arg2 || "".equals(arg2)) ? 0 : Integer.valueOf(arg2);
    if (start < 0) {
      return "";
    } else {
      return text.substring(start);
    }
  }

  private String rightc(Map<String, String> parameterMap) {
    String text = parameterMap.getOrDefault("1", "").trim();
    int posr = Integer.valueOf(parameterMap.getOrDefault("2", "1"));
    int start = text.length() - posr;
    if (start >= 0) {
      return text.substring(start);
    } else {
      return "";
    }
  }

  private String subrev(Map<String, String> parameterMap) throws IOException {
    String rightc = rightc(parameterMap);
    int length = Integer.valueOf(parameterMap.getOrDefault("3", "1"));
    if (length <= rightc.length()) {
      return rightc.substring(0, length);
    } else {
      return "";
    }
  }


  Set<String> ignoredTemplates =
      Stream.of("Adjektivdeklination", "Verbkonjugation", "Adverbdeklination")
          .collect(collectingAndThen(toCollection(HashSet::new), Collections::unmodifiableSet));

  protected InflectedFormSet parseTables(String declinationTemplateCall) {

    WikiText txt = new WikiText(declinationTemplateCall);
    for (Token token : txt.templatesOnUpperLevel()) {
      Template tmpl = (Template) token;
      if (!ignoredTemplates.contains(tmpl.getName())) {
        log.debug("MORPH template: {}", tmpl.getName());
        if (log.isDebugEnabled()) {
          String tmplSource = tmpl.toString().replaceAll("\\n", "");
          log.debug("MORPH template call:  {} @ {}", tmplSource, getPageName());
        }
      }
    }
    if (log.isDebugEnabled())
      return new InflectedFormSet();

    String htmlCode = expandWikiCode(declinationTemplateCall);
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

    Elements elts = doc.select("h3, h4, table");
    LinkedList<String> h2Context = new LinkedList<>();
    LinkedList<String> h3Context = new LinkedList<>();
    LinkedList<String> h4Context = new LinkedList<>();
    for (Element elt : elts) {

      // log.debug(" parseTables: elt.tagName = {} text = {}", elt.tagName(), elt.text());
      if (elt.tagName().equalsIgnoreCase("h4")) {
        h4Context.clear();
        h4Context.addAll(h3Context);
        h4Context.add(elt.text());
      }
      if (elt.tagName().equalsIgnoreCase("h3")) {
        h3Context.clear();
        h4Context.clear();
        h3Context.addAll(h2Context);
        h3Context.add(elt.text());

      } else if (elt.tagName().equalsIgnoreCase("h2")) {
        // but h2 was not selected ?
        if (!elt.text().contains("Deutsch")) {
          log.debug("Breaking flexion parse due to header {} --in-- {}", elt.text(),
              wdh.currentLexEntry());
          return forms;
        }
        h2Context.clear();
        h3Context.clear();
        h4Context.clear();
        h2Context.addAll(decodeH2Context(elt.text()));
      } else {
        if (alreadyParsedTables.contains(elt)) {
          log.debug("Ignoring already parsed table in {}", wdh.currentLexEntry());
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
    return new LinkedList<>();
  }


  protected InflectedFormSet parseTable(Element table, List<String> globalContext) {
    InflectedFormSet forms = new InflectedFormSet();
    alreadyParsedTables.add(table);
    Elements rows = table.select("tr");
    ArrayMatrix<String> columnHeaders = new ArrayMatrix<>();
    int nrow = 0;
    for (Element row : rows) {
      // Filter out rows that belong to nested tables
      if (!inCurrentTable(table, row)) {
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
              columnHeaders.set(nrow + j, ncol, cell.text());
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
            log.debug("Non null rowspan in data cell ({},{}) for {}", nrow, ncol,
                wdh.currentLexEntry());
          }

          for (int i = 0; i < cspan; i++) {
            List<String> context = getRowAndColumnContext(nrow, ncol, columnHeaders);
            // prepend global context
            context.addAll(0, globalContext);
            if (cell.select("table").isEmpty()) {
              // No nested table in current cell.
              // only get inflection for cells without bgcolour!

              if (cell.attr("bgcolor").isEmpty()) {
                GermanInflectionData inflection = getInflectionDataFromCellContext(context);
                if (null != inflection) {
                  forms.add(inflection, getInflectedForms(cell));
                }
              }
            } else {
              // handle tables that are nested in cells
              Elements tables = cell.select("table");
              for (Element nestedTable : tables) {
                if (alreadyParsedTables.contains(nestedTable)) {
                  log.debug("Ignoring already parsed nested table in {}", wdh.currentLexEntry());
                  continue;
                }
                if (nestedTable.id().equalsIgnoreCase("toc")) {
                  continue;
                }
                forms.addAll(parseTable(nestedTable, context));
              }
            }
            ncol++;
          }
        } else {
          log.debug("Row child \"{}\"is not a cell in {}", cell.tagName(), wdh.currentLexEntry());
        }
      }
      nrow++;
    }
    return forms;
  }

  private boolean inCurrentTable(Element table, Element row) {
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
      ArrayMatrix<String> columnHeaders) {
    LinkedList<String> res = new LinkedList<>();
    for (int i = 0; i < nrow; i++) {
      String header = columnHeaders.get(i, ncol);
      if (null != header && (header.trim().length() != 0)) {
        res.add(header);
      }
    }
    for (int i = 0; i < ncol; i++) {
      String header = columnHeaders.get(nrow, i);
      if (null != header && (header.trim().length() != 0)) {
        res.add(header);
      }
    }
    return res;
  }

  protected abstract GermanInflectionData getInflectionDataFromCellContext(List<String> context);

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

  private static ITemplateFunction germanInvoke = new GermanInvoke();

  @Override
  public ITemplateFunction getTemplateFunction(String name) {
    if ("#invoke".equals(name))
      return germanInvoke;
    return getTemplateMap().get(name);
  }

}
