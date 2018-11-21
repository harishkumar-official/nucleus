package com.nucleus.proxy.amazon;

import org.springframework.web.multipart.MultipartFile;

/**
 * Adapter for Amazon S3 proxy.
 */
public interface IAmazonS3Adapter {

  public String uploadFileTos3bucket(MultipartFile multipartFile);

}
