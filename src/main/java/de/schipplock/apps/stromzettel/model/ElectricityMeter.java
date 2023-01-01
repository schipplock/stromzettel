package de.schipplock.apps.stromzettel.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "ELECTRICITY_METERS")
public class ElectricityMeter {

    @Id
    @SequenceGenerator(name = "electricity_meter_seq_gen", sequenceName = "electricity_meter_seq", initialValue = 1, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "electricity_meter_seq_gen")
    private Long id;

    private String name;

    protected double kwhPrice;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "electricityMeter", orphanRemoval = true)
    private List<Reading> readings = new ArrayList<>();

    public ElectricityMeter() {}

    public ElectricityMeter(String name, double kwhPrice) {
        this.name = name;
        this.kwhPrice = kwhPrice;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Reading> getReadings() {
        return readings;
    }

    public Reading getLatestReading() {
        return readings.get(readings.size()-1);
    }

    public Reading getPreviousReading(Reading referenceReading) {
        long referenceReadingId = referenceReading.getId();
        Reading previousReading = referenceReading;
        for (int i = readings.size()-1; i >= 0; i--) {
            var reading = readings.get(i);
            if (reading.getId() < referenceReadingId) {
                previousReading = readings.get(i);
                break;
            }
        }
        return previousReading;
    }

    public void setReadings(List<Reading> readings) {
        this.readings = readings;
    }

    public void addReading(Reading reading) {
        readings.add(reading);
    }

    public void removeReading(Reading reading) {
        readings.remove(reading);
    }

    public double getKwhPrice() {
        return kwhPrice;
    }

    public void setKwhPrice(double kwhPrice) {
        this.kwhPrice = kwhPrice;
    }

    @Override
    public String toString() {
        return "<html><b>" + name + "</b></html>";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ElectricityMeter that = (ElectricityMeter) o;
        return Double.compare(that.kwhPrice, kwhPrice) == 0 && Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(readings, that.readings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, kwhPrice);
    }
}
