package org.getalp.dbnary.experiment.preprocessing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FrenchGlossFilter extends GlossFilter {

	private static String aTrierRegExp;
	static {
		aTrierRegExp = (new StringBuffer())
				.append("(?:")
		        .append("à trier")
		        .append(")|(?:")
		        .append("À trier")
		        .append(")|(?:")
		        .append("Traduction")
		        .append(")|(?:")
		        .append("traduction")
		        .append(")|(?:")
		        .append("à classer")
		        .append(")|(?:")
		        .append("À classer")
		        .append(")").toString();
	}
	private static Pattern aTrierPattern = Pattern.compile(aTrierRegExp);
	private static Matcher aTrierMatcher = aTrierPattern.matcher("");
	
	private static String simpleSenseNumberingRegExp = "^([^\\|]*)\\|(.*)$";
	private static Pattern simpleSenseNumberingPattern = Pattern.compile(simpleSenseNumberingRegExp);
	private static Matcher simpleSenseNumberingMatcher = simpleSenseNumberingPattern.matcher("");

	private static String numGlossSenseNumberingRegExp = "^\\s*\\(([^\\)]*)\\)(.*)$";
	private static Pattern numGlossSenseNumberingPattern = Pattern.compile(numGlossSenseNumberingRegExp);
	private static Matcher numGlossSenseNumberingMatcher = numGlossSenseNumberingPattern.matcher("");
	
	private static String glossNumSenseNumberingRegExp = "^(.*)\\s*\\(([^\\)]*)\\)(?:[\\p{Punct}\\s])*$";
	private static Pattern glossNumSenseNumberingPattern = Pattern.compile(glossNumSenseNumberingRegExp);
	private static Matcher glossNumSenseNumberingMatcher = glossNumSenseNumberingPattern.matcher("");

	private static String senseNRegExp = "^\\s*Sens\\s+(\\d+)\\s*$";
	private static Pattern senseNPattern = Pattern.compile(senseNRegExp);
	private static Matcher senseNMatcher = senseNPattern.matcher("");

	private static String senseDashGlossRegExp = "^\\s*((?:\\d|et|\\s|,)+)\\s*(?:[-:\\)\\.])\\s*(.*)\\s*$";
	private static Pattern senseDashGlossPattern = Pattern.compile(senseDashGlossRegExp);
	private static Matcher senseDashGlossMatcher = senseDashGlossPattern.matcher("");
	
	public StructuredGloss extractGlossStructure(String rawGloss) {
		aTrierMatcher.reset(rawGloss);
		if (aTrierMatcher.find()) return null; // non relevant gloss should be discarded
		rawGloss = normalize(rawGloss);
		if (rawGloss.length() == 0) return null;
		simpleSenseNumberingMatcher.reset(rawGloss);
		if (simpleSenseNumberingMatcher.matches()) {
			return new StructuredGloss(simpleSenseNumberingMatcher.group(2), simpleSenseNumberingMatcher.group(1));
		}
		numGlossSenseNumberingMatcher.reset(rawGloss);
		if (numGlossSenseNumberingMatcher.matches()) {
			return new StructuredGloss(numGlossSenseNumberingMatcher.group(1), numGlossSenseNumberingMatcher.group(2));
		}
		glossNumSenseNumberingMatcher.reset(rawGloss);
		if (glossNumSenseNumberingMatcher.matches()) {
			return new StructuredGloss(glossNumSenseNumberingMatcher.group(2), glossNumSenseNumberingMatcher.group(1));
		}
		senseNMatcher.reset(rawGloss);
		if (senseNMatcher.matches()) {
			return new StructuredGloss(senseNMatcher.group(1), null);
		}
		if (rawGloss.matches("[\\d\\s\\p{Punct}]+")) {
			return new StructuredGloss(rawGloss, null);
		}
		senseDashGlossMatcher.reset(rawGloss);
		if (senseDashGlossMatcher.matches()) {
			return new StructuredGloss(senseDashGlossMatcher.group(1), senseDashGlossMatcher.group(2));
		}
		
		// if (rawGloss.matches(".*\\d.*")) System.err.println("Digit in gloss: " + rawGloss );
		
		return new StructuredGloss(null, rawGloss);
	}
}
