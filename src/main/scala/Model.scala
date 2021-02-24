package ml.tools

// - traits ------------
import ml.dmlc.xgboost4j.LabeledPoint

// Boolean indicates categoric fields which will be hashed into a hashSize array
trait MLPoint {
  def fields(): Map[String, Boolean]
}
trait MLLabeledPoint extends MLPoint {
  def label(): String
  def hashSize(): Int
}
trait MLDataset extends MLLabeledPoint with Dataset {
}

trait Model extends MLLabeledPoint {
  def trained(): Boolean
  def train(lps: Iterator[LabeledPoint]): Unit
  def save(path: String): Unit
  def load(path: String): Boolean
  def predict(m: Map[String, String]): Float
}
