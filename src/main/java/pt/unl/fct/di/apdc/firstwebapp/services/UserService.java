package pt.unl.fct.di.apdc.firstwebapp.services;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;

public class UserService {

    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    public UserService() {
    }

    public Entity getUserById(String id, Datastore datastore) {
        Key userKey = userKeyFactory.newKey(id); // assumes id is a string name
        System.out.println("Attempting to fetch user with key: " + userKey);

        Entity result = datastore.get(userKey);

        if (result == null) {
            System.out.println("No user found for key: " + userKey);
            return null;
        }

        return result;
    }

    public Entity getUserByUsername(String username, Datastore datastore) {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("User")
                .setFilter(PropertyFilter.eq("username", username))
                .build();

        var results = datastore.run(query);
        if (!results.hasNext()) {
            return null;
        }

        Entity result = results.next();
        return result != null ? datastore.get(result.getKey()) : null;
    }

    public Entity getUserByEmail(String email, Datastore datastore) {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("User")
                .setFilter(PropertyFilter.eq("email", email))
                .build();

        var results = datastore.run(query);
        if (!results.hasNext()) {
            return null;
        }

        Entity result = results.next();
        return result != null ? datastore.get(result.getKey()) : null;
    }

}
