package pt.unl.fct.di.apdc.firstwebapp.types;

import pt.unl.fct.di.apdc.firstwebapp.enums.UserPrivacy;
import pt.unl.fct.di.apdc.firstwebapp.enums.UserRole;
import pt.unl.fct.di.apdc.firstwebapp.enums.UserState;

public class RegisterData {

    public String username;
    public String password;
    public String confirmation;
    public String email;
    public String name;
    public UserPrivacy privacy;
    public String phone;

    // Opcional
    public String citizenCard;
    public UserRole role;
    public String nif;
    public String employer;
    public String jobTitle;
    public String address;
    public String employerNIF;
    public UserState accountState;
    public String photoBase64;

    public RegisterData() {

    }

    public RegisterData(String username, String password, String confirmation, String email, String name,
            UserPrivacy privacy, String phone, String citizenCard, String nif,
            String employer, String jobTitle, String address, String employerNIF) {
        this.username = username;
        this.password = password;
        this.confirmation = confirmation;
        this.email = email;
        this.name = name;
        this.privacy = privacy;
        this.phone = phone;
        this.citizenCard = citizenCard;
        this.nif = nif;
        this.employer = employer;
        this.jobTitle = jobTitle;
        this.address = address;
        this.employerNIF = employerNIF;
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
        return nonEmptyOrBlankField(username) &&
                nonEmptyOrBlankField(password) &&
                nonEmptyOrBlankField(email) &&
                nonEmptyOrBlankField(name) &&
                email.contains("@") &&
                password.equals(confirmation) &&
                isStrongPassword(password) &&
                nonEmptyOrBlankField(phone);
    }

}