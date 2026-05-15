package org.springframework.samples.petclinic.owner;

import com.blazebit.persistence.spring.data.repository.KeysetAwarePage;
import com.blazebit.persistence.spring.data.repository.KeysetPageRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class OwnerService {

	public record OwnerWithPetsPage(
		OwnerView owner,
		KeysetAwarePage<PetDetailView> petsPage,
		Map<Integer, List<Visit>> visitsByPetId,
		Map<Integer, Long> visitCountByPetId) {}

	public record PetVisitsPage(
		OwnerView owner,
		PetDetailView pet,
		KeysetAwarePage<VisitView> visitsPage) {}

	private final OwnerRepository ownerRepository;
	private final OwnerViewRepository ownerViewRepository;
	private final PetViewRepository petViewRepository;
	private final VisitViewRepository visitViewRepository;

	public OwnerService(OwnerRepository ownerRepository,
						OwnerViewRepository ownerViewRepository,
						PetViewRepository petViewRepository,
						VisitViewRepository visitViewRepository) {
		this.ownerRepository = ownerRepository;
		this.ownerViewRepository = ownerViewRepository;
		this.petViewRepository = petViewRepository;
		this.visitViewRepository = visitViewRepository;
	}

	public Page<OwnerView> findPaginatedForOwnersLastName(String lastname, Pageable pageable) {
		KeysetPageRequest keysetPageRequest = new KeysetPageRequest(null, Sort.by("id"),
			(int) pageable.getOffset(), pageable.getPageSize());
		return ownerViewRepository.findByLastNameLike(lastname + "%", keysetPageRequest);
	}

	public OwnerWithPetsPage findOwnerWithPaginatedPets(Integer id, int petPage, int petPageSize) {
		OwnerView owner = ownerViewRepository.findOne(id);
		if (owner == null) {
			return null;
		}

		Sort petSort = Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"));
		KeysetPageRequest petPageRequest = new KeysetPageRequest(
			null, petSort, (petPage - 1) * petPageSize, petPageSize);
		KeysetAwarePage<PetDetailView> petsPage = petViewRepository.findByOwnerId(id, petPageRequest);

		List<Integer> petIds = petsPage.getContent().stream()
			.map(PetDetailView::getId)
			.collect(Collectors.toList());

		Map<Integer, List<Visit>> visitsByPetId = Map.of();
		Map<Integer, Long> visitCountByPetId = Map.of();
		if (!petIds.isEmpty()) {
			List<Visit> visits = ownerRepository.findMaxVisitsByPetIds(petIds, 5);
			visitsByPetId = visits.stream()
				.collect(Collectors.groupingBy(v -> v.getPet().getId()));
			visitCountByPetId = ownerRepository.countVisitsByPetIds(petIds).stream()
				.collect(Collectors.toMap(
					OwnerRepository.PetVisitCount::getPetId,
					OwnerRepository.PetVisitCount::getVisitCount
				));
		}

		return new OwnerWithPetsPage(owner, petsPage, visitsByPetId, visitCountByPetId);
	}

	public PetVisitsPage findPaginatedVisitsForPet(Integer ownerId, Integer petId, int visitPage, int visitPageSize) {
		OwnerView owner = ownerViewRepository.findOne(ownerId);
		PetDetailView pet = petViewRepository.findOne(petId);
		if (owner == null || pet == null) {
			return null;
		}

		Sort visitSort = Sort.by(Sort.Order.desc("date"), Sort.Order.desc("id"));
		KeysetPageRequest visitPageRequest = new KeysetPageRequest(
			null, visitSort, (visitPage - 1) * visitPageSize, visitPageSize);
		KeysetAwarePage<VisitView> visitsPage = visitViewRepository.findByPetId(petId, visitPageRequest);

		return new PetVisitsPage(owner, pet, visitsPage);
	}
}
