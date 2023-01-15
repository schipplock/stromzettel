/*
 * Copyright 2023 Andreas Schipplock
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schipplock.apps.stromzettel.dao;

import de.schipplock.apps.stromzettel.model.ElectricityMeter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;

import java.util.List;

public class ElectricityMeterDAO {

    public final EntityManager em;

    public ElectricityMeterDAO() {
        em = Persistence.createEntityManagerFactory("jpaPU").createEntityManager();
    }

    public List<ElectricityMeter> findAll() {
        return em.createQuery("from ElectricityMeter", ElectricityMeter.class).getResultList();
    }

    public ElectricityMeter merge(ElectricityMeter electricityMeter) {
        ElectricityMeter mergedElectricityMeter;
        em.getTransaction().begin();
        mergedElectricityMeter = em.merge(electricityMeter);
        em.getTransaction().commit();
        return mergedElectricityMeter;
    }

    public void delete(ElectricityMeter electricityMeter) {
        em.getTransaction().begin();
        em.remove(electricityMeter);
        em.getTransaction().commit();
    }
}
