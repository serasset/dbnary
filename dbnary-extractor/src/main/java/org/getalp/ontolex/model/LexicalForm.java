package org.getalp.ontolex.model;

import com.github.andrewoma.dexx.collection.List;
import java.math.BigInteger;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.xml.bind.DatatypeConverter;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.SplitIRI;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.morphology.InflectionScheme;

public class LexicalForm {
  InflectionScheme features;
  Set<Representation> values = new LinkedHashSet<>();

  public LexicalForm() {}

  public LexicalForm(InflectionScheme features) {
    this.features = features;
  }

  public InflectionScheme getFeature() {
    return features;
  }

  public void setFeature(InflectionScheme features) {
    this.features = features;
  }

  public Set<Representation> getValues() {
    return values;
  }

  public void addValue(Representation representation) {
    this.values.add(representation);
  }

  public void removeValue(Representation representation) {
    this.values.remove(representation);
  }


  public Resource attachTo(Resource lexEntry) {
    Resource lexForm =
        lexEntry.getModel().createResource(computeResourceName(lexEntry), OntolexOnt.Form);
    features.attachTo(lexForm);
    values.forEach(v -> v.attachTo(lexForm));
    lexEntry.getModel().add(lexEntry, OntolexOnt.otherForm, lexForm);
    return lexEntry;
  }

  private String computeResourceName(Resource lexEntry) {
    String lexEntryLocalName = getLocalName(lexEntry);
    String lexEntryPrefix = getNameSpace(lexEntry);
    if (!lexEntry.getURI().equals(lexEntryPrefix + lexEntryLocalName)) {
      System.err.println("ERROR: getNameSpace and getLocalName did not work !!!");
    }
    String compactProperties = DatatypeConverter
        .printBase64Binary(
            BigInteger.valueOf(features.hashCode() + values.hashCode()).toByteArray())
        .replaceAll("[/=\\+]", "-");

    return lexEntryPrefix + "__wf_" + compactProperties + "_" + lexEntryLocalName;
  }

  private static String getLocalName(Resource lexEntry) {
    String uri = lexEntry.getURI();
    int splitPos = uri.lastIndexOf('/');
    return (-1 == splitPos) ? "" : uri.substring(splitPos + 1);
  }

  private static String getNameSpace(Resource lexEntry) {
    String uri = lexEntry.getURI();
    int splitPos = uri.lastIndexOf('/');
    return (-1 == splitPos) ? "" : uri.substring(0, splitPos + 1);
  }

}
