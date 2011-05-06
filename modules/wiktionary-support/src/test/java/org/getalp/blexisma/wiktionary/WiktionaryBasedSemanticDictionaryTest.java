package org.getalp.blexisma.wiktionary;


import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.getalp.blexisma.api.ConceptualVector;
import org.getalp.blexisma.api.SemanticDefinition;
import org.getalp.blexisma.api.VectorialBase;
import org.getalp.blexisma.api.syntaxanalysis.MorphoProperties;
import org.getalp.blexisma.impl.vectorialbase.RAM_VectorialBase;
import org.jmock.Mockery;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class WiktionaryBasedSemanticDictionaryTest {
    private Mockery context = new JUnit4Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};
    

	SimpleSemanticNetwork<String, String> wn;
	RAM_VectorialBase vb = context.mock(RAM_VectorialBase.class);
	BufferedReader br;
	InputStream fis;
	
	WiktionaryBasedSemanticDictionary dict;
	
	@Before
	public void setUp() throws Exception {
		
		fis = WiktionaryBasedSemanticDictionaryTest.class.getResourceAsStream("sample-semnet.snet");
        br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
        wn = new SimpleSemanticNetwork<String, String>();
		TextOnlySemnetReader.readFromReader(wn, br);
		
		
		dict = new WiktionaryBasedSemanticDictionary(vb, wn);
	}
	
	@After
	public void tearDown() throws Exception {
		br.close();
		br = null;
		fis.close();
		fis = null;
	}

	@Test
	public void testSemanticDictionary() {
		context.checking(new Expectations() {{
			allowing(vb).getVector(with(aNonNull(String.class)));   
			will(returnValue(ConceptualVector.randomisedCV(2000, 2000000, 5, 5)));
	    }});

		SemanticDefinition sdef = dict.getDefinition("dictionnaire","fra");
		
		assertEquals("Incorrect number of senses.", sdef.getSenseList().size(), 10);
		assertTrue("Morpho should be a noun.", sdef.getSenseList().get(0).getMorpho().contains(MorphoProperties.NOUN));
	}

	@Test
	public void testDefinitionIsNotNull() {
		context.checking(new Expectations() {{
			allowing(vb).getVector(with(aNonNull(String.class)));    // The turtle can be asked about its pen any number of times and will always
		    will(returnValue(ConceptualVector.randomisedCV(2000, 2000000, 5, 5)));
	    }});

		SemanticDefinition sdef = dict.getDefinition("tagada","fra");
		
		assertNotNull("getDefinition should never return null.", sdef);
		assertNotNull("Sense list should not be null.", sdef.getSenseList());
		assertEquals("Sense list should be empty.", sdef.getSenseList().size(), 0);
	}

}
