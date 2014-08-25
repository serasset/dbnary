package org.getalp.dbnary.fra;

import java.util.Locale;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.getalp.dbnary.PropertyObjectPair;

import org.getalp.dbnary.DbnaryModel;
import org.getalp.dbnary.DbnaryWikiModel;
import org.getalp.dbnary.WiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;

import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.DBnaryOnt;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;

import org.w3c.dom.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jakse
 */

public class FrenchExtractorWikiModel extends DbnaryWikiModel {
	private static Logger log = LoggerFactory.getLogger(FrenchExtractorWikiModel.class);

	private static Literal trueLiteral = DbnaryModel.tBox.createTypedLiteral(true);

	private static Property extractedFromConjTable = DbnaryModel.tBox.createProperty(DBnaryOnt.getURI() + "extractedFromConjTable");
	private static Property extractedFromInflectionTable = DbnaryModel.tBox.createProperty(DBnaryOnt.getURI() + "extractedFromInflectionTable");

	private static Pattern frAccordPattern = Pattern.compile("^\\{\\{(?:fr-accord|fr-rég)");

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

	private void getMoodTense(Element table, HashSet<PropertyObjectPair> infos, NodeList lines) {
		if (lines.getLength() < 1) {
			log.debug("Missing lines in the conjugation table for '" + delegate.currentLexEntry() + "'");
			return;
		}

		// tense
		WiktionaryExtractor.addAtomicMorphologicalInfo(
			infos,
			lines.item(0).getTextContent().trim().toLowerCase(WiktionaryExtractor.frLocale)
		);

		Node parent = table.getParentNode();
		while (parent != null && parent.getNodeName().toLowerCase() != "div") {
			parent = parent.getParentNode();
		}

		if (parent == null) {
			log.debug("Cannot find mood in the conjugation table for '" + delegate.currentLexEntry() + "'");
			return;
		}

		while (parent != null && parent.getNodeName().toLowerCase() != "h3") {
			parent = parent.getPreviousSibling();
		}

		if (parent == null) {
			log.debug("Cannot find mood title in the conjugation table for '" + delegate.currentLexEntry() + "'");
			return;
		}

		WiktionaryExtractor.addAtomicMorphologicalInfo(
			infos,
			parent.getTextContent().trim().toLowerCase(WiktionaryExtractor.frLocale)
		);
	}

	public void getPerson(HashSet<PropertyObjectPair> infos, String person, int rowNumber, int rowCount) {
		if (person.equals("") && rowCount == 4) {
			// imperative
			switch (rowNumber) {
			case 1:
				infos.add(new PropertyObjectPair(LexinfoOnt.person, LexinfoOnt.secondPerson));
				infos.add(new PropertyObjectPair(LexinfoOnt.number, LexinfoOnt.singular));
				return;
			case 2:
				infos.add(new PropertyObjectPair(LexinfoOnt.person, LexinfoOnt.firstPerson));
				infos.add(new PropertyObjectPair(LexinfoOnt.number, LexinfoOnt.plural));
				return;
			case 3:
				infos.add(new PropertyObjectPair(LexinfoOnt.person, LexinfoOnt.secondPerson));
				infos.add(new PropertyObjectPair(LexinfoOnt.number, LexinfoOnt.plural));
				return;
			default:
				log.debug("BUG: unexpected row number '" + person + "' while parsing imperative table for '" + delegate.currentLexEntry() + "'");
				return;
			}
		}

		switch (person) {
		case "je":
		case "que je":
			infos.add(new PropertyObjectPair(LexinfoOnt.person, LexinfoOnt.firstPerson));
			infos.add(new PropertyObjectPair(LexinfoOnt.number, LexinfoOnt.singular));
			break;
		case "tu":
		case "que tu":
			infos.add(new PropertyObjectPair(LexinfoOnt.person, LexinfoOnt.secondPerson));
			infos.add(new PropertyObjectPair(LexinfoOnt.number, LexinfoOnt.singular));
			break;
		case "il/elle/on":
		case "qu’il/elle/on":
			infos.add(new PropertyObjectPair(LexinfoOnt.person, LexinfoOnt.thirdPerson));
			infos.add(new PropertyObjectPair(LexinfoOnt.number, LexinfoOnt.singular));
			break;
		case "nous":
		case "que nous":
			infos.add(new PropertyObjectPair(LexinfoOnt.person, LexinfoOnt.firstPerson));
			infos.add(new PropertyObjectPair(LexinfoOnt.number, LexinfoOnt.plural));
			break;
		case "vous":
		case "que vous":
			infos.add(new PropertyObjectPair(LexinfoOnt.person, LexinfoOnt.secondPerson));
			infos.add(new PropertyObjectPair(LexinfoOnt.number, LexinfoOnt.plural));
			break;
		case "ils/elles":
		case "qu’ils/elles":
			infos.add(new PropertyObjectPair(LexinfoOnt.person, LexinfoOnt.thirdPerson));
			infos.add(new PropertyObjectPair(LexinfoOnt.number, LexinfoOnt.plural));
			break;
		default:
			log.debug("Unexpected person '" + person + "' for '" + delegate.currentLexEntry() + "'");
			break;
		}
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

		HashSet<PropertyObjectPair> infos = new HashSet<PropertyObjectPair>();

		infos.add(new PropertyObjectPair(extractedFromConjTable, trueLiteral));

		NodeList lines = table.getElementsByTagName("tr");

		getMoodTense(table, infos, lines);

		for (int i = 1; i < lines.getLength(); i++) {
			Element line = (Element) lines.item(i);
			NodeList tdList = line.getElementsByTagName("td");

			if (tdList.getLength() < 2) {
				log.debug("Missing cells in the conjugation table for '" + delegate.currentLexEntry() + "'");
			} else {
				HashSet<PropertyObjectPair> infl = new HashSet<PropertyObjectPair>(infos);

				getPerson(infl, tdList.item(0).getTextContent().trim(), i, lines.getLength());

				delegate.registerInflection(
					"fr",
					"-verb-",
					tdList.item(1).getTextContent().trim(),
					delegate.currentLexEntry(),
					0,
					infl,
					null
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

			HashSet<PropertyObjectPair> infos;

			if (interestingTDs.getLength() < 3) {
				log.error("Cannot get present and past participle of '" + delegate.currentLexEntry() + "'");
			} else {
				infos = new HashSet<PropertyObjectPair>();
				String presentParticiple = interestingTDs.item(2).getTextContent().trim();
				infos.add(new PropertyObjectPair(extractedFromConjTable, trueLiteral));
				infos = new HashSet<PropertyObjectPair>();
				infos.add(new PropertyObjectPair(LexinfoOnt.verbFormMood, LexinfoOnt.participle));
				infos.add(new PropertyObjectPair(LexinfoOnt.tense, LexinfoOnt.present));
				delegate.registerInflection(
					"fr",
					"-verb-",
					presentParticiple,
					delegate.currentLexEntry(),
					0,
					infos,
					null
				);

				if (interestingTDs.getLength() < 6) {
					log.error("Cannot get past participle of '" + delegate.currentLexEntry() + "'");
				} else {
					String pastParticiple = interestingTDs.item(5).getTextContent();
					infos = new HashSet<PropertyObjectPair>();
					infos.add(new PropertyObjectPair(extractedFromConjTable, trueLiteral));
					infos.add(new PropertyObjectPair(LexinfoOnt.verbFormMood, LexinfoOnt.participle));
					infos.add(new PropertyObjectPair(LexinfoOnt.tense, LexinfoOnt.past));
					infos.add(new PropertyObjectPair(LexinfoOnt.gender, LexinfoOnt.masculine));
					infos.add(new PropertyObjectPair(LexinfoOnt.number, LexinfoOnt.singular));
					delegate.registerInflection(
						"fr",
						"-verb-",
						pastParticiple,
						delegate.currentLexEntry(),
						0,
						infos,
						null
					);
				}
			}
		}
	}

	public void handleConjugationDocument(Element parent) {
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

		handleConjugationDocument(doc.getDocumentElement());
	}

	public void parseImpersonnalTableConjugation(String conjugationTemplateCall) {
		Document doc = wikicodeToHtmlDOM(conjugationTemplateCall);
		if (doc == null) {
			return; // failing silently: error message already given.
		}

		handleImpersonnalTableConjugation(doc.getDocumentElement()); // "Orthographe traditionnelle"

	}

	private void addAtomicMorphologicalInfo(HashSet<PropertyObjectPair> properties, NodeList list) {
		for (int i = 0; i < list.getLength(); i++) {
			WiktionaryExtractor.addAtomicMorphologicalInfo(
				properties,
				list.item(i).getTextContent().trim().toLowerCase(WiktionaryExtractor.frLocale)
			);
		}
	}

	private void registerInflectionFromCellChild(Node c, String word) {
		HashSet<PropertyObjectPair> properties = new HashSet<PropertyObjectPair>();
		properties.add(new PropertyObjectPair(extractedFromInflectionTable, trueLiteral));

		Node cell = c;
		while (cell != null && !cell.getNodeName().toLowerCase().equals("td")) {
			cell = cell.getParentNode();
		}

		if (cell == null) {
			if (c.getParentNode().getNodeName().toLowerCase().equals("tr")) {
				// horrible but seen in wiktionary in the last version of Jully 2014
				cell = c;
				log.debug("[HORRIBLE] link is not in a TD, but in a TR element! Page: " + delegate.currentLexEntry() + ", form: " + word);
			} else {
				log.error("Could not find the parent cell while extracting other form's template. Page: " + delegate.currentLexEntry() + ", form: " + word);
				return;
			}
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
				log.error("BUG: no lines found in the table. Page: " + delegate.currentLexEntry() + ", form: " + word);
				return;
			}

			NodeList ths = ((Element) trs.item(0)).getElementsByTagName("th");

			if (ths.getLength() <= colNumber) {
				 log.error("BUG: not enougth cols in the row of the table. Page: " + delegate.currentLexEntry() + ", form: " + word);
				 return;
			}

			Node th = ths.item(colNumber);
			String text = th.getTextContent();
			WiktionaryExtractor.addAtomicMorphologicalInfo(properties, text.trim().toLowerCase(WiktionaryExtractor.frLocale));
		}

		if (word.equals(delegate.currentLexEntry())) {
			for (PropertyObjectPair p : properties) {
				delegate.registerProperty(p.getKey(), p.getValue());
			}
		} else {
			delegate.registerInflection(
				"",
				delegate.currentWiktionaryPos(),
				word,
				delegate.currentLexEntry(),
				delegate.currentDefinitionNumber(),
				properties
			);
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
			String wordLower = word.toLowerCase(WiktionaryExtractor.frLocale);
			Node title = a.getAttributes().getNamedItem("title");
			String t = null;

			if (title != null) {
				 t = title.getTextContent().toLowerCase(WiktionaryExtractor.frLocale);
			}

			if (t != null && !word.startsWith("Modèle:") && (t.equals(wordLower) || t.equals(wordLower + " (page inexistante)"))) {
				registerInflectionFromCellChild(a, word);
			}
		}

		links = doc.getElementsByTagName("strong");

		for (int i = 0; i < links.getLength(); i++) {
			Node a = links.item(i);
			Node className = a.getAttributes().getNamedItem("class");
			if (className != null && "selflink".equals(className.getTextContent())) {
				registerInflectionFromCellChild(a, a.getTextContent());
			}
		}
	}
}
