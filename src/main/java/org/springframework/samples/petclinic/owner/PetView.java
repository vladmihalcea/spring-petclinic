package org.springframework.samples.petclinic.owner;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;

/**
 * Blaze Persistence Entity View for {@link Pet}. Fetches only the pet name, as required by
 * the owners list page.
 *
 * @author Vlad Mihalcea
 */
@EntityView(Pet.class)
public abstract class PetView {

	@IdMapping
	public abstract Integer getId();

	public abstract String getName();

	@Override
	public String toString() {
		return getName();
	}

}

