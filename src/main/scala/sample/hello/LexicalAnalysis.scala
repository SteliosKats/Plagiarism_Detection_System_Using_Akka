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
case class compare_source_plag(source_filepath :String,plag_filepath :String)
case class calculate_features(wk_Arr_Dr :Map[String,Int],wk_Arr_Ds :Map[String,Int],fi_frg : Map[Int,Int],seq_conc :Map[String,Int])
case class information_gain_evaluator(source_file :List[String],plag_file :List[String])
case class ShutdownMessage(file_handler :PrintWriter)
case class word_line_comp(word :String,plag_filepath :String, counter: Int,plag_file_matches :HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int])
case class word_line_comp_inception(word :String,line :Array[String],counter_source :Int,counter_plag :Int,plag_lines_size :Int,plag_file_matches :HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int])
case class returned_Multimaps(word_thesis_plag:HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int],source_word :Map[String,Int],times_found :Int,plag_lines_size :Int)
case class calculate_wks(plag_file_matches:HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int],source_file_matches:HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int])
case class calculate_wks2(start :Int,end :Int,seq_start :String,plag_file_matches:HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int],source_key_pos :Int,source_file_matches :HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int])
case class frag_calculate(seq_str :String, start_end_fltr:Array[Int])


object  LexicalAnalysis {
  def main(args: Array[String]): Unit = {
    var tot_files=1
    val current_directory=new File("/root/Desktop/FileInd/")
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
        //println(self+","+sender())
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
      println("TERMINATED")

    case _ =>
      println("I got nothing")

  }


}

class LineLemmaExtractor extends Actor with ActorLogging{
  def manOf[T: Manifest](t: T): Manifest[T] = manifest[T]
  def receive ={

    case routingmessages2(line, source_receiver_ref,filename,file_handler,file_lines_size) =>
      val err :PrintStream = System.err;
      System.setErr(new PrintStream(new OutputStream() {
        def write(b:Int):Unit ={
        }
      }))

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
      val listed_lemmas_pre:List[String]=lemmas.toList.filterNot(_.forall(!_.isLetterOrDigit))

      val listed_lemmas=
        if(last_char_line=='-'){
          listed_lemmas_pre.dropRight(1) :+(listed_lemmas_pre.last + "-")
        }
        else{
          listed_lemmas_pre
        }

      System.setErr(err);

      //println(listed_lemmas)

      if(source_receiver_ref.toString().contains("plag_analysis")){
        //println(context.actorSelection(source_receiver_ref.path.parent))
        context.actorSelection(source_receiver_ref.path.parent).!(plag_file_transf(filename,listed_lemmas,file_lines_size,file_handler))(context.parent)
      }
      else if (source_receiver_ref.toString().contains("source_analysis")){
        //context.actorSelection("../plag_analysis").!(source_file_transf(source_file_name, listed_lemmas))
        source_receiver_ref.!(returned_line_lemmas(listed_lemmas,file_handler,file_lines_size))
      }

    case ShutdownMessage(file_handler) =>
      context.stop(self)

    case _ =>
      println("No line received")
  }

}

class PlagFileAnalysis extends Actor {
  var source_filename = new String()
  val linediting=context.actorOf(Props[LineSeparate], name= "line_separate")
  var file_counter=0
  var counter_terminated :Int=0
  val fragment=context.actorOf(Props[Fragmentation], name= "fragmentation")
  var source_filepath :String =new String()

  val info_gain_eval=context.actorOf(Props[InfoGain], name= "information_gain")
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
        fragment.!(compare_source_plag(source_filepath,filepath_plag))
      }
    case _ =>
      println("Nothing received")
  }

}


class LineSeparate extends Actor {
  val linediting=context.actorOf(Props[LineLemmaExtractor], name= "line_separate_plag")

  def receive = {

    case import_plag_file(path_filename,filepath_plag) =>

      val file_handler_plag :PrintWriter= new PrintWriter(new File(filepath_plag))
      val file_lines_size=Source.fromFile(path_filename).getLines().size
      //println(file_lines_size)
      for (line <- Source.fromFile(path_filename).getLines()) {
        //println(file_lines_size)
        linediting.!(routingmessages2(line ,self ,path_filename.getName() ,file_handler_plag ,file_lines_size))
        //router2.!(routingmessages2(line, line_leng,counter,line.length(), self ,path_filename.getName()))
      }
    case _ =>
      println("The line of the current file has not received")
  }

}

class Fragmentation extends Actor {
  val frgm2=context.actorOf(Props[Fragmentation2], name= "fragmentation2_features")
  val relevance= context.actorOf(Props[Relevance], name= "relevance_features")

  var numb_of_app :Map[String,Int]=Map()
  var source_file_matches =new HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]
  var plag_file_matches =new HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]

  var plag_lines_size :Int= -1
  var external_counter :Int =0

  var fi_frg : Map[Int,Int]= Map()
  var seq_conc :Map[String,Int]=Map()
  var clearing_map =new HashMap[String, scala.collection.mutable.Set[String]] with scala.collection.mutable.MultiMap [String,String]
  //var clearing_map2 =new HashMap[String, scala.collection.mutable.Set[String]] with scala.collection.mutable.MultiMap [String,String]
  var wk_Arr_Dr :Map[String,Int]= Map()
  var wk_Arr_Ds :Map[String,Int]= Map()

  var all_terminated :Int=0

  var router ={
    val workerCount=Runtime.getRuntime().availableProcessors()
    val routees=Vector.fill(workerCount*2){
      val linecompare=context.actorOf(Props[LineComparison])
      context watch linecompare
      ActorRefRoutee(linecompare)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive ={
    case compare_source_plag(source_filepath,plag_filepath) =>
      plag_lines_size =Source.fromFile(plag_filepath).getLines().size
      var counter=0
      for (line <- Source.fromFile(source_filepath).getLines()) {
        val word_arr=line.mkString.split(" ")
        for(word <- word_arr){
          counter+=1
          router.route(word_line_comp(word,plag_filepath,counter,plag_file_matches),sender())
        }
      }
      router.route(Broadcast("Terminate"), sender())

    case returned_Multimaps(plag_file_matches,source_word,times_found,plag_lines_size) =>
      if(!source_word.isEmpty){
        source_file_matches.addBinding(source_word.head._1,source_word.head._2)
      }

    case "End of Source File" =>
      external_counter+=1
      if(external_counter==Runtime.getRuntime().availableProcessors()*2){
        println("\t plagiarized file matches: "+plag_file_matches+"\t source file matches: "+source_file_matches)
        frgm2.!(calculate_wks(plag_file_matches,source_file_matches))
        /////Mhdenismos twn arxikwpoihmenwn hashmaps
        /////
      }

    case frag_calculate(seq_str,start_end_fltr) =>

      val condition :String=(start_end_fltr(0)+start_end_fltr(3))+"."+start_end_fltr(4)+"."+(start_end_fltr(4)+start_end_fltr(3))
      if(!clearing_map.entryExists(seq_str,_ == (condition)) ){ //&& !clearing_map2.entryExists(seq_str, _.==( start_end_fltr(4)+"."+(start_end_fltr(4)+start_end_fltr(3)) )) ){ //(seq_str -> plag_string_start_point)  && source_file_point != apo to idio key me source file point
        //println("Seq_str:"+seq_str+"  plag_string_start_point:"+start_end_fltr(0))
        clearing_map=clearing_map.addBinding(seq_str , condition)
        if(fi_frg.containsKey(start_end_fltr(3))){                   //if else gia ton ypologismo map Fragmentation features
          val new_value :Int=fi_frg.apply(start_end_fltr(3))+start_end_fltr(3)
          //common_sequences.put(temp_str,new_value)
          fi_frg = fi_frg.+(start_end_fltr(3) -> new_value)
        }
        else {
          if(start_end_fltr(3) != 0) {
            fi_frg = fi_frg.+(start_end_fltr(3) -> start_end_fltr(3))
          }
        }

        if(seq_conc.containsKey(seq_str)){
          val new_value2 :Int=seq_conc.apply(seq_str)+1
          seq_conc=seq_conc.+(seq_str -> new_value2)
        }
        else{
          if(!seq_str.isEmpty()) {
            seq_conc = seq_conc.+(seq_str -> 1)
          }
        }
      }

    case "FR Calcs Routees Terminated" =>
      all_terminated +=1
      if(all_terminated==(Runtime.getRuntime().availableProcessors()*2)){
         for(keyvalue <- source_file_matches.iterator){
            wk_Arr_Dr=wk_Arr_Dr.+(keyvalue._1 -> keyvalue._2.size )
         }

         for(keyvalue2 <- plag_file_matches.iterator){
          wk_Arr_Ds=wk_Arr_Dr.+(keyvalue2._1 -> keyvalue2._2.size )
         }
        println("seq_conq: "+seq_conc+"\t fi_frg: "+fi_frg+"wk_Arr_Ds: "+wk_Arr_Ds+"\t wk_Arr_Dr: "+wk_Arr_Dr)
        relevance.!(calculate_features(wk_Arr_Dr,wk_Arr_Ds,fi_frg,seq_conc) )
      }



    case _ =>
      println("nothing happened")
  }

}

class LineComparison extends Actor with ActorLogging {

  var router ={
    val workerCount=Runtime.getRuntime().availableProcessors()
    val routees=Vector.fill(workerCount*2){
      val wordcompare2=context.actorOf(Props[WordComparison_Inception])
      context watch wordcompare2
      ActorRefRoutee(wordcompare2)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive ={

    case word_line_comp(word,plag_filepath,counter_source,plag_file_matches) =>
      var counter_plag=0
      var plag_lines_size :Int=Source.fromFile(plag_filepath).getLines().size
      for (line <- Source.fromFile(plag_filepath).getLines()) {
        val line_arr :Array[String]=line.split(" ")
        router.route(word_line_comp_inception(word,line_arr,counter_source,counter_plag,plag_lines_size,plag_file_matches),sender())
        counter_plag=counter_plag+ line.split(" ").size
      }

    case "Terminate" =>
      router.route("I Got Terminated",sender())

    case _ =>
      println("I didn't got word from the Source File this time!")

  }

}

class WordComparison_Inception extends Actor with ActorLogging {
  def receive ={
    case word_line_comp_inception(word,line_arr,counter_source,counter_plag,plag_lines_size,plag_file_matches) =>
      var inception_counter=0
      var times_found=0
      var word_found :Boolean=false
      for(plag_word <-line_arr){
        inception_counter+=1
        if(plag_word==word){
          //println("Found word:"+word)
          times_found+=1
          word_found=true
          plag_file_matches.addBinding(word,counter_plag+inception_counter)
        }
      }

      val source_word :Map[String,Int]=if(word_found==true) Map().+(word -> counter_source) else Map()
      if(!plag_file_matches.isEmpty){
        //println(sender())
        context.actorSelection("/user/plag_analysis/fragmentation").!(returned_Multimaps(plag_file_matches,source_word,times_found,plag_lines_size))
      }

    case "I Got Terminated" =>
      context.actorSelection("/user/plag_analysis/fragmentation").!("End of Source File")

    case _ =>
      println("Wrong data sent from the source or pontentially palgiarised file!")

  }

}

class Fragmentation2 extends Actor with ActorLogging {

  var router ={
    val workerCount=Runtime.getRuntime().availableProcessors()
    val routees=Vector.fill(workerCount*2){
      val frag3=context.actorOf(Props[Fragmentation3])
      context watch frag3
      ActorRefRoutee(frag3)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }
  //val frag3=context.actorOf(Props[Fragmentation3])
  def receive ={
    case calculate_wks(plag_file_matches,source_file_matches) =>
      var start :Int = -1
      for(key1 <- source_file_matches.iterator){     ///gia kathe koinh leksh tou source file
        for(position <- key1._2.iterator ) {        //gia kathe thesh pou vrethike h koinh leksh sto source file
          for (plag_iter <- plag_file_matches.iterator if (plag_iter._1 == key1._1)) {
            find_first_seq(position, plag_iter, plag_file_matches, source_file_matches) //briskoume thn prwth emfanish ths sto plag_file
          } //kai ekteloume th synarthsh pou tha mas dwsei thn prwth koinh akolouthia
        }
      }
      router.route(Broadcast("Terminate FR Calcs"), sender())
    case _ => println("Nothing Happened!")
  }

  def find_first_seq(source_key_pos:Int,plag_iter :(String,mutable.Set[Int]),plag_file_matches :HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int],source_file_matches :HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]):Unit ={
    var  start :Int= 0    //epilegoume opoiadhpote apo tis emfaniseis sto keimeno mias kai de mas endiaferei to pou emfanistike
    var  end :Int= 0
    var seq_start :String= new String()
    //for(iter <- plag_file_matches.iterator if(iter._1 == plag_iter._1)) {
      seq_start = plag_iter._1
    for (pos_found <- plag_iter._2.iterator){     //h thesi sthn opoia vrethike h koinh leksh metaksy source kai plagiarised document (sto plagiarised document)
      start = pos_found
      end = pos_found
      //println(pos_found +" and "+iter._1)
      router.route(calculate_wks2(start,end,seq_start,plag_file_matches,source_key_pos,source_file_matches),sender())   //stelnoume tis lekseis se routees gia thn pio grhgorh dhmiourgia twn koinwn string metaksy twn dyo keimenwn
    }
   // }
  }

}

class Fragmentation3 extends Actor with ActorLogging {
  def receive ={
    case calculate_wks2(start,end,seq_start,plag_file_matches,source_key_pos,source_file_matches) =>
      //println(seq_start)
      var start_end_fltr :Array[Int]= new Array[Int](6)
      start_end_fltr(0)=start  //shmeio pou vrethike h arxh tou koinou string sto plag_file
      start_end_fltr(1)=end   //shmeio pou vrethike to telos tou koinou string sto plag_file
      start_end_fltr(2)=1    //synthiki termatismou ths while (einai 1 otan ektelestike ena apo ta 2 if sth for ths synrathshs pou kalitai)
      start_end_fltr(3)=1    //arithmos leksewn tou koinou string
      start_end_fltr(4)=source_key_pos   //shmeio pou vrethike h arxh tou koinou string sto source_file (start)
      start_end_fltr(5)=source_key_pos  //shmeio pou vrethike to telos tou koinou string sto source_file (end)
      var seq_str :String=seq_start
      while(start_end_fltr(2)==1){
        start_end_fltr(2)=0
         val tup_le = find_whole_seq(start_end_fltr,seq_str,plag_file_matches,source_file_matches)
         seq_str=tup_le._1
         //println("start:"+start_end_fltr(0)+"\t end:"+ start_end_fltr(1))
         start_end_fltr(2)=tup_le._2(2)
      }
      //println(seq_str+" Gia to word "+seq_start+" sto source file pou vrisketai sth thesh "+source_key_pos+" kai sygrithike me to idio word sto plag file sth thesh "+start)
      context.actorSelection("/user/plag_analysis/fragmentation").!(frag_calculate(seq_str,start_end_fltr))

    case "Terminate FR Calcs" =>
      context.actorSelection("/user/plag_analysis/fragmentation").!("FR Calcs Routees Terminated")

  }

  def find_whole_seq(start_end_fltr :Array[Int] ,seq_str :String ,plag_file_matches :HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int],source_file_matches :HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]):(String,Array[Int]) = {
    //var seq_str: String = new String()
    var seq_str2 :String=new String()
    breakable{
      for (iter <- plag_file_matches.iterator) {
       // println(start+" "+iter._2)
        if (iter._2.contains(start_end_fltr(0) - 1) && source_file_matches.entryExists(iter._1, _==(start_end_fltr(4)-1)) ) {
          start_end_fltr(0) -= 1
          start_end_fltr(2)=1
          start_end_fltr(3)+=1
          start_end_fltr(4) -= 1
          seq_str2 = iter._1 + " " + seq_str
          break()
        }
        if (iter._2.contains(start_end_fltr(1) + 1) && source_file_matches.entryExists(iter._1, _==(start_end_fltr(5) +1)) ) {
          seq_str2 = seq_str + " " + iter._1
          start_end_fltr(1)+= 1
          start_end_fltr(2)=1
          start_end_fltr(3)+=1
          start_end_fltr(5)+=1
          break()
        }
      }
    }
    if(seq_str2.isEmpty()){
      seq_str2=seq_str
    }
    //println(seq_str2)
    return(seq_str2,start_end_fltr)
  }
}


class Relevance extends Actor {

  def receive ={
    case calculate_features(wk_Arr_Dr,wk_Arr_Ds,fi_frg,seq_conc) =>
      var relevance_map :Map[String,Float]=Map()
      var Relevance_of_Sequnces :Array[Int]= Array.empty
      var counter :Int =0
      var ginomeno :Float=1
      for(key2 <- seq_conc.keys){
        //println(key2)
        val wk_Arr_R :Map[String,Int]=occ_wk(key2.split(" +"),wk_Arr_Dr)  //to prwto orisma diagrafei ta kena sta keys tou cos_seq p.x.Map(the  -> 1, by  -> 5) diagrafei ta kena sto the kai to by
        val wk_Arr_S :Map[String,Int]=occ_wk(key2.split(" +"),wk_Arr_Ds)  //to prwto orisma diagrafei ta kena sta keys tou cos_seq p.x.Map(the  -> 1, by  -> 5) diagrafei ta kena sto the kai to by
        //println(wk_Arr_R+"\t and \t"+wk_Arr_R)

        val first_fraction :Float= (1/pow(2.71828,seq_conc.apply(key2)-1)).toFloat
        println(first_fraction)
        val array_source :Array[Int]=wk_Arr_R.values.toArray     //pinakas pou periexei twn arithmo emfanisewn kathe lekshs tou key2 (sequence) sto source file tou sygkekrimenou
        val array_plag :Array[Int]=wk_Arr_S.values.toArray      //pinakas pou periexei twn arithmo emfanisewn kathe lekshs tou key2 (sequence) sto plagiarised file tou sygkekrimenou
        //println(array_plag)
        for(k <- 0 to (key2.split(" +").length-1)){
          ginomeno=ginomeno*( 2.toFloat/(array_plag(k).toFloat + array_source(k).toFloat) )  //array_plag(k).toFloat
          //println("Ginomeno:"+ginomeno)
        }
        val second_fraction=ginomeno
        val result :Float=first_fraction*second_fraction
        relevance_map=relevance_map+(key2 -> result)
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
      //println("Fragmentation Features :"+fi_frg)
      //println("RELEVANCE features MAP:"+relevance_features)

      val filepath=new File(".").getAbsolutePath().dropRight(1)+"data/FragRelev.txt"
      val nerfile = new PrintWriter(new File(filepath))
      nerfile.write("Fragmentation Features :"+fi_frg+" RELEVANCE features MAP:"+relevance_features)
      //nerfile.write("other"+"\tO\n")
      nerfile.close()
  }
  def occ_wk(key_Arr :Array[String],old_wk_Arr:Map[String,Int]): Map[String,Int] ={
    var wk_arr : Map[String,Int]= Map()
    var counter :Int=0
    var external_counter= 0   //p.x. by roadgood by roadgood by  de mporousan ta by na pane ksexwrista sto map
    for(key1 <- key_Arr){
      //println(key1)
      wk_arr=wk_arr.+(key1+"@"+external_counter -> old_wk_Arr.apply(key1))
      external_counter+=1
      counter=0
    }
    return(wk_arr)
  }

}

class InfoGain extends Actor{
  def receive ={
    case information_gain_evaluator(source_file,plag_file) =>
      val fixed_source_file :List[String]=for(key <- source_file)yield key.substring(0,key.lastIndexOf("@"))
      val fixed_plag_file :List[String]=for(key <- plag_file)yield key.substring(0,key.lastIndexOf("@"))

      //println(new File(".").getAbsolutePath())//NERModel.trainClassifier()
      val data_dir :File=new File(new File(".").getAbsolutePath().dropRight(1)+"data/")
      val xval_dir :File = new File(data_dir+"/xval/")

      if(!xval_dir.exists()){
        xval_dir.mkdirs()                   //create xval directory
      }
      else{
        println("Directory already exists and therefore not created")
      }
      val filepath=new File(".").getAbsolutePath().dropRight(1)+"data/ScalaNerFile.txt"
      val nerfile = new PrintWriter(new File(filepath))
      for(key <- fixed_source_file){
        nerfile.write(key+"\tplagiarised\n")
      }
      //nerfile.write("other"+"\tO\n")
      nerfile.close()
      NERModel.trainClassifier((new File(".").getAbsolutePath().dropRight(1)+"data/ScalaNerFile.txt").toString(), (new File(".").getAbsolutePath().dropRight(1)+"data/xval/NERModel.ser.gz").toString())
      val testInstance = new ApplyModel(new File(".").getAbsolutePath().dropRight(1)+"data/xval/NERModel.ser.gz")

      val filepath2=new File(".").getAbsolutePath().dropRight(1)+"data/trainingdata.txt"
      val nerfile2 = new PrintWriter(new File(filepath2))
      for(key <- fixed_plag_file){
        nerfile2.write(key+"\tplagiarised\n")
      }
      nerfile2.close()

      val testInstance2 = new CrossValidation(10, new File(".").getAbsolutePath().dropRight(1)+"data/trainingdata.txt")
      val xvalResults=testInstance2.runCrossValidation(new File(".").getAbsolutePath().dropRight(1)+"data/xval/")
      println(xvalResults)
    case _ => println("Nothing Happened!")

  }
}

