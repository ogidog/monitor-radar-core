

spark-submit \
  --class edu.myapp.radar.processing.TOPSSplit \
  --master yarn \
  --deploy-mode client \
  --conf spark.executor.instances=4 \
  --conf spark.task.cpus=1 \
  --conf spark.executor.cores=8 \
  --conf spark.executor.memory="16g" \
  --conf spark.driver.memory="8g" \
  --conf spark.driver.extraClassPath="/mnt/hdfs/user/jars/spark_api/*: ...",
  --conf spark.executor.extraClassPath="/mnt/hdfs/user/jars/spark_api/*:/..." \
  ... # другие опции
  radar.processing.jar \
  .. # application-arguments

