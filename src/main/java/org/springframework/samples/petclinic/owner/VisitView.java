package org.springframework.samples.petclinic.owner;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import java.time.LocalDate;

/**
 * Blaze Persistence Entity View for {@link Visit}.
 * Fetches id, date, and description.
 */
@EntityView(Visit.class)
public interface VisitView {

	@IdMapping
	Integer getId();

	LocalDate getDate();

	String getDescription();
}
