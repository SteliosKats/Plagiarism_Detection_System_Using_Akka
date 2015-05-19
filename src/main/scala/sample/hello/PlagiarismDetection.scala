package sample.hello

import java.io.File

/**
 * Created by root on 2/25/15.
 */
object PlagiarismDetection {
  def main(args: Array[String]): Unit = {
    val current_directory=new File("/root/Desktop/FileInd/")
    var source_str=readLine("Enter The Source File Name To Be Checked for Citation-based Plagiarism Detection:")
    while(!new File(current_directory+"/"+source_str).exists()){
      source_str=readLine("File Not found!Try Again with different file or check your spelling:")
    }
    println("Executing citation-based algorithms...")
    FileIndexer.ReadFiles(source_str,current_directory)

    println("Performing Lexical Analysis please wait...")
    LexicalAnalysis.ReadFiles2(source_str,current_directory)
  }

}
