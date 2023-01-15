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
package de.schipplock.apps.stromzettel.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import static java.lang.String.format;

import de.schipplock.apps.stromzettel.StromZettel;
import jakarta.persistence.*;

@Entity
@Table(name = "READINGS")
public class Reading implements Serializable {

    @Serial
    private static final long serialVersionUID = 6222864265152509463L;

    @ManyToOne
    @JoinColumn(name = "electricity_meter_id")
    private ElectricityMeter electricityMeter;
    
    @Id
    @SequenceGenerator(name = "readings_seq", sequenceName = "readings_sequence", initialValue = 1, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "readings_seq")
    protected Long id;
    
    protected Long readingValue;
    
    @Column(name = "readingDate")
    protected LocalDateTime readingDate;
    
    public Reading() {}

    public Reading(Long readingValue, LocalDateTime readingDate) {
        super();
        this.readingValue = readingValue;
        this.readingDate = readingDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReadingValue() {
        return readingValue;
    }

    public void setReadingValue(Long readingValue) {
        this.readingValue = readingValue;
    }

    public LocalDateTime getReadingDate() {
        return readingDate;
    }

    public void setReadingDate(LocalDateTime readingDate) {
        this.readingDate = readingDate;
    }

    public ElectricityMeter getElectricityMeter() {
        return electricityMeter;
    }

    public void setElectricityMeter(ElectricityMeter electricityMeter) {
        this.electricityMeter = electricityMeter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reading reading = (Reading) o;
        return Objects.equals(electricityMeter, reading.electricityMeter) && Objects.equals(id, reading.id) && Objects.equals(readingValue, reading.readingValue) && Objects.equals(readingDate, reading.readingDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(electricityMeter, id, readingValue, readingDate);
    }

    @Override
    public String toString() {
        var dateTemplate = format("<font color=\"#41474d\">%s</font>", getReadingDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        var readingValueTemplate = format("<font color=\"#151c16\"><b>%s</b></font>", readingValue);
        var kwhTemplate = format("<font size=1 color=\"%s\"><b>kwH</b></font>", StromZettel.COLOR_GREEN);
        return format("<html>%s | %s %s</html>", dateTemplate, readingValueTemplate, kwhTemplate);
    }
}
