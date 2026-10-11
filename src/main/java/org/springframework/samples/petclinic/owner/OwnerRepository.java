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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.QueryHint;
import org.hibernate.jpa.AvailableHints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

/**
 * Repository class for <code>Owner</code> domain objects. All method names are compliant
 * with Spring Data naming conventions so this interface can easily be extended for Spring
 * Data. See:
 * https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.query-creation
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Michael Isvy
 * @author Wick Dynex
 */
public interface OwnerRepository extends JpaRepository<Owner, Integer> {

	/**
	 * A lightweight projection of the number of visits a pet has, used to show the
	 * "showing 5 of N" hint without loading every visit.
	 */
	interface PetVisitCount {

		Integer getPetId();

		Long getVisitCount();

	}

	/**
	 * Retrieve {@link Owner}s from the data store by last name, returning all owners
	 * whose last name <i>starts</i> with the given name.
	 * @param lastName Value to search for
	 * @return a Collection of matching {@link Owner}s (or an empty Collection if none
	 * found)
	 */
	Page<Owner> findByLastNameStartingWith(String lastName, Pageable pageable);

	/**
	 * Retrieve an {@link Owner} from the data store by id.
	 * <p>
	 * This method returns an {@link Optional} containing the {@link Owner} if found. If
	 * no {@link Owner} is found with the provided id, it will return an empty
	 * {@link Optional}.
	 * </p>
	 * @param id the id to search for
	 * @return an {@link Optional} containing the {@link Owner} if found, or an empty
	 * {@link Optional} if not found.
	 * @throws IllegalArgumentException if the id is null (assuming null is not a valid
	 * input for id)
	 */
	Optional<Owner> findById(Integer id);

	/**
	 * Retrieve an {@link Owner} together with its {@link Pet}s and their types, for the
	 * forms that look a pet up through its owner.
	 */
	@Query("select o from Owner o left join fetch o.pets p left join fetch p.type where o.id = :id")
	Optional<Owner> findByIdWithPets(@Param("id") Integer id);

	/**
	 * Retrieve the given {@link Owner}s together with their {@link Pet}s, so a page of
	 * owners can list their pets without a query per owner.
	 */
	@Query("select o from Owner o left join fetch o.pets where o.id in :ids order by o.id")
	List<Owner> findAllWithPetsByIdIn(@Param("ids") Collection<Integer> ids);

	/**
	 * Retrieve one page of a given owner's {@link Pet}s, fetching each pet's type in the
	 * same query so the owner-details view never triggers an N+1.
	 */
	@Query(value = "select p from Pet p left join fetch p.type where p.owner.id = :id",
			countQuery = "select count(p) from Pet p where p.owner.id = :id")
	Page<Pet> findPetsByOwnerId(@Param("id") Integer id, Pageable pageable);

	/**
	 * Retrieve a single {@link Pet} together with its type.
	 */
	@Query("select p from Pet p left join fetch p.type where p.id = :id")
	Optional<Pet> findPetById(@Param("id") Integer id);

	/**
	 * Retrieve the most recent visits of the given pets, at most {@code maxVisitsCount}
	 * per pet, in a single query that ranks the visits per pet with a window function.
	 */
	@Query("""
			select v
			from Visit v
			where v.id in (
			    select vr.id
			    from (
			      select
			         v1.id as id,
			         dense_rank() over (partition by v1.pet.id order by v1.date desc, v1.id desc) as ranking
			      from Visit v1
			      where v1.pet.id in :petIds
			    ) vr
			    where vr.ranking <= :maxVisitsCount
			)
			order by v.pet.id asc, v.date desc, v.id desc
			""")
	@QueryHints(@QueryHint(name = AvailableHints.HINT_READ_ONLY, value = "true"))
	List<Visit> findMaxVisitsByPetIds(@Param("petIds") Collection<Integer> petIds,
			@Param("maxVisitsCount") int maxVisitsCount);

	/**
	 * Count the visits of the given pets in a single grouped query.
	 */
	@Query("select v.pet.id as petId, count(v) as visitCount from Visit v where v.pet.id in :petIds group by v.pet.id")
	List<PetVisitCount> countVisitsByPetIds(@Param("petIds") Collection<Integer> petIds);

	/**
	 * Retrieve one page of a given pet's visits, most recent first.
	 */
	@Query(value = "select v from Visit v where v.pet.id = :petId order by v.date desc, v.id desc",
			countQuery = "select count(v) from Visit v where v.pet.id = :petId")
	Page<Visit> findVisitsByPetId(@Param("petId") Integer petId, Pageable pageable);

}
