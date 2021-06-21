package com.nucleus.proxy.amazon;

import com.nucleus.exception.NucleusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

/** Adapter for Amazon S3 proxy. */
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
    OutputStream os = Files.newOutputStream(Paths.get(file.getOriginalFilename()));
    os.write(file.getBytes());
    os.close();
    return convFile;
  }

  private String generateFileName(MultipartFile multiPart) {
    return new Date().getTime() + "-" + multiPart.getOriginalFilename().replace(" ", "_");
  }
}
