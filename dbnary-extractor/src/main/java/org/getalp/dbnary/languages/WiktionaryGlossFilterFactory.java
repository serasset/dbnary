package org.getalp.dbnary.languages;

import java.lang.reflect.InvocationTargetException;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WiktionaryGlossFilterFactory {

  private static final Logger log = LoggerFactory.getLogger(WiktionaryGlossFilterFactory.class);
  private static final String GLOSS_FILTER_CLASSNAME = "GlossFilter";

  public static AbstractGlossFilter getGlossFilter(String language) {
    AbstractGlossFilter glossFilter = null;

    String cname = WiktionaryGlossFilterFactory.class.getCanonicalName();
    int dpos = cname.lastIndexOf('.');
    String pack = cname.substring(0, dpos);
    try {
      Class<?> wdhc = Class.forName(pack + "." + language + "." + GLOSS_FILTER_CLASSNAME);
      glossFilter = (AbstractGlossFilter) wdhc.getConstructor().newInstance();
    } catch (ClassNotFoundException e) {
      log.warn("No wiktionary gloss filter found for {}", language);
    } catch (InstantiationException e) {
      log.warn("Could not instanciate wiktionary gloss filter for {}", language);
    } catch (IllegalAccessException e) {
      log.warn("Illegal access to wiktionary gloss filter for {}", language);
    } catch (IllegalArgumentException e) {
      log.warn("No constructor {}() for {}", GLOSS_FILTER_CLASSNAME, language);
    } catch (SecurityException e) {
      log.error("Security exception while instanciating wiktionary gloss filter for {}", language);
    } catch (InvocationTargetException e) {
      log.warn(
          "InvocationTargetException exception while instanciating wiktionary gloss filter for {}",
          language);
      e.printStackTrace(System.err);
    } catch (NoSuchMethodException e) {
      log.warn("No appropriate constructor when instanciating wiktionary gloss filter for {}",
          language);
    }

    if (null == glossFilter) {
      log.warn("Using default gloss filter for {}.", language);
      glossFilter = new DefaultGlossFilter();
    }
    return glossFilter;
  }

}
