package org.esfinge.plugin.refactoring.helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryBuilderMethodHelper {

	private String type = null;
	private String by = null;
	private String orderBy = null;

	private QueryBuilderMethodHelper(String method) {
		if (method.startsWith("get")) {
			String[] split;
			if (method.matches(".+OrderBy.+")) {
				split = method.split("OrderBy");
				method = split[0];
				orderBy = split[1];
			}
			if (method.matches(".+By.+")) {
				split = method.split("By");
				method = split[0];
				by = split[1];
			}
			type = method.substring(3);
		} else {
			// TODO: raise malformed exception
		}
	}

	private String getFullName() {
		String fullName = "get" + type;
		if (by != null)
			fullName = fullName + "By" + by;
		if (orderBy != null)
			fullName = fullName + "OrderBy" + orderBy;
		return fullName;
	}

	private boolean containsField(String field) {
		if (by != null && Pattern.compile(byRegex(field)).matcher(by).find())
			return true;
		if (orderBy != null && Pattern.compile(orderByRegex(field)).matcher(orderBy).find())
			return true;
		return false;
	}

	private void replaceField(String oldField, String newField) {
		if (by != null)
			by = replaceQueryField(by, newField, byRegex(oldField));
		if (orderBy != null)
			orderBy = replaceQueryField(orderBy, newField, orderByRegex(oldField));
	}

	private String replaceQueryField(String query, String newField, String regex) {
		Matcher matcher = Pattern.compile(regex).matcher(query);
		while (matcher.find()) {
			String string = "";
			if (matcher.group(1) != null)
				string = string + matcher.group(1);
			string = string + newField;
			if (matcher.group(2) != null)
				string = string + matcher.group(2);
			query = query.replaceFirst(matcher.group(0), string);
			matcher = Pattern.compile(regex).matcher(query);
		}
		return query;
	}

	private String orderByRegex(String field) {
		return "(And|^)" + field + "(Asc|Desc|$|And)";
	}

	private String byRegex(String field) {
		return "(And|Or|^)" + field + "(Lesser|Greater|LesserOrEquals|GreaterOrEquals|NotEquals|Contains|Starts|Ends|$|And|Or)";
	}

	public static boolean isValid(String method) {
		return method.startsWith("get");
	}

	public static boolean hasField(String methodName, String field) {
		QueryBuilderMethodHelper method = new QueryBuilderMethodHelper(methodName);
		return method.containsField(field);
	}

	public static String replaceType(String methodName, String newType) {
		QueryBuilderMethodHelper method = new QueryBuilderMethodHelper(methodName);
		method.type = newType;
		return method.getFullName();
	}

	public static String replaceField(String methodName, String oldField, String newField) {
		QueryBuilderMethodHelper method = new QueryBuilderMethodHelper(methodName);
		method.replaceField(oldField, newField);
		return method.getFullName();
	}

}
