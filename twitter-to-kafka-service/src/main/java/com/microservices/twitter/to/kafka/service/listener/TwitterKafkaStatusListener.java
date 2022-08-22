package com.microservices.twitter.to.kafka.service.listener;

import com.microservices.twitter.kafka.avro.model.TwitterAvroModel;
import com.microservices.twitter.kafka.producer.config.service.KafkaProducer;
import com.microservices.twitter.to.kafka.service.config.KafkaConfigData;
import com.microservices.twitter.to.kafka.service.transformer.TwitterStatusToAvroModelFromStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import twitter4j.Status;
import twitter4j.StatusAdapter;

@Component
public class TwitterKafkaStatusListener extends StatusAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(TwitterKafkaStatusListener.class);

    private final KafkaConfigData kafkaConfigData;
    private final KafkaProducer<Long, TwitterAvroModel> kafkaProducer;
    private final TwitterStatusToAvroModelFromStatus twitterStatusToAvroModelFromStatus;

    public TwitterKafkaStatusListener(KafkaConfigData configData,
                                      KafkaProducer<Long, TwitterAvroModel> producer,
                                      TwitterStatusToAvroModelFromStatus converterStatusToAvroModelFromStatus) {
        this.kafkaConfigData = configData;
        this.kafkaProducer = producer;
        this.twitterStatusToAvroModelFromStatus = converterStatusToAvroModelFromStatus;
    }

    @Override
    public void onStatus(Status status) {
        LOG.info("Twitter status with text {}", status.getText());
        TwitterAvroModel twitterAvroModel = twitterStatusToAvroModelFromStatus.getTwitterAvroModelFromStatus(status);
        kafkaProducer.send(kafkaConfigData.getTopicName(), twitterAvroModel.getUserId(), twitterAvroModel);
    }
}
