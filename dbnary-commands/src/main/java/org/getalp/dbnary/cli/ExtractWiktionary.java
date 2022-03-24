package org.getalp.dbnary.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.concurrent.Callable;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.languages.OntolexBasedRDFDataHandler;
import org.getalp.wiktionary.WiktionaryIndexer;
import org.getalp.wiktionary.WiktionaryIndexerException;
import org.getalp.dbnary.cli.mixins.BatchExtractorMixin;
import org.getalp.dbnary.cli.mixins.Extractor;
import org.getalp.dbnary.cli.utils.ExtractionPreferences;
import org.getalp.dbnary.cli.utils.VersionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "extract", mixinStandardHelpOptions = true,
    header = "extract all pages from a dump and write resulting RDF files.",
    description = "Process all pages and extract lexical data according to options that are passed "
        + "to the program. The extracted lexical data is encoded as RDF graphs using ontolex, "
        + "lexinfo, olia and other standard vocabularies.")
public class ExtractWiktionary extends Extractor implements Callable<Integer> {

  private static final Logger log = LoggerFactory.getLogger(OntolexBasedRDFDataHandler.class);

  private ExtractionPreferences prefs;
  @Mixin
  BatchExtractorMixin batch;

  @Option(names = {"--date", "--suffix"})
  protected String suffix;

  public static final XMLInputFactory2 xmlif;

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

  @Override
  public Integer call() {
    Integer returnCode;
    try {
      returnCode = prepareExtraction();
    } catch (WiktionaryIndexerException e) {
      spec.commandLine().getErr().println("Could not read dump.");
      // e.printStackTrace(spec.commandLine().getErr());
      return -1;
    } catch (IOException e) {
      spec.commandLine().getErr().println("IOException while preparing extraction, aborting.");
      // e.printStackTrace(spec.commandLine().getErr());
      return -1;
    }
    if (returnCode != 0)
      return returnCode;
    try {
      return extract();
    } catch (IOException e) {
      spec.commandLine().getErr().format("IOException while extracting, stopping: %s%n",
          e.getLocalizedMessage());
      // e.printStackTrace(spec.commandLine().getErr());
      return -1;
    }
  }

  protected Integer prepareExtraction() throws WiktionaryIndexerException, IOException {
    prefs = new ExtractionPreferences(parent.dbnaryDir);

    if (batch.useTdb())
      batch.createTDBTempDirectory();
    setupHandlers(batch.tdbDir());

    if (parent.isVerbose()) {
      PrintWriter err = spec.commandLine().getErr();
      err.println("Extracting Wiktionary Dump:");
      err.println("  Language: " + lm.getLanguage());
      err.println("  Dump: " + wi.getDumpFile());
      err.println("  TDB : " + batch.tdbDir());
      err.println("  Format : " + features.getOutputFormat());
      features.getEndolexFeatures()
          .forEach(f -> err.format("  %s : %s%n", f.toString(), prefs.outputFileForFeature(f,
              lm.getLanguage(), suffix, features.getOutputFormat(), batch.doCompress(), false)));
      if (null != features.getExolexFeatures())
        features.getExolexFeatures()
            .forEach(f -> err.format("  %s : %s%n", f.toString(), prefs.outputFileForFeature(f,
                lm.getLanguage(), suffix, features.getOutputFormat(), batch.doCompress(), true)));
    }
    return 0;
  }

  public Integer extract() throws IOException {

    try {
      // create new XMLStreamReader
      long startTime = System.currentTimeMillis();
      long totalRelevantTime = 0, relevantStartTime, relevantTimeOfLastThousands;
      int nbPages = 0, nbRelevantPages = 0;
      relevantTimeOfLastThousands = System.currentTimeMillis();

      XMLStreamReader2 xmlr = null;
      try {
        // pass the file name. all relative entity references will be
        // resolved against this as base URI.
        xmlr = xmlif.createXMLStreamReader(wi.getDumpFile());

        // check if there are more events in the input stream
        String title = "";
        String page = "";
        while (xmlr.hasNext()) {
          xmlr.next();
          if (xmlr.isStartElement() && xmlr.getLocalName().equals(WiktionaryIndexer.pageTag)) {
            title = "";
            page = "";
          } else if (xmlr.isStartElement()
              && xmlr.getLocalName().equals(WiktionaryIndexer.titleTag)) {
            title = xmlr.getElementText();
          } else if (xmlr.isStartElement() && xmlr.getLocalName().equals("text")) {
            page = xmlr.getElementText();
          } else if (xmlr.isEndElement() && xmlr.getLocalName().equals(WiktionaryIndexer.pageTag)) {
            if (!title.equals("")) {
              nbPages++;
              int nbnodes = wdh.nbEntries();
              if (nbPages < batch.fromPage()) {
                continue;
              }
              if (nbPages > batch.toPage()) {
                break;
              }
              try {
                we.extractData(title, page);
              } catch (RuntimeException e) {
                spec.commandLine().getErr().println("Runtime exception while extracting  page<<"
                    + title + ">>, proceeding to next pages.");
                if (log.isDebugEnabled())
                  e.printStackTrace(spec.commandLine().getErr());
                spec.commandLine().getErr().println(e.getMessage());
                // e.printStackTrace();
              }
              if (nbnodes != wdh.nbEntries()) {
                totalRelevantTime = (System.currentTimeMillis() - startTime);
                nbRelevantPages++;
                if (nbRelevantPages % 1000 == 0) {
                  spec.commandLine().getErr()
                      .println("Extracted: " + nbRelevantPages + " pages in: "
                          + formatHMS(totalRelevantTime) + " / Average = "
                          + (totalRelevantTime / nbRelevantPages) + " ms/extracted page ("
                          + (System.currentTimeMillis() - relevantTimeOfLastThousands) / 1000
                          + " ms) (" + nbPages + " processed Pages)");
                  // System.err.println(" NbNodes = " + s.getNbNodes());
                  relevantTimeOfLastThousands = System.currentTimeMillis();
                }
              }
            }
          }
        }
      } catch (XMLStreamException ex) {
        spec.commandLine().getErr().println(ex.getMessage());

        if (ex.getNestedException() != null) {
          spec.commandLine().getErr()
              .println("  Nested Exception: " + ex.getNestedException().getMessage());
        }
        throw new IOException("XML Stream Exception while reading dump", ex);
      } catch (Exception ex) {
        spec.commandLine().getErr().println(ex.getMessage());
        // ex.printStackTrace();
      } finally {
        try {
          if (xmlr != null) {
            xmlr.close();
          }
        } catch (XMLStreamException ex) {
          spec.commandLine().getErr().println(ex.getMessage());
        }
      }

      spec.commandLine().getErr().println("Extracted " + nbRelevantPages + " pages in: "
          + formatHMS(totalRelevantTime) + " (" + nbPages + " scanned Pages)");

      relevantStartTime = System.currentTimeMillis();
      postProcessAfterExtraction(VersionProvider.getDumpVersion(wi.getDumpFile().getName()));
      totalRelevantTime = System.currentTimeMillis() - relevantStartTime;
      spec.commandLine().getErr().format("Post processed %d entries in %s%n", nbRelevantPages,
          formatHMS(totalRelevantTime));

      for (ExtractionFeature f : features.getEndolexFeatures()) {
        if (!f.equals(ExtractionFeature.HDT)) {
          saveBox(f, false);
        }
      }

      if (null != features.getExolexFeatures()) {
        for (ExtractionFeature f : features.getExolexFeatures()) {
          if (!f.equals(ExtractionFeature.HDT)) {
            saveBox(f, true);
          }
        }
      }

      if (wdh.getEndolexFeatureBox(ExtractionFeature.HDT) != null) {
        saveAllAsHDT(false);
      }
      if (wdh.getExolexFeatureBox(ExtractionFeature.HDT) != null) {
        saveAllAsHDT(true);
      }
    } finally {
      cleanupHandlers();
      // Force TDB dir deletion after language extraction to avoid disk exhaustion when the main
      // method is called by UpdateAndExtractDumps (in the same JDK instance).
      if (null != batch.tdbDir()) {
        try {
          FileUtils.deleteDirectory(new File(batch.tdbDir()));
        } catch (IOException e) {
          spec.commandLine().getErr().println("Caught " + e.getClass()
              + " when attempting to delete the temporary TDB directory " + batch.tdbDir());
          spec.commandLine().getErr().println(e.getLocalizedMessage());
        }
      }
    }
    return 0;
  }

  private String formatHMS(long durationInMillis) {
    Duration d = Duration.ofMillis(durationInMillis);
    long h = d.toHours();
    long m = d.toMinutes() % 60;
    long s = d.getSeconds() % 60;
    return String.format("%d:%2d:%2d", h, m, s);
  }

  private void saveBox(ExtractionFeature f, boolean isExolex) throws IOException {
    File of = prefs.outputFileForFeature(f, lm.getLanguage(), suffix, features.getOutputFormat(),
        batch.doCompress(), isExolex).toFile();
    Model model = isExolex ? wdh.getExolexFeatureBox(f) : wdh.getEndolexFeatureBox(f);
    try (OutputStream ostream =
        batch.doCompress() ? new BZip2CompressorOutputStream(new FileOutputStream(of))
            : new FileOutputStream(of)) {
      spec.commandLine().getErr().println(
          "Dumping " + features.getOutputFormat() + " representation of " + f.toString() + ".");
      wdh.dump(model, ostream, features.getOutputFormat());
    } catch (IOException e) {
      spec.commandLine().getErr()
          .println("Caught IOException while printing extracted data: " + e.getLocalizedMessage());
      // e.printStackTrace(spec.commandLine().getErr());
      throw e;
    }
  }

  private void saveAllAsHDT(boolean isExolex) throws IOException {
    File hdtOutputFile = prefs.outputFileForFeature(ExtractionFeature.HDT, lm.getLanguage(), suffix,
        features.getOutputFormat(), batch.doCompress(), isExolex).toFile();
    try (OutputStream ostream =
        batch.doCompress() ? new BZip2CompressorOutputStream(new FileOutputStream(hdtOutputFile))
            : new FileOutputStream(hdtOutputFile)) {
      spec.commandLine().getErr().format("Dumping all features as a single HDT file in %s.%n",
          hdtOutputFile);
      wdh.dumpAllFeaturesAsHDT(ostream, isExolex);
    } catch (IOException e) {
      spec.commandLine().getErr()
          .println("Caught IOException while producing HDT file: \n" + e.getLocalizedMessage());
      // e.printStackTrace(spec.commandLine().getErr());
      throw e;
    }
  }

  public static void main(String[] args) {
    new CommandLine(new ExtractWiktionary()).execute(args);
  }
}
