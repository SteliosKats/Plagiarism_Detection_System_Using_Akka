package sample.hello
import akka.actor._
import java.io._
import collection.JavaConversions._
import edu.stanford.nlp.pipeline.Annotation
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import java.util.Properties
import edu.stanford.nlp.ling.CoreAnnotations.{TokenBeginAnnotation, LemmaAnnotation, TokensAnnotation, SentencesAnnotation}

/**
 * Created by root on 3/3/15.
 */
class LineLemmaExtractor extends Actor with ActorLogging{
  def manOf[T: Manifest](t: T): Manifest[T] = manifest[T]
  def receive ={

    case routingmessages2(line, source_receiver_ref,filename,file_handler,file_lines_size) =>
      val err :PrintStream = System.err;
      System.setErr(new PrintStream(new OutputStream() {
        def write(b:Int):Unit ={
        }
      }))

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
