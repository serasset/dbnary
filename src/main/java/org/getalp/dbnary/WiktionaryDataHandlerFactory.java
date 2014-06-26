package org.getalp.dbnary;

import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WiktionaryDataHandlerFactory {
	private static Logger log = LoggerFactory.getLogger(WiktionaryDataHandlerFactory.class);

	public static WiktionaryDataHandler getDataHandler(String language) {
		WiktionaryDataHandler wdh = null;

		String cname = WiktionaryDataHandlerFactory.class.getCanonicalName();
		int dpos = cname.lastIndexOf('.');
		String pack = cname.substring(0, dpos);
		try {
			Class<?> wdhc = Class.forName(pack + "." + language + ".WiktionaryDataHandler");
			wdh = (WiktionaryDataHandler) wdhc.getConstructor(String.class).newInstance(language);
		} catch (ClassNotFoundException e) {
			log.debug("No wiktionary data handler found for {}", language);
		} catch (InstantiationException e) {
			log.debug("Could not instanciate wiktionary data handler for {}", language);
		} catch (IllegalAccessException e) {
			log.debug("Illegal access to wiktionary data handler for {}", language);
		} catch (IllegalArgumentException e) {
			log.debug("Illegal argument passed to wiktionary data handler's constructor for {}", language);
			e.printStackTrace(System.err);
		} catch (SecurityException e) {
			log.debug("Security exception while instanciating wiktionary data handler for {}", language);
			e.printStackTrace(System.err);
		} catch (InvocationTargetException e) {
			log.debug("InvocationTargetException exception while instanciating wiktionary data handler for {}", language);
			e.printStackTrace(System.err);
		} catch (NoSuchMethodException e) {
			log.debug("No appropriate constructor when instanciating wiktionary data handler for {}", language);
		}

		if (null == wdh) {
			wdh = new LemonBasedRDFDataHandler(language);
		}

		return wdh;
	}

}
