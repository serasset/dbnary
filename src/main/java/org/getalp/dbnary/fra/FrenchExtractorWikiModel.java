package org.getalp.dbnary.fra;

import java.util.Locale;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.getalp.dbnary.PropertyResourcePair;

import org.getalp.dbnary.DbnaryWikiModel;
import org.getalp.dbnary.WiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;

import org.getalp.dbnary.LexinfoOnt;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import org.w3c.dom.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jakse
 */

public class FrenchExtractorWikiModel extends DbnaryWikiModel {
	private static Logger log = LoggerFactory.getLogger(FrenchExtractorWikiModel.class);

	private static Pattern frAccordPattern = Pattern.compile("^\\{\\{(?:fr-accord|fr-rég)");

	private WiktionaryDataHandler wdh;

	public FrenchExtractorWikiModel(WiktionaryDataHandler wdh, WiktionaryIndex wi, Locale locale, String imageBaseURL, String linkBaseURL) {
		super(wdh, wi, locale, imageBaseURL, linkBaseURL);
		this.wdh = wdh;
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

			if (tdList.getLength() < 2) {
				log.debug("Missing cells in the conjugation table for '" + delegate.currentLexEntry() + "'");
			} else {
				delegate.registerOtherForm(
					tdList.item(1).getTextContent().trim()
				);
			}
		}
	}

	public int handleImpersonnalTableConjugation(NodeList tables) {
		for (int i = 0; i < tables.getLength(); i++) {
			Element table = (Element) tables.item(i);
			Node modeTH = table.getElementsByTagName("th").item(0);
			if (modeTH != null && modeTH.getTextContent().replace('\u00A0', ' ').trim().equals("Mode")) {
				handleImpersonnalTableConjugation(table);
				return i;
			}
		}

		log.error("Cannot find the impersonal mood table for '" + delegate.currentLexEntry() + "'");
		return -1;
	}

	public void  handleImpersonnalTableConjugation(Element impersonalMoodTable) {
		if (impersonalMoodTable == null) {
			log.error("impersonalMoodTable is null for '" + delegate.currentLexEntry() + "'");
		} else {
			NodeList interestingTDs = ((Element) (impersonalMoodTable.getElementsByTagName("tr").item(3)))
			                          .getElementsByTagName("td");

	        if (interestingTDs.getLength() < 3) {
				log.error("Cannot get present and past participle of '" + delegate.currentLexEntry() + "'");
	        } else {
				String presentParticiple = interestingTDs.item(2).getTextContent();
				delegate.registerOtherForm(presentParticiple.trim());

		        if (interestingTDs.getLength() < 6) {
					log.error("Cannot get past participle of '" + delegate.currentLexEntry() + "'");
		        } else {
					String pastParticiple = interestingTDs.item(5).getTextContent();
					delegate.registerOtherForm(pastParticiple.trim());
				}
	        }
        }
	}

	public void handleConjugationAtom(Element parent) {
		if (parent == null) {
			log.error("Cannot get the element containing the conjugation tables of '" + delegate.currentLexEntry() + "'");
		}

		NodeList tables = parent.getElementsByTagName("table");

		int impersonnalTableIndex = handleImpersonnalTableConjugation(tables);

		for (int i = 1 + impersonnalTableIndex; i < tables.getLength(); i++) {
			handleConjugationTable(tables, i);
		}
	}

	public void parseConjugation(String conjugationTemplateCall) {
		// Render the conjugation to html, while ignoring the example template

		Document doc = wikicodeToHtmlDOM(conjugationTemplateCall);

		if (doc == null) {
			return; // failing silently: error message already given.
		}

		handleConjugationAtom(doc.getDocumentElement());
	}

	public void parseImpersonnalTableConjugation(String conjugationTemplateCall) {
		Document doc = wikicodeToHtmlDOM(conjugationTemplateCall);
		if (doc == null) {
			return; // failing silently: error message already given.
		}

		handleImpersonnalTableConjugation(doc.getDocumentElement()); // "Orthographe traditionnelle"

	}

	private void addAtomicMorphologicalInfo(HashSet<PropertyResourcePair> properties, NodeList list) {
		for (int i = 0; i < list.getLength(); i++) {
			WiktionaryExtractor.addAtomicMorphologicalInfo(properties, list.item(i).getTextContent());
		}
	}

	public void parseOtherForm(String templateCall) {
		Document doc = wikicodeToHtmlDOM(templateCall);

		Matcher frAccordMatcher = frAccordPattern.matcher(templateCall);

		if (doc == null) {
			return; // failing silently: error message already given.
		}

		NodeList links = doc.getElementsByTagName("a");
		Locale fr = new Locale("fr");

		for (int i = 0; i < links.getLength(); i++) {
			Node a = links.item(i);

			String word = a.getTextContent().trim();
			Node title = a.getAttributes().getNamedItem("title");
			if (!word.equals(delegate.currentLexEntry()) && title != null && title.getTextContent().equals(word)) {

				HashSet<PropertyResourcePair> properties = new HashSet<PropertyResourcePair>();

				Node cell = a;
				while (cell != null && !cell.getNodeName().toLowerCase().equals("td")) {
					cell = cell.getParentNode();
				}

				if (cell == null) {
					log.error("Could not find the parent cell while extracting other form's template.");
					return;
				}


				Element cellParent = (Element) cell.getParentNode();
				addAtomicMorphologicalInfo(properties, cellParent.getElementsByTagName("th"));
				addAtomicMorphologicalInfo(properties, cellParent.getElementsByTagName("b"));

				NodeList tds = cellParent.getElementsByTagName("td");

				int colNumber = -1;

				for (int j = 0; j < tds.getLength(); j++) {
					if (tds.item(j).equals(cell)) {
						colNumber = j;
						break;
					}
				}

				if (colNumber != -1) {
					Element table = (Element) cellParent.getParentNode();
					NodeList trs = table.getElementsByTagName("tr");

					if (trs.getLength() == 0) {
						log.error("BUG: no lines found in the table!");
						return;
					}

					NodeList ths = ((Element) trs.item(0)).getElementsByTagName("th");

					if (ths.getLength() <= colNumber) {
						 log.error("BUG: not enougth cols in the row of the table!");
						 return;
					}

					Node th = ths.item(colNumber);
					String text = th.getTextContent();
					WiktionaryExtractor.addAtomicMorphologicalInfo(properties, text.trim().toLowerCase(WiktionaryExtractor.frLocale));
				}

				delegate.registerInflection(
					"",
					wdh.currentWiktionaryPos(),
					word,
					wdh.currentLexEntry(),
					wdh.currentDefinitionNumber(),
					properties
				);
			}
		}
	}
}