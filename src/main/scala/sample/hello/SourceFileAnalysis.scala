package sample.hello

import java.io.File
import akka.actor._
import java.io._
import scala.io.Source
import collection.mutable.Map

/**
 * Created by root on 3/3/15.
 */
class SourceFileAnalysis extends Actor {
  var external_counter :Int =0
  var file_id_counter :Int =0
  val lemaextr=context.actorOf(Props[LineLemmaExtractor], name= "lema_extractor")
  var source_files_counter :Int =0
  context.watch(lemaextr)
  var total_plag_files :Int =0
  var source_file_words :Map[String,Int]=Map()

  val temp_dir :File=new File(new File(".").getAbsolutePath().dropRight(1)+"temp_source/")   //Creating the folder which will store temp files (lemmatized source and plag files)
  if(!temp_dir.exists()){
    temp_dir.mkdir()                   //create temp directory
  }
  var filepath :String=new String()
  def receive ={

    case file_properties2(source_file,tot_files,source_str,counter) =>
      source_files_counter=counter
      total_plag_files=tot_files
      filepath =new File(".").getAbsolutePath().dropRight(1)+"temp_source/"+"LEMMA_"+source_str
      val file_lines_size=Source.fromFile(source_file).getLines().size
      val file_handler :PrintWriter= new PrintWriter(new File(filepath))
      val textsource =Source.fromFile(source_file)
      for (line <- textsource.getLines()) {
        lemaextr ! routingmessages2(line ,self ,source_str ,file_handler ,file_lines_size)
      }
      textsource.close()

    case returned_line_lemmas(listed_lemmas,file_handler,file_lines_size,source_filename) =>
      external_counter+=1
      for(key <- listed_lemmas.iterator ) {
        if(key!=listed_lemmas.last)
          file_handler.write(key + " ")
        else
          file_handler.write(key)

        if(source_file_words.contains(key))
          source_file_words=source_file_words.+(key -> (source_file_words.apply(key) +1 ))
        else
          source_file_words=source_file_words.+(key -> 1)
      }
      file_handler.write("\n")

      if(external_counter==file_lines_size){
        file_id_counter+=1
        file_handler.close()
        external_counter=0
        if (file_id_counter==1) {
          //stelnoume mono to prwto source file gia na ektelesoume mono 1 fora to suspicious file preprocessing
          val id_source_filename :Tuple2[Int,String]=(file_id_counter,source_filename)
          context.actorSelection("../plag_analysis").!(source_file_transf(id_source_filename, source_file_words, filepath))
          source_file_words=Map()
        }
        else {
          val source_words :Map [String,Int] =source_file_words
          self.!(send_filepath(source_filename,source_words,file_id_counter))
          source_file_words=Map()
        }
      }
      if(file_id_counter == source_files_counter){
        lemaextr.!(ShutdownMessage(file_handler))
      }

    case send_filepath(source_filename,source_words,source_file_id) =>
      val temp_dir :File=new File(new File(".").getAbsolutePath().dropRight(1)+"temp_plag/")
      var plag_id :Int =0
      val source_filepath=new File(".").getAbsolutePath().dropRight(1)+"temp_source/"+"LEMMA_"+source_filename
      for(file2 <- temp_dir.listFiles if(file2.getName.endsWith(".txt")) ){
        plag_id+=1
        val tup_le :Tuple3[String,Int,Int]=(file2.getName(),0,total_plag_files)
        val  comp_file_ids :Tuple2[Int,Int]=(source_file_id,plag_id)
        context.actorSelection("/user/plag_analysis/comparing_s_p").!(compare_source_plag(source_filepath,new File(".").getAbsolutePath().dropRight(1)+"temp_plag/"+file2.getName(),source_words, comp_file_ids,tup_le))
      }

    case Terminated(corpse) =>
       println("Pre-processed all source files")


    case _ =>
      println("I got nothing")

  }
}
