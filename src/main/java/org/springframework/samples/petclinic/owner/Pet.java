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
package org.springframework.samples.petclinic.owner;

import io.hypersistence.utils.hibernate.id.BatchSequence;
import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.samples.petclinic.model.NamedEntity;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Simple business object representing a pet.
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Wick Dynex
 */
@Entity
@Table(name = "pets")
public class Pet extends NamedEntity {

	@Id
	@BatchSequence(name = "pets_seq", fetchSize = 1000)
	private Integer id;

	@Column
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate birthDate;

	@ManyToOne
	@JoinColumn(name = "type_id")
	private PetType type;

	@OneToMany(mappedBy = "pet", cascade = CascadeType.ALL)
	@OrderBy("date ASC")
	private final Set<Visit> visits = new LinkedHashSet<>();

	@ManyToOne(fetch = FetchType.LAZY)
	private Owner owner;

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public void setId(Integer id) {
		this.id = id;
	}

	public void setBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
	}

	public LocalDate getBirthDate() {
		return this.birthDate;
	}

	public PetType getType() {
		return this.type;
	}

	public void setType(PetType type) {
		this.type = type;
	}

	public Collection<Visit> getVisits() {
		return this.visits;
	}

	public void addVisit(Visit visit) {
		getVisits().add(visit);
		visit.setPet(this);
	}

	public void removeVisit(Visit visit) {
		getVisits().remove(visit);
		visit.setPet(null);
	}

	public Owner getOwner() {
		return this.owner;
	}
	public void setOwner(Owner owner) {
		this.owner = owner;
	}
}
