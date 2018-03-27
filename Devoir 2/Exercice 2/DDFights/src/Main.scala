
import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkConf, SparkContext}

object Main extends App {

    println("Initialize Spark ...")

    System.setProperty("hadoop.home.dir", "C:/hadoop")
    val conf = new SparkConf().setAppName("ScalaRDD").setMaster("local[*]")
    Logger.getRootLogger.setLevel(Level.WARN)
    val sc = new SparkContext(conf)

    println("Spark initialized")
}
