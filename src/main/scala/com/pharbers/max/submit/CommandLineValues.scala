package com.pharbers.max.submit

import java.util
import java.util.UUID

import scala.collection.JavaConverters._
import org.kohsuke.args4j.CmdLineParser
import org.kohsuke.args4j.Option

/** 功能描述
  *
  * @param args 构造参数
  * @tparam T 构造泛型参数
  * @author dcs
  * @version 0.0
  * @since 2019/09/11 11:18
  * @note 一些值得注意的地方
  */
class CommandLineValues {
    @Option(name = "-c", aliases = Array("--config"), required = true,
        usage = "config")
    private var config: java.util.Map[String, String] = new util.HashMap[String, String]()
    @Option(name = "-p", aliases = Array("--path"), required = true,
        usage = "yaml path")
    private var path: String = ""
    @Option(name = "-i", aliases = Array("--id"), required = true,
        usage = "job id")
    private var jobId: String = UUID.randomUUID().toString
    private var errorFree = false


    def this(args: String*){
        this()
        val parser = new CmdLineParser(this)
        parser.getProperties.withUsageWidth(80)
        parser.parseArgument(args: _*)
        errorFree = true
    }

    def isErrorFree: Boolean = errorFree

    def getConfig:Map[String, String] = config.asScala.toMap

    def getJobId: String = jobId

    def getPath: String = path
}
