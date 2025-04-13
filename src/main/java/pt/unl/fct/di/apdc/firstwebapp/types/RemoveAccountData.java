package pt.unl.fct.di.apdc.firstwebapp.types;

public class RemoveAccountData {

    public String username;
    public String email;

    public RemoveAccountData() {

    }

    public RemoveAccountData(String username, String email) {
        this.username = username;
        this.email = email;
    }

    private boolean nonEmptyOrBlankField(String field) {
        return field != null && !field.isBlank();
    }

    public boolean validRegistration() {
        return nonEmptyOrBlankField(username) || nonEmptyOrBlankField(email);
    }
}
