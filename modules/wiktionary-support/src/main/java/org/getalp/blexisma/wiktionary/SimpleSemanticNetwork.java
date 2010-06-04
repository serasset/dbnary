package org.getalp.blexisma.wiktionary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import org.getalp.blexisma.api.SemanticNetwork;

public class SimpleSemanticNetwork<N, R> implements SemanticNetwork<N, R> {
    public class Relation {
        protected Relation(N o, N d, R r, int c) {
            this.origin = o; this.destination = d; this.label = r; this.confidence = c;
        }
        
        public N origin, destination;
        public R label;
        public int confidence;
    }
    
    private ArrayList<N> nodes;
    private ArrayList<R> relationLabels;
    
    private HashMap<R,Collection<Relation>> labelToRelations;
    private HashMap<N,Collection<Relation>> outgoingRelations;
 //   private HashMap<N,Relation> incomingRelations;

    public SimpleSemanticNetwork() {
        nodes = new ArrayList<N>();
        relationLabels = new ArrayList<R>();
        labelToRelations = new HashMap<R, Collection<Relation>>();
        outgoingRelations = new HashMap<N, Collection<Relation>>();
    }
    
    public SimpleSemanticNetwork(int originalNodesSize, int originalRelationsSize) {
        nodes = new ArrayList<N>(originalNodesSize);
        relationLabels = new ArrayList<R>(originalRelationsSize);
        labelToRelations = new HashMap<R, Collection<Relation>>(originalRelationsSize);
        outgoingRelations = new HashMap<N, Collection<Relation>>(originalNodesSize);
    }
    
    @Override
    public void addNode(N node) {
        nodes.add(node);
    }

    @Override
    public void addRelation(N origin, N destination, int confidence, R relationLabel) {
        int nodeIndex; int relIndex;

        // Add or canonicalize origin node 
        if ((nodeIndex = nodes.indexOf(origin)) == -1) {
            nodes.add(origin);
        } else {
            origin = nodes.get(nodeIndex);
        }
        // Add or canonicalize destination node
        if ((nodeIndex = nodes.indexOf(destination)) == -1) {
            nodes.add(destination);
        } else {
            destination = nodes.get(nodeIndex);
        }
        // Add or canonicalize relation label 
        if ((relIndex = relationLabels.indexOf(relationLabel)) == -1) {
            relationLabels.add(relationLabel);
        } else {
            relationLabel = relationLabels.get(relIndex);
        }
        
        Relation r = new Relation(origin, destination, relationLabel, confidence);

        Collection<Relation> rels;
        if ((rels = labelToRelations.get(relationLabel)) != null) {
            rels.add(r);
        } else {
            rels = new ArrayList<Relation>();
            rels.add(r);
            labelToRelations.put(relationLabel, rels);
        }
        
        if ((rels = outgoingRelations.get(origin)) != null) {
            rels.add(r);
        } else {
            rels = new ArrayList<Relation>();
            rels.add(r);
            outgoingRelations.put(origin, rels);
        }    
    }

    @Override
    public int getNbEdges() {
        int nb = 0;
        for (Entry<N, Collection<Relation>> entry : outgoingRelations.entrySet()) {
            Collection<Relation> rels = entry.getValue();
            if (rels != null) nb += rels.size();
        }
        return nb;
    }

    @Override
    public int getNbNodes() {
        return nodes.size();
    }

    public void clear() {
        nodes.clear();
        relationLabels.clear();
        labelToRelations.clear();
        outgoingRelations.clear();
    }
    
}
