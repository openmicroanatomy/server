package fi.ylihallila.server.storage;

import fi.ylihallila.server.Application;
import fi.ylihallila.server.util.Configuration;
import org.javaswift.joss.client.factory.AccountFactory;
import org.javaswift.joss.client.factory.AuthenticationMethod;
import org.javaswift.joss.client.factory.AuthenticationMethodScope;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Allas implements StorageProvider {

    private final String TILE_NAME_FORMAT = "{level}_{tileX}_{tileY}_{tileWidth}_{tileHeight}.jpg";
    private final String TILE_URL         = "{host}/{id}/tiles/" + TILE_NAME_FORMAT;

    private final String THUMBNAIL_NAME_FORMAT = "{id}/thumbnail.jpg";
    private final String THUMBNAIL_URL         = "{host}/" + THUMBNAIL_NAME_FORMAT;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Account used to upload tiles with.
     */
    private Account account;

    /**
     * Container used to save tiles to.
     */
    private Container container;

    public Allas(String username, String domain, String password, String authUrl, String tenantId, String tenantName, String container) {
        this(new AccountFactory()
                .setUsername(username)
                .setPassword(password)
                .setDomain(domain)
                .setAuthUrl(authUrl)
                .setTenantId(tenantId)
                .setTenantName(tenantName)
                .setAuthenticationMethod(AuthenticationMethod.KEYSTONE_V3)
                .setAuthenticationMode(AuthenticationMethodScope.PROJECT_NAME)
                .setMock(false),
            container);
    }

    public Allas(AccountFactory accountFactory, String containerName) {
        this.account = accountFactory.createAccount();
        this.container = account.getContainer(containerName);

        logger.debug("Created new Allas instance");

        if (!container.exists()) {
            container.create();
            logger.debug("Created new Allas Bucket: {}", containerName);
        }

        if (!container.isPublic()) {
            container.makePublic();
            logger.debug("Made Allas Bucket {} public", containerName);
        }
    }

    /**
     * Uploads a single file to Allas Object Storage.
     *
     * @param file file to upload.
     */
    @Override public void commitFile(File file) {
		StoredObject object = container.getObject(file.getName());
		object.uploadObject(file);

        logger.debug("Uploaded file {} to Allas Bucket {}", file.getName(), container.getName());
    }

    @Override public void commitFile(byte[] bytes, String fileName) {
        StoredObject object = container.getObject(fileName);
        object.uploadObject(bytes);

        logger.debug("Uploaded file {} to Allas Bucket {}", fileName, container.getName());
    }

    /**
     * Uploads an archive to Allas Object Storage. The container object "tiles" is used,
     * as that will be the folder the archive contents will be extracted to.
     *
     * @param file archive to upload.
     */
    @Override public void commitArchive(File file) {
        StoredObject object = container.getObject("tiles");
        object.uploadArchive(file, "tar");

        logger.debug("Uploaded archive {} to Allas Bucket {}", file.getName(), container.getName());
    }

    @Override public String getTilesURI() {
        String host = account.getPublicURL();

        return TILE_URL.replace("{host}", host);
    }

    @Override public String getThumbnailURI() {
        String host = account.getPublicURL();

        return THUMBNAIL_URL.replace("{host}", host);
    }

    @Override public String getTileNamingFormat() {
        return TILE_NAME_FORMAT;
    }

    @Override public String getThumbnailNamingFormat() {
        return THUMBNAIL_NAME_FORMAT;
    }

    @Override
    public String getName() {
        return "CSC Allas";
    }

    public static class Builder {

        private AccountFactory factory;
        private String container;

        public Builder() {
            this.factory = new AccountFactory();
        }

        public Builder setConfigDefaults() {
            Configuration.AllasConfiguration config = Application.getConfiguration().allas();

            factory.setUsername(config.username())
                    .setPassword(config.password())
                    .setAuthUrl(config.authUrl())
                    .setDomain(config.domain())
                    .setTenantId(config.tenantId())
                    .setTenantName(config.tenantName())
                    .setAuthenticationMethod(AuthenticationMethod.KEYSTONE_V3)
                    .setAuthenticationMode(AuthenticationMethodScope.PROJECT_NAME)
                    .setMock(false);

            return this;
        }

        public Builder setAccountFactory(AccountFactory factory) {
            this.factory = factory;

            return this;
        }

        public Builder setUsername(String username) {
            factory.setUsername(username);
            factory.setAuthenticationMethod(AuthenticationMethod.KEYSTONE_V3);
            factory.setAuthenticationMode(AuthenticationMethodScope.PROJECT_NAME);

            return this;
        }

        public Builder setPassword(String password) {
            factory.setPassword(password);

            return this;
        }

        public Builder setDomain(String domain) {
            factory.setDomain(domain);

            return this;
        }

        public Builder setAuthUrl(String authUrl) {
            factory.setAuthUrl(authUrl);

            return this;
        }

        public Builder setTenantId(String tenantId) {
            factory.setTenantId(tenantId);

            return this;
        }

        public Builder setTenantName(String tenantName) {
            factory.setTenantName(tenantName);

            return this;
        }

        public Builder setContainer(String container) {
            this.container = container;

            return this;
        }

        public Allas build() {
            return new Allas(factory, container);
        }
    }
}
