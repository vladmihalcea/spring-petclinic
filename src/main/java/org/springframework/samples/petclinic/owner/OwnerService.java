package org.springframework.samples.petclinic.owner;

import com.blazebit.persistence.spring.data.repository.KeysetPageRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class OwnerService {

	private final OwnerViewRepository ownerViewRepository;

	public OwnerService(OwnerViewRepository ownerViewRepository) {
		this.ownerViewRepository = ownerViewRepository;
	}

	public Page<OwnerView> findPaginatedForOwnersLastName(String lastname, Pageable pageable) {
		KeysetPageRequest keysetPageRequest = new KeysetPageRequest(null, Sort.by("id"),
				(int) pageable.getOffset(), pageable.getPageSize());
		return ownerViewRepository.findByLastNameLike(lastname + "%", keysetPageRequest);
	}

}
