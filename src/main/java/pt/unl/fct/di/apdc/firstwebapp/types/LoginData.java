package pt.unl.fct.di.apdc.firstwebapp.types;

public class LoginData {

	public String username;
	public String email;
	public String password;

	public LoginData() {

	}

	public LoginData(String username, String email, String password) {
		this.username = username;
		this.password = password;
		this.email = email;
	}

	private boolean nonEmptyOrBlankField(String field) {
		return field != null && !field.isBlank();
	}

	public boolean validRegistration() {
		return nonEmptyOrBlankField(username) || nonEmptyOrBlankField(email);
	}

}
