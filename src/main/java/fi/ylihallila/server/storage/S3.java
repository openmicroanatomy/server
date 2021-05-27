package fi.ylihallila.server.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;

import static fi.ylihallila.server.util.Config.Config;

public class S3 implements StorageProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final S3Client client;

    private final Region region = Region.EU_CENTRAL_1;
    private final String bucket;

    public S3(S3Client client, String bucket) {
        this.client = client;
        this.bucket = bucket;

        client.createBucket(CreateBucketRequest
                .builder()
                .bucket(bucket)
                .createBucketConfiguration(CreateBucketConfiguration
                    .builder()
                    .locationConstraint(region.id())
                    .build()
                )
                .build());

        logger.debug("Creating S3 Bucket {}", bucket);

        client.waiter().waitUntilBucketExists(HeadBucketRequest
                .builder()
                .bucket(bucket)
                .build());

        logger.debug("S3 Bucket {} created", bucket);
    }

    @Override public void commitFile(File file) {
        var request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(file.getName())
                .build();

        client.putObject(request, RequestBody.fromFile(file));

        logger.debug("Uploaded file {} to S3 Bucket {}", file.getName(), bucket);
    }

    // https://stackoverflow.com/questions/51276201/how-to-extract-files-in-s3-on-the-fly-with-boto3
    @Override public void commitArchive(File file) {
        commitFile(file);
    }

    @Override public String getTilesURI() {
        return null;
    }

    @Override public String getThumbnailURI() {
        return null;
    }

    public void finish() {
        client.close();
    }

    public static class Builder {

        private S3ClientBuilder builder;
        private String bucket;

        public Builder() {
            this.builder = S3Client.builder();
        }

        public S3.Builder setConfigDefaults() {
            // TODO: Check for errors
            builder.region(Region.of(Config.getString("aws.region")));

            AwsCredentials awsCredentials = AwsBasicCredentials.create(
                Config.getString("s3.aws.access.key.id"),
                Config.getString("s3.aws.secret.access.key")
            );

            builder.credentialsProvider(StaticCredentialsProvider.create(awsCredentials));

            return this;
        }

        public S3.Builder setBucket(String bucket) {
            this.bucket = bucket;

            return this;
        }

        public S3 build() {
            return new S3(builder.build(), bucket);
        }
    }
}
