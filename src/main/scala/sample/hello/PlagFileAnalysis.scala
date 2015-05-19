package sample.hello

import java.io.File
import akka.actor._
import collection.mutable.Map

/**
 * Created by root on 3/3/15.
 */
class PlagFileAnalysis extends Actor {
  var source_filename = new String()
  val linediting=context.actorOf(Props[LineSeparator2], name= "line_separate")
  var counter_terminated :Int=0
  val comp_s_p=context.actorOf(Props[CompareSourcePlag], name= "comparing_s_p")
  var source_filepath :String =new String()
  var plagfile_id :Int =0
  var total_plag_files :Int =0  //counts how many pontentially plagiarised files are found in the current directory
  var filepath_plag :String=new String()
  var source_words :Map[String,Int]=Map()
  var plagfile_wordcount :Int=0

  val temp_dir :File=new File(new File(".").getAbsolutePath().dropRight(1)+"temp_plag/")
  if(!temp_dir.exists()){
    temp_dir.mkdir()                   //create temp directory
  }
  def receive = {
    case source_file_transf(id_source_filename,source_file_words,filepath) =>
      source_words=source_file_words
      source_filepath=filepath
      source_filename=id_source_filename._2
      var path_filename=new File(" ")
      val current_directory=new File(new File(".").getAbsolutePath().dropRight(1)+"suspicious_files/")
      for(file <- current_directory.listFiles if(file.getName.endsWith(".txt")) ){
        path_filename=new File(file.toString())
        filepath_plag =new File(".").getAbsolutePath().dropRight(1)+"temp_plag/"+"LEMMA_"+file.getName
        //println(path_filename)
        total_plag_files+=1
        linediting.!(import_plag_file(path_filename,filepath_plag))
      }
      val file_done :Boolean=true

    case plag_file_transf(plag_filename, listed_lemmas_plag,file_lines_size,file_handler_plag) =>
      if (counter_terminated!=file_lines_size) {
        counter_terminated+=1
        var line_counter =0

        for (key <- listed_lemmas_plag.iterator ) {
          plagfile_wordcount=plagfile_wordcount.+(listed_lemmas_plag.size)
          line_counter+=1
          if (key != listed_lemmas_plag.last || line_counter.!=(listed_lemmas_plag.size))
            file_handler_plag.write(key + " ")
          else
            file_handler_plag.write(key)
        }
        file_handler_plag.write("\n")
      }
      if(counter_terminated==file_lines_size){
        file_handler_plag.close()
        counter_terminated=0
        plagfile_id+=1
        val tup_le :Tuple3[String,Int,Int]=(plag_filename,plagfile_wordcount,total_plag_files)
        //println("sending:  "+source_filepath)
        val comp_file_ids :Tuple2[Int,Int]=(1,plagfile_id)
        comp_s_p.!(compare_source_plag(source_filepath,new File(".").getAbsolutePath().dropRight(1)+"temp_plag/"+"LEMMA_"+plag_filename,source_words,comp_file_ids,tup_le))
        plagfile_wordcount=0
        if(total_plag_files==plagfile_id){
          self.!(all_sources_compare(source_filename))
        }
      }
    case all_sources_compare(source_filename) =>
      var source_dir=new File(new File(".").getAbsolutePath().dropRight(1)+"source_files/")
      var counter :Int=1 //giati exoume kai to prwto source arxeio pou steilame hdh kai den peranei ton elegxo ths for
      for(file <- source_dir.listFiles if(file.getName().!=(source_filename) && !file.getName().endsWith("~")) ){
        counter+=1
        var plag_id :Int =0
        val source_file=new File(source_dir+"/"+file.getName())
        context.actorSelection("/user/source_analysis/").!(file_properties2(source_file,total_plag_files,file.getName(),counter))
      }
    case _ =>
      println("Nothing received")
  }

}