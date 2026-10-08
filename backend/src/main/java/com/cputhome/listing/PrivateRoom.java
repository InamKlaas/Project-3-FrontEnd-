package com.cputhome.listing;

import com.cputhome.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/* private room extras, only one subtype row per property */
@Entity
@Table(name = "private_rooms")
public class PrivateRoom extends AuditableEntity {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "accommodation_id", nullable = false, unique = true)
  private Accommodation accommodation;

  @Column(name = "ensuite", nullable = false)
  private boolean ensuite = false;

  @Column(name = "furnished", nullable = false)
  private boolean furnished = true;

  protected PrivateRoom() {
    /* jpa only */
  }

  public PrivateRoom(Accommodation accommodation) {
    this.accommodation = accommodation;
  }

  public Accommodation getAccommodation() {
    return accommodation;
  }

  public boolean isEnsuite() {
    return ensuite;
  }

  public void setEnsuite(boolean ensuite) {
    this.ensuite = ensuite;
  }

  public boolean isFurnished() {
    return furnished;
  }

  public void setFurnished(boolean furnished) {
    this.furnished = furnished;
  }
}
