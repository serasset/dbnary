package org.getalp.blexisma.wiktionary;


import static org.junit.Assert.*;

import java.io.IOException;

import org.getalp.blexisma.api.GraphMLizableElement;
import org.getalp.blexisma.api.SemanticNetwork;
import org.junit.Before;
import org.junit.Test;

public class StringSemNetGraphMLizerTest {
    
    SemanticNetwork<String, String> sm = new SimpleSemanticNetwork<String, String>();
    StringSemNetGraphMLizer gout = new StringSemNetGraphMLizer();
    @Before
    public void setUp() throws Exception {
        sm.addRelation("A", "B", 1.0f, "def");
        sm.addRelation("A", "C", 1.0f, "pos");
    }
    
    @Test
    public void testGraphML() throws IOException {
        gout.dump(sm);
    }

}
