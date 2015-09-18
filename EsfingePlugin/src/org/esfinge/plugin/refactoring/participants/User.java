package org.esfinge.plugin.refactoring.participants;

public class User {

	private String name;
	private String lastName;

	public String getName() {
		return name;
	}

	public String getSurname() {
		return lastName;
	}

	public String getFullName() {
		return getName() + getSurname();
	}

}
