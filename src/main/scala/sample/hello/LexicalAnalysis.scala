package sample.hello

import java.io.File
import akka.actor._
import java.io._
import weka.core.Attribute
import collection.mutable.{HashMap,MultiMap,Map}

/**
 * Created by root on 11/2/14.
 */
case class file_properties2(filename :File, total_files: Int,source_str :String,counter :Int)
case class routingmessages2(fileline :String,ref_act :ActorRef ,file_name :String, file_handler :PrintWriter ,file_lines_size :Int)
case class source_file_transf(id_source_filename :Tuple2[Int,String],source_file_words:Map[String,Int],filepath :String)
case class plag_file_transf(plag_file_name: String,listed_lemmas_plag :List[String], file_lines_size :Int,file_handler_plag :PrintWriter)
case class all_sources_compare(source_filename :String)
case class returned_line_lemmas(listed_lemmas :List[String],file_handler :PrintWriter,file_lines_size :Int,source_filename :String)
case class send_filepath(source_filename :String,source_words:Map[String,Int],source_files_counter:Int)
case class import_plag_file(plag_file:File,filepath_plag:String)
case class compare_source_plag(source_filepath :String,plag_filepath :String,source_file_words :Map[String,Int],comp_file_ids:Tuple2[Int,Int],tup_le :Tuple3[String,Int,Int])
case class ShutdownMessage(file_handler :PrintWriter)
case class Routees_Termination(id_size_filename_total :Tuple4[Int,Int,String,Int],source_file_words :Map[String,Int],compared_tuple_w_ids :Tuple4 [String,String,Int,Int])
case class Routees_Inception_Termination(id_size_filename_total :Tuple4[Int,Int,String,Int],source_file_words :Map[String,Int],compared_tuple_w_ids :Tuple4 [String,String,Int,Int])
case class End_Of_SourceFile(id_size_filename_total :Tuple4[Int,Int,String,Int],source_file_words :Map[String,Int],compared_tuple_w_ids :Tuple4 [String,String,Int,Int])
case class word_line_comp(word :String,plag_filepath :String, counter: Int,comp_file_ids :Tuple2[Int,Int])
case class word_line_comp_inception(word :String,line :Array[String],counter_source :Int,counter_plag :Int,plag_filepath :String,comp_file_ids :Tuple2[Int,Int])
case class returned_Multimaps(plag_tuple:Tuple2[String,Int],source_word :Map[String,Int],times_found :Int,comp_file_ids :Tuple2[Int,Int])
case class calculate_classification(source_file_matches:HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int],plag_file_matches:HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int],source_file_words :Map[String,Int],compared_tuple_w_ids:Tuple4 [String ,String,Int,Int],id_size_filename_total :Tuple4[Int,Int,String,Int])
case class cosine_similarity(all_frg_y:HashMap[Int, scala.collection.mutable.Set[String]] with scala.collection.mutable.MultiMap [Int,String],all_rel_y :HashMap[Int, scala.collection.mutable.Set[String]] with scala.collection.mutable.MultiMap [Int,String],normalised_term_frequency:Map[String,Float],normalised_source_term_freq:Map[String,Float],term_files_occ :Map [String,Int],id_total :Tuple2[Int,Int],compared_tuple_w_ids:Tuple4 [String ,String,Int,Int])
case class calculate_wks(plag_file_matches:HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int],source_file_matches:HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int],normalised_term_frequency:Map[String,Float],normalised_source_term_freq:Map[String,Float],term_files_occ :Map [String,Int],id_total :Tuple2[Int,Int],compared_tuple_w_ids :Tuple4 [String ,String,Int,Int])
case class calculate_wks2(start :Int,end :Int,seq_start :String,plag_file_matches:HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int],source_key_pos :Int,source_file_matches :HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int],compared_tuple_w_ids:Tuple2 [Int,Int])
case class Terminate_FR_Calcs(source_file_matches :HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int],plag_file_matches :HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int],normalised_term_frequency:Map[String,Float],normalised_source_term_freq:Map[String,Float],term_files_occ :Map [String,Int],id_total :Tuple2[Int,Int],compared_tuple_w_ids :Tuple4 [String ,String,Int,Int])
case class FR_Calcs_Routees_Terminated(source_file_matches :HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int],plag_file_matches :HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int],normalised_term_frequency:Map[String,Float],normalised_source_term_freq:Map[String,Float],term_files_occ :Map [String,Int],id_total :Tuple2[Int,Int],compared_tuple_w_ids :Tuple4 [String ,String,Int,Int])
case class frag_calculate(seq_str :String, start_end_fltr:Array[Int])
case class calculate_features(wk_Arr_Dr :Map[String,Int],wk_Arr_Ds :Map[String,Int],fi_frg : Map[Int,Int],seq_conc :Map[String,Int],normalised_term_frequency:Map[String,Float],normalised_source_term_freq:Map[String,Float],term_files_occ :Map [String,Int],id_total :Tuple2[Int,Int],compared_tuple_w_ids :Tuple4 [String ,String,Int,Int])
case class ig_chunking(all_frg_y:HashMap[Int, scala.collection.mutable.Set[String]] with scala.collection.mutable.MultiMap [Int,String],all_rel_y :HashMap[Int, scala.collection.mutable.Set[String]] with scala.collection.mutable.MultiMap [Int,String],classification_map :Map[Int ,String],id_total :Tuple2[Int,Int],compared_tuple_w_ids:Tuple4 [String ,String,Int,Int])
case class EvalIG(subseq :scala.collection.immutable.Map[Int, scala.collection.mutable.Set[java.lang.String]],counter2:Int,plag_id_counter :Int,source_file_counter :Int)
case class average_ig_calculation(infogainscores :HashMap [Attribute,Double])

object LexicalAnalysis {
  def ReadFiles2(source_dir :File,plag_dir :File): Unit = {
    var tot_files=0
    val indexingSystem= ActorSystem("CitationExtractionSystem2")//,ConfigFactory.load(application_is_remote))
    val plag_file_analysis = indexingSystem.actorOf(Props[PlagFileAnalysis],"plag_analysis")
    val source_analysis=indexingSystem.actorOf(Props[SourceFileAnalysis],"source_analysis")

    var counter :Int=0
    for(file <- source_dir.listFiles if(file.getName.endsWith(".txt") )){
      counter+=1  //count the number of source files on souce_file folder
      for(file2 <- plag_dir.listFiles if(file2.getName.endsWith(".txt")) ){ //&& file.getName()!=file.getName())){
        tot_files+=1 //counts number of plag files in plag_file folder that do not have the same name with the current source file
      }
    }
    val file :String=source_dir.listFiles().head.getName
    if(file.endsWith(".txt")){
      val source_file=new File(source_dir+"/"+file)
      source_analysis ! file_properties2(source_file,tot_files,file,counter)
    }
  }
}

