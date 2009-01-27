package org.gbif.provider.webapp.action.portal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.struts2.interceptor.SessionAware;
import org.gbif.provider.model.ChecklistResource;
import org.gbif.provider.model.DataResource;
import org.gbif.provider.model.OccurrenceResource;
import org.gbif.provider.model.Resource;
import org.gbif.provider.model.eml.Eml;
import org.gbif.provider.model.eml.Role;
import org.gbif.provider.model.eml.TaxonKeyword;
import org.gbif.provider.model.voc.Rank;
import org.gbif.provider.service.EmlManager;
import org.gbif.provider.service.ResourceKeywordManager;
import org.gbif.provider.webapp.action.BaseMetadataResourceAction;
import org.gbif.provider.webapp.action.BaseResourceAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.opensymphony.xwork2.Preparable;

public class ResourceAction extends BaseMetadataResourceAction implements Preparable{
	@Autowired
	private EmlManager emlManager;
	@Autowired
	private ResourceKeywordManager keywordManager;
	private Eml eml;
	private String format;
    private List<Resource> resources;
    // for meta portal
    private List<String> alphabet;
    private List<String> keywords;
    private Map<String, Integer> tagcloud;
    // for searching
	private String keyword;
	private String q;
	private Double bbox_top;
	private Double bbox_bottom;
	private Double bbox_left;
	private Double bbox_right;
	
	public String execute(){
		if (resource!=null){
			eml = emlManager.load(resource);
			tagcloud=keywordManager.getCloud();
			return SUCCESS;
		}		
		return RESOURCE404;
	}
	
	public String forward(){
		if (resource instanceof OccurrenceResource) {
			return OCCURRENCE;
		}else if (resource instanceof ChecklistResource) {
			return CHECKLIST;
		}else{
			return METADATA;
		}
	}

	public String list(){
		resource=null;
		resources = resourceManager.getLatest(0, 500);
		alphabet=keywordManager.getAlphabet();
		if (alphabet.isEmpty()){
			keywords = new ArrayList();
		}else{
			keywords = keywordManager.getKeywords(alphabet.get(0));
		}
		tagcloud=keywordManager.getCloud();
		return SUCCESS;
	}

	public String rss(){
		resources = resourceManager.getLatest(0, 25);
		return SUCCESS;
	}

	public String search() {
		if (q!=null){
			resources = resourceManager.search(q);
		}else{
			resources = resourceManager.searchByKeyword(keyword);
		}
		tagcloud=keywordManager.getCloud();
		return SUCCESS;
	}
	
	public String geoSearch() {
		resources = resourceManager.searchByBBox(bbox_top,bbox_bottom,bbox_left,bbox_right);
		tagcloud=keywordManager.getCloud();
		return SUCCESS;
	}
	
	
	
	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}
	
	public Eml getEml() {
		return eml;
	}

	public void setEml(Eml eml) {
		this.eml = eml;
	}

	public List<Resource> getResources() {
		return resources;
	}

	public Map<String, Integer> getTagcloud() {
		return tagcloud;
	}

	public List<String> getAlphabet() {
		return alphabet;
	}

	public List<String> getKeywords() {
		return keywords;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public String getQ() {
		return q;
	}

	public void setQ(String q) {
		this.q = q;
	}

	public void setBbox_top(Double bbox_top) {
		this.bbox_top = bbox_top;
	}

	public void setBbox_bottom(Double bbox_bottom) {
		this.bbox_bottom = bbox_bottom;
	}

	public void setBbox_left(Double bbox_left) {
		this.bbox_left = bbox_left;
	}

	public void setBbox_right(Double bbox_right) {
		this.bbox_right = bbox_right;
	}
		
}
