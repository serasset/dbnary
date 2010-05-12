package org.getalp.blexisma.wiktionary;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class WiktionaryIndexer {

    public static final String pageTag = "page";
    public static final String titleTag = "title";
    public static final int tagSize = pageTag.length() + 3;

    public static final XMLInputFactory xmlif;

    static {
        try {
            xmlif = XMLInputFactory.newInstance();
            xmlif.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            xmlif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        } catch (Exception ex) {
            System.err.println("Cannot intialize XMLInputFactory while classloading WiktionaryIndexer.");
            throw new RuntimeException("Cannot initialize XMLInputFactory", ex);
        }
    }

    public static void createIndex(File dumpFile, Map<String, OffsetValue> map) throws WiktionaryIndexerException {

        // create new XMLStreamReader

        long starttime = System.currentTimeMillis();
        int nbPages = 0;

        XMLStreamReader xmlr = null;
        try {
            // pass the file name. all relative entity references will be
            // resolved against this as base URI.
            xmlr = xmlif.createXMLStreamReader(new FileInputStream(dumpFile));

            // check if there are more events in the input stream
            int boffset = 0, eoffset = 0;
            String title = "";
            while (xmlr.hasNext()) {
                xmlr.next();
                if (xmlr.isStartElement() && xmlr.getLocalName().equals(pageTag)) {
                    boffset = xmlr.getLocation().getCharacterOffset();
                    title = "";
                    eoffset = 0;
                    nbPages++;
                } else if (xmlr.isStartElement() && xmlr.getLocalName().equals(titleTag)) {
                    title = xmlr.getElementText();
                } else if (xmlr.isEndElement() && xmlr.getLocalName().equals(pageTag)) {
                    eoffset = xmlr.getLocation().getCharacterOffset();
                    if (!title.equals(""))
                        map.put(title, new OffsetValue(boffset, (eoffset - boffset) + tagSize));
                }
            }
        } catch (XMLStreamException ex) {
            System.out.println(ex.getMessage());

            if (ex.getNestedException() != null) {
                ex.getNestedException().printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (xmlr != null)
                    xmlr.close();
            } catch (XMLStreamException ex) {
                ex.printStackTrace();
            }
        }

        long endtime = System.currentTimeMillis();
        System.out.println(" Parsing Time = " + (endtime - starttime) + "; " + nbPages + " pages parsed.");
    }

    public static String getTextElementContent(String wiktionaryPageContent) {
        StringReader sr = new StringReader(wiktionaryPageContent);
        XMLStreamReader xmlr = null;
        try {
            xmlr = xmlif.createXMLStreamReader(sr);

            // check if there are more events in the input stream
            while (xmlr.hasNext()) {
                xmlr.next();
                if (xmlr.isStartElement() && xmlr.getLocalName().equals("text")) {
                    return xmlr.getElementText(); 
                 }
            }
        } catch (XMLStreamException ex) {
            System.out.println(ex.getMessage());

            if (ex.getNestedException() != null) {
                ex.getNestedException().printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (xmlr != null)
                    xmlr.close();
            } catch (XMLStreamException ex) {
                ex.printStackTrace();
            }
        }
        // This happens only when no text element is found in the page.
        return null;
    }

}
