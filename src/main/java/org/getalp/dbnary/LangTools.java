package org.getalp.dbnary;

import java.util.Iterator;
import org.getalp.blexisma.api.ISO639_3;

public class LangTools {
	public static String threeLettersCode(java.util.HashMap<String,String> h, String s) {
		if(s == null || s.equals("")) {
			return s;
		}

		s= s.trim();
		s=s.toLowerCase();
		String res = getCode(s);

		if (res == null && h != null && h.containsKey(s)) {
			s = h.get(s);
			res = getCode(s);
		}

		return res;
	}

	public static String threeLettersCode(String s) {
		return threeLettersCode(null, s);
	}

	public static String getCode(String lang) {
		return ISO639_3.sharedInstance.getIdCode(lang);
	}

	public static String normalize(String lang) {
		return normalize(lang, lang);
	}
	
	private static String normalize(String lang, String fallback) {
		String normLangCode = getCode(lang);

		if (normLangCode == null) {
			return fallback;
		}

		return normLangCode;
	}

	public static String inEnglish(String lang) {
		return ISO639_3.sharedInstance.getLanguageNameInEnglish(lang);
	}

	public static String getTerm2Code(String l) {
		return ISO639_3.sharedInstance.getTerm2Code(l);
	}
}
