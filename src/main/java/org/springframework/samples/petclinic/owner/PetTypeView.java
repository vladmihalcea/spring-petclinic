package org.springframework.samples.petclinic.owner;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;

/**
 * Blaze Persistence Entity View for {@link PetType}. Fetches id and name, and overrides
 * {@code toString()} so that Thymeleaf's {@code ${pet.type}} expression renders the type name.
 *
 * @author Vlad Mihalcea
 */
@EntityView(PetType.class)
public abstract class PetTypeView {

	@IdMapping
	public abstract Integer getId();

	public abstract String getName();

	@Override
	public String toString() {
		return getName();
	}

}

