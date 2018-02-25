import tensorflow as tf
from tensorflow.python.framework import graph_util

saver = tf.train.import_meta_graph('./model/model.meta', clear_devices=True)
graph = tf.get_default_graph()
input_graph_def = graph.as_graph_def()
sess = tf.Session()
saver.restore(sess, "./model/model")

output_node_names="prediction"
output_graph_def = graph_util.convert_variables_to_constants(sess,
            input_graph_def,
            output_node_names.split(","))

output_graph="./model/frozen_model.pb"
with tf.gfile.GFile(output_graph, "wb") as f:
    f.write(output_graph_def.SerializeToString())

sess.close()
'''
input_graph_def = tf.GraphDef()
with tf.gfile.Open(output_graph, "r") as f:
    data = f.read()
    input_graph_def.ParseFromString(data)

output_graph_def = optimize_for_inference_lib.optimize_for_inference(
        input_graph_def,
        ["images"],
        ["prediction"],
        tf.float32.as_datatype_enum)

f = tf.gfile.FastGFile(output_graph, "w")
f.write(output_graph_def.SerializeToString())
'''
