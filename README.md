Streaming Machine Translation on Apache Flink pipelines leveraging Apache OpenNLP

The NMT model has been trained offline using a parallel WMT corpus for German and English, 
uses Byte-Pair Encoding (BPE) to account for unseen vocabulary.
The model has been dockered into Sagemaker as a Flask app - honestly the performance is 
infinitely better if just deployed as an RPC server (gRPC), but... why not try something new.

Given the > 6 mins time to create a Sagemaker endpoint - its best to use 
Sagemaker only for model inference. For streaming model training - consider using Oryx2, PredictionIO
or Flink's Queryable State.



