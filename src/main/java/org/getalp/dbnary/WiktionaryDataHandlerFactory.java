package org.getalp.dbnary;

import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WiktionaryDataHandlerFactory {
	private static Logger log = LoggerFactory.getLogger(WiktionaryDataHandlerFactory.class);

	public static WiktionaryDataHandler getDataHandler(String language) {
		WiktionaryDataHandler we = null;

		String cname = WiktionaryDataHandlerFactory.class.getCanonicalName();
		int dpos = cname.lastIndexOf('.');
		String pack = cname.substring(0, dpos);
		try {
			Class<?> wec = Class.forName(pack + "." + language + ".WiktionaryDataHandler");
			we = (WiktionaryDataHandler) wec.getConstructor(String.class).newInstance(language);
		} catch (ClassNotFoundException e) {
			log.debug("No wiktionary extractor found for {}", language);
		} catch (InstantiationException e) {
			log.debug("Could not instanciate wiktionary extractor for {}", language);
		} catch (IllegalAccessException e) {
			log.debug("Illegal access to wiktionary extractor for {}", language);
		} catch (IllegalArgumentException e) {
			log.debug("Illegal argument passed to wiktionary extractor's constructor for {}", language);
			e.printStackTrace(System.err);
		} catch (SecurityException e) {
			log.debug("Security exception while instanciating wiktionary extractor for {}", language);
			e.printStackTrace(System.err);
		} catch (InvocationTargetException e) {
			log.debug("InvocationTargetException exception while instanciating wiktionary extractor for {}", language);
			e.printStackTrace(System.err);
		} catch (NoSuchMethodException e) {
			log.debug("No appropriate constructor when instanciating wiktionary extractor for {}", language);
		}

		if (null == we) {
			we = new LemonBasedRDFDataHandler(language);
		}
		return we;
	}

}
