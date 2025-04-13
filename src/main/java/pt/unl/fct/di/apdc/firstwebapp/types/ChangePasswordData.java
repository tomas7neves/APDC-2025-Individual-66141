package pt.unl.fct.di.apdc.firstwebapp.types;

public class ChangePasswordData {

    public String password;
    public String newPassword;
    public String confirmation;

    public ChangePasswordData() {

    }

    public ChangePasswordData(String password, String newPassword, String confirmation) {
        this.password = password;
        this.newPassword = newPassword;
        this.confirmation = confirmation;
    }

    private boolean nonEmptyOrBlankField(String field) {
        return field != null && !field.isBlank();
    }

    private boolean isStrongPassword(String password) {
        if (password.length() < 8)
            return false;

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSymbol = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c))
                hasUpper = true;
            else if (Character.isLowerCase(c))
                hasLower = true;
            else if (Character.isDigit(c))
                hasDigit = true;
            else if (!Character.isLetterOrDigit(c))
                hasSymbol = true;
        }

        return hasUpper && hasLower && hasDigit && hasSymbol;
    }

    public boolean validRegistration() {
        return nonEmptyOrBlankField(password) &&
                nonEmptyOrBlankField(newPassword) &&
                nonEmptyOrBlankField(confirmation) &&
                newPassword.equals(confirmation) &&
                isStrongPassword(password);
    }
}
