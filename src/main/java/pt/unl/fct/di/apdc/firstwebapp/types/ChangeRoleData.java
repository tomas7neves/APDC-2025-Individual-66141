package pt.unl.fct.di.apdc.firstwebapp.types;

public class ChangeRoleData {

    public String username;
    public String role;

    public ChangeRoleData() {

    }

    public ChangeRoleData(String username, String role) {
        this.username = username;
        this.role = role;
    }

    private boolean nonEmptyOrBlankField(String field) {
        return field != null && !field.isBlank();
    }

    public boolean validRegistration() {
        if (!nonEmptyOrBlankField(username) || !nonEmptyOrBlankField(role)) {
            return false;
        }

        String newRole = role.toUpperCase();
        return newRole.equals("ENDUSER") || newRole.equals("BACKOFFICE") || newRole.equals("ADMIN")
                || newRole.equals("PARTNER");
    }
}
