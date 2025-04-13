package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.Query;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import pt.unl.fct.di.apdc.firstwebapp.services.UserService;
import pt.unl.fct.di.apdc.firstwebapp.types.RegisterData;
import pt.unl.fct.di.apdc.firstwebapp.util.Functions;

@Path("/register")
public class RegisterResource {

    private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    private static final UserService userUtil = new UserService();
    private static final Functions functions = new Functions();

    public RegisterResource() {
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerUser(RegisterData data) {
        LOG.fine("Attempt to register user: " + data.username);

        if (!data.validRegistration()) {
            return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
        }

        if (!data.privacy.toString().toUpperCase().equals("PUBLIC")
                && !data.privacy.toString().toUpperCase().equals("PRIVATE")) {
            return Response.status(Status.BAD_REQUEST).entity("Privacy must be PUBLIC or PRIVATE.").build();
        }

        Transaction txn = datastore.newTransaction();
        try {
            String id = generateId();

            Entity result = userUtil.getUserById(id, datastore);

            if (result != null) {
                id = generateId();
            }

            if (id == null || id.isEmpty()) {
                txn.rollback();
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error generating user ID.").build();
            }

            // Check if the username is already taken
            result = userUtil.getUserByUsername(data.username, datastore);

            if (result != null) {
                txn.rollback();
                return Response.status(Status.CONFLICT).entity("Username already taken.").build();
            }

            // Check if the email is already taken
            result = userUtil.getUserByEmail(data.email, datastore);

            if (result != null) {
                txn.rollback();
                return Response.status(Status.CONFLICT).entity("Email already taken.").build();
            }

            Key userKey = datastore.newKeyFactory().setKind("User").newKey(id);
            Entity user = txn.get(userKey);

            String photoUrl = functions.getPhotoUrl(data.username, data.photoBase64);

            user = Entity.newBuilder(userKey)
                    .set("username", data.username)
                    .set("name", data.name)
                    .set("pwd", DigestUtils.sha512Hex(data.password))
                    .set("email", data.email)
                    .set("privacy", data.privacy.toString().toUpperCase())
                    .set("phone", data.phone)
                    .set("role", "ENDUSER")
                    .set("state", "DESACTIVATED")
                    .set("creation_time", Timestamp.now()).build();

            if (data.citizenCard != null) {
                user = Entity.newBuilder(user)
                        .set("citizen_card", data.citizenCard).build();
            }
            ;

            if (data.nif != null) {
                user = Entity.newBuilder(user)
                        .set("nif", data.nif).build();
            }

            if (data.employer != null) {
                user = Entity.newBuilder(user)
                        .set("employer", data.employer).build();
            }

            if (data.jobTitle != null) {
                user = Entity.newBuilder(user)
                        .set("job_title", data.jobTitle).build();
            }

            if (data.address != null) {
                user = Entity.newBuilder(user)
                        .set("address", data.address).build();
            }

            if (data.employerNIF != null) {
                user = Entity.newBuilder(user)
                        .set("employer_nif", data.employerNIF).build();
            }

            if (photoUrl != null) {
                user = Entity.newBuilder(user)
                        .set("photo", photoUrl)
                        .build();
            }

            txn.put(user);
            txn.commit();
            LOG.info("User registered " + data.username);
            return Response.ok("User successfully registered.").build();
        } catch (DatastoreException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    @POST
    @Path("/admin")
    public Response createAdmin() {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("User")
                .setFilter(StructuredQuery.PropertyFilter.eq("role", "ADMIN"))
                .build();

        QueryResults<Entity> results = datastore.run(query);

        if (results.hasNext()) {
            return Response.ok("Admin already exists.").build();
        }

        String userId = generateId();
        Key key = datastore.newKeyFactory().setKind("User").newKey(userId);

        Entity admin = Entity.newBuilder(key)
                .set("username", "root")
                .set("name", "Root")
                .set("pwd", DigestUtils.sha512Hex("Root123!"))
                .set("email", "root@root.com")
                .set("privacy", "PRIVATE")
                .set("phone", "000000000")
                .set("role", "ADMIN")
                .set("state", "ACTIVE")
                .set("creation_time", Timestamp.now())
                .build();

        datastore.put(admin);
        return Response.ok("Admin user created.").build();
    }

    private String generateId() {
        String id = "";
        for (int i = 0; i < 10; i++) {
            id += (char) ((Math.random() * 26) + 65);
        }
        return id;
    }

}