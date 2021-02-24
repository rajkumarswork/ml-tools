package ml.tools

import com.google.cloud.bigquery._
import com.google.cloud.bigquery.FieldValue.Attribute
import com.google.protobuf.ByteString
import org.tensorflow.example._

import scala.collection.JavaConverters._

// - traits ------------
trait Dataset {
  def rows(): Iterator[Map[String,String]]
}

// - class -------------
class BQ(path: String) extends MLPoint with Dataset {
  def fields(): Map[String,Boolean] = BQ.tableFields(path)
  def rows(): Iterator[Map[String,String]] = BQ.tableRows(path)
}

// - object ---------------
// wrappers to read BigQuery tables and their schemas in String and tf formats
object BQ {


  def tableExists(path: String): Boolean =
    if (isValidTable(path)) service.getTable(tableId(path)).exists else false

  // Map[field-name, isCategoric]
  def tableFields(path: String): Map[String, Boolean] = {
    val fields = getSchema(tableId(path)).getFields
    (0 until fields.size).map(i ⇒
      (fields.get(i).getName.toLowerCase, !isNumeric(fields.get(i)))).toMap
  }

  def tableRows(path: String): Iterator[Map[String, String]] = {
    val fs = getSchema(tableId(path)).getFields
    service.listTableData(tableId(path)).iterateAll.asScala.toIterator.map(r ⇒ rowMap(fs, r))
  }

  def tableExamples(path: String): Iterator[Example] = {
    val fields = getSchema(tableId(path)).getFields
    val rows = service.listTableData(tableId(path)).iterateAll.asScala.toIterator
    rows.map(r ⇒ rowExample(fields, r))
  }

  // - privates ------
  private val service = BigQueryOptions.getDefaultInstance().getService()

  private def getSchema(tid: TableId): Schema = {
    val t: Table = service.getTable(tid)
    val td: TableDefinition = t.getDefinition()
    td.getSchema()
  }

  private def isNumeric(f: Field): Boolean =
    f.getType match {
      case LegacySQLTypeName.FLOAT | LegacySQLTypeName.INTEGER | LegacySQLTypeName.NUMERIC
        | LegacySQLTypeName.BIGNUMERIC ⇒ true
      case LegacySQLTypeName.BOOLEAN | LegacySQLTypeName.BYTES | LegacySQLTypeName.DATE
        | LegacySQLTypeName.DATETIME | LegacySQLTypeName.GEOGRAPHY | LegacySQLTypeName.RECORD
        | LegacySQLTypeName.STRING | LegacySQLTypeName.TIME | LegacySQLTypeName.TIMESTAMP
        | LegacySQLTypeName.BIGNUMERIC ⇒ false
      case _ ⇒ false
    }

  private def rowMap(fl: FieldList, r: FieldValueList): Map[String, String] =
    (0 until r.size).map(i ⇒ (fl.get(i).getName, fvString(r.get(i)))).toMap

  private def rowString(r: FieldValueList): Seq[String] =
    (0 until r.size).map(i ⇒ fvString(r.get(i))).toSeq

  private def fvString(fv: FieldValue): String =
    fv.getAttribute match {
      case Attribute.PRIMITIVE ⇒ if (fv.getValue != null) fv.getValue.toString else ""
      case Attribute.RECORD    ⇒ rowString(fv.getRecordValue()).mkString("[", ", ", "]")
      case Attribute.REPEATED  ⇒ fv.getRepeatedValue().asScala.map(fv ⇒ fvString(fv)).mkString(", ")
      case _                   ⇒ ""
    }
  private def rowExample(fl: FieldList, fvl: FieldValueList): Example = {
    val fbuilder = Features.newBuilder()
    (0 until fvl.size).foreach(i ⇒ fbuilder.putFeature(
      fl.get(i).getName,
      fvFeature(fl.get(i), fvl.get(i))))
    Example.newBuilder.setFeatures(fbuilder.build()).build
  }
  private def fvFeature(f: Field, fv: FieldValue): Feature = {
    f.getType match {
      case LegacySQLTypeName.INTEGER ⇒ intFeature(fv)
      case LegacySQLTypeName.FLOAT | LegacySQLTypeName.NUMERIC
        | LegacySQLTypeName.BIGNUMERIC ⇒ floatFeature(fv)
      case _ ⇒ bytesFeature(fv)
    }
  }
  private def intFeature(fv: FieldValue): Feature =
    Feature.newBuilder.setInt64List(Int64List.newBuilder()
      .addValue(util.Try(fv.getLongValue).getOrElse(0L)).build()).build()
  private def floatFeature(fv: FieldValue): Feature =
    Feature.newBuilder.setFloatList(FloatList.newBuilder()
      .addValue(util.Try(fv.getDoubleValue.toFloat).getOrElse(0f)).build()).build()
  private def bytesFeature(fv: FieldValue): Feature =
    Feature.newBuilder.setBytesList(BytesList.newBuilder()
      .addValue(ByteString.copyFromUtf8(util.Try(fv.getStringValue).getOrElse(""))).build()).build()

  val BQPath = """^bq://([\w-]+)/([\w-_]+)/([\w-_.]+)$""".r
  val DatasetPath = """^([\w_-]+):([\w-_]+).([\w-_.]+)$""".r
  private def tableId(path: String): TableId = {
    val (dataset, table) = parse(path)
    TableId.of(dataset, table)
  }
  def parse(path: String): (String, String) =
    path match {
      case BQPath(_, dataset, table)      ⇒ (dataset, table)
      case DatasetPath(_, dataset, table) ⇒ (dataset, table)
      case _                              ⇒ ("", "")
    }
  def isValidTable(path: String): Boolean = {
    val (dataset, table) = parse(path)
    dataset.size > 0 && table.size > 0
  }
}

