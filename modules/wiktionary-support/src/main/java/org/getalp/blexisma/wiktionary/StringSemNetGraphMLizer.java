package org.getalp.blexisma.wiktionary;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;

import org.getalp.blexisma.api.GraphMLizableElement;
import org.getalp.blexisma.api.SemanticNetwork;
import org.getalp.blexisma.api.SemanticNetworkGraphMLizer;

public class StringSemNetGraphMLizer {

    // TODO: this is all duplicate code: check if it is possible to inherit from graphmlizer.
    protected static final String xmlHeader1 = "<?xml version=\"1.0\" encoding=\"";
    protected static final String xmlHeader2 = "\"?>";
    protected static final String graphmlHeader = "<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\" "
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + "xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns "
            + " http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd\">";
    protected static final String graphMLFooter = "</graphml>";

    protected OutputStreamWriter out = null;
    protected InputStreamReader in = null;
    protected String encoding = "";
    protected String EOL = "\n";
    protected String graphId = "G";

    /**
     * @return the eOL
     */
    public String getEOL() {
        return EOL;
    }

    /**
     * @param eOL
     *            the eOL to set
     */
    public void setEOL(String eOL) {
        EOL = eOL;
    }

    /**
     * @return the graphId
     */
    public String getGraphId() {
        return graphId;
    }

    /**
     * @param graphId
     *            the graphId to set
     */
    public void setGraphId(String graphId) {
        this.graphId = graphId;
    }

    // Constructors
    public StringSemNetGraphMLizer() {
        in = new InputStreamReader(System.in);
        out = new OutputStreamWriter(System.out);
    }

    public StringSemNetGraphMLizer(OutputStream out, String encoding) throws UnsupportedEncodingException {
        this.out = new OutputStreamWriter(out, encoding);
        this.encoding = encoding;
    }

    public StringSemNetGraphMLizer(OutputStream out, String encoding, String endOfLine) throws UnsupportedEncodingException {
        this.out = new OutputStreamWriter(out, encoding);
        this.encoding = encoding;
        this.EOL = endOfLine;
    }

    public StringSemNetGraphMLizer(OutputStreamWriter out) {
        this.out = out;
        this.encoding = out.getEncoding();
    }

    public void load(SemanticNetwork<? extends String, ? extends String> sm) {
        // TODO Auto-generated method stub

    }

    // TODO [urgent]: Export xml using Stax interface to avoid generating non valid xml.
    public void dump(SemanticNetwork<? extends String, ? extends String> sm) throws IOException {
        out.write(xmlHeader1 + encoding + xmlHeader2);
        out.write(EOL);
        out.write(graphmlHeader + EOL);

        Iterator<? extends SemanticNetwork<? extends String, ? extends String>.Edge> edges = sm.getEdgesIterator();
        Iterator<? extends String> nodes = sm.getNodesIterator();

        // Write out key descriptions.
        out.write("  <key id=\"dn0\" for=\"node\"");
        out.write(" attr.name=\"label\"");
        out.write(" attr.type=\"string\"");
        out.write("/>" + EOL);
        
        out.write("  <key id=\"de1\" for=\"edge\"");
        out.write(" attr.name=\"label\"");
        out.write(" attr.type=\"string\"");
        out.write("/>" + EOL);

        // open the graph element
        out.write("<graph id=\"" + graphId + "\" edgedefault=\"directed\">");
        // Write out nodes, keep a node number for each of them
        HashMap<String, Integer> nodeNumbers = new HashMap<String, Integer>((int) (sm.getNbNodes() * 1.25));
        edges = sm.getEdgesIterator();
        nodes = sm.getNodesIterator();
        int cnid = 0;

        // Write out the node
        while (nodes.hasNext()) {
            String node = nodes.next();
            nodeNumbers.put(node, cnid);
            writeNode(node, cnid);
            out.flush();
            cnid++;
        }

        // Write out the node
        while (edges.hasNext()) {
            SemanticNetwork<? extends String, ? extends String>.Edge edge = edges.next();
            nodeNumbers.put(edge.getRelation(), cnid);
            writeEdge(edge, nodeNumbers, cnid);
            out.flush();
            cnid++;
        }
        // terminate graph element
        out.write(EOL + "  </graph>"+ EOL);
        // close the graphml file
        out.write(graphMLFooter + EOL);
        out.flush();
    }

    private void writeNode(String node, int id) throws IOException {
        out.write(EOL + "     <node id=\"n" + id + "\">");
        out.write(EOL + "       <data key=\"dn0\">" + node.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</data>");
        out.write(EOL + "    </node>");
    }

    private void writeEdge(SemanticNetwork<? extends String, ? extends String>.Edge edge, 
            HashMap<String, Integer> nodeNumbers, int cnid) throws IOException {
        out.write(EOL + "    <edge id=\"e" + cnid + "\" ");
        int sid = nodeNumbers.get(edge.getOrigin()).intValue();
        int tid = nodeNumbers.get(edge.getDestination()).intValue();
        out.write("source=\"n" + sid + "\" ");
        out.write("target=\"n" + tid + "\">");
        out.write(EOL + "      <data key=\"de1\">" + edge.getRelation().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</data>");
        out.write(EOL + "    </edge>");
    }

}
