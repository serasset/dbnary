package org.getalp.dbnary;

import info.bliki.wiki.filter.WikipediaParser;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.blexisma.api.ISO639_3;

public class RussianTranslationExtractorWikiModel extends DbnaryWikiModel {
	
	// static Set<String> ignoredTemplates = new TreeSet<String>();
	// static {
	// 	ignoredTemplates.add("Wikipedia");
	// 	ignoredTemplates.add("Incorrect");
	// }
	
	private WiktionaryDataHandler delegate;
	
	
	public RussianTranslationExtractorWikiModel(WiktionaryDataHandler we, Locale locale, String imageBaseURL, String linkBaseURL) {
		this(we, (WiktionaryIndex) null, locale, imageBaseURL, linkBaseURL);
	}
	
	public RussianTranslationExtractorWikiModel(WiktionaryDataHandler we, WiktionaryIndex wi, Locale locale, String imageBaseURL, String linkBaseURL) {
		super(wi, locale, imageBaseURL, linkBaseURL);
		this.delegate = we;
	}

	public void parseTranslationBlock(String block) {
		initialize();
		if (block == null) {
			return;
		}
		WikipediaParser.parse(block, this, true, null);
		initialize();
	}

	

	@Override
	public void substituteTemplateCall(String templateName,
			Map<String, String> parameterMap, Appendable writer)
			throws IOException {
		if ("перев-блок".equals(templateName)) {
			// This is a translation block
            // System.err.println("Template call to translation block");
            // System.err.println("Map is: " + parameterMap.toString());
			String gloss = parameterMap.get("1");
			if (null != gloss) gloss = gloss.trim();
			for (Entry<String, String> kv : parameterMap.entrySet()) {
				if ("1".equals(kv.getKey())) continue;
				String lang = ISO639_3.sharedInstance.getIdCode(kv.getKey());
				if (null == lang) lang = kv.getKey();
				extractTranslations(gloss, lang, kv.getValue());
			}
		} else {
			// Just ignore the other template calls (uncomment to expand the template calls).
			// super.substituteTemplateCall(templateName, parameterMap, writer);
		}
	}

	static Pattern parens = Pattern.compile("\\(([^\\)]*)\\)");

	private void extractTranslations(String gloss, String lang, String value) {
		// First black out commas that appear inside a pair of parenthesis
		value = blackoutCommas(value);
		String translations[] = value.split("[,;]");
		for (int i = 0; i < translations.length; i++) {
			extractTranslation(gloss, lang, translations[i]);
		}
	}

	private String blackoutCommas(String value) {
		Matcher m = parens.matcher(value);
        StringBuffer sb = new StringBuffer((int) (value.length() * 1.4));
        String inParens;
        while (m.find()) {
        	inParens = m.group(1);
        	inParens = inParens.replaceAll(";", "@@SEMICOLON@@");
        	inParens = inParens.replaceAll(",", "@@COMMA@@");
        	m.appendReplacement(sb, "(" + inParens + ")");
        }
        m.appendTail(sb);
		return sb.toString();
	}

	private String restoreCommas(String value) {
		value = value.replaceAll("@@SEMICOLON@@", ";");
		value = value.replaceAll("@@COMMA@@", ",");
		return value;
	}

	static Pattern linkPattern = Pattern.compile("\\[\\[([^\\]]*)\\]\\]");
	static Pattern macroPattern = Pattern.compile("\\{\\{([^\\}]*)\\}\\}");

	private void extractTranslation(String gloss, String lang, String trans) {
		trans = restoreCommas(trans);
		Matcher macros = macroPattern.matcher(trans);
		String word = macros.replaceAll("");
		Matcher links = linkPattern.matcher(word);
		word = links.replaceAll("$1").trim();
		StringBuffer usage = new StringBuffer();
		StringBuffer w = new StringBuffer();
		Matcher m = parens.matcher(word);
		while (m.find()) {
			usage.append(m.group(0));
			usage.append(" ");
			m.appendReplacement(w, " ");
		}
		m.appendTail(w);
		word = w.toString().trim();
		if (usage.length() > 0) usage.deleteCharAt(usage.length()-1);
		if (word != null && !word.equals(""))
			delegate.registerTranslation(lang, gloss, usage.toString(), word);
	}

}
