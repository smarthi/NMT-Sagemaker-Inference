package de.dws.berlin.functions;

import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.amazonaws.services.sagemakerruntime.AmazonSageMakerRuntime;
import com.amazonaws.services.sagemakerruntime.model.InvokeEndpointRequest;
import com.google.gson.Gson;

import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import de.dws.berlin.util.AwsUtil;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.windowing.RichAllWindowFunction;

import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;
import org.apache.flink.util.Collector;

public class SockeyeTranslateFunction extends RichAllWindowFunction<Tuple2<String, String[]>, Tuple2<String,String>, GlobalWindow> {

  // for the record - the only Sagemaker Java api that indeed fucking works and has seen some usage per Stackoverflow
  private AmazonSageMakerRuntime amazonSageMakerRuntime;
  private InvokeEndpointRequest invokeEndpointRequest;

  @Override
  public void open(Configuration parameters) throws Exception {
    // AWS specific initializations
    Properties props = new Properties();
    props.load(SockeyeTranslateFunction.class.getResourceAsStream("/aws.properties"));
    String serviceEndpoint = props.getProperty("aws.sagemaker.endpoint");
    String regionName = props.getProperty("aws.region");
    amazonSageMakerRuntime = AwsUtil.getSageMakerClient(regionName);
    invokeEndpointRequest = AwsUtil.getInvokeEndpointRequest(serviceEndpoint);
  }

  @Override
  public void apply(GlobalWindow window, Iterable<Tuple2<String, String[]>> iterable,
                    Collector<Tuple2<String, String>> collector) throws Exception {

    List<String> sentencesList = new ArrayList<>();

    for (Tuple2<String,String[]> sentences : iterable) {
      sentencesList.addAll(Arrays.asList(sentences.f1));
    }

    // Sort the input to avoid creating "Jagged Tensors" on GPUs/CPUs for inference
    Collections.sort(sentencesList);
    Gson gson = new Gson();

    for (String sentence : sentencesList) {
      // Gotta do this - because the Python side of Sagemaker is 'flasked' and using flask-json which is essentially a Python dict()
      Map<String, String> jsonMap = new HashMap<>();
      jsonMap.put("data", sentence.trim());
      String json = gson.toJson(jsonMap);

      // This is standard Sagemaker client request
      ByteBuffer byteBuffer = ByteBuffer.wrap(json.getBytes());
      invokeEndpointRequest.setBody(byteBuffer);
      String translatedJson = new String((amazonSageMakerRuntime.invokeEndpoint(invokeEndpointRequest).getBody()).array());

      if (translatedJson.length() > 0) {
        System.out.println(sentence + "\n" + translatedJson + "\n");
        collector.collect(new Tuple2<>(sentence, translatedJson.substring(translatedJson.indexOf(':') + 2)));
      } else {
        collector.collect(new Tuple2<>(sentence, translatedJson));
      }

    }
  }
}