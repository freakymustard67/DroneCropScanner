Place your TFLite model file `crop_disease_mobilenet.tflite` in this directory.

The CropAnalyzer loads models from this assets folder. The expected model input is 224x224 RGB images
with output shape [1, 2] (healthy vs disease confidence scores).
