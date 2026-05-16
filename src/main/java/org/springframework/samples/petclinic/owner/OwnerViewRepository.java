package org.springframework.samples.petclinic.owner;

import com.blazebit.persistence.spring.data.repository.EntityViewRepository;
import com.blazebit.persistence.spring.data.repository.KeysetAwarePage;
import com.blazebit.persistence.spring.data.repository.KeysetPageable;

/**
 * Blaze Persistence repository for {@link OwnerView} providing keyset pagination support
 * for the owner search by last name.
 *
 * @author Vlad Mihalcea
 */
public interface OwnerViewRepository extends EntityViewRepository<OwnerView, Integer> {

	/**
	 * Retrieve {@link OwnerView}s whose last name matches the given LIKE pattern. Uses
	 * Blaze Persistence keyset pagination for efficient result-set scrolling.
	 * @param lastName LIKE pattern (e.g. {@code "Smith%"})
	 * @param pageable keyset-aware pageable; supply a {@code KeysetPageRequest} with a
	 * non-null {@code KeysetPage} to benefit from keyset optimization
	 * @return a keyset-aware page of matching owner views
	 */
	KeysetAwarePage<OwnerView> findByLastNameLike(String lastName, KeysetPageable pageable);

}

