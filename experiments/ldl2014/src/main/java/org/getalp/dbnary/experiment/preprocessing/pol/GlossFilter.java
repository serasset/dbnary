package org.getalp.dbnary.experiment.preprocessing.pol;

import org.getalp.dbnary.experiment.preprocessing.AbstractGlossFilter;
import org.getalp.dbnary.experiment.preprocessing.StructuredGloss;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GlossFilter extends AbstractGlossFilter {

	private static String sensenum = "(?:\\d+(?:\\.\\d+)*)";

	private static String senseNumOnlyRegExp = "^\\s*(" + sensenum + ")\\s*$";
	private static Pattern senseNumOnlyPattern = Pattern.compile(senseNumOnlyRegExp);
	private static Matcher senseNumOnlyMatcher = senseNumOnlyPattern.matcher("");

	private static String senseNumOnStartRegexp = "^\\s*(" + sensenum + ")\\s*(.*)$";
	private static Pattern senseNumOnStartPattern = Pattern.compile(senseNumOnStartRegexp);
	private static Matcher senseNumOnStartMatcher = senseNumOnStartPattern.matcher("");

	public StructuredGloss extractGlossStructure(String rawGloss) {

		rawGloss = normalize(rawGloss);
		if (rawGloss.length() == 0) return null;

		senseNumOnlyMatcher.reset(rawGloss);
		if (senseNumOnlyMatcher.matches()) {
			String n = senseNumOnlyMatcher.group(1);
			return new StructuredGloss(n, null);
		}

		senseNumOnStartMatcher.reset(rawGloss);
		if (senseNumOnStartMatcher.matches()) {
			String n = senseNumOnStartMatcher.group(1);
			String g = senseNumOnStartMatcher.group(2);
			return new StructuredGloss(n, g);
		}

		return new StructuredGloss(null, rawGloss);
	}
}
