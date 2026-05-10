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
package org.springframework.samples.petclinic.vet;

import jakarta.persistence.*;

import java.io.Serializable;

/**
 * This class represents the many-to-many relationship between Vet and Specialty.
 *
 * @author Vlad Mihalcea
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Arjen Poutsma
 */
@Entity
@Table(name = "vet_specialties")
public class VetSpeciality {

	@EmbeddedId
	private Id id;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("vetId")
	private Vet vet;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("specialtyId")
	private Specialty specialty;

	public VetSpeciality() {
	}

	public VetSpeciality(Vet vet, Specialty specialty) {
		this.vet = vet;
		this.specialty = specialty;
		this.id = new Id(vet.getId(), specialty.getId());
	}

	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}

	public Vet getVet() {
		return vet;
	}

	public void setVet(Vet vet) {
		this.vet = vet;
	}

	public Specialty getSpecialty() {
		return specialty;
	}

	public void setSpecialty(Specialty specialty) {
		this.specialty = specialty;
	}

	@Embeddable
	public static class Id implements Serializable {

		@Column(name = "vet_id")
		private Integer vetId;

		@Column(name = "specialty_id")
		private Integer specialtyId;

		public Id() {
		}

		public Id(Integer vetId, Integer specialtyId) {
			this.vetId = vetId;
			this.specialtyId = specialtyId;
		}

		public Integer getVetId() {
			return vetId;
		}

		public void setVetId(Integer vetId) {
			this.vetId = vetId;
		}

		public Integer getSpecialtyId() {
			return specialtyId;
		}

		public void setSpecialtyId(Integer specialtyId) {
			this.specialtyId = specialtyId;
		}

		@Override
		public final boolean equals(Object o) {
			if (!(o instanceof Id that)) return false;

			return getVetId().equals(that.getVetId()) && getSpecialtyId().equals(that.getSpecialtyId());
		}

		@Override
		public int hashCode() {
			int result = getVetId().hashCode();
			result = 31 * result + getSpecialtyId().hashCode();
			return result;
		}
	}
}
