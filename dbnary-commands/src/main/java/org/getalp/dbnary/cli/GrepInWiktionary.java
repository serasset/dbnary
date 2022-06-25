package org.getalp.dbnary.cli;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.text.TextStringBuilder;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;
import org.getalp.dbnary.cli.mixins.BatchExtractorMixin;
import org.getalp.dbnary.cli.mixins.WiktionaryIndexMixin;
import org.getalp.wiktionary.WiktionaryIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

@Command(name = "grep", mixinStandardHelpOptions = true,
    header = "grep a given pattern in all pages of a dump.",
    description = "This command looks for a given pattern in all pages of a dump and output "
        + "the matching pages.")
public class GrepInWiktionary implements Callable<Integer> {

  private final Logger log = LoggerFactory.getLogger(GrepInWiktionary.class);

  private static final XMLInputFactory2 xmlif;

  @Spec
  CommandSpec spec; // injected by picocli
  @ParentCommand
  protected DBnary parent; // picocli injects reference to parent command
  @Mixin
  private BatchExtractorMixin batch;
  @Mixin
  WiktionaryIndexMixin wi;
  private Pattern pattern;
  private Matcher match;


  @Parameters(index = "1", description = "The pattern to be searched for.", arity = "1")
  protected void setPattern(String patternString) {
    this.pattern = Pattern.compile(patternString);
    this.match = this.pattern.matcher("");
  }

  @Option(names = {"-l", "--pagename"}, description = "only show the name of the page.")
  protected boolean onlyShowFilename = false;

  @Option(names = {"--plain"}, description = "match is displayed without specific formatting.")
  protected boolean plainDisplay = false;

  @Option(names = {"--all-matches"}, description = "show all matches.")
  protected boolean showAllMatches = false;

  static {
    try {
      xmlif = (XMLInputFactory2) XMLInputFactory2.newInstance();
      xmlif.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
      xmlif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
      xmlif.setProperty(XMLInputFactory2.P_PRESERVE_LOCATION, Boolean.TRUE);
    } catch (Exception ex) {
      System.err.println("Cannot intialize XMLInputFactory while classloading WiktionaryIndexer.");
      throw new RuntimeException("Cannot initialize XMLInputFactory", ex);
    }
  }



  public Integer call() throws IOException {
    int nbPages = 0;
    // create new XMLStreamReader
    XMLStreamReader2 xmlr = null;
    try {
      // pass the file name. all relative entity references will be
      // resolved against this as base URI.
      xmlr = xmlif.createXMLStreamReader(wi.getDumpFile());

      // check if there are more events in the input stream
      String title = "";
      while (xmlr.hasNext()) {
        xmlr.next();
        if (xmlr.isStartElement() && xmlr.getLocalName().equals(WiktionaryIndexer.pageTag)) {
          title = "";
        } else if (xmlr.isStartElement()
            && xmlr.getLocalName().equals(WiktionaryIndexer.titleTag)) {
          title = xmlr.getElementText();
        } else if (xmlr.isStartElement() && xmlr.getLocalName().equals("text")) {
          String text = xmlr.getElementText();
          match.reset(text);
          while (match.find()) {
            showMatch(title, text, match);
            if (onlyShowFilename)
              break;
            if (!showAllMatches)
              break;
          }
        } else if (xmlr.isEndElement() && xmlr.getLocalName().equals(WiktionaryIndexer.pageTag)) {
          if (!title.equals("")) {
            if (title.contains(":"))
              continue;
            nbPages++;
            if (nbPages < batch.fromPage()) {
              continue;
            }
            if (nbPages > batch.toPage()) {
              break;
            }
          }
        }
      }
    } catch (XMLStreamException ex) {
      log.error(ex.getLocalizedMessage());

      if (ex.getNestedException() != null) {
        log.error("  Nested Exception: " + ex.getNestedException().getLocalizedMessage());
      }
      throw new IOException("XML Stream Exception while reading dump", ex);
    } catch (Exception ex) {
      log.error("Unexpected Exception: ", ex);
    } finally {
      try {
        if (xmlr != null) {
          xmlr.close();
        }
      } catch (XMLStreamException ex) {
        log.error("Exception while closing xml stream. ", ex);
      }
    }
    return 0;
  }

  private void showMatch(String title, String text, Matcher match) {
    spec.commandLine().getOut().write(title);
    if (onlyShowFilename)
      return;
    spec.commandLine().getOut().write(": ");
    if (plainDisplay) {
      spec.commandLine().getOut().write(showPlainMatch(text, match.start(), match.end()));
    } else {
      spec.commandLine().getOut().write(showMatchInContext(text, match.start(), match.end()));
    }
    spec.commandLine().getOut().flush();
  }

  private static String showMatchInContext(String text, int start, int end) {
    TextStringBuilder res = new TextStringBuilder(120);
    int before = start - 40;
    int after = end + 40;
    int from = before < 0 ? 0 : before;
    int to = after > text.length() ? text.length() : after;
    res.appendFixedWidthPadLeft(text.substring(from, start).replace('\n', '\u23CE'), 40, ' ');
    res.append("___");
    res.appendFixedWidthPadRight(text.substring(start, end).replace('\n', '\u23CE'), 10, ' ');
    res.append("___");
    res.appendFixedWidthPadRight(text.substring(end, to).replace('\n', '\u23CE'), 40, ' ');
    res.appendNewLine();
    return res.toString();
  }

  private static String showPlainMatch(String text, int start, int end) {
    return text.substring(start, end).replace('\n', '\u23CE') + "\n";
  }
}
