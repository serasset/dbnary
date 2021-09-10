package org.getalp.model.ontolex;

import java.math.BigInteger;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.bind.DatatypeConverter;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.morphology.InflectionScheme;
import org.getalp.dbnary.morphology.MorphoSyntacticFeature;
import org.getalp.dbnary.rdfutils.URI;

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
    // TODO: Check if a similar lexical form already exists in the model
    List<Statement> otherFormsStatements = lexEntry.listProperties(OntolexOnt.otherForm).toList();
    for (Statement otherForm : otherFormsStatements) {
      StatementCompatibility compatibility = isCompatibleWith(otherForm.getResource());
      if (compatibility == StatementCompatibility.IDENTICAL)
        return otherForm.getResource();
      // TODO : handle LESS or MORE precise forms.
    }

    Resource lexForm =
        lexEntry.getModel().createResource(computeResourceName(lexEntry), OntolexOnt.Form);
    features.attachTo(lexForm);
    values.forEach(v -> v.attachTo(lexForm));
    lexEntry.getModel().add(lexEntry, OntolexOnt.otherForm, lexForm);
    return lexEntry;
  }

  private enum StatementCompatibility {
    IDENTICAL, INCOMPATIBLE, MORE_PRECISE, LESS_PRECISE
  };

  /**
   * Check if this LexicalForm is compatible with an existing resource.
   * 
   * @param r a LexicalForm as a resource
   * @return true iff this is compatible with r
   */
  private StatementCompatibility isCompatibleWith(Resource r) {
    Map<Property, List<Statement>> properties = r.listProperties().toList().stream()
        .collect(Collectors.groupingBy(Statement::getPredicate));
    StatementCompatibility result = StatementCompatibility.IDENTICAL;
    for (Map.Entry ps : properties.entrySet()) {
      switch (compatibilityOf(ps)) {
        case INCOMPATIBLE:
          return StatementCompatibility.INCOMPATIBLE;
        case MORE_PRECISE:
          if (result == StatementCompatibility.LESS_PRECISE)
            return StatementCompatibility.INCOMPATIBLE;
          else
            result = StatementCompatibility.MORE_PRECISE;
          break;
        case LESS_PRECISE:
          if (result == StatementCompatibility.MORE_PRECISE)
            return StatementCompatibility.INCOMPATIBLE;
          else
            result = StatementCompatibility.LESS_PRECISE;
          break;
        default:
          break;
      }
    }
    return result;
  }

  private StatementCompatibility compatibilityOf(Map.Entry<Property, List<Statement>> ps) {
    Property p = ps.getKey();
    Set<MorphoSyntacticFeature> features = getFeatures(p);
    boolean featureSubSetOfStatements = subSetEq(features, ps.getValue());
    boolean statementsSubSetOfFeatures = subSetEq(ps.getValue(), features);
    if (featureSubSetOfStatements && statementsSubSetOfFeatures)
      return StatementCompatibility.IDENTICAL;
    else if (featureSubSetOfStatements)
      return StatementCompatibility.LESS_PRECISE;
    else if (statementsSubSetOfFeatures)
      return StatementCompatibility.MORE_PRECISE;
    else
      return StatementCompatibility.INCOMPATIBLE;
  }

  private static boolean subSetEq(Set<MorphoSyntacticFeature> features,
      List<Statement> statements) {
    return features.stream()
        .allMatch(f -> statements.stream().anyMatch(s -> s.getObject().equals(f.value())));
  }

  private static boolean subSetEq(List<Statement> statements,
      Set<MorphoSyntacticFeature> features) {
    return statements.stream()
        .allMatch(s -> features.stream().anyMatch(f -> s.getObject().equals(f.value())));
  }

  private Set<MorphoSyntacticFeature> getFeatures(Property p) {
    return this.getFeature().stream().filter(f -> f.property().equals(p))
        .collect(Collectors.toSet());
  }

  private String computeResourceName(Resource lexEntry) {
    String lexEntryLocalName = URI.getLocalName(lexEntry);
    String lexEntryPrefix = URI.getNameSpace(lexEntry);
    if (!lexEntry.getURI().equals(lexEntryPrefix + lexEntryLocalName)) {
      System.err.println("ERROR: getNameSpace and getLocalName did not work !!!");
    }
    String compactProperties = DatatypeConverter
        .printBase64Binary(
            BigInteger.valueOf(features.hashCode() + values.hashCode()).toByteArray())
        .replaceAll("[/=\\+]", "-");

    return lexEntryPrefix + "__wf_" + compactProperties + "_" + lexEntryLocalName;
  }

}
