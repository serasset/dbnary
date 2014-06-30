package org.getalp.dbnary.fra;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.dbnary.DbnaryWikiModel;
import org.getalp.dbnary.WiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;

import org.w3c.dom.*;

/**
 * @author jakse
 */

public class FrenchExtractorWikiModel extends DbnaryWikiModel {
	public FrenchExtractorWikiModel(WiktionaryDataHandler wdh, WiktionaryIndex wi, Locale locale, String imageBaseURL, String linkBaseURL) {
		super(wdh, wi, locale, imageBaseURL, linkBaseURL);
	}

	public static Element adjacentDiv (Node ele) {
		ele = ele.getNextSibling();
		while (ele != null && !ele.equals("div")) {
			ele = ele.getNextSibling();
		}

		return (Element) ele;
	}

	public static boolean hasRealPreviousSibling(Node ele) {
		do {
			ele = ele.getPreviousSibling();
		} while (ele != null && ele.getNodeType() != Node.ELEMENT_NODE);

		return ele != null;
	}

	public void handleConjugationTable(NodeList tables, int tableIndex) {
		Element table = (Element) tables.item(tableIndex);
		if (table.getElementsByTagName("table").getLength() > 0) {
			// we ignore tables which contain <tables, as they don’t contain conjugations.
 			return;
		}

		if (table.getParentNode().getNodeName().equals("div")) {
			// we ignore the tables at the right of the page as they
			// give composed tenses.
			return;
		}

		NodeList lines = table.getElementsByTagName("tr");

		for (int i = 1; i < lines.getLength(); i++) {
			Element line = (Element) lines.item(i);
			NodeList tdList = line.getElementsByTagName("td");

			delegate.registerOtherForm(
				tdList.item(1).getTextContent().trim()
			);
		}
	}

	public void parseConjugation(String conjugationTemplateCall) {
		// Render the conjugation to html, while ignoring the example template

		Document doc = wikicodeToHtmlDOM(conjugationTemplateCall);

		if (doc == null) {
			return; // failing silently: error message already given.
		}

		NodeList tables = doc.getElementsByTagName("table");

		Element impersonalMoodTable = (Element) tables.item(1);

		NodeList interestingTDs = ((Element) (impersonalMoodTable.getElementsByTagName("tr").item(3)))
		                          .getElementsByTagName("td");

		String presentParticiple = interestingTDs.item(2).getTextContent();
		String pastParticiple    = interestingTDs.item(5).getTextContent();

		delegate.registerOtherForm(presentParticiple.trim());
		delegate.registerOtherForm(pastParticiple.trim());

		for (int i = 2; i < tables.getLength(); i++) {
			handleConjugationTable(tables, i);
		}
	}

	public void parseOtherForm(String templateCall) {
		Document doc = wikicodeToHtmlDOM(templateCall);

		if (doc == null) {
			return; // failing silently: error message already given.
		}


		NodeList links = doc.getElementsByTagName("a");

		for (int i = 0; i < links.getLength(); i++) {
			Node a = links.item(i);

			String word = a.getTextContent().trim();
			if (!word.equals(delegate.currentLexEntry()) && a.getAttributes().getNamedItem("href").getTextContent().toLowerCase(new Locale("fr")).equals("/" + word)) {
				delegate.registerOtherForm(word);
			}
		}
	}
}