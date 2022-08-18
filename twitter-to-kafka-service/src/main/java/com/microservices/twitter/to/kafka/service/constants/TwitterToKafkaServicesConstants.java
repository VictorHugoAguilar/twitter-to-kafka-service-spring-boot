package com.microservices.twitter.to.kafka.service.constants;

public class TwitterToKafkaServicesConstants {

    public static final String AUTHORIZATION = "Authorization";
    public static final String CONTENT_TYPE = "content-type";
    public static final String APPLICATION_JSON = "application/json";
    public static final String UTF_8 = "UTF-8";
    public static final String TWITTER_STATUS_DATE_FORMAT = "EEE MMM dd HH:mm:ss zzz yyyy";
    public static final String tweetAsRowJson = "{" +
            "\"created_at\":\"{0}\"," +
            "\"id\":\"{1}\"," +
            "\"text\":\"{2}\"," +
            "\"user\": { \"id\":\"{3}\"}" +
            "}";

}
