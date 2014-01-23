package org.getalp.dbnary;

import java.lang.reflect.InvocationTargetException;

import org.getalp.dbnary.fin.WiktionaryExtractor;


public class WiktionaryExtractorFactory {

	public static IWiktionaryExtractor getExtractor(String language, WiktionaryDataHandler wdh) {
		IWiktionaryExtractor we = null;
		
		if (language.equals("fra")) {
			we = new FrenchWiktionaryExtractor(wdh);
		} else if (language.equals("eng")) {
			we = new EnglishWiktionaryExtractor(wdh);
		} else if (language.equals("deu")) {
			we = new GermanWiktionaryExtractor(wdh);
		} else if (language.equals("ell")) {
			we = new GreekWiktionaryExtractor(wdh);
		} else if (language.equals("tur")) {
			we = new TurkishWiktionaryExtractor(wdh);
		} else {
			String cname = WiktionaryExtractorFactory.class.getCanonicalName();
			int dpos = cname.lastIndexOf('.');
			String pack = cname.substring(0, dpos);
			try {
				Class<?> wec = Class.forName(pack + "." + language + ".WiktionaryExtractor");
				we = (IWiktionaryExtractor) wec.getConstructor(WiktionaryDataHandler.class).newInstance(wdh);
			} catch (ClassNotFoundException e) {
				System.err.println("No wiktionary extractor found for " + language);
			} catch (InstantiationException e) {
				System.err.println("Could not instanciate wiktionary extractor for " + language);
			} catch (IllegalAccessException e) {
				System.err.println("Illegal access to wiktionary extractor for " + language);
			} catch (IllegalArgumentException e) {
				System.err.println("Illegal argument passed to wiktionary extractor's constructor for " + language);
				e.printStackTrace(System.err);
			} catch (SecurityException e) {
				System.err.println("Security exception while instanciating wiktionary extractor for " + language);
				e.printStackTrace(System.err);
			} catch (InvocationTargetException e) {
				System.err.println("InvocationTargetException exception while instanciating wiktionary extractor for " + language);
				e.printStackTrace(System.err);
			} catch (NoSuchMethodException e) {
				System.err.println("No appropriate constructor when instanciating wiktionary extractor for " + language);
			}
		}
		return we;
	}

}
