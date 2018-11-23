package com.nucleus.config;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.nucleus.proxy.amazon.AmazonS3Proxy;

/**
 * Class to facilitate creating AOP proxies programmatically.
 */
@Configuration
public class ProxiesConfig {

  @Autowired
  private Environment env;

  @Bean(name = "amazonS3Proxy")
  public ProxyFactoryBean amazonS3Proxy() {
    String bucketName = env.getProperty("amazons3.proxy.bucketName", String.class);
    String baseUrl = env.getProperty("amazons3.proxy.url", String.class);
    String region = env.getProperty("amazons3.proxy.region", String.class);

    AmazonS3ClientBuilder clientBuilder = AmazonS3ClientBuilder.standard();
    clientBuilder.setRegion(region);
    AWSCredentialsProvider credentialProvider = new ProfileCredentialsProvider();
    clientBuilder.setCredentials(credentialProvider);

    AmazonS3 client = clientBuilder.build();
    AmazonS3Proxy proxy = new AmazonS3Proxy(client, baseUrl, bucketName);

    return createProxyFactoryBean(proxy);
  }

  private ProxyFactoryBean createProxyFactoryBean(Object target, String... interceptorNames) {
    ProxyFactoryBean aopProxy = new ProxyFactoryBean();
    aopProxy.setTarget(target);
    aopProxy.setInterceptorNames(interceptorNames);
    return aopProxy;
  }
}
