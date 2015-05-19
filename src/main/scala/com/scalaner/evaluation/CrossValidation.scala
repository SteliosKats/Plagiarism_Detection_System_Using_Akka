package com.scalaner.evaluation

import scala.io.Source
import java.io.PrintWriter
import com.scalaner.training.{NERModel, Performance}

case class TrainingExample(token: String, label: String)

class CrossValidation(numFolds: Int, dataLocation: String) {

  def importTrainingData: Vector[TrainingExample] = {

    var trainingData: Vector[TrainingExample] = Vector.empty

    val lineIter = Source.fromFile(dataLocation).getLines()

    while (lineIter.hasNext) {

      val trainingExample = lineIter.next().split("\t").toList

      trainingData :+= TrainingExample(trainingExample(0), trainingExample(1))
    }

    trainingData
  }

  def writeFolds(trainingData: Vector[TrainingExample], writeLocation: String) = {

    val foldSize = trainingData.size / numFolds
    val folds = trainingData.grouped(foldSize).take(numFolds).toVector

    for ((valSet, idx) <- folds.zipWithIndex) {

      val trainSet = folds.filter(x => x != valSet).flatten

      val pWriter1 = new PrintWriter(s"${writeLocation}/train_set_${idx}.txt")

      for (ex <- trainSet) {
        pWriter1.println(ex.token + "\t" + ex.label)
      }

      pWriter1.close()

      val pWriter2 = new PrintWriter(s"${writeLocation}/val_set_${idx}.txt")

      for (ex <- valSet) {
        pWriter2.println(ex.token + "\t" + ex.label + "\t")
      }

      pWriter2.close()
    }
  }

  def runCrossValidation(writeLocation: String = dataLocation, modelName: String = "NER_model")
  : Vector[Map[String, Performance]] = {

    var perfVector: Vector[Map[String, Performance]] = Vector.empty

    println("Importing training data...")
    val trainingData = importTrainingData

    println("Writing crossvalidation sets...")
    writeFolds(trainingData, writeLocation)

    for (i <- 0 to numFolds - 1) {
      println(s"Training Fold ${i + 1}")

      NERModel.trainClassifier(s"${writeLocation}/train_set_${i}.txt", s"${writeLocation}/${modelName}.ser.gz")
      perfVector :+= NERModel.testClassifier(s"${writeLocation}/val_set_${i}.txt",
        s"${writeLocation}/${modelName}.ser.gz")
    }

    perfVector
  }

}
