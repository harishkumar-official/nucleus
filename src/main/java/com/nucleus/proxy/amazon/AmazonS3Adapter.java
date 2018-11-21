package com.nucleus.proxy.amazon;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.nucleus.exception.NucleusException;

/**
 * Adapter for Amazon S3 proxy.
 */
@Component
public class AmazonS3Adapter implements IAmazonS3Adapter {

  IAmazonS3Proxy amazonS3Proxy;

  @Autowired
  public AmazonS3Adapter(IAmazonS3Proxy amazonS3Proxy) {
    this.amazonS3Proxy = amazonS3Proxy;
  }

  @Override
  public String uploadFileTos3bucket(MultipartFile multipartFile) {
    String fileUrl = null;
    try {
      File file = convertMultiPartToFile(multipartFile);
      String fileName = generateFileName(multipartFile);
      fileUrl = amazonS3Proxy.uploadFileTos3bucket(fileName, file);
      file.delete();
    } catch (Exception e) {
      throw new NucleusException("Error, Please try later.", e);
    }
    return fileUrl;
  }

  private File convertMultiPartToFile(MultipartFile file) throws IOException {
    File convFile = new File(file.getOriginalFilename());
    FileOutputStream fos = new FileOutputStream(convFile);
    fos.write(file.getBytes());
    fos.close();
    return convFile;
  }

  private String generateFileName(MultipartFile multiPart) {
    return new Date().getTime() + "-" + multiPart.getOriginalFilename().replace(" ", "_");
  }
}
