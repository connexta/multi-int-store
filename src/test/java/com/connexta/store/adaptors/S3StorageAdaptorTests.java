/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.store.adaptors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.connexta.store.config.AmazonS3Configuration;
import com.connexta.store.exceptions.DatasetNotFoundException;
import com.connexta.store.exceptions.StoreException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class S3StorageAdaptorTests {

  public static final String ASDF = "asdf";
  private static final String MINIO_ADMIN_ACCESS_KEY = "admin";
  private static final String MINIO_ADMIN_SECRET_KEY = "12345678";
  private static final int MINIO_PORT = 9000;
  public static final String KEY = "1234";
  private static AmazonS3Configuration configuration;
  private static AmazonS3 amazonS3;
  private static StorageAdaptor storageAdaptor;
  private static String BUCKET = "metacard-quarantine";

  @Container
  public static final GenericContainer minioContainer =
      new GenericContainer("minio/minio:RELEASE.2019-07-10T00-34-56Z")
          .withEnv("MINIO_ACCESS_KEY", MINIO_ADMIN_ACCESS_KEY)
          .withEnv("MINIO_SECRET_KEY", MINIO_ADMIN_SECRET_KEY)
          .withExposedPorts(MINIO_PORT)
          .withCommand("server --compat /data/metacard-quarantine")
          .waitingFor(
              new HttpWaitStrategy()
                  .forPath("/minio/health/ready")
                  .withStartupTimeout(Duration.ofSeconds(30)));

  @BeforeAll
  public static void setUp() {
    configuration =
        new AmazonS3Configuration(
            String.format(
                "http://%s:%d",
                minioContainer.getContainerIpAddress(), minioContainer.getMappedPort(MINIO_PORT)),
            "local",
            MINIO_ADMIN_ACCESS_KEY,
            MINIO_ADMIN_SECRET_KEY);

    amazonS3 =
        AmazonS3ClientBuilder.standard()
            .withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(
                    configuration.getEndpoint(), configuration.getRegion()))
            .withCredentials(
                new AWSStaticCredentialsProvider(
                    new BasicAWSCredentials(
                        configuration.getAccessKey(), configuration.getSecretKey())))
            .enablePathStyleAccess()
            .build();

    storageAdaptor = new S3StorageAdaptor(amazonS3, BUCKET);
  }

  @BeforeEach
  public void beforeEach() {
    amazonS3.createBucket(BUCKET);
  }

  @AfterEach
  public void afterEach() {
    // Clear out S3
    amazonS3.listObjects(BUCKET).getObjectSummaries().stream()
        .map(S3ObjectSummary::getKey)
        .forEach(key -> amazonS3.deleteObject(BUCKET, key));
    amazonS3.deleteBucket(BUCKET);
  }

  @Test
  public void testSuccessfulStoreRequest() {
    storageAdaptor.store(
        4L,
        MediaType.APPLICATION_XML_VALUE,
        new ByteArrayInputStream(ASDF.getBytes()),
        KEY,
        Map.of());
  }

  @Test
  public void testSuccessfulRetrieveRequest() {
    final String key = KEY;
    final String metacardContents = ASDF;
    storageAdaptor.store(
        4L,
        MediaType.APPLICATION_XML_VALUE,
        new ByteArrayInputStream(metacardContents.getBytes()),
        key,
        Map.of());
    assertThat(storageAdaptor.retrieve(key).getInputStream(), hasContents(metacardContents));
  }

  @Test
  public void testRetrieveRequestWrongKey() {
    String key = KEY;
    storageAdaptor.store(
        4L,
        MediaType.APPLICATION_XML_VALUE,
        new ByteArrayInputStream(ASDF.getBytes()),
        key,
        Map.of());
    assertThrows(DatasetNotFoundException.class, () -> storageAdaptor.retrieve("wrong_key"));
  }

  @Test
  public void testStoringDuplicateKey() {
    // TODO add code for checking duplicate products.
  }

  @Test
  public void testStoreWithContentLengthNotMatching() {
    assertThrows(
        StoreException.class,
        () -> {
          storageAdaptor.store(
              10L,
              MediaType.APPLICATION_XML_VALUE,
              new ByteArrayInputStream(ASDF.getBytes()),
              KEY,
              Map.of());
        });
  }

  @NotNull
  private static Matcher<InputStream> hasContents(final String expectedContents) {
    return new TypeSafeMatcher<InputStream>() {
      @Override
      protected boolean matchesSafely(InputStream actual) {
        try {
          return IOUtils.contentEquals(
              new ByteArrayInputStream(expectedContents.getBytes()), actual);
        } catch (final IOException e) {
          fail("Unable to compare input streams", e);
          return false;
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("contents is " + expectedContents);
      }
    };
  }
}