
package sample.hello

import java.io.File
import akka.actor._
import akka.routing._
import scala.io.Source
import collection.JavaConversions._
import edu.stanford.nlp.pipeline.Annotation
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import java.util.Properties
import edu.stanford.nlp.ling.CoreAnnotations.{TokenBeginAnnotation, LemmaAnnotation, TokensAnnotation, SentencesAnnotation}
import edu.stanford.nlp.ling.{CoreAnnotations, CoreLabel, IndexedWord}
import scala.collection.immutable.ListMap
import scala.math.pow

/**
 * Created by root on 11/2/14.
 */
case class file_properties2(filename :File,fileid :Int, total_files: Int,source_str :String)
case class routingmessages2(fileline :String, line_leng :Int,counter :Int,temp_leng:Int, ref_act :ActorRef ,file_name :String, file_lines_size :Int)
case class source_file_transf(source_file_name: String,listed_lemmas_source :List[String])
case class plag_file_transf(plag_file_name: String,listed_lemmas_plag :Map[String,Int], file_lines_size :Int)
case class returned_line_lemmas(listed_lemmas :Map[String,Int],filename :String)
case class import_plag_file(plag_file:File)
case class compare_source_plag(source_file :List[String],plag_file :List[String])
case class calculate_features(fixed_source_file :List[String],fixed_plag_file :List[String],fi_frg : Map[Int,Int],seq_conc :Map[String,Int])

object  LexicalAnalysis {
  def main(args: Array[String]): Unit = {
    var fileid=1
    var tot_files=1
    val current_directory=new File("/root/Desktop/")
    val indexingSystem= ActorSystem("CitationExtractionSystem2")//,ConfigFactory.load(application_is_remote))
    val plag_file_analysis = indexingSystem.actorOf(Props[PlagFileAnalysis],"plag_analysis")
    val source_analysis=indexingSystem.actorOf(Props[SourceFileAnalysis],"source_analysis")
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

    source_analysis ! file_properties2(source_file,1,tot_files,source_str)

  }
}

class SourceFileAnalysis extends Actor {
  var file_counter=0
  var lem_line_counter=0
  var source_lemmas :Map[String,Int]=Map()
  var counter_terminated :Int =0
  var concated_keys :String =new String()
  var source_file_name=new String()
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
    case file_properties2(source_file,fileid,tot_files,source_str) =>
      source_file_name=source_str
      var line_leng =0
      var counter=0
      for (line <- Source.fromFile(source_file).getLines()) {
        counter+=1
        line_leng= line_leng+line.length()
        //println(self+","+sender())
        router.route(routingmessages2(line, line_leng,counter,line.length(), self ,source_file_name,-1), sender())
      }
      router.route(Broadcast(PoisonPill), sender())
    case returned_line_lemmas(listed_lemmas,filename) =>
      lem_line_counter+=1
      if(!listed_lemmas.isEmpty) {
        source_lemmas = source_lemmas.++(listed_lemmas)
        // println(source_lemmas+","+plag_file_name)
      }
      if(counter_terminated==5) {
        source_lemmas = ListMap(source_lemmas.toList.sortBy {
          _._2
        }: _*)

        var temp_str_hold: String = new String()
        var tmp_key = new String()
        for (key <- source_lemmas.seq) {
          if (key._1.takeRight(1) == "-") {
            temp_str_hold = key._1.substring(0, key._1.lastIndexOf("@")) //.dropRight(1)
            tmp_key = key._1
          }
          else if (!temp_str_hold.isEmpty) {
            tmp_key = temp_str_hold + key._1
            temp_str_hold = new String()
          }
          else {
            tmp_key = key._1
          }
          source_lemmas = source_lemmas.-(key._1)
          if (key._1.takeRight(1) != "-") {
            source_lemmas = source_lemmas.+(tmp_key -> key._2)
          }
        }

        val listed_lemmas: List[String] = source_lemmas.keys.toList
        //println(listed_lemmas)
        context.actorSelection("../plag_analysis").!(source_file_transf(source_file_name, listed_lemmas))

      }
    case Terminated (corpse) =>
      router = router.removeRoutee(corpse)
      counter_terminated.+=(1)
      if(counter_terminated==5){
        self.!(returned_line_lemmas(Map.empty, new String()))
      }

    case _ =>
      println("I got nothing")

  }


}

class LineLemmaExtractor extends Actor with ActorLogging{
  def manOf[T: Manifest](t: T): Manifest[T] = manifest[T]
  def receive ={

    case routingmessages2(line,line_leng,counter,temp_leng, source_receiver_ref,filename,file_lines_size) =>

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
      val lemmas = sentences flatMap  { sentence =>
        val tokens = sentence.get(classOf[TokensAnnotation])
        tokens map { _.get(classOf[LemmaAnnotation]) }  }
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

      val max_key=if(last_char_line=='-'){
        lemma_map.maxBy(_._2)._1+last_char_line
        lemma_map=lemma_map.+(lemma_map.maxBy(_._2)._1+last_char_line -> lemma_map.maxBy(_._2)._2)
        lemma_map=lemma_map.-(lemma_map.maxBy(_._2)._1)
      }
      //println(lemma_map+","+filename)
      //println(source_receiver_ref+" and \t"+context.actorSelection("user/source_analysis"))
      //println(source_receiver_ref)
      if(source_receiver_ref.toString().contains("plag_analysis")){
        //println(context.actorSelection(source_receiver_ref.path.parent))
        context.actorSelection(source_receiver_ref.path.parent).!(plag_file_transf(filename,lemma_map,file_lines_size))(context.parent)
      }
      else if (source_receiver_ref.toString().contains("source_analysis")){
        //context.actorSelection("../plag_analysis").!(source_file_transf(source_file_name, listed_lemmas))
        source_receiver_ref.!(returned_line_lemmas(lemma_map,filename))
      }

    case PoisonPill =>
      context.stop(self)

    case _ =>
      println("No line received")
  }

}

class PlagFileAnalysis extends Actor {
  var source_filename = new String()
  val linediting=context.actorOf(Props[LineSeparate], name= "line_separate")
  var file_counter=0
  var counter_terminated :Int=1
  var plag_lemmas :Map[String,Int]=Map()
  val fragment=context.actorOf(Props[Fragmentation], name= "fragmentation")
  var source_file_lemmas :List[String]= List()

  def receive = {
    case source_file_transf(source_file_name,listed_lemmas_source) =>
      source_file_lemmas=listed_lemmas_source
      source_filename=source_file_name
      var path_filename=new File(" ")
      val current_directory=new File("/root/Desktop/")
      for(file <- current_directory.listFiles if(file.getName.endsWith(".txt") && file.getName()!=source_file_name) ){
        path_filename=new File(file.toString())
        //println(path_filename)
        linediting.!(import_plag_file(path_filename))
      }
      val file_done :Boolean=true

    case plag_file_transf(plag_filename, listed_lemmas_plag,file_lines_size) =>
      //println("Source File:"+source_filename+"\t Plagiarised File:"+plag_filename+"\t  mapped_lemma:"+listed_lemmas_plag)
      if(counter_terminated==file_lines_size) {
        plag_lemmas = plag_lemmas.++(listed_lemmas_plag)
        plag_lemmas = ListMap(plag_lemmas.toList.sortBy {
          _._2
        }: _*)

        var temp_str_hold: String = new String()
        var tmp_key = new String()
        for (key <- plag_lemmas.seq) {
          if (key._1.takeRight(1) == "-") {
            temp_str_hold = key._1.substring(0, key._1.lastIndexOf("@")) //.dropRight(1)
            tmp_key = key._1
          }
          else if (!temp_str_hold.isEmpty) {
            tmp_key = temp_str_hold + key._1
            temp_str_hold = new String()
          }
          else {
            tmp_key = key._1
          }
          plag_lemmas = plag_lemmas.-(key._1)
          if (key._1.takeRight(1) != "-") {
            plag_lemmas = plag_lemmas.+(tmp_key -> key._2)
          }
        }

        val listed_lemmas: List[String] = plag_lemmas.keys.toList
        plag_lemmas=Map()
        counter_terminated=1
        //println(listed_lemmas+",\t"+source_filename)
        fragment.!(compare_source_plag(source_file_lemmas,listed_lemmas))
      }
      else{
          counter_terminated+=1
          plag_lemmas = plag_lemmas.++(listed_lemmas_plag)
      }

    case _ =>
      println("Nothing received")
  }

}


class LineSeparate extends Actor {
  var counter_terminated :Int=0
  var external_counter: Int=0
  //val router2: ActorRef =context.actorOf(RoundRobinPool(5).props(Props[LineLemmaExtractor]), "router2")
  val linediting=context.actorOf(Props[LineLemmaExtractor], name= "line_separate_plag")

  def receive = {

    case import_plag_file(path_filename) =>
      external_counter+=1
      var counter = 0
      var line_leng =0
      counter_terminated=0
      for (line <- Source.fromFile(path_filename).getLines()) {
        val file_lines_size=Source.fromFile(path_filename).getLines().size
        line_leng= line_leng+line.length()
        //println(file_lines_size)
        counter += 1
        linediting.!(routingmessages2(line, line_leng,counter,line.length(), self ,path_filename.getName(),file_lines_size))
        //router2.!(routingmessages2(line, line_leng,counter,line.length(), self ,path_filename.getName()))
      }
    case _ =>
      println("The line of the current file has not received")
  }

}

class Fragmentation extends Actor {
  val relevance=context.actorOf(Props[Relevance], name= "relevance_features")
  def receive ={
    case compare_source_plag(source_file,plag_file) =>
      val fixed_source_file :List[String]=for(key <- source_file)yield key.substring(0,key.lastIndexOf("@"))
      val fixed_plag_file :List[String]=for(key <- plag_file)yield key.substring(0,key.lastIndexOf("@"))
      println("fixed source file:"+fixed_source_file+" \t \t fixed plagiarism file:"+fixed_plag_file)
      var seq_conc :Map[String,Int]=Map()
      var counter : Int =0
      var temp_str= ""
      var fi_frg : Map[Int,Int]=Map()
      var abs_seq=0
      var conc_counter :Int=0
      //println(fixed_source_file.length)
      val min_size= if((fixed_source_file.length-1) >= (fixed_plag_file.length-1)) (fixed_plag_file.length-1) else (fixed_source_file.length-1)
      for (i <- 0 to (fixed_source_file.length-1)){
        for(j <-0 to (fixed_plag_file.length -1) if(fixed_source_file(i)==fixed_plag_file(j)) ){
          if((i==0 || j==0) || fixed_source_file(i-1)!=fixed_plag_file(j-1)) {

            while ( ((i + counter) <= (fixed_source_file.length-1)) && ((j + counter) <= (fixed_plag_file.length -1)) ){
              //println(counter)
              if (fixed_plag_file(j + counter) == fixed_source_file(i + counter)) {
                abs_seq+=1
                temp_str = temp_str+ fixed_plag_file(j + counter)+" "  //","
              }
              else {
                counter = fixed_source_file.length
              }
              counter += 1
            }

            counter=0
          }
          //println(temp_str)
          if(fi_frg.containsKey(abs_seq)){                   //if else gia ton ypologismo map Fragmentation features
            val new_value :Int=fi_frg.apply(abs_seq)+abs_seq
            //common_sequences.put(temp_str,new_value)
            fi_frg = fi_frg.+(abs_seq -> new_value)
          }
          else {
            if(abs_seq!=0) {
              fi_frg = fi_frg.+(abs_seq -> abs_seq)
            }
          }

          if(seq_conc.containsKey(temp_str)){
            val new_value2 :Int=seq_conc.apply(temp_str)+1
            seq_conc=seq_conc.+(temp_str -> new_value2)
          }
          else{
            if(!temp_str.isEmpty()) {
              seq_conc = seq_conc.+(temp_str -> 1)
            }
          }
          abs_seq=0
          temp_str=""
          //println(temp_str)

        }

      }
      //println("Fi_frg:"+fi_frg+"\t seq_conc \t"+seq_conc)
      relevance ! calculate_features(fixed_source_file,fixed_plag_file,fi_frg,seq_conc)
  }

}

class Relevance extends Actor {

  def receive ={
    case calculate_features(source_file,plag_file,fi_frg,seq_conc) =>
      var relevance_map :Map[String,Float]=Map()
      var Relevance_of_Sequnces :Array[Int]= Array.empty
      var counter :Int =0
      var ginomeno :Float=1
      for(key2 <- seq_conc.keys){
        //println(key2)
         val wk_Arr_Dr :Map[String,Int]=occ_wk(key2.split(" +"),source_file)

         val wk_Arr_Ds :Map[String,Int]=occ_wk(key2.split(" +"),plag_file)
         //println(wk_Arr_Dr_temp+"and \t"+wk_Arr_Ds_temp)

         val first_fraction :Float= (1/pow(2.72,seq_conc.apply(key2)-1)).toFloat
         //println(first_fraction)
         val array_source :Array[Int]=wk_Arr_Dr.values.toArray     //pinakas pou periexei twn arithmo emfanisewn kathe lekshs tou key2 (sequence) sto source file tou sygkekrimenou
         val array_plag :Array[Int]=wk_Arr_Ds.values.toArray      //pinakas pou periexei twn arithmo emfanisewn kathe lekshs tou key2 (sequence) sto plagiarised file tou sygkekrimenou
         //println(array_plag(0))
         for(k <- 0 to (key2.split(" +").length-1)){
            ginomeno=ginomeno*( 2.toFloat/(array_plag(k).toFloat + array_source(k).toFloat) )
           //println("Ginomeno:"+ginomeno)
         }
         val second_fraction=ginomeno
         val result :Float=first_fraction*second_fraction
         relevance_map=relevance_map+(key2 ->result)
      }
      var relevance_features :Map[Int,Float]=Map()
      for(key <- relevance_map.keys){
            if(relevance_features.containsKey(key.split(" +").length)){    //an to map periexei kleidi iso me ton arithmo twn leksewn ths sequence
              val relev_value=relevance_features.apply(key.split(" +").length)+relevance_map.apply(key)
              relevance_features=relevance_features.+(key.split(" +").length -> relev_value)
            }
            else{
              relevance_features=relevance_features.+(key.split(" +").length -> relevance_map.apply(key))
            }
      }
      println("Fragmentation Features :"+fi_frg)
      println("RELEVANCE features MAP:"+relevance_features)

  }
  def occ_wk(key_Arr :Array[String],file:List[String]): Map[String,Int] ={
    var wk_arr : Map[String,Int]= Map()
    var counter :Int=0
    for(key1 <- key_Arr){
       //println(key1)
       for (key2 <- file if(key1==key2)){
           counter+=1
       }
       wk_arr=wk_arr.+(key1 -> counter)
       counter=0
    }
    return(wk_arr)
  }

}

