import sys,os
import tensorflow as tf
if __name__ == '__main__':
    #main(parse_arguments(sys.argv[1:]))
    if len(sys.argv)>=2:
        input_model_file=sys.argv[1]
        output_model_file=os.path.splitext(input_model_file)[0]+'.tflite'
        print(output_model_file)
        converter = tf.compat.v1.lite.TFLiteConverter.from_frozen_graph(
            graph_def_file = input_model_file, 
            input_arrays = ['input_1'],
            input_shapes={'input_1':[1,224,224,3]},
            #input_shapes={'input_1':[1,300,300,3]},
            #output_arrays = ['dense_1/Softmax','event_fc/BiasAdd'] #for places_event_mobilenet2_alpha=1.0_augm_ft_sgd_model, 
            output_arrays = ['dense_1/Softmax'] #for mobilenet_v2_1.0_pruning_grad_25percent_model_ft
        )
        #converter.optimizations = [tf.lite.Optimize.DEFAULT]
        tflite_model = converter.convert()

        with tf.io.gfile.GFile(output_model_file, 'wb') as f:
          f.write(tflite_model)