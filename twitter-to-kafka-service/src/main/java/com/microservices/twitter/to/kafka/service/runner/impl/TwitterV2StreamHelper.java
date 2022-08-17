package com.microservices.twitter.to.kafka.service.runner.impl;

import com.microservices.twitter.to.kafka.service.config.TwitterToKafkaServiceConfigData;
import com.microservices.twitter.to.kafka.service.constants.TwitterHttpConstants;
import com.microservices.twitter.to.kafka.service.listener.TwitterKafkaStatusListener;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URISyntaxException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;


@Component
// @ConditionalOnProperty(name = "twitter-to-kafka-service.enable-v2-tweets", havingValue = "true", matchIfMissing = true)
@ConditionalOnExpression("${twitter-to-kafka-service.enable-v2-tweets} && not ${twitter-to-kafka-service.enable-mock-tweets}")
public class TwitterV2StreamHelper {
    private static final Logger LOG = LoggerFactory.getLogger(TwitterV2StreamHelper.class);
    private final TwitterToKafkaServiceConfigData twitterToKafkaServiceConfigData;
    private final TwitterKafkaStatusListener twitterKafkaStatusListener;

    public TwitterV2StreamHelper(TwitterToKafkaServiceConfigData twitterToKafkaServiceConfigData,
                                 TwitterKafkaStatusListener twitterKafkaStatusListener) {
        this.twitterToKafkaServiceConfigData = twitterToKafkaServiceConfigData;
        this.twitterKafkaStatusListener = twitterKafkaStatusListener;
    }

    /*
     * This method calls the filtered stream endpoint and streams Tweets from it
     * */
    void connectStream(String bearerToken) throws IOException, URISyntaxException {

        HttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD).build())
                .build();

        URIBuilder uriBuilder = new URIBuilder(twitterToKafkaServiceConfigData.getTwitterV2BaseUrl());

        HttpGet httpGet = new HttpGet(uriBuilder.build());
        httpGet.setHeader(TwitterHttpConstants.AUTHORIZATION, String.format("Bearer %s", bearerToken));

        HttpResponse response = httpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        if (null != entity) {
            BufferedReader reader = new BufferedReader(new InputStreamReader((entity.getContent())));
            String line = reader.readLine();
            while (line != null) {
                line = reader.readLine();
                if (!line.isEmpty()) {
                    String tweet = getFormattedTweet(line);
                    LOG.info("tweet {}", tweet);
                    Status status = null;
                    try {
                        status = TwitterObjectFactory.createStatus(tweet);
                    } catch (TwitterException e) {
                        LOG.error("Could not create status for text: {}", tweet, e);
                    }
                    if (status != null) {
                        twitterKafkaStatusListener.onStatus(status);
                    }
                }
            }
        }

    }

    /*
     * Helper method to setup rules before streaming data
     * */
    void setupRules(String bearerToken, Map<String, String> rules) throws IOException, URISyntaxException {
        List<String> existingRules = getRules(bearerToken);
        if (existingRules.size() > 0) {
            deleteRules(bearerToken, existingRules);
        }
        createRules(bearerToken, rules);
        LOG.info("Created rule for twitter stream {}", rules.keySet().toArray());
    }

    /*
     * Helper method to create rules for filtering
     * */
    void createRules(String bearerToken, Map<String, String> rules) throws URISyntaxException, IOException {
        HttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD).build())
                .build();

        URIBuilder uriBuilder = new URIBuilder(twitterToKafkaServiceConfigData.getTwitterV2RulesBaseUrl());

        HttpPost httpPost = new HttpPost(uriBuilder.build());
        httpPost.setHeader(TwitterHttpConstants.AUTHORIZATION, String.format("Bearer %s", bearerToken));
        httpPost.setHeader(TwitterHttpConstants.CONTENT_TYPE, TwitterHttpConstants.APPLICATION_JSON);
        StringEntity body = new StringEntity(getFormattedString("{\"add\": [%s]}", rules));
        httpPost.setEntity(body);
        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity entity = response.getEntity();
        if (Objects.nonNull(entity)) {
            LOG.info(EntityUtils.toString(entity, TwitterHttpConstants.UTF_8));
        }
    }

    /*
     * Helper method to get existing rules
     * */
    List<String> getRules(String bearerToken) throws URISyntaxException, IOException {
        List<String> rules = new ArrayList<>();
        HttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD).build())
                .build();

        URIBuilder uriBuilder = new URIBuilder(twitterToKafkaServiceConfigData.getTwitterV2RulesBaseUrl());

        HttpGet httpGet = new HttpGet(uriBuilder.build());
        httpGet.setHeader(TwitterHttpConstants.AUTHORIZATION, String.format("Bearer %s", bearerToken));
        httpGet.setHeader(TwitterHttpConstants.CONTENT_TYPE, TwitterHttpConstants.APPLICATION_JSON);
        HttpResponse response = httpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        if (Objects.nonNull(entity)) {
            JSONObject json = new JSONObject(EntityUtils.toString(entity, TwitterHttpConstants.UTF_8));
            if (json.length() > 1) {
                JSONArray array = (JSONArray) json.get("data");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject jsonObject = (JSONObject) array.get(i);
                    rules.add(jsonObject.getString("id"));
                }
            }
        }
        return rules;
    }

    /*
     * Helper method to delete rules
     * */
    void deleteRules(String bearerToken, List<String> existingRules) throws URISyntaxException, IOException {
        HttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD).build())
                .build();

        URIBuilder uriBuilder = new URIBuilder(twitterToKafkaServiceConfigData.getTwitterV2RulesBaseUrl());

        HttpPost httpPost = new HttpPost(uriBuilder.build());
        httpPost.setHeader(TwitterHttpConstants.AUTHORIZATION, String.format("Bearer %s", bearerToken));
        httpPost.setHeader(TwitterHttpConstants.CONTENT_TYPE, TwitterHttpConstants.APPLICATION_JSON);
        StringEntity body = new StringEntity(getFormattedString("{ \"delete\": { \"ids\": [%s]}}", existingRules));
        httpPost.setEntity(body);
        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity entity = response.getEntity();
        if (Objects.nonNull(entity)) {
            LOG.info(EntityUtils.toString(entity, TwitterHttpConstants.UTF_8));
        }
    }

    String getFormattedTweet(String data) {
        JSONObject jsonData = (JSONObject) new JSONObject(data).get("data");
        String[] params = new String[]{
                ZonedDateTime.parse(jsonData.get("created_at").toString()).withZoneSameInstant(ZoneId.of("UTC"))
                        .format(DateTimeFormatter.ofPattern(TwitterHttpConstants.TWITTER_STATUS_DATE_FORMAT, Locale.ENGLISH)),
                jsonData.get("id").toString(),
                jsonData.get("text").toString().replaceAll("\"", "\\\\\""),
                jsonData.get("author_id").toString()
        };
        return formatTweetAsJsonWithParams(params);
    }

    private String formatTweetAsJsonWithParams(String[] params) {
        String tweet = TwitterHttpConstants.tweetAsRowJson;
        for (int i = 0; i < params.length; i++) {
            tweet = tweet.replace("{" + i + "}", params[i]);
        }
        return tweet;
    }

    String getFormattedString(String string, List<String> ids) {
        StringBuilder sb = new StringBuilder();
        if (ids.size() == 1) {
            return String.format(string, "\"" + ids.get(0) + "\"");
        } else {
            for (String id : ids) {
                sb.append("\"" + id + "\"" + ",");
            }
            String result = sb.toString();
            return String.format(string, result.substring(0, result.length() - 1));
        }
    }

    private static String getFormattedString(String string, Map<String, String> rules) {
        StringBuilder sb = new StringBuilder();
        if (rules.size() == 1) {
            String key = rules.keySet().iterator().next();
            return String.format(string, "{\"value\": \"" + key + "\", \"tag\": \"" + rules.get(key) + "\"}");
        } else {
            for (Map.Entry<String, String> entry : rules.entrySet()) {
                String value = entry.getKey();
                String tag = entry.getValue();
                sb.append("{\"value\": \"" + value + "\", \"tag\": \"" + tag + "\"}" + ",");
            }
            String result = sb.toString();
            return String.format(string, result.substring(0, result.length() - 1));
        }
    }

}
