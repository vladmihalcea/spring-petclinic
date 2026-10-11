/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.model;

import jakarta.persistence.MappedSuperclass;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.util.Objects;

/**
 * Simple JavaBean domain object with an id property. Used as a base class for objects
 * needing this property.
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 */
@MappedSuperclass
public abstract class BaseEntity implements Serializable {

	public abstract Integer getId();

	public abstract void setId(Integer id);

	public boolean isNew() {
		return this.getId() == null;
	}

	/**
	 * Two entities are equal when they are of the same class and share the same,
	 * already-assigned identifier, so an entity can be found in a {@code HashSet} both
	 * before and after it is persisted or merged. The class is resolved without
	 * initializing a Hibernate proxy.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || effectiveClass(this) != effectiveClass(o)) {
			return false;
		}
		BaseEntity other = (BaseEntity) o;
		return getId() != null && Objects.equals(getId(), other.getId());
	}

	@Override
	public int hashCode() {
		return effectiveClass(this).hashCode();
	}

	private static Class<?> effectiveClass(Object entity) {
		return entity instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass()
				: entity.getClass();
	}

}
