package sample.hello

import java.io.File
import akka.actor._
import java.io._
import scala.io.Source

/**
 * Created by root on 3/3/15.
 */
class SourceFileAnalysis extends Actor {
  var external_counter :Int =0
  var source_file_name=new String()
  val lemaextr=context.actorOf(Props[LineLemmaExtractor], name= "lema_extractor")
  context.watch(lemaextr)

  val temp_dir :File=new File(new File(".").getAbsolutePath().dropRight(1)+"temp/")   //Creating the folder which will store temp files (lemmatized source and plag files)
  if(!temp_dir.exists()){
    temp_dir.mkdir()                   //create temp directory
  }
  var filepath :String=new String()
  def receive ={

    case file_properties2(source_file,fileid,tot_files,source_str) =>
      source_file_name=source_str

      filepath =new File(".").getAbsolutePath().dropRight(1)+"temp/"+"LEMMA_"+source_file_name
      val file_lines_size=Source.fromFile(source_file).getLines().size
      val file_handler :PrintWriter= new PrintWriter(new File(filepath))
      for (line <- Source.fromFile(source_file).getLines()) {
        lemaextr ! routingmessages2(line ,self ,source_file_name ,file_handler ,file_lines_size)
      }
      lemaextr.!(ShutdownMessage(file_handler))

    case returned_line_lemmas(listed_lemmas,file_handler,file_lines_size) =>
      if(!listed_lemmas.isEmpty) {
        external_counter+=1
        for(key <- listed_lemmas.iterator ) {
          //println(key)
          if(key!=listed_lemmas.last)
            file_handler.write(key + " ")
          else
            file_handler.write(key)
        }
        file_handler.write("\n")
      }

      if(external_counter==file_lines_size){
        file_handler.close()
        external_counter=0
      }

    case Terminated (corpse) =>
      context.actorSelection("../plag_analysis").!(source_file_transf(source_file_name, filepath))
    //println("TERMINATED")

    case _ =>
      println("I got nothing")

  }
}
