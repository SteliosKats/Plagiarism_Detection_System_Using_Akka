package sample.hello


import java.io.File

import akka.actor._
import collection.mutable.{HashMap,MultiMap}
import scala.collection.immutable.Map
import com.norbitltd.spoiwo.natures.xlsx.Model2XlsxConversions._
import com.norbitltd.spoiwo.model._
import com.norbitltd.spoiwo.model.enums.CellFill
import org.joda.time.LocalDate
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import org.apache.poi.xssf.usermodel._



/**
 * Created by root on 8/1/15.
 */
class XlsFileWrapper extends Actor with ActorLogging{
   override def preStart() = {
     val final_results_dir :File=new File(new File(".").getAbsolutePath().dropRight(1)+"PLAGIARISATION_RESULTS/")
     if(!final_results_dir.exists()){
       final_results_dir.mkdir()
     }
   }

  var chunked_matches :Map[String,Int]= Map()
  var lccs_str=""
  var ids_files :Map[Int,String]= Map()
  var source_filenames_ids :Map[String,Int] =Map()
  var plag_filename_ids :Map[String,Int] =Map()
  var times_entered :Int =0

  def receive ={

    case send_filenames_with_ids(source_file, plag_file,plagfile_id) =>
      if(!source_filenames_ids.isDefinedAt(source_file)) {
        times_entered+=1
        source_filenames_ids = source_filenames_ids + (source_file -> times_entered)
      }
      if(!plag_filename_ids.isDefinedAt(plag_file)){
        plag_filename_ids=plag_filename_ids.+(plag_file -> (plagfile_id-1))
      }


    case send_citation_results(chunk_match ,lccs_string,source_plag_filenames)=>
       chunked_matches=chunk_match
       lccs_str=lccs_string
       ids_files=source_plag_filenames
      //println(chunked_matches)
      val headerStyle = CellStyle(fillPattern = CellFill.Solid, fillForegroundColor = Color.AquaMarine, fillBackgroundColor = Color.AquaMarine, font = Font(bold = true))
      val current_directory=new File(new File(".").getAbsolutePath().dropRight(1)+"PLAGIARISATION_RESULTS/")
      for(id <- 2 to ids_files.keys.max){
           val result_file_name :File= new File(ids_files.apply(1).substring(0, ids_files.apply(1).lastIndexOf('.'))+"-"+ids_files.apply(id).substring(0, ids_files.apply(id).lastIndexOf('.'))+".xlsx")
           val pathWfilename :File =new File(current_directory+"/"+result_file_name)
           //if(ids_files.apply(id).endsWith(".txt") && !pathWfilename.exists() ) {
             //.saveAsXlsx(current_directory+"/"+result_file_name.toString())
             var rNum = 0
             var cNum = 0
             val file = new File(current_directory + "/" + result_file_name.toString())
             val fileOut = new FileOutputStream(file);
             val wb = new XSSFWorkbook
             val sheet = wb.createSheet("Sheet1")
             val row = sheet.createRow(rNum)
             row.createCell(cNum).setCellValue("Source File Name") //topothetoume to name tou source file sto keli
             row.createCell(cNum + 1).setCellValue("Examined File Name") //topothetoume to name tou source file sto keli
             row.createCell(cNum + 2).setCellValue("Chunked Matches")
             row.createCell(cNum + 3).setCellValue("Chuncked Matches Length")
             row.createCell(cNum + 4).setCellValue("Longest Common Citation Sequence(LCCS)") ///de thelei create row se kathe grammh giati svhnei thn prohgoumenh
             row.createCell(cNum + 5).setCellValue("Fragmentation Features (String  Length)")
             row.createCell(cNum + 6).setCellValue("Fragmentation Features (Value)")
             row.createCell(cNum + 7).setCellValue("Relevance Features (String  Length)")
             row.createCell(cNum + 8).setCellValue("Relevance Features (Value)")
             row.createCell(cNum + 9).setCellValue("Average F1-Measure")
             row.createCell(cNum + 10).setCellValue("Plagiarism Classification (with Tf-idf)")

             rNum += 1
             var i: Int = 0
             if (!chunked_matches.isEmpty){
               for (kv <- chunked_matches.iterator) {
                 val row3 = sheet.createRow(rNum)
                 i += 1
                 if (i == 1) {
                   row3.createCell(cNum).setCellValue(ids_files.apply(1))
                   row3.createCell(cNum + 1).setCellValue(ids_files.apply(2))
                   row3.createCell(cNum + 4).setCellValue(lccs_str)
                   row3.createCell(cNum + 5).setCellValue("-")
                   row3.createCell(cNum + 6).setCellValue("-")
                   row3.createCell(cNum + 7).setCellValue("-")
                   row3.createCell(cNum + 8).setCellValue("-")
                   row3.createCell(cNum + 9).setCellValue("-")
                   row3.createCell(cNum + 10).setCellValue("-")
                 }
                 else{
                   row3.createCell(cNum + 5).setCellValue("")
                   row3.createCell(cNum + 6).setCellValue("")
                   row3.createCell(cNum + 7).setCellValue("")
                   row3.createCell(cNum + 8).setCellValue("")
                   row3.createCell(cNum + 9).setCellValue("")
                   row3.createCell(cNum + 10).setCellValue("")
                 }
                 row3.createCell(cNum + 3).setCellValue(kv._2)
                 row3.createCell(cNum + 2).setCellValue(kv._1)
                 rNum += 1
               }
             }
             else{
               val row3 = sheet.createRow(rNum)
               row3.createCell(cNum).setCellValue(ids_files.apply(1))
               row3.createCell(cNum + 1).setCellValue(ids_files.apply(2))
               row3.createCell(cNum + 2).setCellValue("No Matches found between Source Document And Suspicious Document")
               row3.createCell(cNum + 3).setCellValue("-")
               row3.createCell(cNum + 5).setCellValue("-")
               row3.createCell(cNum + 6).setCellValue("-")
               row3.createCell(cNum + 7).setCellValue("-")
               row3.createCell(cNum + 8).setCellValue("-")
               row3.createCell(cNum + 9).setCellValue("-")
               row3.createCell(cNum + 10).setCellValue("-")
               if(lccs_str.isEmpty)
                 row3.createCell(cNum + 4).setCellValue("No LCCS String found between Source Document And Suspicious Document")
               else
                 row3.createCell(cNum + 4).setCellValue(lccs_str)
             }
             //synthikes gia keno lccs kai keno chincked matches (pou de tha vgazei kai ta onomata twn arxeiwn an einai keno)
             for(i <- 0 to 10){
               sheet.autoSizeColumn(i)
             }
             wb.write(fileOut);
             fileOut.close();
           //}
       }

    case send_lexical_results(frg_y ,rel_y,s_set,avg_weighted_F_measure) =>
      val current_directory=new File(new File(".").getAbsolutePath().dropRight(1)+"PLAGIARISATION_RESULTS/")
      //println("Source ids:"+source_filenames_ids)
      //println("Plag ids:"+plag_filename_ids)
      //println("Frg Feaatures: "+frg_y)
      //println("Rel Features: "+rel_y)
      //println("Average F-Measure :"+avg_weighted_F_measure)
      for(source <- source_filenames_ids.keys){
        for(plag <-plag_filename_ids.keys){
          val result_file_name :File= new File(source.substring(0, source.lastIndexOf('.'))+"-"+plag.substring(0, plag.lastIndexOf('.'))+".xlsx")
          val pathWfilename :File =new File(current_directory+"/"+result_file_name)
          if(pathWfilename.exists()){
            var rNum=1
            var cNum=5
            val file = new File(current_directory + "/" + result_file_name.toString())
            val fileis = new FileInputStream(file);
            val wb = new XSSFWorkbook(fileis)
            val first_sheet =wb.getSheet("Sheet1")
            var  counter :Int =0
            var found_once :Boolean=false
            for(kv <- frg_y.iterator ){
              for(value <- kv._2.iterator  if(value.substring(value.lastIndexOf(",")-1,value.length()) == source_filenames_ids.apply(source)+","+plag_filename_ids.apply(plag) ) ){
                //println(source+" and "+plag)
                val row = first_sheet.getRow(rNum)
                counter +=1
                if(counter==1){
                  row.getCell(cNum+4).setCellValue(avg_weighted_F_measure) //average F-Measure
                  row.getCell(cNum+5).setCellValue(value.substring(value.indexOf("@")+1,value.indexOf(",")))  //Tf-IDF Classification
                  found_once=true
                }
                if(row ==null){
                  val row2 = first_sheet.createRow(rNum)
                  row2.createCell(cNum).setCellValue(kv._1) //fragmentation key
                  row2.createCell(cNum+1).setCellValue(value.substring(0,value.indexOf("@"))) //fragmentation value
                  row2.createCell(cNum+2).setCellValue("")
                  row2.createCell(cNum+3).setCellValue("")
                }
                else{
                  row.getCell(cNum).setCellValue(kv._1) //fragmentation key
                  row.getCell(cNum+1).setCellValue(value.substring(0,value.indexOf("@"))) //fragmentation value
                  row.getCell(cNum+2).setCellValue("")
                  row.getCell(cNum+3).setCellValue("")
                }
                rNum+=1
              }
            }
            rNum=1
            cNum=5
            counter=0
            for(kv <- rel_y.iterator){
              for(value <- kv._2.iterator  if(value.substring(value.lastIndexOf(",")-1,value.length()) == source_filenames_ids.apply(source)+","+plag_filename_ids.apply(plag) ) ){
                val row = first_sheet.getRow(rNum)
                counter +=1
                if(counter==1 && found_once==false){
                  row.getCell(cNum+4).setCellValue(avg_weighted_F_measure) //average F-Measure
                  row.getCell(cNum+5).setCellValue(value.substring(value.indexOf("@")+1,value.indexOf(",")))  //Tf-IDF Classification
                }
                if(row ==null){
                  val row2 = first_sheet.createRow(rNum)
                  row2.createCell(cNum+2).setCellValue(kv._1) //fragmentation key
                  row2.createCell(cNum+3).setCellValue(value.substring(0,value.indexOf("@"))) //fragmentation value
                }
                else {
                  row.getCell(cNum+2).setCellValue(kv._1) //relevance key
                  row.getCell(cNum+3).setCellValue(value.substring(0, value.indexOf("@"))) //relevance value
                }
                rNum+=1
              }
            }

            fileis.close()
            val fileos = new FileOutputStream(file);
            wb.write(fileos);
            fileos.close()
          }

        }
      }

    println("Program Ended Succesfully! Results saved on \'PLAGIARISATION_RESULTS\' folder")
    println("Closing System Now")
    //context.system.shutdown()

    case _=>
      println("Wrong type of message sent to XlsFileWrapper Actor")

  }

}
