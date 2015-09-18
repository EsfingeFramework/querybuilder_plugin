package org.esfinge.refactoring.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.esfinge.plugin.refactoring.helpers.QueryBuilderMethodHelper;
import org.junit.Test;

public class QueryBuilderMethodHelperTest {

	@Test
	public void isValidTest() {
		assertTrue(QueryBuilderMethodHelper.isValid("getPersonByLastName"));
		assertTrue(QueryBuilderMethodHelper.isValid("getPersonByNameAndAge"));
		assertTrue(QueryBuilderMethodHelper.isValid("getPersonByNameAndAgeLesser"));
		assertTrue(QueryBuilderMethodHelper.isValid("getPersonByAgeOrderByName"));
		assertTrue(QueryBuilderMethodHelper.isValid("getPersonByNameOrderByNameDesc"));
		assertTrue(QueryBuilderMethodHelper.isValid("getPersonByNameNotEqualsOrAgeGreaterOrderByNameAscAndAgeDesc"));

		assertFalse(QueryBuilderMethodHelper.isValid("findgetPersonByName"));
	}

	@Test
	public void containsFieldTest() {
		assertTrue(QueryBuilderMethodHelper.hasField("getPersonByName", "Name"));
		assertTrue(QueryBuilderMethodHelper.hasField("getPersonByNameAndAge", "Name"));
		assertTrue(QueryBuilderMethodHelper.hasField("getPersonByNameAndAgeLesser", "Age"));
		assertTrue(QueryBuilderMethodHelper.hasField("getPersonByAgeOrderByName", "Name"));
		assertTrue(QueryBuilderMethodHelper.hasField("getPersonByNameOrderByNameDesc", "Name"));
		assertTrue(QueryBuilderMethodHelper.hasField("getPersonByNameNotEqualsOrAgeGreaterOrderByNameAscAndAgeDesc", "Age"));

		assertFalse(QueryBuilderMethodHelper.hasField("getPersonByLastName", "Age"));
		assertFalse(QueryBuilderMethodHelper.hasField("getPersonByLastName", "Name"));
		assertFalse(QueryBuilderMethodHelper.hasField("getPersonByNames", "Name"));
	}

	@Test
	public void replaceTypeTest() {
		assertEquals("getPersonaByLastName", QueryBuilderMethodHelper.replaceType("getPersonByLastName", "Persona"));
		assertEquals("getPersonaByNameAndAge", QueryBuilderMethodHelper.replaceType("getPersonByNameAndAge", "Persona"));
		assertEquals("getPersonaByNameAndAgeLesser", QueryBuilderMethodHelper.replaceType("getPersonByNameAndAgeLesser", "Persona"));
		assertEquals("getPersonaByAgeOrderByName", QueryBuilderMethodHelper.replaceType("getPersonByAgeOrderByName", "Persona"));
		assertEquals("getPersonaByNameOrderByNameDesc", QueryBuilderMethodHelper.replaceType("getPersonByNameOrderByNameDesc", "Persona"));
		assertEquals("getPersonaByNameNotEqualsOrAgeGreaterOrderByNameAscAndAgeDesc", QueryBuilderMethodHelper.replaceType("getPersonByNameNotEqualsOrAgeGreaterOrderByNameAscAndAgeDesc", "Persona"));
	}

	@Test
	public void replaceFieldTest() {
		assertEquals("getPersonByNames", QueryBuilderMethodHelper.replaceField("getPersonByName", "Name", "Names"));
		assertEquals("getPersonByFullName", QueryBuilderMethodHelper.replaceField("getPersonByLastName", "LastName", "FullName"));
		assertEquals("getPersonByLastNameAndAge", QueryBuilderMethodHelper.replaceField("getPersonByNameAndAge", "Name", "LastName"));
		assertEquals("getPersonByNameAndYearsLesser", QueryBuilderMethodHelper.replaceField("getPersonByNameAndAgeLesser", "Age", "Years"));
		assertEquals("getPersonByAgeOrderByLastName", QueryBuilderMethodHelper.replaceField("getPersonByAgeOrderByName", "Name", "LastName"));
		assertEquals("getPersonByLastNameOrderByLastNameDesc", QueryBuilderMethodHelper.replaceField("getPersonByNameOrderByNameDesc", "Name", "LastName"));
		assertEquals("getPersonByLastNameNotEqualsOrAgeGreaterOrderByLastNameAscAndAgeDesc", QueryBuilderMethodHelper.replaceField("getPersonByNameNotEqualsOrAgeGreaterOrderByNameAscAndAgeDesc", "Name", "LastName"));
	}

}
