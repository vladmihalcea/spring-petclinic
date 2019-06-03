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

import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Repository
public class CustomStoreRepositoryImpl implements CustomStoreRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void insertAll(Iterable<Store> stores) {
        Session session = entityManager.unwrap(Session.class);
        session.setJdbcBatchSize(100);

        for (Store store : stores) {
            entityManager.persist(store);
        }
    }
}
