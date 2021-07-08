package org.getalp.dbnary.rdfutils;

import org.apache.jena.rdf.model.Resource;

public class URI {

  public static String getLocalName(Resource lexEntry) {
    String uri = lexEntry.getURI();
    int splitPos = uri.lastIndexOf('/');
    return (-1 == splitPos) ? "" : uri.substring(splitPos + 1);
  }

  public static String getNameSpace(Resource lexEntry) {
    String uri = lexEntry.getURI();
    int splitPos = uri.lastIndexOf('/');
    return (-1 == splitPos) ? "" : uri.substring(0, splitPos + 1);
  }
}
