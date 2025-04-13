package pt.unl.fct.di.apdc.firstwebapp.types;

import java.util.Date;

import pt.unl.fct.di.apdc.firstwebapp.enums.WorkSheetState;
import pt.unl.fct.di.apdc.firstwebapp.enums.WorkSheetType;

public class CreateWorkSheetData {
    public String ref;
    public String description;
    public WorkSheetType type;
    public boolean adjudicated;

    // Optional
    public Date adjudicationDate;
    public Date predictedStartDate;
    public Date predictedEndDate;
    public String partnerId;
    public String adjudicationEntity;
    public String adjudicationNIF;
    public WorkSheetState workState;
    public String observations;

    public CreateWorkSheetData() {
    }

    private boolean nonEmptyOrBlankField(String field) {
        return field != null && !field.isBlank();
    }

    public boolean isValidRegistration() {
        if (!nonEmptyOrBlankField(ref) || !nonEmptyOrBlankField(description) || type == null) {
            return false;
        }

        if (!type.toString().toUpperCase().equals("PRIVATE_PROPERTY")
                && !type.toString().toUpperCase().equals("PUBLIC_PROPERTY")) {
            return false;
        }

        if (adjudicated) {
            if (!nonEmptyOrBlankField(adjudicationEntity) ||
                    !nonEmptyOrBlankField(adjudicationNIF) ||
                    !nonEmptyOrBlankField(partnerId) ||
                    !nonEmptyOrBlankField(observations) ||
                    !nonEmptyOrBlankField(adjudicationDate.toString()) ||
                    !nonEmptyOrBlankField(predictedStartDate.toString()) ||
                    !nonEmptyOrBlankField(predictedEndDate.toString())) {
                return false;
            }

            if (adjudicationDate.after(predictedStartDate) || predictedStartDate.after(predictedEndDate)) {
                return false;
            }

            if (!workState.toString().toUpperCase().equals("NON_INITIALIZED")
                    && !workState.toString().toUpperCase().equals("ON_GOING")
                    && !workState.toString().toUpperCase().equals("COMPLETED")) {
                return false;
            }
        }

        return true;
    }
}
