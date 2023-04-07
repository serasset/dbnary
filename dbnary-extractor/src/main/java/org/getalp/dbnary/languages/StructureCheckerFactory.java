package org.getalp.dbnary.languages;

import java.lang.reflect.InvocationTargetException;
import org.getalp.dbnary.api.IStructureChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class StructureCheckerFactory {

  private static final Logger log = LoggerFactory.getLogger(StructureCheckerFactory.class);
  private static final String STRUCTURE_CHECKER_CLASSNAME = "WiktionaryStructureChecker";

  public static IStructureChecker getStructureChecker(String language,
      String tdbDir) {
    IStructureChecker wsc = null;

    String cname = StructureCheckerFactory.class.getCanonicalName();
    int dpos = cname.lastIndexOf('.');
    String pack = cname.substring(0, dpos);
    try {
      Class<?> wscc = Class.forName(pack + "." + language + "." + STRUCTURE_CHECKER_CLASSNAME);
      wsc = (IStructureChecker) wscc.getConstructor(String.class, String.class)
          .newInstance(language, tdbDir);
    } catch (ClassNotFoundException e) {
      log.error("No wiktionary structure checker found for {}", language);
      log.error(e.getLocalizedMessage());
    } catch (InstantiationException e) {
      log.error("Could not instanciate wiktionary structure checker for {}", language);
      log.error(e.getLocalizedMessage());
    } catch (IllegalAccessException e) {
      log.error("Illegal access to wiktionary structure checker for {}", language);
      log.error(e.getLocalizedMessage());
    } catch (IllegalArgumentException e) {
      log.error("Illegal argument passed to wiktionary structure checker constructor for {}",
          language);
      log.error(e.getLocalizedMessage());
    } catch (SecurityException e) {
      log.error("Security exception while instantiating structure checker handler for {}",
          language);
      log.error(e.getLocalizedMessage());
    } catch (InvocationTargetException e) {
      log.error(
          "InvocationTargetException exception while instantiating wiktionary structure checker for {}",
          language);
      log.error(e.getLocalizedMessage());
    } catch (NoSuchMethodException e) {
      log.error("No appropriate constructor when instantiating wiktionary structure checker for {}",
          language);
      log.error(e.getLocalizedMessage());
    }

    return wsc;
  }

}
