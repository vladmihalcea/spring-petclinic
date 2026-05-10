package org.springframework.samples.petclinic.owner;

import com.blazebit.persistence.spring.data.repository.KeysetPageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class OwnerService {

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

	public Owner findOwnerWithPetsAndVisits(Integer id) {
		Owner owner = ownerRepository.findByIdWithPets(id);
		List<Pet> pets = ownerRepository.findPetsWithVisitsByOwnerId(id);
		return owner;
	}
}
