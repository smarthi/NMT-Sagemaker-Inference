Streaming Machine Translation on Apache Flink pipelines leveraging Apache OpenNLP


The NMT model has been trained offline using the WMT corpus for German and English, uses Byte-Pair Encoding (BPE).
The model has been shoved into Sagemaker as a Flask app - honestly the performance is 
infinitely better if just deployed as an RPC server (gRPC), but... why not try something new.

Given that it takes atleast 6 mins to create a Sagemaker endpoint - its best to use 
Sagemaker only for model inference - for streaming model training - consider using Oryx2, PredictionIO
or Flink's Queryable State.

