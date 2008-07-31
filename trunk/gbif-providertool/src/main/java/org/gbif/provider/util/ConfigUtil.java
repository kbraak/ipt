/***************************************************************************
* Copyright (C) 2008 Global Biodiversity Information Facility Secretariat.
* All Rights Reserved.
*
* The contents of this file are subject to the Mozilla Public
* License Version 1.1 (the "License"); you may not use this file
* except in compliance with the License. You may obtain a copy of
* the License at http://www.mozilla.org/MPL/
*
* Software distributed under the License is distributed on an "AS
* IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
* implied. See the License for the specific language governing
* rights and limitations under the License.

***************************************************************************/

package org.gbif.provider.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gbif.provider.job.OccDbUploadJob;
import org.gbif.provider.model.CoreRecord;
import org.gbif.provider.model.ViewCoreMapping;
import org.gbif.provider.model.DatasourceBasedResource;
import org.gbif.provider.model.Extension;
import org.gbif.provider.model.ExtensionProperty;
import org.gbif.provider.model.Resource;
import org.gbif.provider.model.ViewMappingBase;


/**
 * Constant values used throughout the application.
 */
public class ConfigUtil {
	protected static final Log log = LogFactory.getLog(ConfigUtil.class);
    private static String WEBAPP_DIR;
    private static File WEBAPP_DIR_FILE;
    private static String APP_BASE_URL;
    
    
    /**
     * only sets base dir once when first called
     * @param webappDir
     */
    public static void setWebappDir(String webappDir) {
    	if (WEBAPP_DIR == null){
    		if (webappDir == null){
    		    throw new NullPointerException();    			
    		}else{
    			WEBAPP_DIR_FILE = new File(webappDir);
    			if (WEBAPP_DIR_FILE.exists()){
    	    		log.info("set webapp directory to "+webappDir);
    	    		WEBAPP_DIR = webappDir;
    			}else{
    				throw new IllegalArgumentException(String.format("Webapp directory %s doesn't exist.", webappDir));
    			}
    		}
    	}
	}

	public static File getWebappDir() {
		return WEBAPP_DIR_FILE;
	}

	public void setAppBaseUrl(String appBaseUrl) {
		appBaseUrl=appBaseUrl.trim();
		while(appBaseUrl.endsWith("/")){
			appBaseUrl = (String) appBaseUrl.subSequence(0, appBaseUrl.length()-1);
		}
		APP_BASE_URL = appBaseUrl;
	}

	public static String getAppBaseUrl(){
		return APP_BASE_URL;
	}
	
}
