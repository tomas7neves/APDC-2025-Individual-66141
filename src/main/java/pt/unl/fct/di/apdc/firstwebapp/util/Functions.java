package pt.unl.fct.di.apdc.firstwebapp.util;

import java.util.Base64;
import java.util.Collections;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Entity;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

public class Functions {

    public String getUserAttribute(Entity user, String attribute) {
        if (!user.contains(attribute)) {
            return "NOT DEFINED";
        }

        if ("creation_time".equals(attribute)) {
            try {
                Timestamp creationTime = user.getTimestamp(attribute);
                return creationTime != null ? creationTime.toDate().toString() : "NOT DEFINED";
            } catch (Exception e) {
                return "NOT DEFINED";
            }
        }

        try {
            return user.getString(attribute);
        } catch (Exception e) {
            return "NOT DEFINED";
        }
    }

    public String getPhotoUrl(String username, String photoBase64) {
        Storage storage = StorageOptions.getDefaultInstance().getService();

        byte[] photoBytes = null;
        String photoUrl = null;
        if (photoBase64 != null && !photoBase64.isEmpty()) {
            String base64Data = photoBase64;

            // Support both full data URI and raw base64
            if (base64Data.contains(",")) {
                String[] parts = base64Data.split(",", 2);
                base64Data = parts[1];
            }

            photoBytes = Base64.getDecoder().decode(base64Data);

            BlobId blobId = BlobId.of("avaliacao-individual-456215.appspot.com",
                    "photos/" + username + ".jpg");

            BlobInfo blobInfo = BlobInfo
                    .newBuilder(blobId)
                    .setAcl(Collections.singletonList(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)))
                    .setContentType("image/jpeg")
                    .build();

            storage.create(blobInfo, photoBytes);

            photoUrl = String.format("https://storage.googleapis.com/%s/%s",
                    blobId.getBucket(), blobId.getName());

        }

        return photoUrl;
    }
}
