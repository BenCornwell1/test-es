package dev.appsody.starter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;

// 922252036173b26b316b86d7589305ae89ced98d

@Path("/vehicleSale")
@Produces({"application/json"})
public class VehicleSaleConsumer {

    private static final String APP_ID = "ben-es-test";
    private static final String TOPIC_NAME= "vehicle-sales";
	private static final String API_KEY = "obFgccPFlaczy3a0pa30xvUsZBXf8MmRRtpTbwE77jA5";
	private static final String BOOTSTRAP_SERVERS = "ben-es1-ibm-es-proxy-route-bootstrap-eventstreams.bentest2-403315c8b53cfaaf40d7fd4ee4d91267-0000.eu-gb.containers.appdomain.cloud:443";
	private static final String TRUST_STORE = "/project/user-app/src/main/resources/es-cert.jks";
    private static final String TRUST_PASSWORD = "password";
    private static final int POLL_DURATION = 10000;
    
    private Logger logger = Logger.getLogger(VehicleSaleConsumer.class.getName());

    private KafkaConsumer<String, String> consumer;

    public VehicleSaleConsumer() {

        consumer = createConsumer(BOOTSTRAP_SERVERS);
        TopicPartition topicPartition = new TopicPartition(TOPIC_NAME, 0); 
        consumer.assign(Arrays.asList(topicPartition));

        int count = 0;
        boolean exit = false;
        while (consumer.assignment().size() < 1 && exit == false) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // Do nothing
            }
            count++;
            if (count < 1000 ) {
                logger.warning("Topics assignment not fully complete");
                exit = true;
            }
        }
    }

    @GET
    public String getRequest(@QueryParam("partition") int partition, @QueryParam("offset") long offset) {

        String response;

        consumer.seek(new TopicPartition(TOPIC_NAME, partition), offset);
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(POLL_DURATION));
        if (records.iterator().hasNext()) {
            response = records.iterator().next().value();
        } else {
            response = "Timed out after " + POLL_DURATION + "ms";
        }
        return response;
    }

    private KafkaConsumer<String, String> createConsumer(String brokerList) {

        Properties properties = new Properties();
        properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, APP_ID);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(SslConfigs.SSL_PROTOCOL_CONFIG, "TLSv1.2");
        properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, TRUST_STORE);
        properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, TRUST_PASSWORD);
        properties.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        properties.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"token\" password=\"" + API_KEY + "\";");

        KafkaConsumer<String, String> kafkaConsumer = null;

        try {
            kafkaConsumer = new KafkaConsumer<>(properties);
        } catch (KafkaException kafkaError) {
            logger.log(Level.SEVERE, "Error creating kafka consumer.", kafkaError);
            throw kafkaError;
        }
        
        return kafkaConsumer;
    }

}
