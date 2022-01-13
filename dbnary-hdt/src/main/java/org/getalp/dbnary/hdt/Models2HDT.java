package org.getalp.dbnary.hdt;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Iterator;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.jena.rdf.model.Model;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.rdf.parsers.JenaModelIterator;
import org.rdfhdt.hdt.triples.TripleString;

public class Models2HDT {
  private HDTSpecification spec = new HDTSpecification();
  private boolean quiet = false;

  public Models2HDT(boolean quiet) {
    this.quiet = quiet;
  }

  public Models2HDT() {
    this(true);
  }

  public void models2hdt(OutputStream hdtOutputStream, String baseURI, Model... models) {
    Iterator<TripleString> source = new JenaModelIterator(models[0]);
    HDT hdt = null;
    try {
      hdt = HDTManager.generateHDT(source, baseURI, spec, null);
    } catch (IOException | ParserException e) {
      e.printStackTrace();
      return;
    }

    try {
      // Show Basic stats
      if (!quiet) {
        System.out.println("Total Triples: " + hdt.getTriples().getNumberOfElements());
        System.out.println("Different subjects: " + hdt.getDictionary().getNsubjects());
        System.out.println("Different predicates: " + hdt.getDictionary().getNpredicates());
        System.out.println("Different objects: " + hdt.getDictionary().getNobjects());
        System.out.println("Common Subject/Object:" + hdt.getDictionary().getNshared());
      }

      hdt.saveToHDT(hdtOutputStream, null);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (hdt != null) {
        try {
          hdt.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

  }
}
