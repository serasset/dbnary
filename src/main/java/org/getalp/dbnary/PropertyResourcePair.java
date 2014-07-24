package org.getalp.dbnary;

import java.util.AbstractMap.SimpleImmutableEntry;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Literal;

public class PropertyResourcePair extends SimpleImmutableEntry<Property, Object> {
	public PropertyResourcePair(Property p, Resource o) {
		super(p, (Object) o);
	}

	public PropertyResourcePair(Property p, Literal o) {
		super(p, (Object) o);
	}

	public Resource getResource() {
		return (Resource) getValue();
	}

	public Literal getLiteral() {
		return (Literal) getValue();
	}
}
