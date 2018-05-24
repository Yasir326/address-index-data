package uk.gov.ons.addressindex.models

import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.Row
import uk.gov.ons.addressindex.utils.SparkProvider

import scala.io.{BufferedSource, Source}

case class HybridAddressEsDocument(
  uprn: Long,
  postcodeIn: String,
  postcodeOut: String,
  parentUprn: Long,
  relatives: Array[Map[String, Any]],
  lpi: Seq[Map[String, Any]],
  paf: Seq[Map[String, Any]],
  crossRefs: Array[Map[String, String]]
)

object HybridAddressEsDocument {

  private val config = ConfigFactory.load()

  def rowToLpi(row: Row): Map[String, Any] = Map(
    "uprn" -> row.getLong(0),
    "postcodeLocator" -> row.getString(1),
    "addressBasePostal" -> row.getString(2),
    "location" -> row.get(3),
    "easting" -> row.getFloat(4),
    "northing" -> row.getFloat(5),
    "parentUprn" -> (if (row.isNullAt(6)) null else row.getLong(6)),
    "multiOccCount" -> row.getShort(7),
    "blpuLogicalStatus" -> row.getByte(8),
    "localCustodianCode" -> row.getShort(9),
    "rpc" -> row.getByte(10),
    "organisation" -> row.getString(11),
    "legalName" -> row.getString(12),
    "classScheme" -> row.getString(13),
    "classificationCode" -> row.getString(14),
    "usrn" -> row.getInt(15),
    "lpiKey" -> row.getString(16),
    "paoText" -> row.getString(17),
    "paoStartNumber" -> (if (row.isNullAt(18)) null else row.getShort(18)),
    "paoStartSuffix" -> row.getString(19),
    "paoEndNumber" -> (if (row.isNullAt(20)) null else row.getShort(20)),
    "paoEndSuffix" -> row.getString(21),
    "saoText" -> row.getString(22),
    "saoStartNumber" -> (if (row.isNullAt(23)) null else row.getShort(23)),
    "saoStartSuffix" -> row.getString(24),
    "saoEndNumber" -> (if (row.isNullAt(25)) null else row.getShort(25)),
    "saoEndSuffix" -> row.getString(26),
    "level" -> row.getString(27),
    "officialFlag" -> row.getString(28),
    "lpiLogicalStatus" -> row.getByte(29),
    "usrnMatchIndicator" -> row.getByte(30),
    "language" -> row.getString(31),
    "streetDescriptor" -> row.getString(32),
    "townName" -> row.getString(33),
    "locality" -> row.getString(34),
    "streetClassification" -> (if (row.isNullAt(35)) null else row.getByte(35)),
    "lpiStartDate" -> row.getDate(36),
    "lpiLastUpdateDate" -> row.getDate(37),
    "lpiEndDate" -> row.getDate(38),
    "nagAll" ->  concatNag(
      if (row.isNullAt(23)) "" else row.getShort(23).toString,
      if (row.isNullAt(25)) "" else row.getShort(25).toString,
      row.getString(26), row.getString(24), row.getString(22), row.getString(11),
      if (row.isNullAt(18)) "" else row.getShort(18).toString,
      row.getString(19),
      if (row.isNullAt(20)) "" else row.getShort(20).toString,
      row.getString(21), row.getString(17), row.getString(32),
      row.getString(33), row.getString(34), row.getString(1)
    ),
    "mixedNag" -> generateFormattedNagAddress(
      if (row.isNullAt(23)) "" else row.getShort(23).toString,
      row.getString(24),
      if (row.isNullAt(25)) "" else row.getShort(25).toString,
      row.getString(26), row.getString(22), row.getString(11),
      if (row.isNullAt(18)) "" else row.getShort(18).toString,
      row.getString(19),
      if (row.isNullAt(20)) "" else row.getShort(20).toString,
      row.getString(21), row.getString(17), row.getString(32), row.getString(34), row.getString(33), row.getString(1)
    )
  )

  def rowToPaf(row: Row): Map[String, Any] = Map(
    "recordIdentifier" -> row.getByte(0),
    "changeType" -> row.getString(1),
    "proOrder" -> row.getLong(2),
    "uprn" -> row.getLong(3),
    "udprn" -> row.getInt(4),
    "organisationName" -> row.getString(5),
    "departmentName" -> row.getString(6),
    "subBuildingName" -> row.getString(7),
    "buildingName" -> row.getString(8),
    "buildingNumber" -> (if (row.isNullAt(9)) null else row.getShort(9)),
    "dependentThoroughfare" -> row.getString(10),
    "thoroughfare" -> row.getString(11),
    "doubleDependentLocality" -> row.getString(12),
    "dependentLocality" -> row.getString(13),
    "postTown" -> row.getString(14),
    "postcode" -> row.getString(15),
    "postcodeType" -> row.getString(16),
    "deliveryPointSuffix" -> row.getString(17),
    "welshDependentThoroughfare" -> row.getString(18),
    "welshThoroughfare" -> row.getString(19),
    "welshDoubleDependentLocality" -> row.getString(20),
    "welshDependentLocality" -> row.getString(21),
    "welshPostTown" -> row.getString(22),
    "poBoxNumber" -> row.getString(23),
    "processDate" -> row.getDate(24),
    "startDate" -> row.getDate(25),
    "endDate" -> row.getDate(26),
    "lastUpdateDate" -> row.getDate(27),
    "entryDate" -> row.getDate(28),
    "pafAll" -> concatPaf(row.getString(23), if (row.isNullAt(9)) "" else row.getShort(9).toString, row.getString(10),
      row.getString(18), row.getString(11), row.getString(19), row.getString(6), row.getString(5), row.getString(7), row.getString(8),
      row.getString(12), row.getString(20), row.getString(13), row.getString(21), row.getString(14), row.getString(22), row.getString(15)),
    "mixedPaf" -> generateFormattedPafAddress(
      row.getString(23),
      if (row.isNullAt(9)) "" else row.getShort(9).toString,
      row.getString(10), row.getString(11), row.getString(6), row.getString(5), row.getString(7), row.getString(8),
      row.getString(12), row.getString(13), row.getString(14), row.getString(15)
    ),
    "mixedWelshPaf" -> generateWelshFormattedPafAddress(
      row.getString(23),
      if (row.isNullAt(9)) "" else row.getShort(9).toString,
      row.getString(18), row.getString(19), row.getString(6), row.getString(5), row.getString(7), row.getString(8),
      row.getString(20), row.getString(21), row.getString(22), row.getString(15)
    )
  )

  def splitAndCapitalise(input: String) : String = {
    input.trim.split(" ").map({case y => if (!acronyms.contains(y)) y.toLowerCase.capitalize else y }).mkString(" ")
  }

  /**
    * Creates formatted address from PAF address
    * Adapted from API code
    * @return String of formatted address
    */
  def generateFormattedPafAddress(poBoxNumber: String, buildingNumber: String, dependentThoroughfare: String,
                                  thoroughfare: String, departmentName: String, organisationName: String,
                                  subBuildingName: String, buildingName: String, doubleDependentLocality: String,
                                  dependentLocality: String, postTown: String, postcode: String): String = {

    val poBoxNumberEdit = if (poBoxNumber.isEmpty) "" else s"PO BOX ${poBoxNumber}"

    val trimmedBuildingNumber = buildingNumber.trim
    val trimmedDependentThoroughfare = splitAndCapitalise(dependentThoroughfare)
    val trimmedThoroughfare = splitAndCapitalise(thoroughfare)

    val buildingNumberWithStreetName =
      s"$trimmedBuildingNumber ${ if(trimmedDependentThoroughfare.nonEmpty) s"$trimmedDependentThoroughfare, " else "" }$trimmedThoroughfare"

    val departmentNameEdit = splitAndCapitalise(departmentName)
    val organisationNameEdit = splitAndCapitalise(organisationName)
    val subBuildingNameEdit = splitAndCapitalise(subBuildingName)
    val buildingNameEdit = splitAndCapitalise(buildingName)
    val doubleDependentLocalityEdit = splitAndCapitalise(doubleDependentLocality)
    val dependentLocalityEdit = splitAndCapitalise(dependentLocality)
    val postTownEdit = splitAndCapitalise(postTown)

    Seq(departmentNameEdit, organisationNameEdit, subBuildingNameEdit, buildingNameEdit,
      poBoxNumberEdit, buildingNumberWithStreetName, doubleDependentLocalityEdit, dependentLocalityEdit,
      postTownEdit, postcode).map(_.trim).filter(_.nonEmpty).mkString(", ")
  }

  /**
    * Creates Welsh formatted address from PAF address
    * Adapted from API code
    * @return String of Welsh formatted address
    */
  def generateWelshFormattedPafAddress(poBoxNumber: String, buildingNumber: String, welshDependentThoroughfare: String,
                                       welshThoroughfare: String, departmentName: String, organisationName: String,
                                       subBuildingName: String, buildingName: String, welshDoubleDependentLocality: String,
                                       welshDependentLocality: String, welshPostTown: String, postcode: String): String = {

    val poBoxNumberEdit = if (poBoxNumber.isEmpty) "" else s"PO BOX ${poBoxNumber}"

    val trimmedBuildingNumber = buildingNumber.trim
    val trimmedDependentThoroughfare = splitAndCapitalise(welshDependentThoroughfare)
    val trimmedThoroughfare = splitAndCapitalise(welshThoroughfare)

    val buildingNumberWithStreetName =
      s"$trimmedBuildingNumber ${ if(trimmedDependentThoroughfare.nonEmpty) s"$trimmedDependentThoroughfare, " else "" }$trimmedThoroughfare"

    val departmentNameEdit = splitAndCapitalise(departmentName)
    val organisationNameEdit = splitAndCapitalise(organisationName)
    val subBuildingNameEdit = splitAndCapitalise(subBuildingName)
    val buildingNameEdit = splitAndCapitalise(buildingName)
    val welshDoubleDependentLocalityEdit = splitAndCapitalise(welshDoubleDependentLocality)
    val welshDependentLocalityEdit = splitAndCapitalise(welshDependentLocality)
    val welshPostTownEdit = splitAndCapitalise(welshPostTown)

    Seq(departmentNameEdit, organisationNameEdit, subBuildingNameEdit, buildingNameEdit,
      poBoxNumberEdit, buildingNumberWithStreetName, welshDoubleDependentLocalityEdit, welshDependentLocalityEdit,
      welshPostTownEdit, postcode).map(_.trim).filter(_.nonEmpty).mkString(", ")
  }

  /**
    * Formatted address should contain commas between all fields except after digits
    * The actual logic is pretty complex and should be treated on example-to-example level
    * (with unit tests)
    * Adapted from API code
    * @return String of formatted address
    */
  def generateFormattedNagAddress(saoStartNumber: String, saoStartSuffix: String, saoEndNumber: String,
                               saoEndSuffix: String, saoText: String, organisation: String, paoStartNumber: String,
                               paoStartSuffix: String, paoEndNumber: String, paoEndSuffix: String, paoText: String,
                               streetDescriptor: String, locality: String, townName: String, postcodeLocator: String): String = {

    val saoLeftRangeExists = saoStartNumber.nonEmpty || saoStartSuffix.nonEmpty
    val saoRightRangeExists = saoEndNumber.nonEmpty || saoEndSuffix.nonEmpty
    val saoHyphen = if (saoLeftRangeExists && saoRightRangeExists) "-" else ""
    val saoNumbers = Seq(saoStartNumber, saoStartSuffix, saoHyphen, saoEndNumber, saoEndSuffix)
      .map(_.trim).mkString
    val sao =
      if (saoText == organisation || saoText.isEmpty) saoNumbers
      else if (saoNumbers.isEmpty) s"${splitAndCapitalise(saoText)},"
      else s"$saoNumbers, ${splitAndCapitalise(saoText)},"

    val paoLeftRangeExists = paoStartNumber.nonEmpty || paoStartSuffix.nonEmpty
    val paoRightRangeExists = paoEndNumber.nonEmpty || paoEndSuffix.nonEmpty
    val paoHyphen = if (paoLeftRangeExists && paoRightRangeExists) "-" else ""
    val paoNumbers = Seq(paoStartNumber, paoStartSuffix, paoHyphen, paoEndNumber, paoEndSuffix)
      .map(_.trim).mkString
    val pao =
      if (paoText == organisation || paoText.isEmpty) paoNumbers
      else if (paoNumbers.isEmpty) s"${splitAndCapitalise(paoText)},"
      else s"${splitAndCapitalise(paoText)}, $paoNumbers"

    val trimmedStreetDescriptor = splitAndCapitalise(streetDescriptor)
    val buildingNumberWithStreetDescription =
      if (pao.isEmpty) s"$sao $trimmedStreetDescriptor"
      else if (sao.isEmpty) s"$pao $trimmedStreetDescriptor"
      else if (pao.isEmpty && sao.isEmpty) trimmedStreetDescriptor
      else s"$sao $pao $trimmedStreetDescriptor"

    Seq(splitAndCapitalise(organisation), buildingNumberWithStreetDescription, splitAndCapitalise(locality),
      splitAndCapitalise(townName), postcodeLocator).map(_.trim).filter(_.nonEmpty).mkString(", ")
  }

  def concatPaf(poBoxNumber: String, buildingNumber: String, dependentThoroughfare: String, welshDependentThoroughfare:
  String, thoroughfare: String, welshThoroughfare: String, departmentName: String, organisationName: String,
    subBuildingName: String, buildingName: String, doubleDependentLocality: String,
    welshDoubleDependentLocality: String, dependentLocality: String, welshDependentLocality: String,
    postTown: String, welshPostTown: String, postcode: String): String = {

    val langDependentThoroughfare = if (dependentThoroughfare == welshDependentThoroughfare)
      s"$dependentThoroughfare" else s"$dependentThoroughfare $welshDependentThoroughfare"

    val langThoroughfare = if (thoroughfare == welshThoroughfare)
      s"$thoroughfare" else s"$thoroughfare $welshThoroughfare"

    val langDoubleDependentLocality = if (doubleDependentLocality == welshDoubleDependentLocality)
      s"$doubleDependentLocality" else s"$doubleDependentLocality $welshDoubleDependentLocality"

    val langDependentLocality = if (dependentLocality == welshDependentLocality)
      s"$dependentLocality" else s"$dependentLocality $welshDependentLocality"

    val langPostTown = if (postTown == welshPostTown)
      s"$postTown" else s"$postTown $welshPostTown"

    val buildingNumberWithStreetName =
      s"$buildingNumber ${if (langDependentThoroughfare.nonEmpty) s"$langDependentThoroughfare " else ""}$langThoroughfare"

    Seq(departmentName, organisationName, subBuildingName, buildingName,
      poBoxNumber, buildingNumberWithStreetName, langDoubleDependentLocality, langDependentLocality,
      langPostTown, postcode).map(_.trim).filter(_.nonEmpty).mkString(" ")
  }

  def concatNag(saoStartNumber: String, saoEndNumber: String, saoEndSuffix: String, saoStartSuffix: String,
    saoText: String, organisation: String, paoStartNumber: String, paoStartSuffix: String,
    paoEndNumber: String, paoEndSuffix: String, paoText: String, streetDescriptor: String,
    townName: String, locality: String, postcodeLocator: String): String = {

    val saoLeftRangeExists = saoStartNumber.nonEmpty || saoStartSuffix.nonEmpty
    val saoRightRangeExists = saoEndNumber.nonEmpty || saoEndSuffix.nonEmpty
    val saoHyphen = if (saoLeftRangeExists && saoRightRangeExists) "-" else ""

    val saoNumbers = Seq(saoStartNumber, saoStartSuffix, saoHyphen, saoEndNumber, saoEndSuffix)
      .map(_.trim).mkString
    val sao =
      if (saoText == organisation || saoText.isEmpty) saoNumbers
      else if (saoNumbers.isEmpty) s"$saoText"
      else s"$saoNumbers $saoText"

    val paoLeftRangeExists = paoStartNumber.nonEmpty || paoStartSuffix.nonEmpty
    val paoRightRangeExists = paoEndNumber.nonEmpty || paoEndSuffix.nonEmpty
    val paoHyphen = if (paoLeftRangeExists && paoRightRangeExists) "-" else ""

    val paoNumbers = Seq(paoStartNumber, paoStartSuffix, paoHyphen, paoEndNumber, paoEndSuffix)
      .map(_.trim).mkString
    val pao =
      if (paoText == organisation || paoText.isEmpty) paoNumbers
      else if (paoNumbers.isEmpty) s"$paoText"
      else s"$paoText $paoNumbers"

    val trimmedStreetDescriptor = streetDescriptor.trim
    val buildingNumberWithStreetDescription =
      if (pao.isEmpty) s"$sao $trimmedStreetDescriptor"
      else if (sao.isEmpty) s"$pao $trimmedStreetDescriptor"
      else if (pao.isEmpty && sao.isEmpty) trimmedStreetDescriptor
      else s"$sao $pao $trimmedStreetDescriptor"

    Seq(organisation, buildingNumberWithStreetDescription, locality,
      townName, postcodeLocator).map(_.trim).filter(_.nonEmpty).mkString(" ")
  }

  /**
    * List of acronyms to not capitalise
    */
  lazy val acronyms: Seq[String] = fileToList(s"acronyms")

  /**
    * Convert external file into list
    * @param fileName
    * @return
    */
  private def fileToList(fileName: String): Seq[String] = {
    val resource = getResource(fileName)
    resource.getLines().toList
  }

  /**
    * Fetch file stream as buffered source
    * @param fileName
    * @return
    */
  def getResource(fileName: String): BufferedSource = {
    val path = "/" + fileName
    val currentDirectory = new java.io.File(".").getCanonicalPath
    // `Source.fromFile` needs an absolute path to the file, and current directory depends on where sbt was lauched
    // `getResource` may return null, that's why we wrap it into an `Option`
    Option(getClass.getResource(path)).map(Source.fromURL).getOrElse(Source.fromFile(currentDirectory + path))
  }

}
