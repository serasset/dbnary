package org.getalp.blexisma.wiktionary;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Map.Entry;

import org.getalp.blexisma.api.SemanticNetwork;

public class SimpleSemanticNetwork<N, R> extends SemanticNetwork<N, R> {
    public class Relation extends SemanticNetwork<N,R>.Edge{
        protected Relation(N o, N d, R r, float c) {
            this.origin = o; this.destination = d; this.label = r; this.confidence = c;
        }
        
        public N origin, destination;
        public R label;
        public float confidence;
        
        @Override
        public float getConfidence() {
            return this.confidence;
        }

        @Override
        public N getDestination() {
            return this.destination;
        }

        @Override
        public N getOrigin() {
            return this.origin;
        }

        @Override
        public R getRelation() {
            // TODO Auto-generated method stub
            return this.label;
        }
    }
    
    private static final Random rand = new Random();
    public class SemnetInfiniteNodesIterator implements Iterator<N> {
        
        private Iterator<Entry<N,N>> entrySetIterator;
        
        public SemnetInfiniteNodesIterator() {
            entrySetIterator = nodes.entrySet().iterator();
            int startingPos = rand.nextInt(nodes.size());
            for (int i=0; i < startingPos; i++) {
                entrySetIterator.next();
            }
        }
        
        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public N next() {
            if (entrySetIterator.hasNext()) return entrySetIterator.next().getValue();
            entrySetIterator = nodes.entrySet().iterator();
            return entrySetIterator.next().getValue();
        }

        @Override
        public void remove() {
            entrySetIterator.remove();
        }
        
    }
    private HashMap<N,N> nodes;
    private HashMap<R,R> relationLabels;
    
    private HashMap<R,Collection<Relation>> labelToRelations;
    private HashMap<N,Collection<Relation>> outgoingRelations;
 //   private HashMap<N,Relation> incomingRelations;

    // TODO: remove the nodes and relationLabels HashMap as everything is present in label and outgoingrels
    public SimpleSemanticNetwork() {
        nodes = new HashMap<N,N>();
        relationLabels = new HashMap<R,R>();
        labelToRelations = new HashMap<R, Collection<Relation>>();
        outgoingRelations = new HashMap<N, Collection<Relation>>();
    }
    
    public SimpleSemanticNetwork(int originalNodesSize, int originalRelationsSize) {
        nodes = new HashMap<N,N>(originalNodesSize);
        relationLabels = new HashMap<R,R>(originalRelationsSize);
        labelToRelations = new HashMap<R, Collection<Relation>>(originalRelationsSize);
        outgoingRelations = new HashMap<N, Collection<Relation>>(originalNodesSize);
    }
    
    @Override
    public void addNode(N node) {
    	if (! nodes.containsKey(node))
    		nodes.put(node, node);
    }

    @Override
    public void addRelation(N origin, N destination, float confidence, R relationLabel) {
        N n; R r;

        // Add or canonicalize origin node 
        if ((n = nodes.get(origin)) == null) {
            nodes.put(origin, origin);
        } else {
            origin = n;
        }
        // Add or canonicalize destination node
        if ((n = nodes.get(destination)) == null) {
            nodes.put(destination,destination);
        } else {
            destination = n;
        }
        // Add or canonicalize relation label 
        if ((r = relationLabels.get(relationLabel)) == null) {
            relationLabels.put(relationLabel,relationLabel);
        } else {
            relationLabel = r;
        }
        
        Relation rel = new Relation(origin, destination, relationLabel, confidence);

        Collection<Relation> rels;
        if ((rels = labelToRelations.get(relationLabel)) != null) {
            rels.add(rel);
        } else {
            rels = new ArrayList<Relation>();
            rels.add(rel);
            labelToRelations.put(relationLabel, rels);
        }
        
        if ((rels = outgoingRelations.get(origin)) != null) {
            rels.add(rel);
        } else {
            rels = new ArrayList<Relation>();
            rels.add(rel);
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
    
    @Override
    public Collection<Relation> getEdges(N origin) {
        return (Collection<Relation>) outgoingRelations.get(origin);
    }
    
    @Override
    public void clear() {
        nodes.clear();
        relationLabels.clear();
        labelToRelations.clear();
        outgoingRelations.clear();
    }

    @Override
    public Iterator<N> getNodesIterator() {
        throw new RuntimeException("Unimplemented abstract method.");
    }

    @Override
    public  Iterator<N> getInfiniteNodesIterator() {
        return new SemnetInfiniteNodesIterator();
    }
     
    @Override
    public Iterator<Edge> getEdgesIterator() {
        throw new RuntimeException("Unimplemented abstract method.");
    }
    
    @Override
    public  Iterator<Edge> getInfiniteEdgesIterator() {
        throw new RuntimeException("Unimplemented abstract method.");
    }
   
   
    public void dumpToWriter(PrintStream out) {
        for(Entry<N, Collection<Relation>> e: outgoingRelations.entrySet()) {
            out.println("-O- " +e.getKey().toString());
            for (Relation r: e.getValue()) {
                out.println("  -R- " + r.getRelation());
                out.println("  -C- " + r.getConfidence());
                out.println("  -D- " + r.getDestination());
            }
        }
    }
    
}
