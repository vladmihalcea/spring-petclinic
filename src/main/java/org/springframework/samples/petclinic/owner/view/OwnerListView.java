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
package org.springframework.samples.petclinic.owner.view;

import java.util.List;

import com.blazebit.persistence.view.*;
import org.springframework.samples.petclinic.owner.Owner;

@EntityView(Owner.class)
public interface OwnerListView {

	@IdMapping
	Integer getId();

	String getFirstName();

	String getLastName();

	String getAddress();

	String getCity();

	String getTelephone();

	@Mapping(value = "pets", fetch = FetchStrategy.MULTISET)
	@Limit(limit = "50", order = {"id ASC"})
	List<OwnerPetNameView> getPets();

}
