/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.ipt.task;

import org.gbif.ipt.config.AppConfig;
import org.gbif.ipt.config.DataDir;
import org.gbif.ipt.model.DataSchema;
import org.gbif.ipt.model.DataSchemaField;
import org.gbif.ipt.model.DataSchemaFieldMapping;
import org.gbif.ipt.model.DataSchemaMapping;
import org.gbif.ipt.model.DataSubschema;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.SubSchemaRequirement;
import org.gbif.ipt.model.datapackage.metadata.camtrap.CamtrapMetadata;
import org.gbif.ipt.model.datapackage.metadata.col.ColMetadata;
import org.gbif.ipt.service.manage.MetadataReader;
import org.gbif.ipt.service.manage.SourceManager;
import org.gbif.utils.file.ClosableReportingIterator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import io.frictionlessdata.datapackage.JSONBase;
import io.frictionlessdata.datapackage.Package;
import io.frictionlessdata.datapackage.Profile;
import io.frictionlessdata.datapackage.resource.FilebasedResource;
import io.frictionlessdata.tableschema.schema.Schema;

import static org.gbif.ipt.config.Constants.CAMTRAP_DP;
import static org.gbif.ipt.config.Constants.CAMTRAP_PROFILE;
import static org.gbif.ipt.config.Constants.COL_DP;

public class GenerateDataPackage extends ReportingTask implements Callable<Map<String, Integer>> {

  private enum STATE {
    WAITING, STARTED, DATAFILES, METADATA, BUNDLING, COMPLETED, ARCHIVING, VALIDATING, CANCELLED, FAILED
  }

  private static final Pattern ESCAPE_CHARS = Pattern.compile("[\t\n\r]");

  private final Resource resource;
  private final SourceManager sourceManager;
  private MetadataReader metadataReader;
  private final AppConfig cfg;
  private STATE state = STATE.WAITING;
  private Exception exception;
  private File dataPackageFolder;
  private Package dataPackage;
  private int currRecords = 0;
  private int currRecordsSkipped = 0;
  private String currSchema;
  private String currSubschema;

  @Inject
  public GenerateDataPackage(@Assisted Resource resource, @Assisted ReportHandler handler, DataDir dataDir,
                      SourceManager sourceManager, AppConfig cfg, MetadataReader metadataReader) throws IOException {
    super(1000, resource.getShortname(), handler, dataDir);
    this.resource = resource;
    this.sourceManager = sourceManager;
    this.cfg = cfg;
    this.metadataReader = metadataReader;
  }

  @Override
  public Map<String, Integer> call() throws Exception {
    try {
      checkForInterruption();
      setState(STATE.STARTED);

      // initial reporting
      addMessage(Level.INFO, "Data package generation started for version #" + resource.getDataPackageMetadataVersion());

      // create a temp dir to copy all dwca files to
      dataPackageFolder = dataDir.tmpDir();

      // create data files
      createDataFiles();

      // copy metadata file (datapackage.json)
      addMetadata();

      // validation is a part of frictionless datapackage generation
      // zip archive and copy to resource folder
      bundleArchive();

      // reporting
      addMessage(Level.INFO, "Archive version #" + resource.getDataPackageMetadataVersion() + " generated successfully!");

      // set final state
      setState(STATE.COMPLETED);

      Map<String, Integer> result = new HashMap<>();
      result.put(resource.getSchemaIdentifier(), 0);
      return result;
    } catch (GeneratorException e) {
      // set last error report!
      setState(e);

      // write exception to publication log file when IPT is in debug mode, otherwise just log it
      if (cfg.debug()) {
        writeFailureToPublicationLog(e);
      } else {
        log.error(
            "Exception occurred trying to generate data package for resource " + resource.getTitleAndShortname()
                + ": " + e.getMessage(), e);
      }

      // rethrow exception, which gets wrapped in an ExecutionException and re caught when calling Future.get
      throw e;
    } catch (InterruptedException e) {
      setState(e);
      writeFailureToPublicationLog(e);
      throw e;
    } catch (Exception e) {
      setState(e);
      writeFailureToPublicationLog(e);
      throw new GeneratorException(e);
    } finally {
      // cleanup temp dir that was used to store data package files
      if (dataPackageFolder != null && dataPackageFolder.exists()) {
        FileUtils.deleteQuietly(dataPackageFolder);
      }
      // ensure publication log writer is closed
      closePublicationLogWriter();
    }
  }

  @Override
  protected boolean completed() {
    return STATE.COMPLETED == this.state;
  }

  @Override
  protected Exception currentException() {
    return exception;
  }

  @Override
  protected String currentState() {
    switch (state) {
      case WAITING:
        return "Not started yet";
      case STARTED:
        return "Starting data package generation";
      case DATAFILES:
        return "Processing record " + currRecords + " for data file <em>" + currSubschema + "</em>";
      case METADATA:
        return "Creating metadata files";
      case BUNDLING:
        return "Compressing data package (archive)";
      case COMPLETED:
        return "Data package generated!";
      case VALIDATING:
        return "Validating data package, " + currRecords + " for data file <em>" + currSubschema + "</em>";
      case ARCHIVING:
        return "Archiving version of data package";
      case CANCELLED:
        return "Data package generation cancelled";
      case FAILED:
        return "Failed. Fatal error!";
      default:
        return "You should never see this";
    }
  }

  /**
   * Zips the data package folder. A temp version is created first, and when successful, it's moved into the resource's
   * data directory.
   *
   * @throws GeneratorException if data package could not be zipped or moved
   * @throws InterruptedException if executing thread was interrupted
   */
  private void bundleArchive() throws Exception {
    checkForInterruption();
    setState(STATE.BUNDLING);
    File zip = null;
    BigDecimal version = resource.getDataPackageMetadataVersion();
    try {
      // create zip
      zip = dataDir.tmpFile("data_package", ".zip");
      if (COL_DP.equals(resource.getCoreType())) {
        dataPackage.write(zip, this::writeCustomColDPMetadata, true);
      } else {
        dataPackage.write(zip, true);
      }

      if (zip.exists()) {
        // move to data dir with versioned name
        File versionedFile = dataDir.resourceDataPackageFile(resource.getShortname(), version);
        if (versionedFile.exists()) {
          FileUtils.forceDelete(versionedFile);
        }
        FileUtils.moveFile(zip, versionedFile);
      } else {
        throw new GeneratorException("Archive bundling failed: temp archive not created: " + zip.getAbsolutePath());
      }
    } catch (IOException e) {
      throw new GeneratorException("Problem occurred while bundling data package", e);
    } finally {
      // cleanup zip directory, if compression was incomplete for example due to Exception
      // if moving zip to data dir was successful, it won't exist any more and cleanup will be skipped
      if (zip != null && zip.exists()) {
        FileUtils.deleteQuietly(zip);
      }
    }
    // final reporting
    addMessage(Level.INFO, "Archive has been compressed");
  }

  /**
   * Apart from standard frictionless metadata ColDP archive must contain specific metadata.yaml file.
   */
  private void writeCustomColDPMetadata(Path outputDir) {
    Path target = outputDir.getFileSystem().getPath("metadata.yaml");
    try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
      metadataReader.writeValue(writer, resource.getDataPackageMetadata());
    } catch (IOException e) {
      log.error("Failed to write metadata.yaml", e);
      addMessage(Level.ERROR, "Failed to write metadata.yaml");
    }
  }

  /**
   * Checks if the executing thread has been interrupted, i.e. generation was cancelled.
   *
   * @throws InterruptedException if the thread was found to be interrupted
   */
  private void checkForInterruption() throws InterruptedException {
    if (Thread.interrupted()) {
      StatusReport report = report();
      String msg = "Interrupting dwca generator. Last status: " + report.getState();
      log.info(msg);
      throw new InterruptedException(msg);
    }
  }

  /**
   * Checks if the executing thread has been interrupted, i.e. package generation was cancelled.
   *
   * @param line number of lines currently processed at the time of the check
   * @throws InterruptedException if the thread was found to be interrupted
   */
  private void checkForInterruption(int line) throws InterruptedException {
    if (Thread.interrupted()) {
      StatusReport report = report();
      String msg = "Interrupting package generator at line " + line + ". Last status: " + report.getState();
      log.info(msg);
      throw new InterruptedException(msg);
    }
  }

  /**
   * Sets only the state of the worker. The final StatusReport is generated at the end.
   *
   * @param s STATE of worker
   */
  private void setState(STATE s) {
    state = s;
    report();
  }

  /**
   * Sets an exception and state of the worker to FAILED. The final StatusReport is generated at the end.
   *
   * @param e exception
   */
  private void setState(Exception e) {
    exception = e;
    state = (exception instanceof InterruptedException) ? STATE.CANCELLED : STATE.FAILED;
    report();
  }

  /**
   * Create data files.
   *
   * @throws GeneratorException if the resource had no core file that was mapped
   * @throws InterruptedException if the thread was interrupted
   */
  private void createDataFiles() throws GeneratorException, InterruptedException {
    checkForInterruption();
    setState(STATE.DATAFILES);
    if (resource.getSchemaIdentifier() == null && CollectionUtils.isEmpty(resource.getDataSchemaMappings())) {
      throw new GeneratorException("Schema identifier or mappings are not set");
    }

    List<DataSchemaMapping> allMappings = resource.getDataSchemaMappings();
    Set<String> mappedSubSchemas = allMappings.stream()
        .map(DataSchemaMapping::getDataSchemaFile)
        .collect(Collectors.toSet());
    DataSchema dataSchema = resource.getDataSchemaMappings().get(0).getDataSchema();
    currSchema = dataSchema.getName();

    // before starting to add subschemas, check all required schemas mapped
    checkRequiredSubSchemasMapped(mappedSubSchemas, dataSchema);

    for (DataSubschema subSchema : dataSchema.getSubSchemas()) {
      // skip un-mapped (optional) schemas
      if (!mappedSubSchemas.contains(subSchema.getName())) {
        continue;
      }

      report();
      try {
        addDataFile(currSchema, subSchema, allMappings);
      } catch (IOException | IllegalArgumentException e) {
        throw new GeneratorException("Problem occurred while writing data file", e);
      }
    }

    // final reporting
    addMessage(Level.INFO, "All data files completed");
    report();
  }

  /**
   * Checks if all required schemas mapped, otherwise throws an exception.
   *
   * @param mappedSubSchemas mapped subschemas
   * @param dataSchema data schema
   */
  private void checkRequiredSubSchemasMapped(Set<String> mappedSubSchemas, DataSchema dataSchema)
      throws GeneratorException {
    SubSchemaRequirement requirements = dataSchema.getSubSchemaRequirements();
    SubSchemaRequirement.ValidationResult validationResult = requirements.validate(mappedSubSchemas);

    if (!validationResult.isValid()) {
      throw new GeneratorException(validationResult.getReason());
    }
  }

  /**
   * Adds a single data file for a subschema mapping.
   *
   * @throws IllegalArgumentException if not all mappings are mapped to the same extension
   * @throws InterruptedException if the thread was interrupted
   * @throws IOException if problems occurred while persisting new data files
   * @throws GeneratorException if any problem was encountered writing data file
   */
  public void addDataFile(String schemaName, DataSubschema subschema, List<DataSchemaMapping> allMappings) throws IOException,
      IllegalArgumentException, InterruptedException, GeneratorException {
    checkForInterruption();
    if (subschema == null || CollectionUtils.isEmpty(allMappings)) {
      return;
    }

    // update reporting
    currRecords = 0;
    currRecordsSkipped = 0;
    currSubschema = subschema.getName();

    List<DataSchemaField> fields = subschema.getFields();

    // file header
    String header = fields.stream()
        .map(DataSchemaField::getName)
        .collect(Collectors.joining(",", "", "\n"));

    // total column count (number of fields in the subschema)
    int totalColumns = fields.size();

    String fn = subschema.getName() + ".csv";
    File dataFile = new File(dataPackageFolder, fn);

    // ready to go through each mapping and dump the data
    try (Writer writer = org.gbif.utils.file.FileUtils.startNewUtf8File(dataFile)) {
      addMessage(Level.INFO, "Start writing data file for " + subschema.getName());
      boolean headerWritten = false;

      for (DataSchemaMapping dataSchemaMapping : allMappings) {
        if (dataSchemaMapping.getDataSchemaFile().equals(subschema.getName())) {
          // write header line 1 time only to file
          if (!headerWritten) {
            writer.write(header);
            headerWritten = true;
          }

          dumpData(writer, dataSchemaMapping, dataSchemaMapping.getFields(), totalColumns);
        }
      }
    } catch (IOException e) {
      // some error writing this file, report
      log.error("Fatal Package Generator Error encountered while writing header line to data file", e);
      // set last error report!
      setState(e);
      throw new GeneratorException("Error writing header line to data file", e);
    }

    // create resource from file
    @SuppressWarnings({"rawtypes", "unchecked"})
    io.frictionlessdata.datapackage.resource.Resource packageResource =
        new FilebasedResource(
            subschema.getName(),
            Collections.singleton(new File(fn)),
            dataPackageFolder);
    packageResource.setProfile(Profile.PROFILE_TABULAR_DATA_RESOURCE);
    packageResource.setFormat(io.frictionlessdata.datapackage.resource.Resource.FORMAT_CSV);
    if (subschema.getUrl() != null) {
      ((JSONBase) packageResource).getOriginalReferences().put(JSONBase.JSON_KEY_SCHEMA, subschema.getUrl().toString());
    }

    try {
      Schema schema = Schema.fromJson(subschema.getUrl(), true);
      packageResource.setSchema(schema);
    } catch (Exception e) {
      log.error("Fatal Package Generator Error encountered while adding schema data", e);
      // set last error report!
      setState(e);
      throw new GeneratorException("Error adding schema file", e);
    }

    // add resource to package
    if (dataPackage == null) {
      dataPackage = new Package(Collections.singleton(packageResource));
      if (CAMTRAP_DP.equals(schemaName)) {
        dataPackage.setProperty("profile", CAMTRAP_PROFILE);
      } else {
        dataPackage.setProperty("profile", schemaName);
      }
    } else {
      dataPackage.addResource(packageResource);
    }

    // final reporting
    addMessage(Level.INFO, "Data file written for " + currSubschema + " with " + currRecords + " records and "
        + totalColumns + " columns");
    // how many records were skipped?
    if (currRecordsSkipped > 0) {
      addMessage(Level.WARN, "!!! " + currRecordsSkipped + " records were skipped for " + currSubschema
          + " due to errors interpreting line, or because the line was empty");
    }
  }

  /**
   * Write data file for mappings.
   *
   * @param writer file writer for single data file
   * @param schemaMapping schema mapping
   * @param subschemaFieldMappings field mappings
   * @param dataFileRowSize number of columns in data file
   * @throws GeneratorException if there was an error writing data file for mapping.
   * @throws InterruptedException if the thread was interrupted
   */
  private void dumpData(Writer writer, DataSchemaMapping schemaMapping,
                        List<DataSchemaFieldMapping> subschemaFieldMappings, int dataFileRowSize)
      throws GeneratorException, InterruptedException {
    int recordsWithError = 0;
    int linesWithWrongColumnNumber = 0;
    int recordsFiltered = 0;
    int emptyLines = 0;
    ClosableReportingIterator<String[]> iter = null;
    int line = 0;
    Optional<Integer> maxMappedColumnIndexOpt = subschemaFieldMappings.stream()
        .map(DataSchemaFieldMapping::getIndex)
        .filter(Objects::nonNull)
        .max(Comparator.naturalOrder());

    try {
      // get the source iterator
      iter = sourceManager.rowIterator(schemaMapping.getSource());

      while (iter.hasNext()) {
        line++;
        if (line % 1000 == 0) {
          checkForInterruption(line);
          reportIfNeeded();
        }
        String[] in = iter.next();
        if (in == null || in.length == 0) {
          continue;
        }

        // Exception on reading row was encountered, meaning record is incomplete and not written
        if (iter.hasRowError()) {
          writePublicationLogMessage("Error reading line #" + line + "\n" + iter.getErrorMessage());
          recordsWithError++;
          currRecordsSkipped++;
        }
        // empty line was encountered, meaning record only contains empty values and not written
        else if (isEmptyLine(in)) {
          writePublicationLogMessage("Empty line was skipped. SourceBase:"
              + schemaMapping.getSource().getName() + " Line #" + line + ": " + printLine(in));
          emptyLines++;
          currRecordsSkipped++;
        } else {

          if (maxMappedColumnIndexOpt.isPresent() && in.length <= maxMappedColumnIndexOpt.get()) {
            writePublicationLogMessage("Line with fewer columns than mapped. SourceBase:"
                + schemaMapping.getSource().getName()
                + " Line #" + line + " has " + in.length + " Columns: " + printLine(in));
            // input row is smaller than the highest mapped column. Resize array by adding nulls
            String[] in2 = new String[maxMappedColumnIndexOpt.get() + 1];
            System.arraycopy(in, 0, in2, 0, in.length);
            in = in2;
            linesWithWrongColumnNumber++;
          }

          String newRow = commaRow(in, subschemaFieldMappings);
          writer.write(newRow);
          currRecords++;
        }
      }
    } catch (InterruptedException e) {
      // set last error report!
      setState(e);
      throw e;
    } catch (Exception e) {
      // some error writing this file, report
      log.error("Fatal Data Package Generator Error encountered", e);
      // set last error report!
      setState(e);
      throw new GeneratorException("Error writing data file for mapping " + currSubschema
          + " in source " + schemaMapping.getSource().getName() + ", line " + line, e);
    } finally {
      if (iter != null) {
        // Exception on advancing cursor encountered?
        if (!iter.hasRowError() && iter.getErrorMessage() != null) {
          writePublicationLogMessage("Error reading data: " + iter.getErrorMessage());
        }
        try {
          iter.close();
        } catch (Exception e) {
          log.error("Error while closing iterator", e);
        }
      }
    }

    // common message part used in constructing all reporting messages below
    String mp = " for mapping " + schemaMapping.getDataSchema().getTitle() + " in source " + schemaMapping.getSource().getName();

    // add lines incomplete message
    if (recordsWithError > 0) {
      addMessage(Level.WARN, recordsWithError + " record(s) skipped due to errors" + mp);
    } else {
      writePublicationLogMessage("No lines were skipped due to errors" + mp);
    }

    // add empty lines message
    if (emptyLines > 0) {
      addMessage(Level.WARN, emptyLines + " empty line(s) skipped" + mp);
    } else {
      writePublicationLogMessage("No lines were skipped due to errors" + mp);
    }

    // add wrong lines user message
    if (linesWithWrongColumnNumber > 0) {
      addMessage(Level.WARN, linesWithWrongColumnNumber + " line(s) with fewer columns than mapped" + mp);
    } else {
      writePublicationLogMessage("No lines with fewer columns than mapped" + mp);
    }
  }

  /**
   * Generates a single comma delimited row from the list of values of the provided array.
   * </br>
   * Note all line breaking characters in the value get replaced with an empty string before its added to the row.
   * </br>
   * The row ends in a newline character.
   *
   * @param columns the array of values from the source
   * @param subschemaFieldMappings field mappings
   *
   * @return the comma delimited String, {@code null} if provided array only contained null values
   */
  protected String commaRow(String[] columns, List<DataSchemaFieldMapping> subschemaFieldMappings) {
    Objects.requireNonNull(columns);
    StringBuilder sb = new StringBuilder();
    Iterator<DataSchemaFieldMapping> iter = subschemaFieldMappings.iterator();

    while (iter.hasNext()) {
      DataSchemaFieldMapping fieldMapping = iter.next();

      // append iv value is mapped
      if (fieldMapping.getIndex() != null) {
        sb.append(ESCAPE_CHARS.matcher(columns[fieldMapping.getIndex()]).replaceAll(""));
      }

      if (iter.hasNext()) {
        sb.append(",");
      }
    }

    return sb.append("\n").toString();
  }

  /**
   * Check if each string in array is empty. Method joins each string together and then checks if it is blank. A
   * blank string represents an empty line in a source data file.
   *
   * @param line string array
   *
   * @return true if each string in array is empty, false otherwise
   */
  private boolean isEmptyLine(String[] line) {
    String joined = Arrays.stream(line)
        .filter(Objects::nonNull)
        .collect(Collectors.joining(""));
    return StringUtils.isBlank(joined);
  }

  /**
   * Print a line representation of a string array used for logging.
   *
   * @param in String array
   * @return line
   */
  private String printLine(String[] in) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int i = 0; i < in.length; i++) {
      sb.append(in[i]);
      if (i != in.length - 1) {
        sb.append("; ");
      }
    }
    sb.append("]");
    return sb.toString();
  }

  /**
   * Write message from exception to publication log file as a new line but suffocate any exception thrown.
   *
   * @param e exception to write message from
   */
  private void writeFailureToPublicationLog(Throwable e) {
    StringBuilder sb = new StringBuilder();
    sb.append("Data package generation failed!\n");

    // write exception as nicely formatted string
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    sb.append(sw);

    // write to publication log file
    writePublicationLogMessage(sb.toString());
  }

  /**
   * Adds metadata to data package.
   *
   * @throws GeneratorException if there are issues with metadata file
   * @throws InterruptedException if executing thread was interrupted
   */
  private void addMetadata() throws GeneratorException, InterruptedException {
    checkForInterruption();
    setState(GenerateDataPackage.STATE.METADATA);
    try {
      String type = resource.getCoreType();

      if (CAMTRAP_DP.equals(type)) {
        addCamtrapMetadata();
      } else if (COL_DP.equals(type)) {
        addColMetadata();
      } else {
        addMessage(Level.WARN, "Metadata was not added: unknown type " + type);
      }

    } catch (IOException e) {
      throw new GeneratorException("Problem occurred while adding metadata file to data package folder", e);
    }
    // final reporting
    addMessage(Level.INFO, "Metadata added");
  }

  private void addColMetadata() throws IOException {
    File metadataFile = dataDir.resourceDatapackageMetadataFile(resource.getShortname(), resource.getCoreType());
    ColMetadata colMetadata = metadataReader.readValue(metadataFile, ColMetadata.class);

    // Basic metadata
    setDataPackageProperty("created",
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date()));
    setDataPackageProperty("version", colMetadata.getVersion());
    setDataPackageStringProperty("title", colMetadata.getTitle());
    setDataPackageCollectionProperty("contributors", colMetadata.getContributor());
    setDataPackageStringProperty("description", colMetadata.getDescription());
    setDataPackageCollectionProperty("keywords", colMetadata.getKeyword());
    setDataPackageProperty("homepage", colMetadata.getUrl());
    setDataPackageCollectionProperty("licenses", Collections.singleton(colMetadata.getLicense()));

    // additional properties
    colMetadata.getAdditionalProperties().forEach((key, value) -> dataPackage.setProperty(key, value));
  }

  private void addCamtrapMetadata() throws IOException {
    File metadataFile = dataDir.resourceDatapackageMetadataFile(resource.getShortname(), resource.getCoreType());
    CamtrapMetadata camtrapMetadata = metadataReader.readValue(metadataFile, CamtrapMetadata.class);

    // Basic metadata
    setDataPackageProperty("created",
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(camtrapMetadata.getCreated()));
    setDataPackageProperty("version", camtrapMetadata.getVersion());
    setDataPackageStringProperty("title", camtrapMetadata.getTitle());
    setDataPackageCollectionProperty("contributors", camtrapMetadata.getContributors());
    setDataPackageStringProperty("description", camtrapMetadata.getDescription());
    setDataPackageCollectionProperty("keywords", camtrapMetadata.getKeywords());
    setDataPackageStringProperty("image", camtrapMetadata.getImage());
    setDataPackageProperty("homepage", camtrapMetadata.getHomepage());
    setDataPackageCollectionProperty("sources", camtrapMetadata.getSources());
    setDataPackageCollectionProperty("licenses", camtrapMetadata.getLicenses());

    // Geographic scope
    setDataPackageProperty("spatial", camtrapMetadata.getSpatial());
    setDataPackageProperty("coordinatePrecision", camtrapMetadata.getCoordinatePrecision());

    // Taxonomic scope
    setDataPackageCollectionProperty("taxonomic", camtrapMetadata.getTaxonomic());

    // Temporal scope
    setDataPackageProperty("temporal", camtrapMetadata.getTemporal());

    // Project
    setDataPackageProperty("project", camtrapMetadata.getProject());

    // Other metadata
    setDataPackageStringProperty("bibliographicCitation", camtrapMetadata.getBibliographicCitation());
    setDataPackageCollectionProperty("references", camtrapMetadata.getReferences());
    setDataPackageCollectionProperty("relatedIdentifiers", camtrapMetadata.getRelatedIdentifiers());
  }

  private void setDataPackageProperty(String name, Object property) {
    if (property != null) {
      dataPackage.setProperty(name, property);
    }
  }

  private void setDataPackageStringProperty(String name, String property) {
    if (StringUtils.isNotEmpty(property)) {
      dataPackage.setProperty(name, property);
    }
  }

  @SuppressWarnings("rawtypes")
  private void setDataPackageCollectionProperty(String name, Collection property) {
    if (property != null && !property.isEmpty()) {
      dataPackage.setProperty(name, property);
    }
  }
}