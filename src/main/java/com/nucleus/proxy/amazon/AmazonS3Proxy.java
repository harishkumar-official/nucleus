package com.nucleus.proxy.amazon;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.File;

/** Proxy to connect with Amazon S3 service. */
public class AmazonS3Proxy implements IAmazonS3Proxy {

  private final String fileLinkBucketUrl;
  private final String bucketName;

  private AmazonS3 client;

  public AmazonS3Proxy(AmazonS3 client, String baseUrl, String bucketName) {
    this.client = client;
    this.fileLinkBucketUrl = baseUrl + "/";
    this.bucketName = bucketName;
  }

  @Override
  public String uploadFileTos3bucket(String fileName, File file) {
    client.putObject(
        new PutObjectRequest(bucketName, fileName, file)
            .withCannedAcl(CannedAccessControlList.PublicRead));
    return fileLinkBucketUrl + fileName;
  }
}
