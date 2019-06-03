/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.samples.petclinic.store;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "store_details")
public class StoreDetails {

    @Id
    private Integer id;

    @Column(name = "created_on")
    private LocalDate createdOn = LocalDate.now();

    private String owner;

    @OneToOne(cascade = CascadeType.ALL)
    @MapsId
    @JoinColumn(name = "id")
    private Store store;

    public Integer getId() {
        return id;
    }

    public StoreDetails setId(Integer id) {
        this.id = id;
        return this;
    }

    public LocalDate getCreatedOn() {
        return createdOn;
    }

    public StoreDetails setCreatedOn(LocalDate createdOn) {
        this.createdOn = createdOn;
        return this;
    }

    public String getOwner() {
        return owner;
    }

    public StoreDetails setOwner(String owner) {
        this.owner = owner;
        return this;
    }

    public Store getStore() {
        return store;
    }

    public StoreDetails setStore(Store store) {
        this.store = store;
        return this;
    }
}
