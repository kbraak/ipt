package org.gbif.provider.job;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.appfuse.service.GenericManager;
import org.gbif.logging.log.I18nLog;
import org.gbif.logging.log.I18nLogFactory;
import org.gbif.provider.datasource.impl.RdbmsImportSource;
import org.gbif.provider.model.DatasourceBasedResource;
import org.gbif.provider.model.Extension;
import org.gbif.provider.model.OccurrenceResource;
import org.gbif.provider.model.PropertyMapping;
import org.gbif.provider.model.Resource;
import org.gbif.provider.model.UploadEvent;
import org.gbif.provider.model.ViewMapping;
import org.gbif.provider.service.DatasourceBasedResourceManager;
import org.gbif.provider.service.DatasourceInspectionManager;
import org.gbif.provider.service.OccurrenceUploadManager;
import org.gbif.scheduler.model.Job;
import org.gbif.scheduler.scheduler.Launchable;
import org.gbif.util.JSONUtils;

public class RdbmsUploadJob implements Launchable{
	protected static final Log log = LogFactory.getLog(RdbmsUploadJob.class);
	private static I18nLog logdb = I18nLogFactory.getLog(RdbmsUploadJob.class);


    private DatasourceBasedResourceManager<OccurrenceResource> occResourceManager;
    private GenericManager<UploadEvent, Long> uploadEventManager;
    private DatasourceInspectionManager datasourceInspectionManager;
    private OccurrenceUploadManager occurrenceUploadManager;

	public static Job newUploadJob(Resource resource){
		// create job data
		Map<String, Object> seed = new HashMap<String, Object>();
		seed.put("resourceId", resource.getId());
		// create upload job
		Job job = new Job();
		job.setJobClassName(RdbmsUploadJob.class.getCanonicalName());
		job.setDataAsJSON(JSONUtils.jsonFromMap(seed));
		job.setJobGroup(JobUtils.getJobGroup(resource));
		job.setName("RDBMS data upload");
		job.setDescription("Data upload from RDBMS to resource "+resource.getTitle());
		return job;				
	}
    
	public void setOccResourceManager(
			DatasourceBasedResourceManager<OccurrenceResource> occResourceManager) {
		this.occResourceManager = occResourceManager;
	}

	public void setUploadEventManager(
			GenericManager<UploadEvent, Long> uploadEventManager) {
		this.uploadEventManager = uploadEventManager;
	}

	public void setDatasourceInspectionManager(
			DatasourceInspectionManager datasourceInspectionManager) {
		this.datasourceInspectionManager = datasourceInspectionManager;
	}

	public void setOccurrenceUploadManager(
			OccurrenceUploadManager occurrenceUploadManager) {
		this.occurrenceUploadManager = occurrenceUploadManager;
	}

	
	
	public void launch(Map<String, Object> seed) throws Exception {
		Long resourceId = Long.valueOf(seed.get("resourceId").toString()); 
		log.info("Starting Upload job for resource "+resourceId);
		OccurrenceResource resource = occResourceManager.get(resourceId);
		// create rdbms source
		ViewMapping coreViewMapping = resource.getCoreMapping();
		ResultSet rs = datasourceInspectionManager.executeViewSql(coreViewMapping.getViewSql());	        
		RdbmsImportSource source = RdbmsImportSource.newInstance(rs, coreViewMapping);
        UploadEvent coreEvent = new UploadEvent();
        coreEvent.setResource(resource);
		// upload records
		Map<String, Long> idMap = occurrenceUploadManager.uploadCore(source, resource, coreEvent);
		// save upload event
        Date now = new Date();
        coreEvent.setExecutionDate(now);
		uploadEventManager.save(coreEvent);
		// update resource properties
		resource.setLastImport(now);
		resource.setRecordCount(coreEvent.getRecordsUploaded());
		occResourceManager.save(resource);
		// upload further extensions one by one
		for (ViewMapping view : resource.getExtensionMappings().values()){
			rs = datasourceInspectionManager.executeViewSql(view.getViewSql());	        
			source = RdbmsImportSource.newInstance(rs, view);
			occurrenceUploadManager.uploadExtension(source, idMap, resource, view.getExtension());
		}
	}

	private void fakeUpload(Long resourceId){
		// add a week from last import
		OccurrenceResource resource = occResourceManager.get(resourceId);
        Date now = new Date(resource.getLastImport().getTime() + 604800000l);
		log.info("Fake upload for resource "+resourceId + " at date "+now);
        UploadEvent coreEvent = getFakeUploadEvent(resource);
        coreEvent.setResource(resource);
        coreEvent.setExecutionDate(now);
		// save upload event
		uploadEventManager.save(coreEvent);
		// update resource properties
		resource.setLastImport(now);
		resource.setRecordCount(coreEvent.getRecordsUploaded());
		occResourceManager.save(resource);
	}
	
	private static UploadEvent getFakeUploadEvent(DatasourceBasedResource resource){
		Random rnd = new Random();
        int numExistingRecords = resource.getRecordCount();
		int numAdded = rnd.nextInt(10000);
		int numDeleted = rnd.nextInt(numExistingRecords/100);
		int numChanged = rnd.nextInt((numExistingRecords-numDeleted-numAdded)/10);
		int numUploaded = numExistingRecords+numAdded-numDeleted;
        UploadEvent event = new UploadEvent();
        event.setRecordsAdded(numAdded);
        event.setRecordsDeleted(numDeleted);
        event.setRecordsChanged(numChanged);
        event.setRecordsUploaded(numUploaded);
		return event;
	}
}
