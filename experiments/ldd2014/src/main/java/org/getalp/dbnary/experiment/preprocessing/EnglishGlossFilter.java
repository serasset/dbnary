package org.getalp.dbnary.experiment.preprocessing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnglishGlossFilter extends GlossFilter {

	
	private static String simpleSenseNumberingRegExp = "^([^\\|]*)\\|(\\d+*)$";
	private static Pattern simpleSenseNumberingPattern = Pattern.compile(simpleSenseNumberingRegExp);
	private static Matcher simpleSenseNumberingMatcher = simpleSenseNumberingPattern.matcher("");

	private static String glossNumSenseNumberingRegExp = "^(.*)\\s*\\((\\d)\\)(?:[\\p{Punct}\\s])*$";
	private static Pattern glossNumSenseNumberingPattern = Pattern.compile(glossNumSenseNumberingRegExp);
	private static Matcher glossNumSenseNumberingMatcher = glossNumSenseNumberingPattern.matcher("");

	private static String senseDashGlossRegExp = "^\\s*(\\d)\\s*(?:[-:\\)\\.])\\s*(.*)\\s*$";
	private static Pattern senseDashGlossPattern = Pattern.compile(senseDashGlossRegExp);
	private static Matcher senseDashGlossMatcher = senseDashGlossPattern.matcher("");
	
	public StructuredGloss extractGlossStructure(String rawGloss) {
		rawGloss = normalize(rawGloss);
		if (rawGloss.length() == 0) return null;
		simpleSenseNumberingMatcher.reset(rawGloss);
		if (simpleSenseNumberingMatcher.matches()) {
			return new StructuredGloss(simpleSenseNumberingMatcher.group(2), simpleSenseNumberingMatcher.group(1));
		}
		glossNumSenseNumberingMatcher.reset(rawGloss);
		if (glossNumSenseNumberingMatcher.matches()) {
			return new StructuredGloss(glossNumSenseNumberingMatcher.group(2), glossNumSenseNumberingMatcher.group(1));
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
