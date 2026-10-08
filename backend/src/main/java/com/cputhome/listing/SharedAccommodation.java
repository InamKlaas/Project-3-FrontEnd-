package com.cputhome.listing;

import com.cputhome.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/* shared accommodation extras, only one subtype row per property */
@Entity
@Table(name = "shared_accommodations")
public class SharedAccommodation extends AuditableEntity {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "accommodation_id", nullable = false, unique = true)
  private Accommodation accommodation;

  @Column(name = "total_beds", nullable = false)
  private int totalBeds = 2;

  @Column(name = "shared_kitchen", nullable = false)
  private boolean sharedKitchen = true;

  protected SharedAccommodation() {
    /* jpa only */
  }

  public SharedAccommodation(Accommodation accommodation, int totalBeds) {
    this.accommodation = accommodation;
    this.totalBeds = totalBeds;
  }

  public Accommodation getAccommodation() {
    return accommodation;
  }

  public int getTotalBeds() {
    return totalBeds;
  }

  public void setTotalBeds(int totalBeds) {
    this.totalBeds = totalBeds;
  }

  public boolean isSharedKitchen() {
    return sharedKitchen;
  }

  public void setSharedKitchen(boolean sharedKitchen) {
    this.sharedKitchen = sharedKitchen;
  }
}
