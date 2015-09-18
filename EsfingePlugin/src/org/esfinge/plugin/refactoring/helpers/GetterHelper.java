package org.esfinge.plugin.refactoring.helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetterHelper {

	public static boolean isValid(String method) {
		return method.matches("^get.+");
	}

	public static String getField(String method) {
		Matcher matcher = Pattern.compile("^get(.+)").matcher(method);
		if (matcher.find())
			return matcher.group(1);
		return null;
	}

}
