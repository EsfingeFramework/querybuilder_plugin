package org.esfinge.plugin.refactoring.helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryObjectHelper {

	private static final String FIELD_PARAMETERS = "(Lesser|Greater|LesserOrEquals|GreaterOrEquals|NotEquals|Contains|Starts|Ends|$)";

	private static String fieldRegex(String field) {
		return "^" + field + FIELD_PARAMETERS;
	}

	private static String methodRegex(String field) {
		return "^(get|set)" + field + FIELD_PARAMETERS;
	}

	public static String uncapitalize(String string) {
		return string.substring(0, 1).toLowerCase() + string.substring(1);
	}

	public static class Field {
		public static boolean hasField(String name, String field) {
			field = uncapitalize(field);
			if (Pattern.compile(fieldRegex(field)).matcher(name).find())
				return true;
			return false;
		}

		public static String replaceField(String name, String oldField, String newField) {
			oldField = uncapitalize(oldField);
			newField = uncapitalize(newField);
			Matcher matcher = Pattern.compile(fieldRegex(oldField)).matcher(name);
			if (matcher.find())
				return newField + matcher.group(1);
			return name;
		}
	}

	public static class Method {
		public static boolean hasField(String name, String field) {
			if (Pattern.compile(methodRegex(field)).matcher(name).find())
				return true;
			return false;
		}

		public static String replaceField(String name, String oldField, String newField) {
			Matcher matcher = Pattern.compile(methodRegex(oldField)).matcher(name);
			if (matcher.find())
				return matcher.group(1) + newField + matcher.group(2);
			return name;
		}
	}

}
