/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dws.berlin;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import software.amazon.codeguruprofilerjavaagent.Profiler;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.endpoint.StreamingEndpoint;

import de.dws.berlin.functions.SentenceDetectorFunction;
import de.dws.berlin.functions.SockeyeTranslateFunction;
import de.dws.berlin.twitter.TweetJsonConverter;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.java.io.CsvOutputFormat;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.twitter.TwitterSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerModel;


/**
 * Skeleton for a Flink Streaming Job.
 *
 * <p>For a tutorial how to write a Flink streaming application, check the
 * tutorials and examples on the <a href="http://flink.apache.org/docs/stable/">Flink Website</a>.
 *
 * <p>To package your appliation into a JAR file for execution, run
 * 'mvn clean package' on the command line.
 *
 * <p>If you change the name of the main class (with the public static void main(String[] args))
 * method, change the respective entry in the POM.xml file (simply search for 'mainClass').
 *
 * @author Suneel Marthi
 */
public class StreamingNmt {

  private static final Logger LOG = LoggerFactory.getLogger(StreamingNmt.class);

  private static SentenceModel engSentenceModel, deSentenceModel;
  private static TokenizerModel deTokenizerModel;

  private static void initializeModels() throws IOException {
    engSentenceModel = new SentenceModel(StreamingNmt.class.getResource("/opennlp-models/en-sent.bin"));
    deSentenceModel = new SentenceModel(StreamingNmt.class.getResource("/opennlp-models/de-sent.bin"));
    deTokenizerModel = new TokenizerModel((StreamingNmt.class.getResource("/opennlp-models/de-token.bin")));
  }

  public static void main(String[] args) throws Exception {

    initializeModels();
    String outputPath;

    ParameterTool parameterTool = ParameterTool.fromArgs(args);

    // get output path
    if (parameterTool.has("output")) {
      // read the text file from given input path
      outputPath = parameterTool.get("output");
    } else {
      System.out.println("Please provide an output path");
      outputPath = "/Users/marthism/NmtOutput";
    }

    final StreamExecutionEnvironment env =
        StreamExecutionEnvironment.getExecutionEnvironment();

    env.setStreamTimeCharacteristic(TimeCharacteristic.IngestionTime);
    env.setParallelism(3);

    // twitter credentials and source
    Properties props = new Properties();
    props.load(StreamingNmt.class.getResourceAsStream("/twitter.properties"));
    TwitterSource twitterSource = new TwitterSource(props);

    Set<String> langList = Stream.of(props.getProperty("twitter-source.langs")).collect(Collectors.toSet());

    // Create a DataStream from TwitterSource filtering out deleted tweets
    // and then filter out all non-deu tweets
    DataStream<Tweet> twitterStream = env.addSource(twitterSource)
        .filter((FilterFunction<String>) value -> value.contains("created_at"))
        .flatMap(new TweetJsonConverter())
        .filter((FilterFunction<Tweet>) tweet -> langList.contains(tweet.getLanguage()) &&
        tweet.getText().length() > 100);

    DataStream<Tuple2<String, String>> sentenceStream =
        twitterStream.map(new SentenceDetectorFunction(deSentenceModel))
        .keyBy(0)
        .countWindowAll(3)
        .apply(new SockeyeTranslateFunction());

    sentenceStream.writeAsCsv(outputPath, FileSystem.WriteMode.OVERWRITE, CsvOutputFormat.DEFAULT_LINE_DELIMITER, "\n").setParallelism(1);

    // execute program
    env.execute("Executing Streaming Machine Translation");
  }

  private static class FilterEndpoint implements TwitterSource.EndpointInitializer, Serializable {
    private final List<String> tags;

    FilterEndpoint(final String... tags) {
      this.tags = Stream.of(tags).collect(Collectors.toList());
    }

    @Override
    public StreamingEndpoint createEndpoint() {
      StatusesFilterEndpoint ep = new StatusesFilterEndpoint();
      ep.trackTerms(tags);
      return ep;
    }
  }
}
