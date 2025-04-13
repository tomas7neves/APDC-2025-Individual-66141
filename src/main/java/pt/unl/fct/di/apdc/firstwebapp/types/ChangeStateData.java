package pt.unl.fct.di.apdc.firstwebapp.types;

public class ChangeStateData {

    public String username;
    public String state;

    public ChangeStateData() {

    }

    public ChangeStateData(String username, String state) {
        this.username = username;
        this.state = state;
    }

    private boolean nonEmptyOrBlankField(String field) {
        return field != null && !field.isBlank();
    }

    public boolean validRegistration() {
        if (!nonEmptyOrBlankField(username) || !nonEmptyOrBlankField(state)) {
            return false;
        }

        String newState = state.toUpperCase();
        return newState.equals("ACTIVE") || newState.equals("SUSPENDED") || newState.equals("DESACTIVATED");
    }
}
