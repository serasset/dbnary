package org.getalp.dbnary;

import java.util.AbstractMap.SimpleImmutableEntry;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;

public class PropertyObjectPair extends SimpleImmutableEntry<Property, RDFNode> {
	public PropertyObjectPair(Property p, RDFNode o) {
		super(p, o);
	}

	public Resource getResource() {
		return getValue().asResource();
	}

	public Literal getLiteral() {
		return getValue().asLiteral();
	}
}
