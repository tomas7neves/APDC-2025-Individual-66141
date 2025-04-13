package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.Transaction;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import pt.unl.fct.di.apdc.firstwebapp.services.UserService;
import pt.unl.fct.di.apdc.firstwebapp.types.ChangePasswordData;
import pt.unl.fct.di.apdc.firstwebapp.types.ChangeRoleData;
import pt.unl.fct.di.apdc.firstwebapp.types.ChangeStateData;
import pt.unl.fct.di.apdc.firstwebapp.types.RemoveAccountData;
import pt.unl.fct.di.apdc.firstwebapp.types.UpdateAccountData;
import pt.unl.fct.di.apdc.firstwebapp.util.IdBlacklist;
import pt.unl.fct.di.apdc.firstwebapp.util.Functions;
import pt.unl.fct.di.apdc.firstwebapp.util.TokenBlacklist;
import pt.unl.fct.di.apdc.firstwebapp.util.TokenUtil;

@Path("/users")
public class UserResource {

    private static final Logger LOG = Logger.getLogger(UserResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    private static final UserService userService = new UserService();
    private static final Functions functions = new Functions();

    public UserResource() {
    }

    @POST
    @Path("/role")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeRole(ChangeRoleData data, @HeaderParam("Authorization") String authorization) {
        String token = null;

        // Check if Authorization header is present
        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }

        if (token == null || token.isEmpty()) {
            return Response.status(Status.UNAUTHORIZED).entity("Token is missing.").build();
        }

        // Validate the token
        if (TokenBlacklist.isTokenBlacklisted(token)) {
            return Response.status(Status.UNAUTHORIZED).entity("Token is invalid (logged out).").build();
        }

        if (!TokenUtil.verifyToken(token)) {
            return Response.status(Status.UNAUTHORIZED).entity("Invalid token.").build();
        }

        if (TokenUtil.isTokenExpired(token)) {
            return Response.status(Status.UNAUTHORIZED).entity("Token expired.").build();
        }

        // Validate the data
        if (!data.validRegistration()) {
            return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
        }

        // Extract user ID from the token
        String id = TokenUtil.getIdFromToken(token);

        // Check if the user is blacklisted
        if (IdBlacklist.isBlacklisted(id)) {
            return Response.status(Status.UNAUTHORIZED).entity("Your session is invalid. Please log in again.").build();
        }

        // Extract username and role from the token
        String role = TokenUtil.getRoleFromToken(token);

        LOG.fine("Attempt to change role for user: " + data.username);

        // Check if the user has sufficient privileges to change roles
        if ("ENDUSER".equals(role.toUpperCase()) || "PARTNER".equals(role.toUpperCase())) {
            return Response.status(Status.UNAUTHORIZED).entity("You are not authorized to change roles.")
                    .build();
        } else if ("BACKOFFICE".equals(role.toUpperCase())
                && ("BACKOFFICE".equals(data.role.toUpperCase()) || "ADMIN".equals(data.role.toUpperCase()))) {
            return Response.status(Status.UNAUTHORIZED).entity("You are not authorized to change this roles.")
                    .build();
        }

        Transaction txn = datastore.newTransaction();

        try {

            Entity result = userService.getUserByUsername(data.username, datastore);

            Key userKey = userKeyFactory.newKey(result.getKey().getName());
            Entity user = datastore.get(userKey);

            if (user == null) {
                txn.rollback();
                return Response.status(Status.CONFLICT).entity("User does not exist.").build();
            } else {

                Entity.Builder updatedUserBuilder = Entity.newBuilder(user);
                updatedUserBuilder.set("role", data.role);
                Entity updatedUser = updatedUserBuilder.build();

                txn.put(updatedUser);
                txn.commit();

                IdBlacklist.addToBlacklist(updatedUser.getKey().getName());
                LOG.info("Role changed for user: " + data.username);
                return Response.ok("Role changed.").build();

            }
        } catch (DatastoreException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    @POST
    @Path("/state")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeAccountState(ChangeStateData data, @HeaderParam("Authorization") String authorization) {
        String token = null;

        // Check if Authorization header is present
        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7); // Remove "Bearer " prefix
        }

        if (token == null || token.isEmpty()) {
            return Response.status(Status.UNAUTHORIZED).entity("Token is missing.").build();
        }

        // Validate the token
        if (TokenBlacklist.isTokenBlacklisted(token)) {
            return Response.status(Status.UNAUTHORIZED).entity("Token is invalid (logged out).").build();
        }

        if (!TokenUtil.verifyToken(token)) {
            return Response.status(Status.UNAUTHORIZED).entity("Invalid token.").build();
        }

        if (TokenUtil.isTokenExpired(token)) {
            return Response.status(Status.UNAUTHORIZED).entity("Token expired.").build();
        }

        if (!data.validRegistration()) {
            return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
        }

        String id = TokenUtil.getIdFromToken(token);

        // Extract username and role from the token
        String username = TokenUtil.getUsernameFromToken(token);

        // Check if the username is blacklisted
        if (IdBlacklist.isBlacklisted(id)) {
            return Response.status(Status.UNAUTHORIZED).entity("Your session is invalid. Please log in again.").build();
        }

        String role = TokenUtil.getRoleFromToken(token);

        LOG.fine("Attempt to change account state for user: " + username);

        // Check if the user has sufficient privileges to change roles
        if ("ENDUSER".equals(role.toUpperCase()) || "PARTNER".equals(role.toUpperCase())) {
            return Response.status(Status.UNAUTHORIZED).entity("You are not authorized to change account states.")
                    .build();
        } else if ("BACKOFFICE".equals(role.toUpperCase())
                && (!"DESACTIVATED".equals(data.state.toUpperCase()) && !"ACTIVE".equals(data.state.toUpperCase()))) {
            return Response.status(Status.UNAUTHORIZED).entity("You are not authorized to change this roles.")
                    .build();
        }

        Transaction txn = datastore.newTransaction();

        try {
            // Check if username is provided
            Key userKey = null;
            if (data.username != null) {
                Entity result = userService.getUserByUsername(data.username, datastore);
                if (result != null) {
                    userKey = result.getKey(); // Get the userKey from the query result
                }
            }
            // If no user key is found, return an error response
            if (userKey == null) {
                txn.rollback();
                return Response.status(Status.CONFLICT).entity("User does not exist.").build();
            }

            // Fetch the user entity from Datastore
            Entity user = datastore.get(userKey);

            if (user == null) {
                txn.rollback();
                return Response.status(Status.CONFLICT).entity("User does not exist.").build();
            } else {

                String userState = user.getString("state");

                if (!"ADMIN".equals(role.toUpperCase()) && "SUSPENDED".equals(userState.toUpperCase())) {
                    return Response.status(Status.UNAUTHORIZED)
                            .entity("You are not authorized to change this account state.")
                            .build();
                }

                // Change the user state
                Entity.Builder updatedUserBuilder = Entity.newBuilder(user);
                updatedUserBuilder.set("state", data.state);
                Entity updatedUser = updatedUserBuilder.build();

                txn.put(updatedUser);
                txn.commit();

                LOG.info("State changed for user: " + data.username);
                return Response.ok("State changed.").build();
            }
        } catch (DatastoreException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    @POST
    @Path("/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeUserAccount(RemoveAccountData data, @HeaderParam("Authorization") String authorization) {
        String token = null;

        // Check if Authorization header is present
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

        if (!data.validRegistration()) {
            return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
        }

        String id = TokenUtil.getIdFromToken(token);

        // Check if the user is blacklisted
        if (IdBlacklist.isBlacklisted(id)) {
            return Response.status(Status.UNAUTHORIZED).entity("Your session is invalid. Please log in again.").build();
        }

        // Extract username and role from the token
        String username = TokenUtil.getUsernameFromToken(token);
        String role = TokenUtil.getRoleFromToken(token);

        LOG.fine("Attempt to remove user: " + username);

        // Check if the user has sufficient privileges to remove accounts
        if ("ENDUSER".equals(role.toUpperCase()) || "PARTNER".equals(role.toUpperCase())) {
            return Response.status(Status.UNAUTHORIZED).entity("You are not authorized to remove accounts.")
                    .build();
        }

        Transaction txn = datastore.newTransaction();

        try {
            Key userKey = null;

            // Check if username or email is provided
            if (data.username != null) {
                Entity result = userService.getUserByUsername(data.username, datastore);
                if (result != null) {
                    userKey = result.getKey(); // Get the userKey from the query result
                }
            } else if (data.email != null) {
                Entity result = userService.getUserByEmail(data.email, datastore);
                if (result != null) {
                    userKey = result.getKey(); // Get the userKey from the query result
                }
            }

            // If no user key is found, return an error response
            if (userKey == null) {
                txn.rollback();
                return Response.status(Status.CONFLICT).entity("User does not exist.").build();
            }

            // Fetch the user entity from Datastore
            Entity user = datastore.get(userKey);

            if (user == null) {
                txn.rollback();
                return Response.status(Status.CONFLICT).entity("User does not exist.").build();
            } else {
                String userRole = user.getString("role");

                // Check if the role allows the removal of this user
                if ("BACKOFFICE".equals(role.toUpperCase()) &&
                        ("BACKOFFICE".equals(userRole.toUpperCase()) || "ADMIN".equals(userRole.toUpperCase()))) {
                    return Response.status(Status.UNAUTHORIZED)
                            .entity("You are not authorized to remove this account.")
                            .build();
                }

                // Remove the user
                datastore.delete(userKey);
                txn.commit();

                LOG.info("User deleted: " + (data.username != null ? data.username : data.email));
                return Response.ok("User deleted.").build();
            }
        } catch (DatastoreException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    @POST
    @Path("/list")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listUsers(@HeaderParam("Authorization") String authorization) {
        String token = null;

        // Check if Authorization header is present
        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7); // Remove "Bearer " prefix
        }

        if (token == null || token.isEmpty()) {
            return Response.status(Status.UNAUTHORIZED).entity("Token is missing.").build();
        }

        // Validate the token
        if (TokenBlacklist.isTokenBlacklisted(token)) {
            return Response.status(Status.UNAUTHORIZED).entity("Token is invalid (logged out).").build();
        }

        if (!TokenUtil.verifyToken(token)) {
            return Response.status(Status.UNAUTHORIZED).entity("Invalid token.").build();
        }

        if (TokenUtil.isTokenExpired(token)) {
            return Response.status(Status.UNAUTHORIZED).entity("Token expired.").build();
        }

        String id = TokenUtil.getIdFromToken(token);

        // Check if the user is blacklisted
        if (IdBlacklist.isBlacklisted(id)) {
            return Response.status(Status.UNAUTHORIZED).entity("Your session is invalid. Please log in again.").build();
        }

        // Extract username and role from the token
        String username = TokenUtil.getUsernameFromToken(token);
        String role = TokenUtil.getRoleFromToken(token);

        LOG.fine("Attempt to list users: " + username);

        // Check if the user has sufficient privileges to list users
        if ("PARTNER".equals(role.toUpperCase())) {
            return Response.status(Status.UNAUTHORIZED).entity("You are not authorized to list accounts.").build();
        }

        Transaction txn = datastore.newTransaction();

        try {
            // Build the base query to fetch users
            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind("User")
                    .build();

            List<Map<String, String>> userList = new ArrayList<>();
            Iterator<Entity> results = datastore.run(query);

            while (results.hasNext()) {
                Entity user = results.next();

                String userRole = user.getString("role");
                String userState = user.getString("state");
                String userPrivacy = user.getString("privacy");

                // Filter based on role and account status
                boolean isUserAllowed = false;

                if ("ENDUSER".equals(role.toUpperCase())) {
                    // ENDUSER can only view active, public profile users with role ENDUSER
                    if ("ENDUSER".equals(userRole.toUpperCase()) && "ACTIVE".equals(userState.toUpperCase())
                            && "PUBLIC".equals(userPrivacy.toUpperCase())) {
                        isUserAllowed = true;
                    }
                } else if ("BACKOFFICE".equals(role.toUpperCase())) {
                    // BACKOFFICE can view all ENDUSER users, regardless of profile or status
                    if ("ENDUSER".equals(userRole.toUpperCase())) {
                        isUserAllowed = true;
                    }
                } else if ("ADMIN".equals(role.toUpperCase())) {
                    // ADMIN can view all users regardless of status or role
                    isUserAllowed = true;
                }

                if (isUserAllowed) {
                    // Prepare user data to return
                    Map<String, String> userData = new HashMap<>();

                    userData.put("id", user.getKey().getName());
                    userData.put("username", functions.getUserAttribute(user, "username"));
                    userData.put("email", functions.getUserAttribute(user, "email"));
                    userData.put("name", functions.getUserAttribute(user, "name"));

                    if (!"ENDUSER".equals(role.toUpperCase())) {
                        userData.put("role", functions.getUserAttribute(user, "role"));
                        userData.put("state", functions.getUserAttribute(user, "state"));
                        userData.put("citizen_card", functions.getUserAttribute(user, "citizen_card"));
                        userData.put("address", functions.getUserAttribute(user, "address"));
                        userData.put("employer", functions.getUserAttribute(user, "employer"));
                        userData.put("phone", functions.getUserAttribute(user, "phone"));
                        userData.put("privacy", functions.getUserAttribute(user, "privacy"));
                        userData.put("created_at", functions.getUserAttribute(user, "creation_time"));
                        userData.put("job_title", functions.getUserAttribute(user, "job_title"));
                        userData.put("nif", functions.getUserAttribute(user, "nif"));
                        userData.put("photo", functions.getUserAttribute(user, "photo"));
                    }

                    // Add this user to the list
                    userList.add(userData);
                }
            }

            // Respond with the list of users as JSON array
            return Response.ok(userList).build();

        } catch (DatastoreException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    @POST
    @Path("/update")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUserAccount(UpdateAccountData data, @HeaderParam("Authorization") String authorization) {
        String token = null;

        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }

        if (token == null || token.isEmpty() || !TokenUtil.verifyToken(token)) {
            return Response.status(Status.UNAUTHORIZED).entity("Invalid or missing token.").build();
        }

        if (TokenBlacklist.isTokenBlacklisted(token) || TokenUtil.isTokenExpired(token)) {
            return Response.status(Status.UNAUTHORIZED).entity("Token is invalid or expired.").build();
        }

        String id = TokenUtil.getIdFromToken(token);
        String role = TokenUtil.getRoleFromToken(token);

        String username = TokenUtil.getUsernameFromToken(token);

        if (IdBlacklist.isBlacklisted(id)) {
            return Response.status(Status.UNAUTHORIZED).entity("Session invalid. Please log in again.").build();
        }

        Transaction txn = datastore.newTransaction();

        try {

            boolean isSelfUpdate = data.usernameToUpdate == null || data.usernameToUpdate.equals(username);

            Entity userEntity;
            String state;
            String targetRole = "";

            userEntity = userService.getUserById(id, datastore);

            if (userEntity == null) {
                txn.rollback();
                return Response.status(Status.NOT_FOUND).entity("User not found.").build();
            }

            state = userEntity.getString("state");

            if (!isSelfUpdate) {
                userEntity = userService.getUserByUsername(data.usernameToUpdate, datastore);

                if (userEntity == null) {
                    txn.rollback();
                    return Response.status(Status.NOT_FOUND).entity("User not found.").build();
                }

                targetRole = userEntity.getString("role");
            }

            // --- Role-based permission checks ---
            switch (role.toUpperCase()) {
                case "ENDUSER":
                    if (!isSelfUpdate) {
                        return Response.status(Status.UNAUTHORIZED).entity("ENDUSER can only update their own data.")
                                .build();
                    }

                    if (!"ACTIVE".equals(state.toUpperCase())) {
                        return Response.status(Status.UNAUTHORIZED).entity("Your account must be active.")
                                .build();
                    }

                    // Campos restritos para ENDUSER
                    if (data.role != null || data.accountState != null || data.username != null || data.email != null) {
                        return Response.status(Status.UNAUTHORIZED).entity("You cannot change restricted fields.")
                                .build();
                    }

                    break;

                case "PARTNER":
                    if (!isSelfUpdate) {
                        return Response.status(Status.UNAUTHORIZED).entity("PARTNER can only update their own data.")
                                .build();
                    }

                    if (!"ACTIVE".equals(state.toUpperCase())) {
                        return Response.status(Status.UNAUTHORIZED).entity("Your account must be active.")
                                .build();
                    }

                    // Campos restritos para PARTNER
                    if (data.role != null || data.accountState != null || data.username != null || data.email != null) {
                        return Response.status(Status.UNAUTHORIZED).entity("You cannot change restricted fields.")
                                .build();
                    }

                    break;

                case "BACKOFFICE":
                    if ("BACKOFFICE".equals(targetRole.toUpperCase()) || "ADMIN".equals(targetRole.toUpperCase())) {
                        return Response.status(Status.UNAUTHORIZED)
                                .entity("Cannot update BACKOFFICE or ADMIN accounts.").build();
                    }

                    if (!"ACTIVE".equals(state.toUpperCase())) {
                        return Response.status(Status.UNAUTHORIZED).entity("Your account must be active.")
                                .build();
                    }

                    // Campos que BACKOFFICE não pode modificar
                    if (data.username != null || data.email != null) {
                        return Response.status(Status.UNAUTHORIZED)
                                .entity("BACKOFFICE cannot change username or email.").build();
                    }

                    break;

                case "ADMIN":
                    // ADMIN pode modificar tudo
                    break;

                default:
                    return Response.status(Status.UNAUTHORIZED).entity("Unknown role.").build();
            }

            // --- Apply updates ---
            Entity.Builder builder = Entity.newBuilder(userEntity);

            if (data.username != null)
                builder.set("username", data.username);
            if (data.email != null)
                builder.set("email", data.email);
            if (data.name != null)
                builder.set("name", data.name);
            if (data.privacy != null)
                builder.set("privacy", data.privacy.toString());
            if (data.phone != null)
                builder.set("phone", data.phone);
            if (data.citizenCard != null)
                builder.set("citizenCard", data.citizenCard);
            if (data.nif != null)
                builder.set("nif", data.nif);
            if (data.employer != null)
                builder.set("employer", data.employer);
            if (data.jobTitle != null)
                builder.set("jobTitle", data.jobTitle);
            if (data.address != null)
                builder.set("address", data.address);
            if (data.employerNIF != null)
                builder.set("employerNIF", data.employerNIF);
            if (data.photoBase64 != null)
                builder.set("photo", functions.getPhotoUrl(data.username, data.photoBase64));

            if (data.role != null) {
                builder.set("role", data.role.toString().toUpperCase());

                // Add user to blacklist
                IdBlacklist.addToBlacklist(userEntity.getKey().getName());
            }
            if (data.accountState != null)
                builder.set("state", data.accountState.toString().toUpperCase());

            Entity updatedEntity = builder.build();
            datastore.update(updatedEntity);
            txn.commit();

            if (isSelfUpdate) {
                LOG.info("User " + username + " updated.");
            } else {
                LOG.info("User " + data.usernameToUpdate + " updated by " + username);
            }

            return Response.ok("User updated successfully.").build();

        } catch (DatastoreException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Datastore error: " + e.getMessage()).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    @POST
    @Path("/update/password")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUserPassword(ChangePasswordData data, @HeaderParam("Authorization") String authorization) {
        String token = null;

        // Check if Authorization header is present
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

        if (!data.validRegistration()) {
            return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
        }

        String id = TokenUtil.getIdFromToken(token);

        // Check if the user is blacklisted
        if (IdBlacklist.isBlacklisted(id)) {
            return Response.status(Status.UNAUTHORIZED).entity("Your session is invalid. Please log in again.").build();
        }

        // Extract username and role from the token
        String username = TokenUtil.getUsernameFromToken(token);

        LOG.fine("Attempt to update password: " + username);

        Transaction txn = datastore.newTransaction();

        try {
            Key userKey = null;

            // Check if id is provided
            Entity result = userService.getUserById(id, datastore);
            if (result != null) {
                userKey = result.getKey(); // Get the userKey from the query result
            }

            // If no user key is found, return an error response
            if (userKey == null) {
                txn.rollback();
                return Response.status(Status.CONFLICT).entity("User does not exist.").build();
            }

            // Fetch the user entity from Datastore
            Entity user = datastore.get(userKey);

            if (user == null) {
                txn.rollback();
                return Response.status(Status.CONFLICT).entity("User does not exist.").build();
            } else {

                String hashedPWD = user.getString("pwd");
                if (hashedPWD.equals(DigestUtils.sha512Hex(data.password))) {

                    Entity.Builder updatedUserBuilder = Entity.newBuilder(user);
                    updatedUserBuilder.set("pwd", DigestUtils.sha512Hex(data.newPassword));
                    Entity updatedUser = updatedUserBuilder.build();

                    txn.put(updatedUser);
                    txn.commit();

                    // Blacklist the user
                    IdBlacklist.addToBlacklist(id);

                    LOG.info("Password updated.");
                    return Response.ok("Password updated.").build();
                } else {
                    txn.rollback();
                    return Response.status(Status.UNAUTHORIZED).entity("Wrong password.").build();
                }

            }
        } catch (DatastoreException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

}