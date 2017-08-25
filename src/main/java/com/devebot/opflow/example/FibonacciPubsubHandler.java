package com.devebot.opflow.example;

import com.devebot.opflow.OpflowEngine;
import com.devebot.opflow.OpflowHelper;
import com.devebot.opflow.OpflowMessage;
import com.devebot.opflow.OpflowPubsubHandler;
import com.devebot.opflow.OpflowPubsubListener;
import com.devebot.opflow.OpflowTask;
import com.devebot.opflow.exception.OpflowConstructorException;
import com.devebot.opflow.exception.OpflowOperationException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author drupalex
 */
public class FibonacciPubsubHandler {

    private final static Logger LOG = LoggerFactory.getLogger(FibonacciPubsubHandler.class);
    private final JsonParser jsonParser = new JsonParser();
    private final OpflowPubsubHandler handler;
    private final OpflowPubsubListener listener = new OpflowPubsubListener() {
        @Override
        public void processMessage(OpflowMessage message) throws IOException {
            String content = new String(message.getContent(), "UTF-8");
            JsonObject jsonObject = (JsonObject)jsonParser.parse(content);
            int number = Integer.parseInt(jsonObject.get("number").toString());

            if (countdown != null) countdown.check();
            if (number < 0) throw new OpflowOperationException("number should be positive");
            if (number > 40) throw new OpflowOperationException("number exceeding limit (40)");
            FibonacciGenerator fibonacci = new FibonacciGenerator(number);
            System.out.println(" [-] Received '" + content + "', result: " + fibonacci.finish().getValue());
        }
    };
    private OpflowTask.Countdown countdown;
    
    public FibonacciPubsubHandler() throws OpflowConstructorException {
        handler = OpflowHelper.createPubsubHandler();
    }
    
    public FibonacciPubsubHandler(String propFile) throws OpflowConstructorException {
        handler = OpflowHelper.createPubsubHandler(propFile);
    }
    
    public void publish(int number) {
        handler.publish("{ \"number\": " + number + " }");
    }
    
    public OpflowEngine.ConsumerInfo subscribe() {
        return this.subscribe(1)[0];
    }
    
    public OpflowEngine.ConsumerInfo[] subscribe(int consumerNumber) {
        if (consumerNumber > 0) {
            OpflowEngine.ConsumerInfo[] consumerInfos = new OpflowEngine.ConsumerInfo[consumerNumber];
            for(int i=0; i<consumerNumber; i++) {
                consumerInfos[i] = handler.subscribe(listener);
            }
            return consumerInfos;
        }
        return null;
    }
    
    public int getRedeliveredLimit() {
        return handler.getRedeliveredLimit();
    }
    
    public int countSubscriber() {
        return handler.getExecutor().countQueue(handler.getSubscriberName());
    }
    
    public int countRecyclebin() {
        return handler.getExecutor().countQueue(handler.getRecyclebinName());
    }
    
    public int getNumberOfConsumers() {
        return handler.getExecutor().defineQueue(handler.getSubscriberName()).getConsumerCount();
    }
    
    public String checkState() {
        return handler.check().getConnectionState() == 1 ? "opened" : "closed";
    }
    
    public void close() {
        handler.close();
    }
    
    public OpflowTask.Countdown getCountdown() {
        return countdown;
    }
    
    public void setCountdown(OpflowTask.Countdown countdown) {
        this.countdown = countdown;
    }

    public static void main(String[] argv) throws Exception {
        final FibonacciPubsubHandler pubsub = new FibonacciPubsubHandler();
        
        System.out.println("[+] Start a Subscriber");
        pubsub.subscribe();
        
        System.out.println("[-] publish 20 messages");
        for(int i=20; i<40; i++) {
            pubsub.publish(i);
        }
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException ie) {}
        
        pubsub.close();
        System.out.println("[-] Pub/sub has been done!");
    }
}
