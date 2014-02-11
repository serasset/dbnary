package org.getalp.dbnary.experiment.preprocessing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PortugueseGlossFilter extends GlossFilter {

		
	private static String aTrierRegExp;
	static {
		aTrierRegExp = (new StringBuffer())
				.append("(?:")
		        .append("Traduções")
		        .append(")|(?:")
		        .append("(?:A|a)\\s+ser(?:em)\\s+classificado")
		        .append(")").toString();
	}
	private static Pattern aTrierPattern = Pattern.compile(aTrierRegExp);
	private static Matcher aTrierMatcher = aTrierPattern.matcher("");
	
	private static String sensenum = "(?:(?:\\d+(?:\\.\\d+)?)|e|&|\\s|,|-|—|ou|a)+";

	private static String numSenseGlossRegExp = "^\\(?[Dd]e\\s+(" + sensenum + ")\\)?(.*)$";
	private static Pattern numSenseGlossPattern = Pattern.compile(numSenseGlossRegExp);
	private static Matcher numSenseGlossMatcher = numSenseGlossPattern.matcher("");
		
	
	public StructuredGloss extractGlossStructure(String rawGloss) {
		aTrierMatcher.reset(rawGloss);
		if (aTrierMatcher.find()) return null; // non relevant gloss should be discarded
		
		rawGloss = normalize(rawGloss);
		if (rawGloss.length() == 0) return null;

		numSenseGlossMatcher.reset(rawGloss);
		if (numSenseGlossMatcher.matches()) {
			String g = numSenseGlossMatcher.group(2);
			if (null != g && g.trim().length() == 0) {
				g = null;
			}
			return new StructuredGloss(numSenseGlossMatcher.group(1), g);
		}
		
		return new StructuredGloss(null, rawGloss);
	}
}