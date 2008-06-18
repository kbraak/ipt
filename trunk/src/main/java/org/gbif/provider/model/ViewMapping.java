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

package org.gbif.provider.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.hibernate.annotations.IndexColumn;

/**
 * A mapping between a resource and an extension (incl darwincore itself).
 * The ViewMapping defines the sql statement used to upload data for a certain extension,
 * therefore for every extension there exists a separate sql statement which should be uploaded one after the other.
 * @author markus
 *
 */
@Entity
public class ViewMapping implements Comparable<ViewMapping> {
	private Long id;	
	private DatasourceBasedResource resource;
	private DwcExtension extension;
	private String viewSql;
	private List<PropertyMapping> propertyMappings = new ArrayList<PropertyMapping>();
	
	@Id @GeneratedValue(strategy = GenerationType.AUTO) 
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	@ManyToOne
	@JoinColumn(name="resource_id", nullable=false) 
	public DatasourceBasedResource getResource() {
		return resource;
	}
	public void setResource(DatasourceBasedResource resource) {
		this.resource = resource;
	}
	
	@ManyToOne
	public DwcExtension getExtension() {
		return extension;
	}
	public void setExtension(DwcExtension extension) {
		this.extension = extension;
	}
	
	public String getViewSql() {
		return viewSql;
	}
	public void setViewSql(String sql) {
		this.viewSql = sql;
	}
	
	@OneToMany(cascade=CascadeType.ALL)
	@IndexColumn(name = "mapping_order",base=0, nullable=false)
	@JoinColumn(name="viewMapping_id", nullable=false) 
	public List<PropertyMapping> getPropertyMappings() {
		return propertyMappings;
	}
	public void setPropertyMappings(List<PropertyMapping> propertyMappings) {
		this.propertyMappings = propertyMappings;
	}
	
	public void addPropertyMapping(PropertyMapping propertyMapping) {
		propertyMapping.setViewMapping(this);
		propertyMappings.add(propertyMapping);
	}
	
	/**
	 * Natural sort order is resource, then extension
	 * @see java.lang.Comparable#compareTo(Object)
	 */
	public int compareTo(ViewMapping view) {
		int resCmp = resource.compareTo(view.resource); 
		return (resCmp != 0 ? resCmp : extension.compareTo(view.extension));
	}
	/**
	 * @see java.lang.Object#equals(Object)
	 */
	public boolean equals(Object object) {
		if (!(object instanceof ViewMapping)) {
			return false;
		}
		ViewMapping rhs = (ViewMapping) object;
		return new EqualsBuilder().append(this.extension, rhs.extension)
				.append(this.viewSql, rhs.viewSql).append(
						this.resource, rhs.resource).append(this.id, rhs.id)
				.isEquals();
	}
	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return new HashCodeBuilder(651663619, 212381131).append(this.extension)
				.append(this.viewSql).append(
						this.resource).append(this.id).toHashCode();
	}
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return new ToStringBuilder(this).append("viewSql", this.viewSql).append(
				"resource", this.resource).append("id", this.id).append(
				"extension", this.extension).toString();
	}
	
	
}
