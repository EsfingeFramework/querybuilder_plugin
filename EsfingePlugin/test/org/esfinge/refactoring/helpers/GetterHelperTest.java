package org.esfinge.refactoring.helpers;

import static org.junit.Assert.*;

import org.esfinge.plugin.refactoring.helpers.GetterHelper;
import org.junit.Test;

public class GetterHelperTest {

	@Test
	public void isValidtest() {
		assertTrue(GetterHelper.isValid("getName"));
		assertTrue(GetterHelper.isValid("getLastName"));
		assertTrue(GetterHelper.isValid("getAge"));

		assertFalse(GetterHelper.isValid("findName"));
		assertFalse(GetterHelper.isValid("findLastName"));
		assertFalse(GetterHelper.isValid("setAge"));
	}

	@Test
	public void getFieldTest() {
		assertEquals("Name", GetterHelper.getField("getName"));
		assertEquals("LastName", GetterHelper.getField("getLastName"));
		assertEquals("Age", GetterHelper.getField("getAge"));

		assertNull(GetterHelper.getField("findName"));
		assertNull(GetterHelper.getField("findLastName"));
		assertNull(GetterHelper.getField("findAge"));
	}

}
