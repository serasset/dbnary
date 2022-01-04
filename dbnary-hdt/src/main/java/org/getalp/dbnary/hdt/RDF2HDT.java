package org.getalp.dbnary.hdt;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.options.HDTSpecification;

public class RDF2HDT {
  private HDTSpecification spec = new HDTSpecification();
  private boolean quiet = false;

  public RDF2HDT(boolean quiet) {
    this.quiet = quiet;
  }

  public RDF2HDT() {
    this(true);
  }

  public void rdf2hdt(Path rdfInput, Path hdtOutput) {
    this.rdf2hdt(rdfInput, hdtOutput, null);
  }

  public void rdf2hdt(Path rdfInput, Path hdtOutput, String baseURI) {

    if (baseURI == null) {
      baseURI = "file://" + rdfInput.toString();
    }

    RDFNotation notation;
    try {
      notation = RDFNotation.guess(rdfInput.toString());
    } catch (IllegalArgumentException e) {
      System.err.println("Could not guess notation for " + rdfInput + " Trying Turtle");
      notation = RDFNotation.TURTLE;
    }

    HDT hdt = null;
    try {
      hdt = HDTManager.generateHDT(rdfInput.toString(), baseURI, notation, spec, null);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    } catch (ParserException e) {
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


      try (OutputStream hdtOutputStream =
          new BZip2CompressorOutputStream(new FileOutputStream(hdtOutput.toString()))) {
        // Dump to HDT file
        hdt.saveToHDT(hdtOutputStream, null);
      } catch (IOException e) {
        e.printStackTrace();
        return;
      }
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
