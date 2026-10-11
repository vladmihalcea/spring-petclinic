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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads an owner together with its pets and visits using a small, fixed number of
 * queries, instead of letting the EAGER associations trigger an N+1. The pets are
 * paginated, and only the most recent visits of each pet are loaded, so the owner-details
 * view stays fast no matter how many visits a pet has accumulated.
 *
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class OwnerService {

	public record OwnerWithPetsPage(Owner owner, Page<Pet> petsPage, Map<Integer, Long> visitCountByPetId) {
	}

	public record PetVisitsPage(Owner owner, Pet pet, Page<Visit> visitsPage) {
	}

	private final OwnerRepository ownerRepository;

	public OwnerService(OwnerRepository ownerRepository) {
		this.ownerRepository = ownerRepository;
	}

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * Persists a new pet for the given owner without merging the whole owner aggregate,
	 * which would load the visits of every other pet of that owner.
	 */
	@Transactional
	public void addPet(Integer ownerId, Pet pet) {
		pet.setOwner(entityManager.getReference(Owner.class, ownerId));
		entityManager.persist(pet);
	}

	/**
	 * Updates a pet's details on the managed entity, so dirty checking writes a single
	 * {@code UPDATE} instead of merging the whole owner aggregate.
	 */
	@Transactional
	public void updatePet(Integer petId, Pet pet) {
		Pet existingPet = entityManager.find(Pet.class, petId);
		existingPet.setName(pet.getName());
		existingPet.setBirthDate(pet.getBirthDate());
		existingPet.setType(pet.getType());
	}

	/**
	 * Persists a new visit for the given pet without merging the whole owner aggregate.
	 */
	@Transactional
	public void addVisit(Integer petId, Visit visit) {
		visit.setPet(entityManager.getReference(Pet.class, petId));
		entityManager.persist(visit);
	}

	public OwnerWithPetsPage findOwnerWithPaginatedPets(Integer id, int petPage, int petPageSize) {
		Owner owner = ownerRepository.findById(id).orElse(null);
		if (owner == null) {
			return null;
		}

		Pageable pageable = PageRequest.of(petPage - 1, petPageSize, Sort.by("name"));
		Page<Pet> petsPage = ownerRepository.findPetsByOwnerId(id, pageable);

		List<Integer> petIds = petsPage.getContent().stream().map(Pet::getId).collect(Collectors.toList());
		Map<Integer, Long> visitCountByPetId = Map.of();
		if (!petIds.isEmpty()) {
			Map<Integer, Pet> petById = petsPage.getContent()
				.stream()
				.collect(Collectors.toMap(Pet::getId, pet -> pet));
			for (Pet pet : petsPage.getContent()) {
				pet.setVisits(new LinkedHashSet<>());
			}
			List<Visit> visits = ownerRepository.findMaxVisitsByPetIds(petIds, 5);
			visits.forEach(visit -> petById.get(visit.getPet().getId()).getVisits().add(visit));

			visitCountByPetId = ownerRepository.countVisitsByPetIds(petIds)
				.stream()
				.collect(Collectors.toMap(OwnerRepository.PetVisitCount::getPetId,
						OwnerRepository.PetVisitCount::getVisitCount));
		}

		return new OwnerWithPetsPage(owner, petsPage, visitCountByPetId);
	}

	/**
	 * Loads an owner with its pets and their types, and initializes the visits of the
	 * given pet only, so the new-visit form can list that pet's previous visits after the
	 * transaction has ended, without loading the visits of every other pet.
	 */
	public Owner findOwnerWithPetVisits(Integer ownerId, Integer petId) {
		Owner owner = ownerRepository.findByIdWithPets(ownerId).orElse(null);
		if (owner != null) {
			Pet pet = owner.getPet(petId);
			if (pet != null) {
				Hibernate.initialize(pet.getVisits());
			}
		}
		return owner;
	}

	public PetVisitsPage findPaginatedVisitsForPet(Integer ownerId, Integer petId, int visitPage, int visitPageSize) {
		Owner owner = ownerRepository.findById(ownerId).orElse(null);
		Pet pet = ownerRepository.findPetById(petId).orElse(null);
		if (owner == null || pet == null) {
			return null;
		}

		Pageable pageable = PageRequest.of(visitPage - 1, visitPageSize);
		Page<Visit> visitsPage = ownerRepository.findVisitsByPetId(petId, pageable);
		return new PetVisitsPage(owner, pet, visitsPage);
	}

}
