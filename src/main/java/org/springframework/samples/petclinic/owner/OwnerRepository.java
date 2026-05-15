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
	@Query("""
        select o
        from Owner o
        left join fetch o.pets p
        where o.id in (
            select or.id
            from (
              select
                 o1.id as id,
                 dense_rank() over (order by o1.id) as ranking
              from Owner o1
              where o1.lastName like :lastName
            ) or
            where or.ranking <= :maxCount
        )
        """
	)
	List<Owner> findByLastNameStartingWith(
		@Param("lastName") String lastName,
		@Param("maxCount") int maxCount
	);

	@Query("""
        select o
        from Owner o
        left join fetch o.pets
        where o.id = :id
        """
	)
	Owner findByIdWithPets(@Param("id") Integer id);

	@Query("""
        select p
        from Pet p
        left join fetch p.visits
        where p.owner.id = :id
        """
	)
	List<Pet> findPetsWithVisitsByOwnerId(@Param("id") Integer id);

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
        """
	)
	@QueryHints(
		@QueryHint(name = AvailableHints.HINT_READ_ONLY, value = "true")
	)
	List<Visit> findMaxVisitsByPetIds(
		@Param("petIds") Collection<Integer> petIds,
		@Param("maxVisitsCount") int maxVisitsCount
	);

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

	@Query(value = "select p from Pet p left join fetch p.type where p.owner.id = :id",
		countQuery = "select count(p) from Pet p where p.owner.id = :id")
	Page<Pet> findPetsByOwnerId(@Param("id") Integer id, Pageable pageable);

	@Query("select p from Pet p left join fetch p.type where p.id = :id")
	Optional<Pet> findPetById(@Param("id") Integer id);

	@Query("select v.pet.id as petId, count(v) as visitCount from Visit v where v.pet.id in :petIds group by v.pet.id")
	List<PetVisitCount> countVisitsByPetIds(@Param("petIds") Collection<Integer> petIds);

	@Query(value = "select v from Visit v where v.pet.id = :petId order by v.date desc, v.id desc",
		countQuery = "select count(v) from Visit v where v.pet.id = :petId")
	Page<Visit> findVisitsByPetId(@Param("petId") Integer petId, Pageable pageable);

}
