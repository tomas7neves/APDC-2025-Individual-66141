package pt.unl.fct.di.apdc.firstwebapp.types;

import pt.unl.fct.di.apdc.firstwebapp.enums.UserPrivacy;
import pt.unl.fct.di.apdc.firstwebapp.enums.UserRole;
import pt.unl.fct.di.apdc.firstwebapp.enums.UserState;

public class UpdateAccountData {

    // Opcional
    public String username;
    public String email;
    public String name;
    public UserPrivacy privacy;
    public String phone;
    public String citizenCard;
    public UserRole role;
    public String nif;
    public String employer;
    public String jobTitle;
    public String address;
    public String employerNIF;
    public UserState accountState;
    public String photoBase64;

    public String usernameToUpdate;

    public UpdateAccountData() {

    }

    public UpdateAccountData(String username, String email, String name,
            UserPrivacy privacy, String phone, UserRole role, String citizenCard, String nif,
            String employer, String jobTitle, String address, String employerNIF, UserState accountState,
            String photoBase64, String usernameToUpdate) {
        this.usernameToUpdate = usernameToUpdate;
        this.username = username;
        this.email = email;
        this.name = name;
        this.privacy = privacy;
        this.phone = phone;
        this.citizenCard = citizenCard;
        this.role = role;
        this.nif = nif;
        this.employer = employer;
        this.jobTitle = jobTitle;
        this.address = address;
        this.employerNIF = employerNIF;
        this.accountState = accountState;
        this.photoBase64 = photoBase64;
    }

}