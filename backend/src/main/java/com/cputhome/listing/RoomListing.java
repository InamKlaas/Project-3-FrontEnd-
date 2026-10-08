package com.cputhome.listing;

import com.cputhome.common.AuditableEntity;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/* the bookable unit, price and availability live here, not on the property */
@Entity
@Table(name = "room_listings")
public class RoomListing extends AuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "accommodation_id", nullable = false)
  private Accommodation accommodation;

  @Enumerated(EnumType.STRING)
  @Column(name = "room_type", nullable = false, length = 30)
  private RoomType roomType;

  @Column(name = "monthly_rent", nullable = false, precision = 12, scale = 2)
  private BigDecimal monthlyRent;

  @Column(name = "deposit", precision = 12, scale = 2)
  private BigDecimal deposit = BigDecimal.ZERO;

  @Column(name = "beds", nullable = false)
  private int beds = 1;

  @Column(name = "available", nullable = false)
  private boolean available = true;

  @Column(name = "available_date")
  private LocalDate availableDate;

  @Column(name = "emergency", nullable = false)
  private boolean emergency = false;

  protected RoomListing() {
    /* jpa only */
  }

  public RoomListing(Accommodation accommodation, RoomType roomType, BigDecimal monthlyRent) {
    this.accommodation = accommodation;
    this.roomType = roomType;
    this.monthlyRent = monthlyRent;
  }

  public Accommodation getAccommodation() {
    return accommodation;
  }

  public RoomType getRoomType() {
    return roomType;
  }

  public void setRoomType(RoomType roomType) {
    this.roomType = roomType;
  }

  public BigDecimal getMonthlyRent() {
    return monthlyRent;
  }

  public void setMonthlyRent(BigDecimal monthlyRent) {
    this.monthlyRent = monthlyRent;
  }

  public BigDecimal getDeposit() {
    return deposit;
  }

  public void setDeposit(BigDecimal deposit) {
    this.deposit = deposit;
  }

  public int getBeds() {
    return beds;
  }

  public void setBeds(int beds) {
    this.beds = beds;
  }

  public boolean isAvailable() {
    return available;
  }

  public void setAvailable(boolean available) {
    this.available = available;
  }

  public LocalDate getAvailableDate() {
    return availableDate;
  }

  public void setAvailableDate(LocalDate availableDate) {
    this.availableDate = availableDate;
  }

  public boolean isEmergency() {
    return emergency;
  }

  public void setEmergency(boolean emergency) {
    this.emergency = emergency;
  }

  /* normalized room kinds, UI shows Single/Sharing/Bachelor */
  public enum RoomType {
    SHARED_ACCOMMODATION("Sharing"),
    PRIVATE_ROOM("Single"),
    ENTIRE_UNIT("Bachelor");

    private final String ui;

    RoomType(String ui) {
      this.ui = ui;
    }

    @JsonValue
    public String ui() {
      return ui;
    }
  }
}
