package org.springframework.samples.petclinic.owner;

import com.blazebit.persistence.spring.data.repository.EntityViewRepository;
import com.blazebit.persistence.spring.data.repository.KeysetAwarePage;
import com.blazebit.persistence.spring.data.repository.KeysetPageable;

/**
 * Blaze Persistence repository for {@link VisitView} with keyset pagination support.
 */
public interface VisitViewRepository extends EntityViewRepository<VisitView, Integer> {
	KeysetAwarePage<VisitView> findByPetId(Integer petId, KeysetPageable pageable);
}

