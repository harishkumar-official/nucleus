package com.nucleus.proxy.amazon;

import java.io.File;

/**
 * Interface for proxy classes that facilitate calls to AmazonS3Proxy.
 */
public interface IAmazonS3Proxy {

  /**
   * Returns amazonS3 URL link of file.
   */
  public String uploadFileTos3bucket(String fileName, File file);

}
