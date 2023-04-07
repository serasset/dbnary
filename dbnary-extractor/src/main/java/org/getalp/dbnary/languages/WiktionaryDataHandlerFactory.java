package org.getalp.dbnary.languages;

import java.lang.reflect.InvocationTargetException;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WiktionaryDataHandlerFactory {

  private static final Logger log = LoggerFactory.getLogger(WiktionaryDataHandlerFactory.class);
  private static final String DATA_HANDLER_CLASSNAME = "WiktionaryDataHandler";

  public static IWiktionaryDataHandler getDataHandler(String language,
      String tdbDir) {
    IWiktionaryDataHandler wdh = null;

    String cname = WiktionaryDataHandlerFactory.class.getCanonicalName();
    int dpos = cname.lastIndexOf('.');
    String pack = cname.substring(0, dpos);
    try {
      Class<?> wdhc = Class.forName(pack + "." + language + "." + DATA_HANDLER_CLASSNAME);
      wdh = (IWiktionaryDataHandler) wdhc.getConstructor(String.class, String.class)
          .newInstance(language, tdbDir);
    } catch (ClassNotFoundException e) {
      log.warn("No wiktionary data handler found for {}", language);
    } catch (InstantiationException e) {
      log.warn("Could not instanciate wiktionary data handler for {}", language);
    } catch (IllegalAccessException e) {
      log.warn("Illegal access to wiktionary data handler for {}", language);
    } catch (IllegalArgumentException e) {
      log.warn("No constructor {}(String, String) for {}", DATA_HANDLER_CLASSNAME, language);
    } catch (SecurityException e) {
      log.error("Security exception while instanciating wiktionary data handler for {}", language);
    } catch (InvocationTargetException e) {
      log.warn(
          "InvocationTargetException exception while instanciating wiktionary data handler for {}",
          language);
      e.printStackTrace(System.err);
    } catch (NoSuchMethodException e) {
      log.warn("No appropriate constructor when instanciating wiktionary data handler for {}",
          language);
    }

    if (null == wdh) {
      log.warn("Using default data handler for {}.", language);
      wdh = new OntolexBasedRDFDataHandler(language, tdbDir);
    }
    return wdh;
  }

}
