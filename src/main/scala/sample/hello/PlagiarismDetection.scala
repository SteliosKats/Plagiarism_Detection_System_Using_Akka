package sample.hello

import java.io.File

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

/**
 * Created by root on 2/25/15.
 */
object PlagiarismDetection {
  def main(args: Array[String]): Unit = {
    val source_filepath =new File(".").getAbsolutePath().dropRight(1)+"source_files/"
    val plag_filepath =new File(".").getAbsolutePath().dropRight(1)+"suspicious_files/"
    val plag_dir :File= new File(plag_filepath)
    val source_dir :File= new File(source_filepath)
    var ext_counter :Int= 0
    var source_total :Int =0
    val configFile = getClass.getClassLoader.getResource("remote_application.conf").getFile
    val config = ConfigFactory.parseFile(new File(configFile))
    val Wrapper_Sys =ActorSystem("XmlWrappingSystem",config)
    val xls_creation=Wrapper_Sys.actorOf(Props[XlsFileWrapper], "xls_wrapper")
    println("Executing citation-based algorithms...")
    for(file <- source_dir.listFiles if(file.getName.endsWith(".txt") )){
      //println(file)
      FileIndexer.ReadFiles(file.getName(),source_dir,plag_dir)
     source_total+=1
    }
    println("Performing Lexical Analysis please wait (this may take a while depending on the size of the data)...")
    LexicalAnalysis.ReadFiles2(source_dir,plag_dir)

  }

}
