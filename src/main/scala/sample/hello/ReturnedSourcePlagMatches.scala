package sample.hello

import akka.actor._
import akka.routing._
import scala.collection.mutable
import scala.io.Source
import collection.JavaConversions._
import collection.mutable.{HashMap,MultiMap,Map,Set}
import scala.math._
/**
 * Created by root on 3/5/15.
 */
class ReturnedSourcePlagMatches extends Actor {
  def manOf[T: Manifest](t: T): Manifest[T] = manifest[T]
  val frg_f=context.actorOf(Props[FragmentationFeatures], name= "fragmentation_features")
  val frgm_calc=context.actorOf(Props[FragFeaturesCalc], name= "fragmentation_calculations")
  var mixed_plag_file_matches =new HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]
  var mixed_source_file_matches =new HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]

  var plag_file_matches =new HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]
  var source_file_matches =new HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]
  var file_id_counter :Int=1
  var document_count_ids :Map [Int,Int]= Map()

  def receive ={

    case returned_Multimaps(plag_tuple,source_word,times_found,plagfile_id) =>
      if(!source_word.isEmpty){
        mixed_source_file_matches.addBinding(source_word.head._1+"@"+plagfile_id,source_word.head._2)
      }
      mixed_plag_file_matches.addBinding(plag_tuple._1+"@"+plagfile_id, plag_tuple._2)

    case End_Of_SourceFile(plagfile_id) =>
      if(document_count_ids.contains(plagfile_id)){
        document_count_ids=document_count_ids.+(plagfile_id -> (document_count_ids.apply(plagfile_id)+1))
      }
      else
        document_count_ids=document_count_ids.+(plagfile_id -> 1)

      //println(external_counter)
      if(document_count_ids.containsValue(256)){                      //p.x. Map(1 -> 256 , 2 ->154)
        val x =document_count_ids.find(x => x._2 == 256).get                //pairnoume to 1
        var new_source_file_matches=mixed_source_file_matches.filterKeys(z => z.substring(z.lastIndexOf("@")+1, z.length() ).==(x._1.toString())) // filtraroume gia na paroume ta keys pou exoun to idio id me to arxeio pou theloume na steiloume gia pereterw epeksergasia

        for(kv <- new_source_file_matches.iterator){
          for(value <- kv._2.seq){
            source_file_matches=source_file_matches.addBinding(kv._1.substring(0,kv._1.lastIndexOf("@")) , value)
          }
          //new_source_file_matches=new_source_file_matches.+(kv._1.substring(0,kv._1.lastIndexOf("@")) -> kv._2)
          new_source_file_matches=new_source_file_matches.-(kv._1)
        }

        var new_plag_file_matches =mixed_plag_file_matches.filterKeys(z => z.substring(z.lastIndexOf("@")+1, z.length() ).==(x._1.toString())) // filtraroume gia na paroume ta keys pou exoun to idio id me to arxeio pou theloume na steiloume gia pereterw epeksergasia

        for(kv <- new_plag_file_matches.iterator){
          for(value <- kv._2.seq){
            plag_file_matches=plag_file_matches.addBinding(kv._1.substring(0,kv._1.lastIndexOf("@")) , value)
          }
          //new_plag_file_matches=new_plag_file_matches.+(kv._1.substring(0,kv._1.lastIndexOf("@")) -> kv._2)
          new_plag_file_matches=new_plag_file_matches.-(kv._1)
        }

        println("\t plagiarized file matches: "+plag_file_matches+"\t source file matches: "+source_file_matches)
        document_count_ids=document_count_ids.-(x._1 , x._2)

        frgm_calc.!(calculate_wks(plag_file_matches,source_file_matches,plagfile_id))


        /////Mhdenismos twn arxikwpoihmenwn hashmaps kai external counter (gia na synexistei h sygkrish source me alla plag files)
        plag_file_matches=new HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]
        source_file_matches=new HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]
      }

    case _ =>
      println("Unexpected Error On Returning Matches Between Two Examined Files")


  }

}
