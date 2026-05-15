package org.springframework.samples.petclinic.owner;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import java.time.LocalDate;

/**
 * Blaze Persistence Entity View for {@link Pet}. Fetches only a subset of data required by the pet details page.
 *
 * @author Vlad Mihalcea
 */
@EntityView(Pet.class)
public interface PetDetailView {

	@IdMapping Integer getId();

	String getName();

	LocalDate getBirthDate();

	PetTypeView getType();
}
