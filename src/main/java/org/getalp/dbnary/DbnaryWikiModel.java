package org.getalp.dbnary;

import info.bliki.wiki.model.Configuration;
import info.bliki.wiki.model.WikiModel;

import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.InputSource;

import org.xml.sax.SAXException;
import java.io.IOException;

import org.w3c.dom.*;

import java.io.StringReader;
import info.bliki.wiki.filter.HTMLConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbnaryWikiModel extends WikiModel {

	private static Logger log = LoggerFactory.getLogger(DbnaryWikiModel.class);

	protected WiktionaryDataHandler delegate;

	// static Set<String> ignoredTemplates = new TreeSet<String>();
	// static {
	// 	ignoredTemplates.add("Wikipedia");
	// 	ignoredTemplates.add("Incorrect");
	// }
	
	protected WiktionaryIndex wi = null;
//	protected String templateNamespace = null;
	
	
	public DbnaryWikiModel(Locale locale, String imageBaseURL, String linkBaseURL) {
		this((WiktionaryIndex) null, locale, imageBaseURL, linkBaseURL);
	}
		
	public DbnaryWikiModel(WiktionaryIndex wi, Locale locale, String imageBaseURL, String linkBaseURL) {
		super(Configuration.DEFAULT_CONFIGURATION, locale, imageBaseURL, linkBaseURL);
//		this.templateNamespace = templateNamespace;
		this.wi = wi;
	}

    public DbnaryWikiModel(WiktionaryDataHandler wdh, Locale locale, String imageBaseURL, String linkBaseURL) {
		this(wdh, (WiktionaryIndex) null, locale, imageBaseURL, linkBaseURL);
	}
	
	public 	DbnaryWikiModel(WiktionaryDataHandler wdh, WiktionaryIndex wi, Locale locale, String imageBaseURL, String linkBaseURL) {
		this(wi, locale, imageBaseURL, linkBaseURL);
		this.delegate = wdh;
		setPageName(wdh.currentLexEntry());
	}


	private static DocumentBuilder docBuilder = null;
	private static InputSource docSource = null;

	/* @Override
	public void addCategory(String categoryName, String sortKey) {
		System.err.println("Called addCategory : " + categoryName);
		super.addCategory(categoryName, sortKey);
	}

	@Override
	public void addLink(String topicName) {
		System.err.println("Called addLink: " + topicName);
		super.addLink(topicName);
	}

	@Override
	public boolean addSemanticAttribute(String attribute, String attributeValue) {
		System.err.println("Called addSemanticAttribute : " + attribute);
		return super.addSemanticAttribute(attribute, attributeValue);
	}

	@Override
	public boolean addSemanticRelation(String relation, String relationValue) {
		System.err.println("Called addSemanticRelation");
		return super.addSemanticRelation(relation, relationValue);
	}

	@Override
	public void addTemplate(String template) {
		System.err.println("Called addTemplate: " + template);
		super.addTemplate(template);
	}

	@Override
	public void appendInternalLink(String topic, String hashSection,
			String topicDescription, String cssClass, boolean parseRecursive) {
		System.err.println("Called appendInternalLink: " + topic + "#" + hashSection);
		super.appendInternalLink(topic, hashSection, topicDescription, cssClass,
				parseRecursive);
	}


	@Override
	public void parseInternalImageLink(String imageNamespace,
			String rawImageLink) {
		System.err.println("Called parseInternalImageLink");
		super.parseInternalImageLink(imageNamespace, rawImageLink);
	}

	@Override
	public boolean replaceColon() {
		System.err.println("Called replaceColon");
		return super.replaceColon();
	}

	@Override
	public void setUp() {
		System.err.println("Called setUp");
		super.setUp();
	}
*/

	// get the DOM representation of the HTML code corresponding
	// to the wikicode given in arguments
	public Document wikicodeToHtmlDOM (String wikicode) {

		if (docBuilder == null) {
			try {
				docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				System.err.println("got a ParserConfigurationException in the DBnaryWikiModel class.");
				return null;
			}

			docSource = new InputSource();
		}

		String html = render(new HTMLConverter(), wikicode);

		docSource.setCharacterStream(new StringReader("<div>" + html + "</div>"));

		Document doc = null;

		try {
			doc = docBuilder.parse(docSource);
		} catch (SAXException e) {
			log.error("Unable to parse template call in DBnaryWikiModel.");
		} catch (IOException e) {
			log.error("got IOException in DBnaryWikiModel ‽");
		}

		return doc;
	}
	
	@Override
    public String getRawWikiContent(String namespace, String articleName, Map<String, String> map) {
            String result = super.getRawWikiContent(namespace, articleName, map);
            if (result != null) {
                    // found magic word template
                    return result;
            }
            // replace SPACES with underscore('_') and first character as uppercase
            String name = encodeTitleToUrl(articleName, true);
 
            if (isTemplateNamespace(namespace)) {
            	if (null != wi)
                    return getIncludeOnlyText(wi.getTextOfPage(namespace + ":" + articleName));
            }
            return null;
    }

    public String getIncludeOnlyText(String rawWikiText) {
        if (null == rawWikiText) return null;

        int noIncludeOffset = rawWikiText.indexOf("<noinclude>");

        if (-1 != noIncludeOffset) {
            int noIncludeEndOffset = rawWikiText.indexOf("</noinclude>", noIncludeOffset);

            if (-1 != noIncludeEndOffset) {
                return getIncludeOnlyText(
	                new StringBuffer()
		                .append(rawWikiText.substring(0, noIncludeOffset))
                        .append(rawWikiText.substring(noIncludeEndOffset + "</noinclude>".length()))
                        .toString()
                );
            }
        }

        int includeOnlyOffset = rawWikiText.indexOf("<includeonly>");

        if (-1 != includeOnlyOffset) {
            int includeOnlyEndOffset = rawWikiText.indexOf("</includeonly>", noIncludeOffset);
            if (-1 != includeOnlyEndOffset)
                return rawWikiText.substring(includeOnlyOffset + "<includeonly>".length(), includeOnlyEndOffset);
        }

        return rawWikiText;
    }
}
