package org.esfinge.refactoring.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.esfinge.plugin.refactoring.helpers.QueryObjectHelper;
import org.junit.Test;

public class QueryObjectHelperTest {

	@Test
	public void fieldHasFieldTest() {
		assertTrue(QueryObjectHelper.Field.hasField("age", "Age"));
		assertTrue(QueryObjectHelper.Field.hasField("ageGreater", "Age"));
	}

	@Test
	public void fieldReplaceFieldTest() {
		assertEquals("fullName", QueryObjectHelper.Field.replaceField("name", "Name", "FullName"));
		assertEquals("fullNameNotEquals", QueryObjectHelper.Field.replaceField("nameNotEquals", "Name", "FullName"));
	}

	@Test
	public void methodHasFieldTest() {
		assertTrue(QueryObjectHelper.Method.hasField("getAge", "Age"));
		assertTrue(QueryObjectHelper.Method.hasField("getAgeGreater", "Age"));
	}

	@Test
	public void methodReplaceFieldTest() {
		assertEquals("getFullName", QueryObjectHelper.Method.replaceField("getName", "Name", "FullName"));
		assertEquals("getFullNameNotEquals", QueryObjectHelper.Method.replaceField("getNameNotEquals", "Name", "FullName"));
	}

}
