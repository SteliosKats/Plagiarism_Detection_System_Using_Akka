package sample.hello

import java.io.File
import akka.actor._
import akka.routing._
import java.io._
import scala.collection.mutable
import scala.io.Source
import collection.JavaConversions._
import edu.stanford.nlp.pipeline.Annotation
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import java.util.Properties
import edu.stanford.nlp.ling.CoreAnnotations.{TokenBeginAnnotation, LemmaAnnotation, TokensAnnotation, SentencesAnnotation}
import edu.stanford.nlp.ling.{CoreAnnotations, CoreLabel, IndexedWord}
import scala.collection.immutable.ListMap
import scala.util.control.Breaks._
import scala.math.pow
import com.scalaner.evaluation._
import com.scalaner.training._
import collection.mutable.{HashMap,MultiMap,Map}

/**
 * Created by root on 11/2/14.
 */
case class file_properties2(filename :File,fileid :Int, total_files: Int,source_str :String)
case class routingmessages2(fileline :String,ref_act :ActorRef ,file_name :String, file_handler :PrintWriter ,file_lines_size :Int)
case class source_file_transf(source_file_name: String,filepath :String)
case class plag_file_transf(plag_file_name: String,listed_lemmas_plag :List[String], file_lines_size :Int,file_handler_plag :PrintWriter)
case class returned_line_lemmas(listed_lemmas :List[String],file_handler :PrintWriter,file_lines_size :Int)
case class import_plag_file(plag_file:File,filepath_plag:String)
case class compare_source_plag(source_filepath :String,plag_filepath :String,plagfile_id :Int)
case class calculate_features(wk_Arr_Dr :Map[String,Int],wk_Arr_Ds :Map[String,Int],fi_frg : Map[Int,Int],seq_conc :Map[String,Int])
case class ShutdownMessage(file_handler :PrintWriter)
case class Routees_Termination(plagfile_id :Int)
case class Routees_Inception_Termination(plagfile_id :Int)
case class End_Of_SourceFile(plagfile_id :Int)
case class word_line_comp(word :String,plag_filepath :String, counter: Int,plagfile_id :Int)
case class word_line_comp_inception(word :String,line :Array[String],counter_source :Int,counter_plag :Int,plag_filepath :String,plagfile_id :Int)
case class returned_Multimaps(plag_tuple:Tuple2[String,Int],source_word :Map[String,Int],times_found :Int,plagfile_id :Int)
case class calculate_wks(plag_file_matches:HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int],source_file_matches:HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int],plagfile_id :Int)
case class calculate_wks2(start :Int,end :Int,seq_start :String,plag_file_matches:HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int],source_key_pos :Int,source_file_matches :HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int],plagfile_id :Int)
case class Terminate_FR_Calcs(source_file_matches :HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int],plag_file_matches :HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int],plagfile_id :Int)
case class FR_Calcs_Routees_Terminated(source_file_matches :HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int],plag_file_matches :HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int],plagfile_id :Int)
case class frag_calculate(seq_str :String, start_end_fltr:Array[Int])


object LexicalAnalysis {
  def ReadFiles2(source_str: String,current_directory :File): Unit = {
    var tot_files=1
    val indexingSystem= ActorSystem("CitationExtractionSystem2")//,ConfigFactory.load(application_is_remote))
    val plag_file_analysis = indexingSystem.actorOf(Props[PlagFileAnalysis],"plag_analysis")
    val source_analysis=indexingSystem.actorOf(Props[SourceFileAnalysis],"source_analysis")
    var filenames_ids :Map[String,Int]=Map()

    val source_file=new File(current_directory+"/"+source_str)
    filenames_ids=filenames_ids.+(source_str ->1)

    for(file <- current_directory.listFiles if(file.getName.endsWith(".txt") && file.getName()!=source_str )){
      tot_files+=1
    }
    source_analysis ! file_properties2(source_file,1,tot_files,source_str)
  }
}
