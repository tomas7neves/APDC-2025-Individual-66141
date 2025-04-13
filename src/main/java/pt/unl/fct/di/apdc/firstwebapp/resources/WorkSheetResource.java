package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Logger;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import pt.unl.fct.di.apdc.firstwebapp.types.CreateWorkSheetData;
import pt.unl.fct.di.apdc.firstwebapp.util.IdBlacklist;
import pt.unl.fct.di.apdc.firstwebapp.util.TokenBlacklist;
import pt.unl.fct.di.apdc.firstwebapp.util.TokenUtil;
import pt.unl.fct.di.apdc.firstwebapp.services.UserService;

@Path("/worksheet")
public class WorkSheetResource {

    private static final Logger LOG = Logger.getLogger(UserResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    private static final UserService userService = new UserService();

    @POST
    @Consumes("application/json")
    @Path("/create")
    public Response createWorkSheet(CreateWorkSheetData data, @HeaderParam("Authorization") String authorization) {
        String token = null;

        // Extract the token
        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7); // Remove "Bearer " prefix
        }

        if (token == null || token.isEmpty()) {
            return Response.status(Status.UNAUTHORIZED).entity("Token is missing.").build();
        }

        // Validate the token
        if (!TokenUtil.verifyToken(token)) {
            return Response.status(Status.UNAUTHORIZED).entity("Invalid token.").build();
        }

        if (TokenBlacklist.isTokenBlacklisted(token)) {
            return Response.status(Status.UNAUTHORIZED).entity("Token is invalid (logged out).").build();
        }

        if (TokenUtil.isTokenExpired(token)) {
            return Response.status(Status.UNAUTHORIZED).entity("Token expired.").build();
        }

        if (!data.isValidRegistration()) {
            return Response.status(Status.BAD_REQUEST).entity("Missing or invalid fields.").build();
        }

        String id = TokenUtil.getIdFromToken(token);
        // Check if user is blacklisted
        if (IdBlacklist.isBlacklisted(id)) {
            return Response.status(Status.UNAUTHORIZED).entity("Your session is invalid. Please log in again.").build();
        }

        String username = TokenUtil.getUsernameFromToken(token);
        String role = TokenUtil.getRoleFromToken(token);

        LOG.fine("Attempt to create worksheet by user: " + username);

        if (!"BACKOFFICE".equals(role.toUpperCase())) {
            return Response.status(Status.FORBIDDEN).entity("You do not have permission to create a worksheet.")
                    .build();
        }

        Transaction txn = datastore.newTransaction();

        try {
            // Get user entity to verify it exists
            Entity userEntity = userService.getUserById(id, datastore);
            if (userEntity == null) {
                txn.rollback();
                return Response.status(Status.CONFLICT).entity("User does not exist.").build();
            }

            // Create worksheet key
            Key workSheetKey = datastore.allocateId(datastore.newKeyFactory().setKind("WorkSheet").newKey());

            // Build worksheet entity
            Entity.Builder workSheetBuilder = Entity.newBuilder(workSheetKey)
                    .set("ref", data.ref)
                    .set("description", data.description)
                    .set("type", data.type.toString().toUpperCase())
                    .set("adjudicated", data.adjudicated);

            if (data.adjudicated) {

                Entity partnerEntity = userService.getUserById(data.partnerId, datastore);

                if (partnerEntity == null) {
                    txn.rollback();
                    return Response.status(Status.CONFLICT).entity("Partner does not exist.").build();
                }

                // Check if partner is a valid partner
                if (!"PARTNER".equals(partnerEntity.getString("role").toUpperCase())) {
                    txn.rollback();
                    return Response.status(Status.CONFLICT).entity("Only partners can be associated.").build();
                }

                workSheetBuilder
                        .set("adjudicationDate", data.adjudicationDate.getTime())
                        .set("predictedStartDate", data.predictedStartDate.getTime())
                        .set("predictedEndDate", data.predictedEndDate.getTime())
                        .set("partnerId", data.partnerId)
                        .set("adjudicationEntity", data.adjudicationEntity)
                        .set("adjudicationNIF", data.adjudicationNIF)
                        .set("workState", data.workState.toString().toUpperCase())
                        .set("observations", data.observations != null ? data.observations : "");
            }

            // Save to datastore
            txn.put(workSheetBuilder.build());
            txn.commit();

            LOG.info("WorkSheet created successfully by user: " + username);
            return Response.ok("WorkSheet created successfully.").build();

        } catch (DatastoreException e) {
            if (txn.isActive())
                txn.rollback();
            LOG.warning("Datastore error: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error while creating worksheet.").build();
        } finally {
            if (txn.isActive())
                txn.rollback();
        }
    }

}
