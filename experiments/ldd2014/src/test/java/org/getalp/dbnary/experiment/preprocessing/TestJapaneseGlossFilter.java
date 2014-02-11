package org.getalp.dbnary.experiment.preprocessing;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class TestJapaneseGlossFilter {

	private JapaneseGlossFilter filter;

	@Before
	public void setUp() throws Exception {
		filter = new JapaneseGlossFilter();
	}

	@Test
	public void test1() {
		StructuredGloss sg = filter.extractGlossStructure("及び語義2");
		assertTrue(sg.gloss.equals("及び"));
		assertTrue(sg.senseNumber.equals("2"));
	}
	
	@Test
	public void test2() {
		StructuredGloss sg = filter.extractGlossStructure("市場（シジョウ、語義2）");
		assertTrue(sg.gloss.equals("市場（シジョウ、"));
		assertTrue(sg.senseNumber.equals("2"));
	}

	@Test
	public void test3() {
		StructuredGloss sg = filter.extractGlossStructure("語義1及び語義2");
		assertTrue(sg.gloss == null);
		assertTrue(sg.senseNumber.equals("1,2"));
	}

}
