package pt.unl.fct.di.apdc.firstwebapp.resources;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import pt.unl.fct.di.apdc.firstwebapp.util.TokenBlacklist;

@Path("/logout")
public class LogoutResource {

    @POST
    @Consumes("application/json")
    public Response doLogout(@HeaderParam("Authorization") String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Response.status(Status.UNAUTHORIZED).entity("Token is missing or invalid.").build();
        }

        String token = authorization.substring(7);

        // Add the token to the blacklist
        TokenBlacklist.blacklistToken(token);

        return Response.ok("Successfully logged out.").build();
    }
}
