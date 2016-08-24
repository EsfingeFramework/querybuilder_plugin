package esfingeplugin.util;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.JavaModelException;

public class ParserHelper {

	public static String getFullName(String className, ICompilationUnit cUnit) throws JavaModelException {
		return getMatchedPackage(className, cUnit) + "." + className;
	}

	public static String getMatchedPackage(String className, ICompilationUnit cUnit) throws JavaModelException {
		int higherAccuracy = 0;
		String matchedPackage = cUnit.getPackageDeclarations()[0].getElementName();
		IImportDeclaration[] imports = cUnit.getImports();
		for (IImportDeclaration impDec : imports) {
			String fullName = impDec.getElementName();
			int dotIndex = fullName.lastIndexOf('.');

			String lastNameOfImport = fullName.substring(dotIndex + 1);
			if (lastNameOfImport.equals(className) && higherAccuracy < 2) {
				matchedPackage = fullName.substring(0, dotIndex);
				higherAccuracy = 2;
			} else if (lastNameOfImport.equals("*") && higherAccuracy < 1) {
				matchedPackage = fullName.substring(0, dotIndex);
				higherAccuracy = 1;
			}
		}

		if (higherAccuracy > 0) return matchedPackage;

		if (cUnit.getType(className) != null) return cUnit.getPackageDeclarations()[0].getElementName();

		return "java.lang";
	}

	public static boolean checkPackage(String className, String packageName, ICompilationUnit cUnit) throws JavaModelException {
		return packageName.equals(getMatchedPackage(className, cUnit));
	}

	public static String getEntityFullNameFromReturnTypeSignature(String returnTypeSignature, ICompilationUnit cUnit) throws JavaModelException {
		String entitySimpleName;
		if (returnTypeSignature.contains("<")) {
			int firstIndex = returnTypeSignature.lastIndexOf('<');
			int lastIndex = returnTypeSignature.lastIndexOf('>');

			entitySimpleName = returnTypeSignature.substring(firstIndex + 2, lastIndex - 1);
		} else {
			entitySimpleName = returnTypeSignature.substring(1, returnTypeSignature.length() - 1);
		}

		return getFullName(entitySimpleName, cUnit);
	}

	public static String getSimpleName(String fullName) {
		return fullName.substring(fullName.lastIndexOf('.') + 1);
	}

}
