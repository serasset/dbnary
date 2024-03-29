package org.getalp.dbnary;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashMap;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

public class PropertyObjectPair extends SimpleImmutableEntry<Property, RDFNode> {

  private static HashMap<Property, HashMap<RDFNode, PropertyObjectPair>> instances =
      new HashMap<Property, HashMap<RDFNode, PropertyObjectPair>>();

  private PropertyObjectPair(Property p, RDFNode o) {
    super(p, o);
  }

  public Resource getResource() {
    return getValue().asResource();
  }

  public Literal getLiteral() {
    return getValue().asLiteral();
  }

  public static PropertyObjectPair get(Property p, RDFNode o) {
    HashMap<RDFNode, PropertyObjectPair> objects = instances.get(p);

    if (objects == null) {
      synchronized (PropertyObjectPair.class) {
        objects = instances.computeIfAbsent(p, k -> new HashMap<RDFNode, PropertyObjectPair>());
      }
    }

    PropertyObjectPair po = objects.get(o);

    if (po == null) {
      synchronized (PropertyObjectPair.class) {
        po = objects.computeIfAbsent(o, o1 -> new PropertyObjectPair(p, o1));
      }
    }

    return po;
  }

  @Override
  public String toString() {
    StringBuffer str = new StringBuffer().append(this.getKey().getLocalName()).append("-->");
    if (this.getValue() instanceof Resource)
      str.append(this.getResource());
    else
      str.append(this.getValue());

    return str.toString();
  }
}
