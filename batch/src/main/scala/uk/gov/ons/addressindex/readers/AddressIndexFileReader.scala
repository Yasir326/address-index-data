package uk.gov.ons.addressindex.readers

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types.StructType
import uk.gov.ons.addressindex.models.{CSVSchemas, NisraSchema}
import uk.gov.ons.addressindex.utils.SparkProvider

/**
  * Contains static methods to read different cvs files related to the Address Index
  */
object AddressIndexFileReader {

  lazy val config: Config = ConfigFactory.load()
  lazy val pathToDeliveryPointCsv: String = config.getString("addressindex.files.csv.delivery-point")
  lazy val pathToBlpuCSV: String = config.getString("addressindex.files.csv.blpu")
  lazy val pathToClassificationCSV: String = config.getString("addressindex.files.csv.classification")
  lazy val pathToCrossrefCSV: String = config.getString("addressindex.files.csv.crossref")
  lazy val pathToLpiCSV: String = config.getString("addressindex.files.csv.lpi")
  lazy val pathToOrganisationCSV: String = config.getString("addressindex.files.csv.organisation")
  lazy val pathToStreetCSV: String = config.getString("addressindex.files.csv.street")
  lazy val pathToStreetDescriptorCSV: String = config.getString("addressindex.files.csv.street-descriptor")
  lazy val pathToSuccessorCSV: String = config.getString("addressindex.files.csv.successor")
  lazy val pathToHierarchyCSV: String = config.getString("addressindex.files.csv.hierarchy")
  lazy val pathToNisraTXT: String = config.getString("addressindex.files.txt.nisra")

  /**
    * Reads csv into a `DataFrame`
    *
    * @return `DataFrame` containing the delivery point data from CSV
    */
  def readDeliveryPointCSV(): DataFrame = readCsv(pathToDeliveryPointCsv, CSVSchemas.postcodeAddressFileSchema)

  /**
    * Reads csv into a 'DataFrame'
    *
    * @return 'DataFrame' containing the blpu data from CSV
    */
  def readBlpuCSV(): DataFrame = readCsv(pathToBlpuCSV, CSVSchemas.blpuFileSchema)

  /**
    * Reads csv into a 'DataFrame'
    *
    * @return 'DataFrame' containing the classification data from CSV
    */
  def readClassificationCSV(): DataFrame = readCsv(pathToClassificationCSV, CSVSchemas.classificationFileSchema)

  /**
    * Reads csv into a 'DataFrame'
    *
    * @return 'DataFrame' containing the crossref data from CSV
    */
  def readCrossrefCSV(): DataFrame = readCsv(pathToCrossrefCSV, CSVSchemas.crossrefFileSchema)

  /**
    * Reads csv into a 'DataFrame'
    *
    * @return 'DataFrame' containing the lpi data from CSV
    */
  def readLpiCSV(): DataFrame = readCsv(pathToLpiCSV, CSVSchemas.lpiFileSchema)

  /**
    * Reads csv into a 'DataFrame'
    *
    * @return 'DataFrame' containing the organisation data from CSV
    */
  def readOrganisationCSV(): DataFrame = readCsv(pathToOrganisationCSV, CSVSchemas.organisationFileSchema)

  /**
    * Reads csv into a 'DataFrame'
    *
    * @return 'DataFrame' containing the street data from CSV
    */
  def readStreetCSV(): DataFrame = readCsv(pathToStreetCSV, CSVSchemas.streetFileSchema)

  /**
    * Reads csv into a 'DataFrame'
    *
    * @return 'DataFrame' containing the street-descriptor data from CSV
    */
  def readStreetDescriptorCSV(): DataFrame = readCsv(pathToStreetDescriptorCSV, CSVSchemas.streetDescriptorFileSchema)

  /**
    * Reads csv into a 'DataFrame'
    *
    * @return 'DataFrame' containing the successor data from CSV
    */
  def readSuccessorCSV(): DataFrame = readCsv(pathToSuccessorCSV, CSVSchemas.successorFileSchema)

  /**
    * Reads csv into a 'DataFrame'
    *
    * @return 'DataFrame' containing the hierarchy data from CSV
    */
  def readHierarchyCSV(): DataFrame = readCsv(pathToHierarchyCSV, CSVSchemas.hierarchyFileSchema)

  /**
    * Reads txt into a 'DataFrame'
    *
    * @return 'DataFrame' containing the hierarchy data from TXT (pipe delimited CSV)
    */
  def readNisraTXT(): DataFrame = readTxt(pathToNisraTXT, NisraSchema.nisraFileSchema)

  private def readCsv(path: String, schema: StructType): DataFrame =
    SparkProvider.sparkContext.read
      .format("com.databricks.spark.csv")
      .schema(schema)
      .option("header", "true")
      .option("mode", "PERMISSIVE")
      .load(resolveAbsolutePath(path))

  private def readTxt(path: String, schema: StructType): DataFrame =
    SparkProvider.sparkContext.read
      .format("com.databricks.spark.csv")
      .schema(schema)
      .option("header", "true")
      .option("delimiter", "|")
      .option("mode", "PERMISSIVE")
      .load(resolveAbsolutePath(path))

  private def resolveAbsolutePath(path: String) = {
    val currentDirectory = new java.io.File(".").getCanonicalPath

    if (path.startsWith("hdfs://") || path.startsWith("gs://") ) path
    else {
      if (System.getProperty("os.name").toLowerCase.startsWith("windows")) {
        s"$currentDirectory/$path"
      }
      else {
        s"file://$currentDirectory/$path"
      }
    }
  }

  def validateFileNames(): Boolean = {

    // Not currently validating the NISRA data file name as format is unknown

    val epoch = extractEpoch(pathToDeliveryPointCsv)
    val date = extractDate(pathToDeliveryPointCsv)

    Seq(
      pathToDeliveryPointCsv,
      pathToBlpuCSV,
      pathToClassificationCSV,
      pathToCrossrefCSV,
      pathToLpiCSV,
      pathToOrganisationCSV,
      pathToStreetCSV,
      pathToStreetDescriptorCSV,
      pathToSuccessorCSV,
      pathToHierarchyCSV
    ).forall(fileName => validateFileName(fileName, epoch, date))

  }

  def validateFileName(filePath: String, epoch: Int, date: String): Boolean = {
  val nameRegex1 = s"ABP_E$epoch.+_v$date\\.csv$$".r
  val nameRegex2 = s"ABP_E$epoch.+_v$date\\.csv\\.gz$$".r

    if (nameRegex1.findFirstIn(filePath).isDefined || nameRegex2.findFirstIn(filePath).isDefined) true
    else
      true
      // throw new IllegalArgumentException(s"file $filePath does not contain epoch $epoch and date $date")
    }

  def extractEpoch(filePath: String): Int = {
    val epochRegex = s"ABP_E(\\d+).+$$".r
    val epoch = epochRegex.findFirstMatchIn(filePath).getOrElse(throw new IllegalArgumentException(s"file $filePath does not contain epoch number"))
    epoch.group(1).toInt
  }

  def extractDate(filePath: String): String ={
 val dateRegex1 = s"ABP_E.+(\\d{6})\\.csv$$".r
 val dateRegex2 = s"ABP_E.+(\\d{6})\\.csv\\.gz$$".r
    val date = dateRegex2.findFirstMatchIn(filePath).getOrElse(dateRegex1.findFirstMatchIn(filePath).getOrElse(throw new IllegalArgumentException(s"file $filePath does not contain valid date")))
    date.group(1)
  }

  def generateIndexNameFromFileName(historical : Boolean = true, skinny : Boolean = false, nisra: Boolean = false): String = {
    val epoch = extractEpoch(pathToDeliveryPointCsv)
    val date = extractDate(pathToDeliveryPointCsv)

    val baseIndexName =
      if (historical) {
        config.getString("addressindex.elasticsearch.indices.hybrid")
      } else {
        config.getString("addressindex.elasticsearch.indices.hybridHistorical")
      }

    val subIndex =
      if (skinny) config.getString("addressindex.elasticsearch.indices.skinny") else ""

    val includeNisra =
      if (nisra) config.getString("addressindex.elasticsearch.indices.nisra") else ""

    s"$baseIndexName$subIndex${includeNisra}_${epoch}_${date}_${System.currentTimeMillis()}"
  }
}