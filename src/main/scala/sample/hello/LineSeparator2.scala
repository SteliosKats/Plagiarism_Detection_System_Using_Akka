package sample.hello

import java.io.File
import akka.actor._
import java.io._
import scala.io.Source
import edu.stanford.nlp.ling.CoreAnnotations.{TokenBeginAnnotation, LemmaAnnotation, TokensAnnotation, SentencesAnnotation}
import edu.stanford.nlp.ling.{CoreAnnotations, CoreLabel, IndexedWord}

import collection.mutable.{HashMap,MultiMap,Map}

/**
 * Created by root on 3/3/15.
 */
class LineSeparator2 extends Actor {
  val linediting=context.actorOf(Props[LineLemmaExtractor], name= "line_separate_plag")

  def receive = {

    case import_plag_file(path_filename,filepath_plag) =>

      val file_handler_plag :PrintWriter= new PrintWriter(new File(filepath_plag))
      val file_lines_size=Source.fromFile(path_filename).getLines().size
      //println(filepath_plag)
      for (line <- Source.fromFile(path_filename).getLines()) {
        //println(file_lines_size)
        linediting.!(routingmessages2(line ,self ,path_filename.getName() ,file_handler_plag ,file_lines_size))
        //router2.!(routingmessages2(line, line_leng,counter,line.length(), self ,path_filename.getName()))
      }
    case _ =>
      println("The line of the current file has not received")
  }
}
