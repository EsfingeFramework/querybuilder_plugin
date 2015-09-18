package esfingeplugin.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class ParserHelperTest {

	@Test
	public void testGetSimpleName() {
		String simpleName = ParserHelper.getSimpleName("org.esfinge.querybuilder.QueryObject");
		assertEquals("QueryObject", simpleName);
	}

}
