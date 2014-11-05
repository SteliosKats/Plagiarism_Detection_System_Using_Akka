package sample.hello

import java.io.File
import akka.actor._
import akka.routing._
import edu.arizona.sista.processors.corenlp.CoreNLPProcessor
import scala.io.Source

/**
 * Created by root on 11/2/14.
 */
case class file_properties2(filename :File,fileid :Int, total_files: Int)
case class routingmessages2(fileline :String, counter :Int,ref_act :ActorRef,fileid :Int)
case class source_props(filename :File,fileid :Int)

object  LexicalAnalysis {
  def main(args: Array[String]): Unit = {
    var path_filename=new File(" ")
    var fileid=1
    var tot_files=1
    val current_directory=new File("/root/Desktop/")
    val indexingSystem= ActorSystem("CitationExtractionSystem2")//,ConfigFactory.load(application_is_remote))
    val plag_file_analysis = indexingSystem.actorOf(Props[PlagFileAnalysis],"plag_analysis")
    val source_analysis=indexingSystem.actorOf(Props[SourceFileAnalysis],"source_Analysis")
    var filenames_ids :Map[String,Int]=Map()
    var source_str=readLine("Enter The Source File's Name To Be Checked for Citation-based Plagiarism Detection:")
    while(!new File(current_directory+"/"+source_str).exists()){
      source_str=readLine("File Not found!Try Again with different file or check your spelling:")
    }
    val source_file=new File(current_directory+"/"+source_str)
    filenames_ids=filenames_ids.+(source_str ->1)

    for(file <- current_directory.listFiles if(file.getName.endsWith(".txt") && file.getName()!=source_str )){
      tot_files+=1
    }

    source_analysis ! file_properties2(source_file,1,tot_files)

    for(file <- current_directory.listFiles if(file.getName.endsWith(".txt") && file.getName()!=source_str) ){
      path_filename=new File(file.toString())
      //println(file)
      fileid+=1
      filenames_ids=filenames_ids.+(file.getName() ->fileid)
      plag_file_analysis ! file_properties2(path_filename,fileid,tot_files)
    }
  }
}

class PlagFileAnalysis extends Actor {

  def receive = {

    case file_properties2(source_file,fileid,tot_files) =>
      println("Nothing")


  }

}

class SourceFileAnalysis extends Actor {
  var counter_terminated=0
  var all_refs :Map[String,Int]=Map()
  var file_counter=0
  /* Creating A router to route "Workers" to extract citations for each line of the file */
  var router ={
    val routees=Vector.fill(5){
      val lineseparator=context.actorOf(Props[LineSeparatorSource])
      context watch lineseparator
      ActorRefRoutee(lineseparator)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }
  /* Creating A router to route "Workers" to ectract citations for each line of the file */

  def receive ={
    case file_properties2(source_file,fileid,tot_files) =>
      var counter = 0
      for (line <- Source.fromFile(source_file).getLines()) {
        counter += 1
        router.route(routingmessages2(line, counter, self, fileid), sender())
      }


  }


}

class LineSeparatorSource extends Actor with ActorLogging{

  def receive ={

    case routingmessages2(line,counter,file_receiver_ref,fileid) =>
      // create the processor
      val proc:CoreNLPProcessor = new CoreNLPProcessor(withDiscourse = true)
      val doc = proc.annotate("In computer science and electrical engineering, Lloyd's algorithm")
      println("Sentence line: "+line)//+sentence.tags)
      // print the sentence-level annotations
      var sentenceCount = 0
      for (sentence <- doc.sentences) {
         println("Tokens: "+ sentence.words.mkString(" "))//+sentence.tags)

      }

  }


}
