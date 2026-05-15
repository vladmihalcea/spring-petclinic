package org.springframework.samples.petclinic.owner;

import com.blazebit.persistence.spring.data.repository.KeysetPageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class OwnerService {

	public record OwnerWithPetsPage(Owner owner, Page<Pet> petsPage, Map<Integer, Long> visitCountByPetId) {}

	public record PetVisitsPage(Owner owner, Pet pet, Page<Visit> visitsPage) {}

	private final OwnerRepository ownerRepository;
	private final OwnerViewRepository ownerViewRepository;

	public OwnerService(OwnerRepository ownerRepository,
						OwnerViewRepository ownerViewRepository) {
		this.ownerRepository = ownerRepository;
		this.ownerViewRepository = ownerViewRepository;
	}

	public Page<OwnerView> findPaginatedForOwnersLastName(String lastname, Pageable pageable) {
		KeysetPageRequest keysetPageRequest = new KeysetPageRequest(null, Sort.by("id"),
			(int) pageable.getOffset(), pageable.getPageSize());
		return ownerViewRepository.findByLastNameLike(lastname + "%", keysetPageRequest);
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
			Map<Integer, Pet> petById = petsPage.getContent().stream()
				.collect(Collectors.toMap(Pet::getId, p -> p));
			for (Pet pet : petsPage.getContent()) {
				pet.setVisits(new LinkedHashSet<>());
			}
			List<Visit> visits = ownerRepository.findMaxVisitsByPetIds(petIds, 5);
			visits.forEach(visit -> petById.get(visit.getPet().getId()).getVisits().add(visit));

			visitCountByPetId = ownerRepository.countVisitsByPetIds(petIds).stream()
				.collect(Collectors.toMap(
					OwnerRepository.PetVisitCount::getPetId,
					OwnerRepository.PetVisitCount::getVisitCount
				));
		}

		return new OwnerWithPetsPage(owner, petsPage, visitCountByPetId);
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
