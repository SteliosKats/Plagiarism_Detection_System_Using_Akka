package sample.hello

import akka.actor.{Props, ActorLogging, Actor}
import akka.routing.{Broadcast, RoundRobinRoutingLogic, Router, ActorRefRoutee}

import scala.collection.mutable
import scala.collection.mutable.HashMap
import scala.util.control.Breaks._

/**
 * Created by root on 3/3/15.
 */
class FragFeaturesCalc extends Actor with ActorLogging {

  var router ={
    val workerCount=Runtime.getRuntime().availableProcessors()
    val routees=Vector.fill(workerCount*2){
      val frag_inc=context.actorOf(Props[FragmentationCalcInception])
      context watch frag_inc
      ActorRefRoutee(frag_inc)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }
  //val frag3=context.actorOf(Props[Fragmentation3])
  def receive ={
    case calculate_wks(plag_file_matches,source_file_matches,plagfile_id) =>
      var start :Int = -1
      for(key1 <- source_file_matches.iterator){     ///gia kathe koinh leksh tou source file
        for(position <- key1._2.iterator ) {        //gia kathe thesh pou vrethike h koinh leksh sto source file
          for (plag_iter <- plag_file_matches.iterator if (plag_iter._1 == key1._1)) {
            find_first_seq(position, plag_iter, plag_file_matches, source_file_matches,plagfile_id) //briskoume thn prwth emfanish ths sto plag_file
          } //kai ekteloume th synarthsh pou tha mas dwsei thn prwth koinh akolouthia
        }
      }
      router.route(Broadcast(Terminate_FR_Calcs(source_file_matches,plag_file_matches,plagfile_id)), sender())


    case _ => println("Wrong Data Sent To FragFeaturesCalc Actor!")
  }

  def find_first_seq(source_key_pos:Int,plag_iter :(String,mutable.Set[Int]),plag_file_matches :HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int],source_file_matches :HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int],plagfile_id :Int):Unit ={
    var  start :Int= 0    //epilegoume opoiadhpote apo tis emfaniseis sto keimeno mias kai de mas endiaferei to pou emfanistike
    var  end :Int= 0
    var seq_start :String= new String()
    //for(iter <- plag_file_matches.iterator if(iter._1 == plag_iter._1)) {
    seq_start = plag_iter._1
    for (pos_found <- plag_iter._2.iterator){     //h thesi sthn opoia vrethike h koinh leksh metaksy source kai plagiarised document (sto plagiarised document)
      start = pos_found
      end = pos_found
      //println(pos_found +" and "+iter._1)
      router.route(calculate_wks2(start,end,seq_start,plag_file_matches,source_key_pos,source_file_matches,plagfile_id),sender())   //stelnoume tis lekseis se routees gia thn pio grhgorh dhmiourgia twn koinwn string metaksy twn dyo keimenwn
    }
    // }
  }

}

class FragmentationCalcInception extends Actor with ActorLogging {
  def receive ={
    case calculate_wks2(start,end,seq_start,plag_file_matches,source_key_pos,source_file_matches,plagfile_id) =>
      //println(seq_start)
      var start_end_fltr :Array[Int]= new Array[Int](7)
      start_end_fltr(0)=start  //shmeio pou vrethike h arxh tou koinou string sto plag_file
      start_end_fltr(1)=end   //shmeio pou vrethike to telos tou koinou string sto plag_file
      start_end_fltr(2)=1    //synthiki termatismou ths while (einai 1 otan ektelestike ena apo ta 2 if sth for ths synrathshs pou kalitai)
      start_end_fltr(3)=1    //arithmos leksewn tou koinou string
      start_end_fltr(4)=source_key_pos   //shmeio pou vrethike h arxh tou koinou string sto source_file (start)
      start_end_fltr(5)=source_key_pos  //shmeio pou vrethike to telos tou koinou string sto source_file (end)
      start_end_fltr(6)=plagfile_id
    var seq_str :String=seq_start
      while(start_end_fltr(2)==1){
        start_end_fltr(2)=0
        val tup_le = find_whole_seq(start_end_fltr,seq_str,plag_file_matches,source_file_matches)
        seq_str=tup_le._1
        //println("start:"+start_end_fltr(0)+"\t end:"+ start_end_fltr(1))
        start_end_fltr(2)=tup_le._2(2)
      }
      //println(seq_str+" Gia to word "+seq_start+" sto source file pou vrisketai sth thesh "+source_key_pos+" kai sygrithike me to idio word sto plag file sth thesh "+start)
      context.actorSelection("/user/plag_analysis/comparing_s_p/returned_matches/fragmentation_features").!(frag_calculate(seq_str,start_end_fltr))

    case Terminate_FR_Calcs(source_file_matches,plag_file_matches,plagfile_id) =>
      context.actorSelection("/user/plag_analysis/comparing_s_p/returned_matches/fragmentation_features").!(FR_Calcs_Routees_Terminated(source_file_matches,plag_file_matches,plagfile_id))

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
