

spark-submit \
  --class edu.myapp.radar.processing.TOPSSplit \
  --master yarn \
  --deploy-mode cluster \
  --conf spark.executor.instances=4 \
  --conf spark.task.cpus=1 \
  --conf spark.executor.cores=8 \
  --conf spark.executor.memory="16g" \
  --conf spark.driver.memory="8g" \
  --conf spark.driver.extraClassPath="/mnt/hdfs/user/jars/spark_api/*: ...",
  --conf spark.executor.extraClassPath="/mnt/hdfs/user/jars/spark_api/*:/..." \
  ... # другие опции
  "application-jar"  # например, radar.processing.jar
  "application-arguments" # например список путей к файлам снимков



