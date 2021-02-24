package ml.tools

import org.scalatest.flatspec.AnyFlatSpec
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers

import ml.dmlc.xgboost4j.LabeledPoint

import io.circe.generic.auto._, io.circe.syntax._

class HashTranslateTest extends AnyFlatSpec with Matchers with MockFactory {

  def fixture = new {
    val sfields = Map("id" -> true, "gender" -> true, "height" -> false, "weight" -> false)
    val srows = Seq(
      Map("id" -> "1", "gender" -> "male", "height" -> "69", "weight" -> "165"),
      Map("id" -> "2", "gender" -> "female", "height" -> "63", "weight" -> "105"),
      Map("id" -> "3", "gender" -> "male", "height" -> "70", "weight" -> "160"),
      Map("id" -> "4", "gender" -> "female", "height" -> "64", "weight" -> "125"),
      Map("id" -> "5", "gender" -> "male", "height" -> "70", "weight" -> "180"))
    val fakePoint = stub[MLPoint]
    val fakeDataset = stub[Dataset]
    (fakePoint.fields _) when () returns (sfields)
    (fakeDataset.rows _) when () returns (srows.iterator)

    val allFields = fakePoint.fields()
    val label = "gender"
    val hashSize = 10
    val encoder = new SimpleEncoder(hashSize)

    val specialFields = Set(label, "weight")
    val fields = allFields.filterNot{ case (k, _) ⇒ specialFields.contains(k) }
    val rows = fakeDataset.rows().toSeq
    val row = rows.head
  }

  "labeledPoints()" should "encode BQ rows to a LabeledPoint of the correct length" in {
    val f = fixture

    val lp = HashTranslate.toLabeledPoint(f.row, f.allFields, f.label, f.encoder)

    val encodedSize = lp.size
    val numericFields = f.fields.filterNot(_._2).size
    val expectedSize = numericFields + f.hashSize + 1
    encodedSize shouldBe expectedSize
  }

  "toLabeledPoint" should "encode a row to the values expected" in {
    val f = fixture

    val expected = """{"label":0.0,"size":12,"indices":[1,9],"values":[69.0,1.0],"weight":165.0,"group":-1,"baseMargin":null}"""

    val lp = HashTranslate.toLabeledPoint(f.row, f.allFields, f.label, f.encoder)

    lp.asJson.noSpaces shouldBe expected
  }

  "converting LabeledPoint to SVM-row and back " should "result in the  original LabeledPoint" in {
    val f = fixture

    val lp = HashTranslate.toLabeledPoint(f.row, f.allFields, f.label, f.encoder)
    val svm = HashTranslate.lpToSvm(lp)
    val newLp = HashTranslate.svmToLP(svm)

    assert(HashTranslate.equals(newLp, lp))
  }
}
