package ml.tools

import ml.dmlc.xgboost4j.LabeledPoint

// tools to translate rows-of-fields to LabeledPoints or SvmLight rows

// Non-numeric fields are hash-encoded into HashSize floats and attached to the end
// Numeric+encoded values are stored in sparse-matrix notation (non-zero 1-basee index-value pairs)
// Labels are in range [0,1] (note: svmlite allows [-1,1], but xgboost expects [0,1]
// If a weight-field is specified or if column named weight is present, row-weights are added

object HashTranslate {

  // BigQuery table to iteraror of LabeledPoints
  def BQLabeledPoints(path: String, label: String, hashSize: Int):
    Iterator[LabeledPoint] = {
    val encoder = new SimpleEncoder(hashSize)
    val fields = BQ.tableFields(path)
    val rows = BQ.tableRows(path)
    rows.map(r ⇒ toLabeledPoint(r, fields, label, encoder))
  }

  def BQSvms(path: String, label: String, hashSize: Int):
    Iterator[String] = BQLabeledPoints(path, label, hashSize).map(lpToSvm)

  def distinctTokens(path: String): Long = {
    val catFields = BQ.tableFields(path).filter(_._2).toSeq.map(_._1)
    val rows = BQ.tableRows(path).map(rmap ⇒ catFields.map(f ⇒ mkToken(f, rmap.getOrElse(f, ""))))
    HashEncoder.distinctTokens(rows)
  }

  def minHashSize(path: String): Int =
    HashEncoder.minHashSize(distinctTokens(path))

  // convert LabeledPoint to Svm-string (lastIndex:0 addeed if needed to indicate dimension)
  def lpToSvm(lp: LabeledPoint): String = {
    val ivs = lp.indices.zip(lp.values).sortBy(_._1)
    val sizeIndexValue = if (ivs.last._1 == lp.size) Seq() else Seq((lp.size, 0f))
    val ivstring = (ivs ++ sizeIndexValue).sortBy(_._1).map { case (i, v) ⇒ s"$i:$v" }.mkString(" ")
    lp.label.min(1f).max(0f) + ":" + lp.weight + " " + ivstring
  }
  // svmLite to LabeledPoint (lastIndex needs to be present as LabeledPoint requires dimension)
  def svmToLP(svm: String): LabeledPoint = {
    val tokens = svm.split("\\s+")
    val lwa = tokens.head.split(":")
    val label = stringToFloat(lwa.head, 0f)
    val weight = stringToFloat(if (lwa.size > 1) lwa(1) else "1")
    val ivs = tokens.tail.map(iv ⇒ (iv.split(":").head.toInt, stringToFloat(iv.split(":").last, 0f)))
    val size = ivs.map(_._1).max
    val givs = ivs.filterNot(_._2 == 0f)
    val indices = givs.map(_._1).toArray
    val values = givs.map(_._2).toArray
    LabeledPoint(label = label, weight = weight, indices = indices, values = values, size = size)
  }
  def toLabeledPoint(vm: Map[String, String], fs: Map[String,Boolean], label: String, encoder: Encoder): LabeledPoint = {
    // do not include the label or weight field in the lp-array
    val specialFields = Set(label.toLowerCase, "weight")
    val fields = fs.filterNot(f => specialFields.contains(f._1))
    val values = vm.map { case (k, v) => (k.toLowerCase, v) }

    // numeric values use index = index + 1
    val nfields = fields.filterNot(_._2).map { case (k, _) ⇒ (k, values.getOrElse(k, "0")) }
    val nivs = nfields.toSeq.sortBy(_._1).zipWithIndex
      .map { case ((_, v), i) ⇒ (i + 1, stringToFloat(v, 0f)) }
    val numericSize = nfields.size

    // encoded categoric values get appended after numerics (min-index = #numerics + 1)
    val cfields = fields.filter(_._2).map {
      case (k, _) ⇒
        mkToken(k, values.getOrElse(k, ""))
    }.toSeq
    val cfloats = encoder.encode(cfields)
    val categoricSize = cfloats.size
    val civs = cfloats.zipWithIndex.map { case (f, i) ⇒ (i + 1 + numericSize, f) }

    val givs = (nivs ++ civs).filterNot { case (_, f) ⇒ f == 0 }
    val gis = givs.map(_._1).toArray
    val gvs = givs.map(_._2).toArray

    val ls = values.getOrElse(label.toLowerCase, "0").toLowerCase
    val lbl = if (ls == "1" || ls == "1.0" || ls.startsWith("t")) 1f else 0f
    val weight = stringToFloat(values.getOrElse("weight", "1"))
    val size = numericSize + categoricSize + 1
    // LabeledPoint(label = label, weight = weight.toInt, indices = gis, values = gvs)
    LabeledPoint(label = lbl, weight = weight.toInt, size = size, indices = gis, values = gvs)
  }
  // LabeledPoint's equals method doesn't seem to do a deep comparison of the arrays
  def equals(a: LabeledPoint, b: LabeledPoint): Boolean =
    a.label == b.label && a.size == b.size & a.weight == b.weight &&
      a.indices.sameElements(b.indices) && a.values.sameElements(b.values)

  // - privates ------
  private def stringToFloat(s: String, missing: Float = Float.MinValue): Float =
    util.Try(s.toFloat).getOrElse(missing)

  // field-naming scheme for categoric-fields before encoding (name-of-field : value-of-field)
  private def mkToken(k: String, v: String) = s"$k:$v"
}
