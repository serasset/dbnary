package org.getalp.dbnary.fra;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.StringReader;

import info.bliki.wiki.filter.HTMLConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.getalp.dbnary.DbnaryWikiModel;
import org.getalp.dbnary.WiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.InputSource;
import org.w3c.dom.*;

/* import stupidity */
import org.xml.sax.SAXException;
import java.io.IOException;

public class FrenchConjugationExtractorWikiModel extends DbnaryWikiModel {
	private WiktionaryDataHandler delegate;

    private Logger log = LoggerFactory.getLogger(FrenchConjugationExtractorWikiModel.class);

    public static final String impersonalMoodIdString = "Modes_impersonnels";

	public static DocumentBuilder docBuilder;
	public static final InputSource docSource;

    static {
		try {
			docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			docBuilder = null;
			System.err.println("got a ParserConfigurationException in the FrenchConjugationExtractorWikiModel class.");
		}

		docSource = new InputSource();
    }

    public FrenchConjugationExtractorWikiModel(WiktionaryDataHandler we, Locale locale, String imageBaseURL, String linkBaseURL) {
		this(we, (WiktionaryIndex) null, locale, imageBaseURL, linkBaseURL);
	}
	
	public FrenchConjugationExtractorWikiModel(WiktionaryDataHandler we, WiktionaryIndex wi, Locale locale, String imageBaseURL, String linkBaseURL) {
		super(wi, locale, imageBaseURL, linkBaseURL);
		this.delegate = we;
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
		// Render the conjugation to plain text, while ignoring the example template

		if (docBuilder == null) {
			return;
		}

		String conj = render(new HTMLConverter(), conjugationTemplateCall).trim();

		docSource.setCharacterStream(new StringReader("<div>" + conj + "</div>"));

		Document doc;

		try {
			doc = docBuilder.parse(docSource);
		} catch (SAXException e) {
			log.error("Unable to parse conjugation tables in FrenchConjugationExtractorWikiModel.");
			return;
		} catch (IOException e) {
			log.error("got IOException in FrenchConjugationExtractorWikiModel ‽");
			return;
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

// 		delegate.registerNewConjugation(conj);
	}
}