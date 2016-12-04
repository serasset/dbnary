package org.getalp.dbnary;

import org.getalp.iso639.ISO639_3;
import org.getalp.iso639.ISO639_3.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LangTools {
    static Logger log = LoggerFactory.getLogger(LangTools.class);

    public static String threeLettersCode(java.util.HashMap<String, String> h, String s) {
        if (s == null || s.equals("")) {
            log.debug("Null or empty input language");
            return s;
        }
        s = s.trim();
        s = s.toLowerCase();
        String res = getCode(s);
        if (res == null )
            if (h != null && h.containsKey(s)) {
                res = h.get(s);
	    } else {//e.g.: s.equals("image"), s.equals("w")
		    log.debug("Warning: language {} is not present in input hashmap and in iso639, getCode returns null.", s);
	    }


        return res;
    }

    public static String threeLettersCode(String s) {
        return threeLettersCode(null, s);
    }

    //E.g.: getCode("ita") returns "ita"
    //E.g.: getCode("it") returns "ita"
    public static String getCode(String lang) {
        return ISO639_3.sharedInstance.getIdCode(lang);
    }

    public static String getPart1(String language) {
        Lang l = ISO639_3.sharedInstance.getLang(language);

        if (l == null) {
            return null;
        }
        return l.getPart1();
    }

    public static String getPart1OrId(String lang) {
        Lang l = ISO639_3.sharedInstance.getLang(lang);
        if (l == null) {
            return null;
        }
        String p1 = l.getPart1();
        return (null != p1 && !"".equals(p1.trim())) ? l.getPart1() : l.getId();
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
