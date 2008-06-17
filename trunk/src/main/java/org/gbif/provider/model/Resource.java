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


import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import org.appfuse.model.User;
import org.gbif.provider.model.hibernate.Timestampable;
import org.gbif.provider.service.Resolvable;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * A generic resource describing any digitial, online and non digital available biological resources
 * Should be replaced by a proper GBRDS model class.
 * Lacking most properties and multilingual abilities
 * @author markus
 *
 */
@Entity
public class Resource extends ResolvableBase implements Timestampable{
	// resource metadata
	private String title;
	private String description;
	// resource meta-metadata
	private User creator;
	private Date created;
	private User modifier;

	
	@Column(length=128)
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	@Lob
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	
	@ManyToOne
	public User getCreator() {
		return creator;
	}
	public void setCreator(User creator) {
		this.creator = creator;
	}
	
	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}
	
	@ManyToOne
	public User getModifier() {
		return modifier;
	}
	public void setModifier(User modifier) {
		this.modifier = modifier;
	}
	
	
	/**
	 * @see java.lang.Object#equals(Object)
	 */
	public boolean equals(Object object) {
		if (!(object instanceof Resource)) {
			return false;
		}
		Resource rhs = (Resource) object;
		return new EqualsBuilder().appendSuper(super.equals(object)).append(
				this.created, rhs.created).append(this.creator, rhs.creator)
				.append(this.title, rhs.title).append(this.modifier,
						rhs.modifier).append(this.description, rhs.description)
				.isEquals();
	}
	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return new HashCodeBuilder(-1602376339, -1881026299).appendSuper(
				super.hashCode()).append(this.created).append(this.creator)
				.append(this.title).append(this.modifier).append(
						this.description).toHashCode();
	}
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return new ToStringBuilder(this).appendSuper(super.toString()).append(
				"created", this.created).append("modified", this.getModified())
				.append("creator", this.creator).append("description",
						this.description).append("id", this.getId()).append(
						"title", this.title).append("modifier", this.modifier)
				.append("uuid", this.getUuid()).append("uri", this.getUri())
				.toString();
	}


}
