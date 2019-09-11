package com.pharbers.max.submit

import java.io.{File, InputStream, PrintWriter}
import java.time.Duration
import java.util

import com.pharbers.ipaas.data.driver.api.factory.{PhFactoryTrait, getMethodMirror}
import com.pharbers.ipaas.data.driver.api.model.Job
import com.pharbers.ipaas.data.driver.api.work.{PhJobTrait, PhLogDriverArgs, PhMapArgs, PhSparkDriverArgs}
import com.pharbers.ipaas.data.driver.libs.input.{JsonInput, YamlInput}
import com.pharbers.ipaas.data.driver.libs.kafka.ProducerAvroTopic
import com.pharbers.ipaas.data.driver.libs.log.{PhLogDriver, formatMsg}
import com.pharbers.ipaas.data.driver.libs.read.{HDFSRead, LocalRead}
import com.pharbers.ipaas.data.driver.libs.spark.PhSparkDriver
import com.pharbers.kafka.producer.PharbersKafkaProducer
import com.pharbers.kafka.schema.ListeningJobTask
import com.pharbers.util.log.PhLogable
import com.pharbers.util.log.PhLogable._
import org.apache.commons.io.IOUtils
import org.yaml.snakeyaml.Yaml

import scala.collection.JavaConverters._


object Main extends PhLogable {
    def main(args: Array[String]): Unit = {
        if (args.length < 2) throw new Exception("args length is not equal to 3")
        useJobId("user", "trace", args(1)) {
            _ => run(args)
        }
    }

    def run(args: Array[String]): Unit = {
        // 第一波对接成功后整理Kafka的库封装
        val pkp = new PharbersKafkaProducer[String, ListeningJobTask]
        val shellValues = try {
            new CommandLineValues(args: _*)
        } catch {
            case e: Exception =>
                logger.error("参数错误")
                logger.error(e)
                pkp.produce(Config.topic, "", new ListeningJobTask("", "Error", "参数错误" + e.getMessage, "-1"))
                throw e
        }
        val config = shellValues.getConfig
        val jobModelPath = shellValues.getPath
        val jobId = shellValues.getJobId
        try {
            val readStream = HDFSRead(jobModelPath).toInputStream()
            val jobArgs = Map(
                "topic" -> Config.topic,
                "jobId" -> jobId,
                "cleanCpa" -> s"hdfs:///workData/Clean/cpa-$jobId",
                "cleanGycx" -> s"hdfs:///workData/Clean/gycx-$jobId",
                "panel" -> s"hdfs:///workData/Panel/$jobId",
                "max" -> s"hdfs:///workData/Max/$jobId"
            ) ++ config
            val jobs = YamlInput().readObjects[Job](LocalRead(buildYaml(readStream, jobArgs, "temp")).toInputStream())

            implicit val sd: PhSparkDriver = PhSparkDriver("job-context")

            pkp.produce(Config.topic, jobId, new ListeningJobTask(jobId, "Running", "", "0"))

            sd.sc.setLogLevel("ERROR")
            val ctx = PhMapArgs(Map(
                "sparkDriver" -> PhSparkDriverArgs(sd)
            ))

            val phJobs = jobs.map(x => {
                x.jobId = jobId
                getMethodMirror(x.getFactory)(x, ctx).asInstanceOf[PhFactoryTrait[PhJobTrait]].inst()
            })
            phJobs.head.perform(PhMapArgs(Map()))
            logger.info("Finish")

            pkp.produce(Config.topic, jobId, new ListeningJobTask(jobId, "Finish", "Finish", "100"))
        } catch {
            case e: Exception =>
                logger.error(e)
                pkp.produce(Config.topic, jobId, new ListeningJobTask(jobId, "Error", e.getMessage, "-1"))
        } finally {
            pkp.producer.close(Duration.ofSeconds(10))
        }
        logger.info("执行结束")
    }


    def buildYaml(templateInputStream: InputStream, argsMap: Map[String, String], name: String): String = {

        import scala.collection.JavaConversions._

        val data: java.util.Map[String, java.util.Map[String, String]] = new util.HashMap()
        data.put("args", argsMap.map(x => (x._1, "&" + x._1 + " " + x._2)))
        val headList = new Yaml().dumpAsMap(data).split("\n")
        val head = (headList.head +: headList.tail.map(x => {
            val res = x.replaceFirst("'", "")
            res.substring(0, res.length - 1)
        })).mkString("\n").replace("''", "'")
        val file = new File("./" + name + ".yaml")
        file.deleteOnExit()
        file.createNewFile()
        val writer = new PrintWriter(file)
        writer.println(head)
        val s = IOUtils.toString(templateInputStream)
        writer.println(s)
        writer.close()
        file.getPath
    }
}
