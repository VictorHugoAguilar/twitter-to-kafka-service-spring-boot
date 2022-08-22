package com.microservices.twitter.to.kafka.service.transformer;

import com.microservices.twitter.kafka.avro.model.TwitterAvroModel;
import org.springframework.stereotype.Component;
import twitter4j.Status;

@Component
public class TwitterStatusToAvroModelFromStatus {

    public TwitterAvroModel getTwitterAvroModelFromStatus(Status status){
        return TwitterAvroModel.
                newBuilder()
                .setId(status.getId())
                .setUserId(status.getUser().getId())
                .setText(status.getText())
                .setCreatedAt(status.getCreatedAt().getTime())
                .build();
    }
}
