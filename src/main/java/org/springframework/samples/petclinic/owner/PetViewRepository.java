package org.springframework.samples.petclinic.owner;

import com.blazebit.persistence.spring.data.repository.EntityViewRepository;
import com.blazebit.persistence.spring.data.repository.KeysetAwarePage;
import com.blazebit.persistence.spring.data.repository.KeysetPageable;

/**
 * Blaze Persistence repository for {@link PetDetailView} with keyset pagination support.
 */
public interface PetViewRepository extends EntityViewRepository<PetDetailView, Integer> {
	KeysetAwarePage<PetDetailView> findByOwnerId(Integer ownerId, KeysetPageable pageable);
}
