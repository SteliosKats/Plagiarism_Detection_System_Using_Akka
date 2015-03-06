package sample.hello

import java.io.File
import akka.actor._

import scala.io.Source

/**
 * Created by root on 3/3/15.
 */
class PlagFileAnalysis extends Actor {
  var source_filename = new String()
  val linediting=context.actorOf(Props[LineSeparator2], name= "line_separate")
  var file_counter=0
  var counter_terminated :Int=0
  val comp_s_p=context.actorOf(Props[CompareSourcePlag], name= "comparing_s_p")
  var source_filepath :String =new String()
  var plagfile_id :Int =0

  var filepath_plag :String=new String()
  def receive = {
    case source_file_transf(source_file_name,filepath) =>
      source_filepath=filepath
      source_filename=source_file_name
      var path_filename=new File(" ")
      val current_directory=new File("/root/Desktop/FileInd")
      for(file <- current_directory.listFiles if(file.getName.endsWith(".txt") && file.getName()!=source_file_name) ){
        path_filename=new File(file.toString())
        filepath_plag =new File(".").getAbsolutePath().dropRight(1)+"temp/"+"LEMMA_"+file.getName
        //println(path_filename)
        linediting.!(import_plag_file(path_filename,filepath_plag))
      }
      val file_done :Boolean=true

    case plag_file_transf(plag_filename, listed_lemmas_plag,file_lines_size,file_handler_plag) =>
      if (counter_terminated!=file_lines_size) {
        counter_terminated+=1
        for (key <- listed_lemmas_plag.iterator) {
          if (key != listed_lemmas_plag.last)
            file_handler_plag.write(key + " ")
          else
            file_handler_plag.write(key)
        }
        file_handler_plag.write("\n")
      }
      if(counter_terminated==file_lines_size){
        println("closing file handler")
        file_handler_plag.close()
        counter_terminated=0
        plagfile_id+=1
        comp_s_p.!(compare_source_plag(source_filepath,new File(".").getAbsolutePath().dropRight(1)+"temp/"+"LEMMA_"+plag_filename,plagfile_id))
      }

    case _ =>
      println("Nothing received")
  }

}
