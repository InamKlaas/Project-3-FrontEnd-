package com.cputhome.listing;

import com.cputhome.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/* entire unit extras, only one subtype row per property */
@Entity
@Table(name = "entire_units")
public class EntireUnit extends AuditableEntity {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "accommodation_id", nullable = false, unique = true)
  private Accommodation accommodation;

  @Column(name = "bedrooms", nullable = false)
  private int bedrooms = 1;

  @Column(name = "bathrooms", nullable = false)
  private int bathrooms = 1;

  protected EntireUnit() {
    /* jpa only */
  }

  public EntireUnit(Accommodation accommodation, int bedrooms, int bathrooms) {
    this.accommodation = accommodation;
    this.bedrooms = bedrooms;
    this.bathrooms = bathrooms;
  }

  public Accommodation getAccommodation() {
    return accommodation;
  }

  public int getBedrooms() {
    return bedrooms;
  }

  public void setBedrooms(int bedrooms) {
    this.bedrooms = bedrooms;
  }

  public int getBathrooms() {
    return bathrooms;
  }

  public void setBathrooms(int bathrooms) {
    this.bathrooms = bathrooms;
  }
}
