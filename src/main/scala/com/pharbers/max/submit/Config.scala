package com.pharbers.max.submit

/** 功能描述
  *
  * @param args 构造参数
  * @tparam T 构造泛型参数
  * @author dcs
  * @version 0.0
  * @since 2019/09/06 10:37
  * @note 一些值得注意的地方
  */
object Config {
    val NAME = "MaxJob"
    val job_type = "MaxCal"
    val spark_master = "yarn"
    val Deploy_Mode = "cluster"
    val default_Executor_Memory = "2g"
    val default_Executor_Cores = "2"
    val default_Num_Executors = "1"
    val Queue = "default"
    val default_spark_Confs = List("spark.driver.extraClassPath=./__app__.jar","spark.yarn.appMasterEnv.PHA_CONF_HOME=")
    val default_files = List("kafka_config.xml", "kafka.broker1.truststore.jks", "kafka.broker1.keystore.jks")
    val default_Parameters = "yaml HDFS MZmax.yaml MaxJob"
    val default_App_Resource = "job-context.jar"
    val topic = "listeningJobTask"
}
