package sample.hello

import java.io.File
import akka.actor._
import akka.routing._
import scala.collection.immutable
import scala.io.Source
import collection.JavaConversions._
import edu.stanford.nlp.pipeline.Annotation
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import java.util.Properties
import edu.stanford.nlp.ling.CoreAnnotations.{TokenBeginAnnotation, LemmaAnnotation, TokensAnnotation, SentencesAnnotation}
import edu.stanford.nlp.ling.{CoreAnnotations, CoreLabel, IndexedWord}
import scala.collection.immutable.ListMap

/**
 * Created by root on 11/2/14.
 */
case class file_properties2(filename :File,fileid :Int, total_files: Int)
case class routingmessages2(fileline :String, line_leng :Int,counter :Int,temp_leng:Int, ref_act :ActorRef,fileid :Int)
//case class source_props(filename :File,fileid :Int)
case class returned_line_lemmas(listed_lemmas :Map[String,Int])

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
      println(file)
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
  var file_counter=0
  var lem_line_counter=0
  var source_lemmas :Map[String,Int]=Map()
  var counter_terminated :Int =0
  var concated_keys :String =new String()
  /* Creating A router to route "Workers" to extract citations for each line of the file */
  var router ={
    val routees=Vector.fill(5){
      val lineseparator=context.actorOf(Props[LineLemmaExtractor])
      context watch lineseparator
      ActorRefRoutee(lineseparator)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }
  /* Creating A router to route "Workers" to ectract citations for each line of the file */

  def receive ={
    case file_properties2(source_file,fileid,tot_files) =>
      var line_leng =0
      var counter=0
      for (line <- Source.fromFile(source_file).getLines()) {
        counter+=1
        line_leng= line_leng+line.length()
        //println(line_leng)
        router.route(routingmessages2(line, line_leng,counter,line.length(), self, fileid), sender())
      }
      router.route(Broadcast(PoisonPill), sender())
    case returned_line_lemmas(listed_lemmas) =>
       lem_line_counter+=1
       if(!listed_lemmas.isEmpty) {
         source_lemmas = source_lemmas.++(listed_lemmas)
       }
      if(counter_terminated==5) {
        source_lemmas = ListMap(source_lemmas.toList.sortBy {_._2}: _*)

        var temp_str_hold: String = new String()
        var tmp_key = new String()
        for (key <- source_lemmas.seq) {
          tmp_key = key._1.substring(0, key._1.lastIndexOf("@"))
          if (key._1.takeRight(1) == "-") {
            temp_str_hold = key._1.substring(0, key._1.lastIndexOf("@"))  //.dropRight(1)
          }
          else if (!temp_str_hold.isEmpty) {
            tmp_key = temp_str_hold + tmp_key
            temp_str_hold=new String()
          }
          source_lemmas = source_lemmas.-(key._1)
          if (key._1.takeRight(1) != "-") {
            source_lemmas = source_lemmas.+(tmp_key -> key._2)
          }
        }
        val listed_lemmas :List[String]=source_lemmas.keys.toList
        println(listed_lemmas)
      }
    case Terminated (corpse) =>
      router = router.removeRoutee(corpse)
      counter_terminated.+=(1)
      if(counter_terminated==5){
        self.!(returned_line_lemmas(Map.empty))
      }

    case _ =>
      println("I got nothing")

  }


}

class LineLemmaExtractor extends Actor with ActorLogging{
  def manOf[T: Manifest](t: T): Manifest[T] = manifest[T]
  def receive ={

    case routingmessages2(line,line_leng,counter,temp_leng, source_receiver_ref,fileid) =>

      // create the processor
      val props:Properties=new Properties()
      props.put("annotators","tokenize, ssplit, pos ,lemma")
      val pipeline:StanfordCoreNLP=new StanfordCoreNLP(props)

      val document_line :Annotation=new Annotation(line)
      pipeline.annotate(document_line)
      val sentences =document_line.get(classOf[SentencesAnnotation])

      val lemmas1 = sentences flatMap  { sentence =>
        val tokens = sentence.get(classOf[TokensAnnotation])
        tokens map { x=> x }  }
      val tmp_lem :String= lemmas1.toString()
      val last_char_line= tmp_lem.charAt(tmp_lem.length()-2)
      //println(last_char_line)
      val lemmas = sentences flatMap  { sentence =>
        val tokens = sentence.get(classOf[TokensAnnotation])
        tokens map { _.get(classOf[LemmaAnnotation]) }  }
      //println(lemmas)
      val listed_lemmas:List[String]=lemmas.toList.filterNot(_.forall(!_.isLetterOrDigit))

      var sentOffset=0
      var charOffset=0
      var tokenOffset=0

      val tokens = document_line.get(classOf[TokensAnnotation])
      var counter2=0
      var lemma_map : Map[String,Int]= Map()
      for(lemma <- listed_lemmas){
          counter2+=1
          lemma_map=lemma_map.+(lemma+"@"+counter+","+counter2 -> (counter2+line_leng -temp_leng))
      }
      //println(lemma_map)
      val max_key=if(last_char_line=='-'){
        lemma_map.maxBy(_._2)._1+last_char_line
        lemma_map=lemma_map.+(lemma_map.maxBy(_._2)._1+last_char_line -> lemma_map.maxBy(_._2)._2)
        lemma_map=lemma_map.-(lemma_map.maxBy(_._2)._1)
      }
      //println(lemma_map)
      source_receiver_ref.!(returned_line_lemmas(lemma_map))

    case PoisonPill =>
      context.stop(self)

    case _ =>
      println("No line received")
  }

}

