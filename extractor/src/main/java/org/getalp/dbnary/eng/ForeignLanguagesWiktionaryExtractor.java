package org.getalp.dbnary.eng;

import java.util.regex.Matcher;

import org.getalp.dbnary.IWiktionaryDataHandler;

public class ForeignLanguagesWiktionaryExtractor extends WiktionaryExtractor {

	public ForeignLanguagesWiktionaryExtractor(IWiktionaryDataHandler wdh) {
		super(wdh);
	}

	@Override
	public void extractData() {
        extractData(true);
    }

/*        // System.out.println(pageContent);
        Matcher languageFilter = sectionPattern.matcher(pageContent);
        int startForeignSection = -1;
		String previousLang = null;
		String lang = null;
        while (languageFilter.find()) {
        	lang = getLanguageInHeader(languageFilter);
            if (lang != null && !lang.equals("English") && languageFilter.group().charAt(2) != '=') {
				if (startForeignSection != -1) {
					extractForeignData(startForeignSection, languageFilter.start(), lang);
				}
				previousLang = lang;
				startForeignSection = languageFilter.end();
			} else {
				if (startForeignSection != -1) {
					extractForeignData(startForeignSection, languageFilter.start(), previousLang);
				}
				previousLang = null;
				startForeignSection = -1;
			}
        }
        // Either the filter is at end of sequence or on English language header.
        if (languageFilter.hitEnd()) {
        	if (startForeignSection != -1) {
				extractForeignData(startForeignSection, pageContent.length(), previousLang);
			}
        }
	}
*/

	public String getLanguageInHeader(Matcher m) {
		if (null != m.group(1).trim()){
			return m.group(1).trim();
		}
		else if (null != m.group(2)){
			return m.group(2);
		}
		else 
			return null;
	}
}
