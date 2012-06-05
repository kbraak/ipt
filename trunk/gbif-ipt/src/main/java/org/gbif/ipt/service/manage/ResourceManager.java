package org.gbif.ipt.service.manage;

import org.gbif.ipt.action.BaseAction;
import org.gbif.ipt.model.Ipt;
import org.gbif.ipt.model.Organisation;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.User;
import org.gbif.ipt.model.voc.PublicationStatus;
import org.gbif.ipt.service.AlreadyExistingException;
import org.gbif.ipt.service.DeletionNotAllowedException;
import org.gbif.ipt.service.ImportException;
import org.gbif.ipt.service.InvalidConfigException;
import org.gbif.ipt.service.PublicationException;
import org.gbif.ipt.service.manage.impl.ResourceManagerImpl;
import org.gbif.ipt.task.StatusReport;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.google.inject.ImplementedBy;

/**
 * This interface details ALL methods associated with the main resource entity.
 * The manager keeps a map of the basic metadata and authorisation information in memory, but further details like the
 * full EML or mapping configuration is stored in files and loaded into manager sessions when needed.
 */
@ImplementedBy(ResourceManagerImpl.class)
public interface ResourceManager {

  boolean cancelPublishing(String shortname, BaseAction action) throws PublicationException;

  Resource create(String shortname, File dwca, User creator, BaseAction asction)
    throws AlreadyExistingException, ImportException;

  Resource create(String shortname, User creator) throws AlreadyExistingException;

  void delete(Resource resource) throws IOException, DeletionNotAllowedException;

  Resource get(String shortname);

  /**
   * Return the size of the generated DwC-A file
   */
  long getDwcaSize(Resource resource);

  /**
   * Return the size of the generated EML file
   */
  long getEmlSize(Resource resource);

  /**
   * Returns the URL to a public resource in the IPT
   */
  URL getResourceLink(String shortname);

  /**
   * Returns the size of the generated RTF file
   */
  long getRtfSize(Resource resource);

  /**
   * Validate if the EML file exist for a specific resource in the data directory.
   *
   * @return true if EML File exist. False otherwise.
   */
  boolean isEmlExisting(String shortName);

  /**
   * @return true if resource is currently locked for any management
   */
  boolean isLocked(String shortname);

  /**
   * Validate if the RTF existence for a specific resource in the data directory.
   *
   * @return true if RTF File exist. false in otherwise.
   */
  boolean isRtfExisting(String shortName);

  /**
   * Returns the latest resources , order by last modified
   *
   * @return list of resources
   */
  List<Resource> latest(int startPage, int pageSize);

  /**
   * list all resources in the IPT
   */
  List<Resource> list();

  /**
   * list all resources in the IPT having a certain publication status
   */
  List<Resource> list(PublicationStatus status);

  /**
   * list all resource that can be managed by a given user
   */
  List<Resource> list(User user);

  /**
   * Load all configured resources from the datadir into memory.
   * We do not keep the EML or mapping configuration in memory for all resources, but we
   * maintain a map of the basic metadata and authorisation information in this manager.
   */
  int load();

  /**
   * Publishes a new version of a resource including generating a darwin core archive and issuing a new EML version.
   *
   * @param action the action to use for logging messages to
   *
   * @return true if a new asynchroneous dwca generation job has been issued which requires some mapped data
   *
   * @throws PublicationException if resource was already registered
   */
  boolean publish(Resource resource, BaseAction action) throws PublicationException;

  /**
   * Issues a new EML version for the given resource.
   *
   * @param action the action to use for logging messages
   *
   * @throws PublicationException if resource was already registered
   */
  void publishMetadata(Resource resource, BaseAction action) throws PublicationException;

  /**
   * Registers the resource with the GBIF Registry.
   *
   * @param resource the published resource
   * @param organisation the organization that owns the resource
   * @param ipt          the ipt that the resource will be published through
   */
  void register(Resource resource, Organisation organisation, Ipt ipt) throws InvalidConfigException;

  /**
   * Persists the whole resource configuration *but* not the EML file.
   */
  void save(Resource resource) throws InvalidConfigException;

  /**
   * Save the eml file of a resource only. Complementary method to @See save(Resource)
   */
  void saveEml(Resource resource) throws InvalidConfigException;

  /**
   * @param shortname for the resource
   *
   * @return status report of current task either running or on queue for the requested resource or null if none exists
   */
  StatusReport status(String shortname);

  /**
   * For a published resource, updates its existing dwca with the latest eml (presumably generated by publishEml,
   * above). Note that this method does not republish the dwca (so no data is refreshed, and no load hits any
   * databases), only repackages with the new eml.
   *
   * @param resource the published resource
   * @param action   the calling action that will receive log messages
   *
   * @throws PublicationException if the resource is locked or hasn't been published
   */
  void updateDwcaEml(Resource resource, BaseAction action) throws PublicationException;

  /**
   * Update the registration of the resource with the GBIF Registry.
   *
   * @param resource the published resource
   */
  void updateRegistration(Resource resource) throws InvalidConfigException;

  /**
   * makes a resource private
   *
   * @throws InvalidConfigException if resource was already registered
   */
  void visibilityToPrivate(Resource resource) throws InvalidConfigException;

  /**
   * Makes a resource public
   *
   * @throws InvalidConfigException if resource was already registered
   */
  void visibilityToPublic(Resource resource) throws InvalidConfigException;

}
