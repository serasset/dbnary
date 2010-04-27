package org.getalp.blexisma.wiktionary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;


public class WiktionaryIndexer {

    public static final String pageTag = "page";
    public static final String titleTag = "title";
    public static final int tagSize = pageTag.length() + 3;  

    public static void createIndex(File dumpFile, Map<String, OffsetValue> map) throws WiktionaryIndexerException {
        // get a factory instance
        XMLInputFactory xmlif = null;

        try {
            xmlif = XMLInputFactory.newInstance();
            xmlif.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            xmlif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        } catch (Exception ex) {
            throw new WiktionaryIndexerException("Cannot initialize XMLInputFactory", ex);
        }

        // create new XMLStreamReader

        // TODO: Use a valid way to log traces when verbose mode is on
        // System.out.println("");
        // System.out.println("FACTORY: " + xmlif);
        // System.out.println("filename = " + dumpFile.getPath());
        // System.out.println("");

        long starttime = System.currentTimeMillis();
        int nbPages = 0;

        XMLStreamReader xmlr = null;;
        try {
            // pass the file name.. all relative entity references will be
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
                    if (! title.equals(""))
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
                if (xmlr != null) xmlr.close();
            } catch (XMLStreamException ex) {
                ex.printStackTrace();
            }
        }

        long endtime = System.currentTimeMillis();
        System.out.println(" Parsing Time = " + (endtime - starttime) + "; " + nbPages + " pages parsed.");
    }

}
