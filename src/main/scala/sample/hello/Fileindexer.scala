package sample.hello

import java.nio.channels.FileChannel
import akka.actor._
import akka.routing.RoundRobinRouter
import com.typesafe.config.ConfigFactory
import java.io._
import java.io.File
import scala.collection.immutable.ListMap
import scala.io.Source
import scala.util.control.Breaks._
import akka.routing.ActorRefRoutee
import akka.routing.Router
import akka.routing.RoundRobinRoutingLogic
import akka.routing.RoundRobinPool
import akka.actor.PoisonPill
import akka.routing.Broadcast
import akka.actor.Terminated


/**
 * Created by root on 9/30/14.
 */
case class file_properties(filename :File,fileid :Int, total_files: Int)
case class routingmessages(fileline :String, counter :Int,ref_act :ActorRef,fileid :Int)
case class return_references(reference_array :IndexedSeq[String], line_num :Int ,fileid: Int, poisoned_routees :Int)

case class Citation_Chunking(source_doc_refs :Map[String,Int], plag_doc_refs :Map[String,Int])
case class Greedy_Citation_Tiling(source_doc_refs :Map[String,Int],plag_doc_refs :Map[String,Int])

object FileIndexer{
  def main(args: Array[String]): Unit = {
    var path_filename=new File(" ")
    var fileid=0
    var tot_files=0
    val current_directory=new File("/root/Desktop/")
    val indexingSystem= ActorSystem("indexingsystem")//,ConfigFactory.load(application_is_remote))
    val actor_ref_file = indexingSystem.actorOf(Props[FileReceiver],"indexing")
    for(file <- current_directory.listFiles if file.getName endsWith ".txt"){
      tot_files+=1
    }
    for(file <- current_directory.listFiles if file.getName endsWith ".txt"){
      path_filename=new File(file.toString())
      //println(path_filename)
      fileid+=1

      actor_ref_file ! file_properties(path_filename,fileid,tot_files)
    }
  }
}

class FileReceiver extends Actor{
  var counter_terminated=0
  var all_refs :Map[String,Int]=Map()
  var file_counter=0
  /* Creating A router to route "Workers" to extract citations for each line of the file */
  var router ={
    val routees=Vector.fill(5){
      val lineseparator=context.actorOf(Props[LineSeparator])
      context watch lineseparator
      ActorRefRoutee(lineseparator)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }
  /* Creating A router to route "Workers" to ectract citations for each line of the file */

  def receive = {

    case file_properties(filename, fileid, total_files) =>
      var counter = 0
      file_counter+=1
      for (line <- Source.fromFile(filename).getLines()) {
        counter += 1
        router.route(routingmessages(line, counter, self, fileid), sender())
      }
      if (file_counter == total_files) {
        router.route(Broadcast(PoisonPill), sender())
      }
    case return_references (ref_array, line_num,fileid,poisoned_routees) =>

      /* to "," sto value tou Map ksexwrizei ton monadiko arithmo keimenou apo ton arithmo grammhs tou sygkrkrimenou keimenou */
      if(poisoned_routees!=5) {
        // Store in  Map[reference@line,exact_place -> file_id]  format
        val mapped_refs :Map[String, Int] = Map(ref_array map { s => (s, fileid)}: _*)
        all_refs=all_refs.++(mapped_refs)
        //println(all_refs)
      }
      else {
        /* list Map with alla extracted references by file_id */
        all_refs=ListMap(all_refs.toList.sortBy{_._2}:_*)
        val algo_router: ActorRef =context.actorOf(RoundRobinPool(5).props(Props[Algorithms_Execution]), "algorithms_router")
        context.watch(algo_router)
        //println(all_refs)
        val source_doc_refs :Map[String, Int]=all_refs.filter(_._2==1)
        //println(source_doc_refs)
        for (i <- 2 to all_refs.values.max){   //all_refs.max._2 giati oxi???
          val plag_doc_refs :Map[String, Int]=all_refs.filter(_._2==i)
          //println(plag_doc_refs)
          algo_router ! Citation_Chunking(source_doc_refs,plag_doc_refs)
          algo_router ! Greedy_Citation_Tiling(source_doc_refs,plag_doc_refs)

        }
      }
    case Terminated (corpse) =>
      router = router.removeRoutee(corpse)
      counter_terminated.+=(1)
      if(counter_terminated==5){
        self.!(return_references(Array(" "),0,0,5))
      }
    case _ =>
      println("I got nothing")

  }


}

class LineSeparator extends Actor with ActorLogging {
  def receive ={

    case routingmessages(line,counter,file_receiver_ref,fileid) =>
      val references_ar=for( i <-0 to (line.length()-1) if(line.charAt(i) == '[') )yield i
      val references_de=for( i <-0 to (line.length()-1) if(line.charAt(i) == ']') )yield i

      //Ksexwrizoume ton arithmo ths grammhs pou vrethike h anafora me to mhkos twn dyadikwn pshfiwn  tou arithmou ths grammhs me ton xarakthra "@"
      // evala sto telos kathe string to id=file_id gia argotera pou de mporei to all_refs map na prosthesei idia keys me diaforetika values alla ta antimetwpizei ws updates
      if (!references_ar.isEmpty || !references_de.isEmpty){
        if(references_ar.length==1 && references_de.isEmpty ) {
          val new_reference_array=IndexedSeq[String] (line.substring(references_ar(references_ar.length -1),line.length())+"@"+counter.toString()+"."+references_ar(0).toString()+"&id="+fileid.toString())
          //println(new_reference_array)
          file_receiver_ref.!(return_references(new_reference_array,counter,fileid,0))
        }
        else if(references_de.length==1 && references_ar.isEmpty){
          val new_reference_array= IndexedSeq[String] (line.substring(0,references_de(references_de.length -1)+1)+"@"+counter.toString()+"."+references_de(0).toString()+"&id="+fileid.toString())
          //println(new_reference_array)
          file_receiver_ref.!(return_references(new_reference_array,counter,fileid,0))
        }
        else if(references_ar.length.>(references_de.length) && !references_de.isEmpty){
          val reference_array=for(i <- 0 to (references_ar.length -2) )yield line.substring(references_ar(i),references_de(i)+1)+"@"+counter.toString()+"."+references_ar(i).toString()+"&id="+fileid.toString()
          val new_reference_array=reference_array.++((line.substring(references_ar(references_ar.length -1),line.length())+"@"+counter.toString()+"."+references_ar(references_ar.length -1).toString()+"&id="+fileid.toString()).split("[\r\n]+"))
          //println(new_reference_array)
          file_receiver_ref.!(return_references(new_reference_array,counter,fileid,0))
        }
        else if(references_ar.length.<(references_de.length)) {
          var reference_array=for( i <- 0 to (references_de.length -2) )yield line.substring(references_ar(i),references_de(i+1)+1)+"@"+counter.toString()+"."+references_de(i).toString()+"&id="+fileid.toString()
          val new_reference_array=reference_array.++((line.substring(0,references_de(0)+1)+"@"+counter.toString()+"."+(references_de(0) -1).toString()+"&id="+fileid.toString()).split("[\r\n]+")) //+"&"+counter.toString().length()
          //println(new_reference_array)
          file_receiver_ref.!(return_references(new_reference_array,counter,fileid,0))
        }
        else{
          val new_reference_array=for(i <- 0 to (references_ar.length-1) )yield line.substring(references_ar(i),references_de(i)+1)+"@"+counter.toString()+"."+references_ar(i).toString()+"&id="+fileid.toString()
          //println(new_reference_array)
          file_receiver_ref.!(return_references(new_reference_array,counter,fileid,0))
        }

      }

    case PoisonPill =>
      context.stop(self)

    case _ =>
      println("No line received")

  }

}

class Algorithms_Execution extends Actor with ActorLogging{

  def receive ={

    case Citation_Chunking(source_doc_refs, plag_doc_refs) =>
      val processed_source_doc_refs=MapProcessing(source_doc_refs)
      val processed_plag_doc_refs=MapProcessing(plag_doc_refs)

      val citation_chunked_source_doc_refs :Map[String,Int]=CitationChinkingAlgorithm(processed_source_doc_refs,processed_plag_doc_refs)
      val citation_chunked_plag_doc_refs :Map[String,Int]=CitationChinkingAlgorithm(processed_plag_doc_refs,processed_source_doc_refs)
      //println(citation_chunked_source_doc_refs)
      //println(citation_chunked_plag_doc_refs)

      val chunked_document_matches=ChunkPairMatchingCC(citation_chunked_source_doc_refs,citation_chunked_plag_doc_refs)
      //println(chunked_document_matches)

    case Greedy_Citation_Tiling(source_doc_refs, plag_doc_refs) =>
      val processed_source_doc_refs :Map[String,Float]=MapProcessing(source_doc_refs)
      val processed_plag_doc_refs :Map[String,Float]=MapProcessing(plag_doc_refs)
      val tiled=GCTAlgorithm(processed_source_doc_refs,processed_plag_doc_refs)

  }

  def MapProcessing (mapped_doc_refs :Map[String,Int]): Map[String,Float] ={

    val doc_refs=for(key <- mapped_doc_refs.seq) yield (key._1.dropRight(4+key._2.toString().length()) -> key._2) //afairoume to &id=file_id apo to telos tou key String tou map
    //println(doc_refs)
    var new_source_doc_refs :Map[String,Float] =(for(key <- doc_refs.keys) yield (key.substring(0,key.lastIndexOf("@")) -> key.substring(key.lastIndexOf("@")+1,key.length()).toFloat ) ).toMap

    new_source_doc_refs=ListMap(new_source_doc_refs.toList.sortBy(_._2):_*)
    var concat_row :Float= -1
    var concat_ref=" "
    //println(new_source_doc_refs)
    for(key <- new_source_doc_refs.seq if(!key._1.startsWith("[") || !key._1.endsWith("]")) ){
      if(!key._1.endsWith("]")){
        concat_row=key._2
        concat_ref=key._1
      }
      if(!key._1.startsWith("[")){                                                     //concat_row ==(key._2 +1) &&
        new_source_doc_refs=new_source_doc_refs.+(concat_ref+key._1 -> concat_row)
      }
      new_source_doc_refs=new_source_doc_refs.-(key._1)
    }

    var value_length1= -1
    var max_length= -1
    for(value <- new_source_doc_refs.values) {
      val new_value=value.toString().substring(value.toString().lastIndexOf("."),value.toString().length()-1)
      value_length1=new_value.length()
      if(value_length1 > max_length){
        max_length=value_length1
      }
    }

    for(key <- new_source_doc_refs.seq) {
      val after_comma_str=key._2.toString().substring(key._2.toString().lastIndexOf(".")+1,key._2.toString().length())
      val after_comma=after_comma_str.length()
      val pre_comma=key._2.toString().substring(0,key._2.toString().indexOf("."))
      if(after_comma < max_length) {
        val float_value = (pre_comma+"."+("0"*(max_length-after_comma)+after_comma_str)).toFloat
        //println(float_value.toFloat)
        new_source_doc_refs=new_source_doc_refs.-(key._1)
        new_source_doc_refs=new_source_doc_refs.+(key._1 -> float_value.toFloat)
      }
    }

    return(ListMap(new_source_doc_refs.toList.sortBy(_._2):_*))

  }

  def CitationChinkingAlgorithm(processed_source_doc_refs :Map[String,Float],processed_plag_doc_refs :Map[String,Float]):Map[String,Int] ={
    /*   -----------------------------------------------------------------------------------------------------------------------------  */
    /*                                                                                                                                  */
    /*                                           This Function implements the Citation Chunking (CC) Algorithm                          */
    /*                                                                                                                                  */
    /*    ------------------------------------------------------------------------------------------------------------------------------*/
    val source_matching_citations= (processed_source_doc_refs.keySet.--((processed_source_doc_refs.keySet.--(processed_plag_doc_refs.keySet))))
    val plag_matching_citations= (processed_plag_doc_refs.keySet.--((processed_plag_doc_refs.keySet.--(processed_source_doc_refs.keySet))))
    //println(source_matching_citations)
    //println(plag_matching_citations)
    var counted_non_matched=0   // counts the non matched citations between two documents and marks them as X
    var current_ref_pointer=0   // points the element on the map where the next search for matching citation should start
    var for_counter=0   // a counter for the inner for in order to skip preceding elements already encountered in previous fors in order to search from the current_ref_pointer and after
    var matched_key=new String()    //The string that will be stored as key element in the Citation Chunking map
    var mapped_cc :Map[String,Int]=Map()
    for( plag_key1 <- processed_source_doc_refs.seq if(source_matching_citations.contains(plag_key1._1))){
    var found :Boolean=false
      //println(plag_key1._1)
      for_counter=0
      for(plag_key2 <- processed_source_doc_refs.seq if(found!=true)){
        //println(current_ref_pointer+"\t"+plag_key1._1+"\t"+plag_key2._1+"\t NonMatched:"+counted_non_matched)
        if(current_ref_pointer!=for_counter){
          for_counter+=1
        }
        else {
          if (plag_key1._1 != plag_key2._1) {
            //non matching citations (X)
            counted_non_matched += 1
            //matched_key = new String()
          }
          else if (plag_key1._1 == plag_key2._1 && mapped_cc.isEmpty) {
            //An vriskoume matching citation kai einai to prwto pou vriskoume
            current_ref_pointer +=1
            counted_non_matched = 0
            mapped_cc = mapped_cc.+(plag_key1._1 -> 1)
            matched_key=plag_key1._1
            found = true
          }
          else if (plag_key1._1 == plag_key2._1 && !mapped_cc.isEmpty && (mapped_cc.last._2 >= counted_non_matched) && found!=true) {
            matched_key = matched_key + ","+"X,"*counted_non_matched + plag_key1._1
            val cc_chunk = matched_key
            val cc_number_of_matched = mapped_cc.last._2 + 1
            mapped_cc = mapped_cc.-(mapped_cc.last._1)
            mapped_cc = mapped_cc.+(cc_chunk -> cc_number_of_matched)
            current_ref_pointer = current_ref_pointer + 1 + counted_non_matched //start the for from the last point we encountered matching citation
            counted_non_matched = 0
            found = true
          }
          else if(plag_key1._1==plag_key2._1 && !mapped_cc.isEmpty && (mapped_cc.last._2 < counted_non_matched) && found!=true){
            matched_key=new String()
            matched_key = plag_key1._1
            val cc_chunk = matched_key
            val cc_number_of_matched = 1
            mapped_cc = mapped_cc.+(cc_chunk -> cc_number_of_matched)
            current_ref_pointer = current_ref_pointer + 1 + counted_non_matched
            counted_non_matched=0
            found = true
          }
        }
      }
    }
    return (mapped_cc)
  }

  def ChunkPairMatchingCC(map1 :Map[String,Int],map2 :Map[String,Int]):Map[String,Int] ={
    /*   -----------------------------------------------------------------------------------------------------------------------------  */
    /*                                                                                                                                  */
    /*                         This Function does the matcing on the chuncks found by the CitationChinkingAlgorithm                     */
    /*                                                                                                                                  */
    /*    ------------------------------------------------------------------------------------------------------------------------------*/
    var matched_pairs :Map[String,Int]=Map()
    for (key1 <- map1.seq){
      val array_keys1 :Array[String]=key1._1.replaceAll(",X","").split(",")
      var maxi=0
      var matched_plag_key :String=new String()
      var counter=0
      var previous_key=new String()
      for(key2 <- map2.seq) {
        var i :Int=0
        for(array_key1 <- array_keys1.toSeq) {
          val array_keys2: Array[String] = key2._1.replaceAll(",X", "").split(",")
          array_keys2.foreach(arraykey2 => if (arraykey2 == array_key1) {i = i + 1 })//})
          //println(array_key1)
        }
        if (i > maxi && counter == 0) {
          maxi = i
          matched_pairs = matched_pairs.+(key1._1 + "\t-\t" + key2._1 -> i)
          counter += 1
          previous_key=key2._1
          //println(matched_pairs)
        }
        else if (i > maxi && counter > 0) {
          maxi = i
          matched_pairs = matched_pairs.-(key1._1 + "\t-\t" + previous_key)
          matched_pairs = matched_pairs.+(key1._1 + "\t-\t" + key2._1 -> i)
          previous_key=key2._1
          //println(matched_pairs)
        }
        else if(i==maxi && counter>0){
          matched_pairs = matched_pairs.updated(key1._1 + "\t-\t" +previous_key+"\t-\t"+key2._1,i)
          matched_pairs = matched_pairs.-(key1._1 + "\t-\t" +previous_key)
          previous_key=previous_key+"\t-\t"+key2._1
          //println("ok"+matched_pairs)
        }
      }

    }
    return(matched_pairs)
  }

  def GCTAlgorithm(map1 :Map[String,Float],map2 :Map[String,Float]):List[String] ={
    /*   -----------------------------------------------------------------------------------------------------------------------------  */
    /*                                                                                                                                  */
    /*                                           This Function implements the Greedy Citation Tiling Algorithm  (Still Working on it)   */
    /*                                                                                                                                  */
    /*    ------------------------------------------------------------------------------------------------------------------------------*/
    val in1=map1.keys.toList.inits.toList.reverse
    val in2=map2.keys.toList.inits.toList.reverse
    println(in2)
    var counter_external :Int=0
    var counter_internal :Int=0
    var inception_counter :Int=0
    var tile_length=0
    var longest_cit_patt :List [String]=List()
    var skip_elems :Int=0

    for(elem1 <- in1.seq.tail) {
      counter_external += 1
      inception_counter = counter_external
      if(skip_elems!=0){
        skip_elems=skip_elems - 1
      }
      else{
      breakable {
        for (elem2 <- in2.seq.tail) {
          skip_elems=0
          //println(elem2.apply(counter_internal))
          counter_internal += 1
          if (elem1.apply(counter_external - 1) == (elem2.apply(counter_internal - 1))) {
            //println(elem1.apply(counter_external-1)+","+elem2.apply(counter_internal-1))
            tile_length += 1
            breakable {
              for (i <- counter_internal to (in1.tail.size)) {
                if (inception_counter < in1.tail.size) {
                  inception_counter += 1
                }
                println(inception_counter + "\t" + in1.tail.size)
                //println(in1.tail.apply(inception_counter-1).lastOption+"\t"+in2.tail.apply(i))
                if (in1.tail.apply(inception_counter - 1).lastOption == in2.tail.apply(i).lastOption) {
                  //println(in1.tail.apply(inception_counter - 1).lastOption + "\t" + in2.tail.apply(i).lastOption)
                  skip_elems+=1
                  tile_length += 1
                }
                else {
                  longest_cit_patt = longest_cit_patt.+:(counter_external + "," + counter_internal + "," + tile_length)
                  println(longest_cit_patt)
                  tile_length = 0
                  break()
                }
              }
            }
          println("ok")
          break()
          }

        }
      }
      }
      counter_internal=0
    }
    return(longest_cit_patt)
  }
}
