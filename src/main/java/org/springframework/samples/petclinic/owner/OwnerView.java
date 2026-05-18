package org.springframework.samples.petclinic.owner;

import java.util.List;
import java.util.stream.Collectors;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;

/**
 * Blaze Persistence Entity View for {@link Owner}. Fetches only the fields needed by the
 * owners list page: id, firstName, lastName, address, city, telephone, and the pet names.
 *
 * @author Vlad Mihalcea
 */
@EntityView(Owner.class)
public interface OwnerView {

	@IdMapping
	Integer getId();

	String getFirstName();

	String getLastName();

	String getAddress();

	String getCity();

	String getTelephone();

	List<PetView> getPets();

	default String getPetNames() {
		return getPets().stream().map(PetView::getName).collect(Collectors.joining(", "));
	}

}

