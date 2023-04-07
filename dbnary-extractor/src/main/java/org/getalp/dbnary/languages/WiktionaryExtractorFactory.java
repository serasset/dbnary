package org.getalp.dbnary.languages;

import java.lang.reflect.InvocationTargetException;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.IWiktionaryExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WiktionaryExtractorFactory {

  private static final Logger log = LoggerFactory.getLogger(WiktionaryExtractorFactory.class);
  private static final String EXTRACTOR_CLASSNAME = "WiktionaryExtractor";

  public static IWiktionaryExtractor getExtractor(String language, IWiktionaryDataHandler wdh) {
    IWiktionaryExtractor we = null;

    String cname = WiktionaryExtractorFactory.class.getCanonicalName();
    int dpos = cname.lastIndexOf('.');
    String pack = cname.substring(0, dpos);
    try {
      Class<?> wec = Class.forName(pack + "." + language + "." + EXTRACTOR_CLASSNAME);
      we = (IWiktionaryExtractor) wec.getConstructor(IWiktionaryDataHandler.class).newInstance(wdh);
    } catch (ClassNotFoundException e) {
      log.warn("No wiktionary extractor found for {}", language);
    } catch (InstantiationException e) {
      log.warn("Could not instanciate wiktionary extractor for " + language);
    } catch (IllegalAccessException e) {
      log.warn("Illegal access to wiktionary extractor for " + language);
    } catch (IllegalArgumentException e) {
      System.err
          .println("Illegal argument passed to wiktionary extractor's constructor for " + language);
      e.printStackTrace(System.err);
    } catch (SecurityException e) {
      System.err
          .println("Security exception while instanciating wiktionary extractor for " + language);
      e.printStackTrace(System.err);
    } catch (InvocationTargetException e) {
      log.warn(
          "InvocationTargetException exception while instanciating wiktionary extractor for "
              + language);
      e.printStackTrace(System.err);
    } catch (NoSuchMethodException e) {
      log.error(
          "No appropriate constructor when instanciating wiktionary extractor for " + language);
    }
    return we;
  }

}
