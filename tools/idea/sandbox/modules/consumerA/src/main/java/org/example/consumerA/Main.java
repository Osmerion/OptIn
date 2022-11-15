package org.example.consumerA;

import com.osmerion.optin.OptIn;
import org.example.producer1.ExperimentalProducerOneApi;
import org.example.producer1.ExperimentalType;
import org.example.producer1.ProducerOne;

public class Main {

    public static void main(String[] args) {
        ProducerOne producer = new ProducerOne();

        producer.stableSomething();
    }

    public void foo(@OptIn(ExperimentalProducerOneApi.class) ExperimentalType type) {

    }

}