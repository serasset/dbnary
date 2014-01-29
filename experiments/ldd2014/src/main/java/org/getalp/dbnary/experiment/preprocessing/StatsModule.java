package org.getalp.dbnary.experiment.preprocessing;

import java.util.HashMap;
import java.util.Map;
import java.io.PrintStream;

public class StatsModule {
	private String lang;
	private int nbTranslations = 0;
	private int nbGlosses = 0;
	private Map<String,String> glossesWithSenseNumber;
	private int nbGlossesWithText = 0;
	private int nbGlossesWithSensNumberAndDescription = 0;
	
	public StatsModule(String lang) {
		this.lang = lang;
		this.glossesWithSenseNumber = new HashMap<String,String>();
	}
	
	public void registerTranslation(String translationUri, StructuredGloss sg) {
		nbTranslations++;
		
		if (null != sg) {
			String senseNumbers = sg.getSenseNumber();
			String descr = sg.getGloss();
			if (null != senseNumbers || null != descr) {
				nbGlosses++;
				if (null != senseNumbers) {
					String sn = glossesWithSenseNumber.get(translationUri);
					if (null == sn) {
						glossesWithSenseNumber.put(translationUri, senseNumbers);
					} else {
						glossesWithSenseNumber.put(translationUri, sn + "," + senseNumbers);
					}
				}

				if (null != descr) {
					nbGlossesWithText++;
				}

				if (null != senseNumbers && null != descr) {
					nbGlossesWithSensNumberAndDescription++;
				}
			}
		}
	}
	
	public void displayStats(PrintStream w) {
		w.format("%s & $%d$ & $%d$ & $%d$ & $%d$ & $%d$ \\\\\n", lang, nbTranslations, nbGlosses, nbGlossesWithText, glossesWithSenseNumber.size(), nbGlossesWithSensNumberAndDescription);
	}

}
