package com.cputhome.listing;

import com.cputhome.common.AuditableEntity;
import com.cputhome.user.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/* a property, never flattened — rooms hang off it as bookable units */
@Entity
@Table(name = "accommodations")
public class Accommodation extends AuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @Column(name = "title", nullable = false, length = 150)
  private String title;

  @Column(name = "description", nullable = false, length = 4000)
  private String description;

  @Column(name = "location", nullable = false, length = 200)
  private String location;

  @Column(name = "campus", nullable = false, length = 80)
  private String campus;

  @Column(name = "address", length = 300)
  private String address;

  @Column(name = "on_campus", nullable = false)
  private boolean onCampus = false;

  @Column(name = "nsfas_claim", nullable = false)
  private boolean nsfasClaim = false;

  @Column(name = "gender", length = 20)
  private String gender = "Any";

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "accommodation_amenities", joinColumns = @JoinColumn(name = "accommodation_id"))
  @Column(name = "amenity", length = 60)
  private List<String> amenities = new ArrayList<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "accommodation_images", joinColumns = @JoinColumn(name = "accommodation_id"))
  @Column(name = "image_url", length = 500)
  @OrderBy("image_url")
  private List<String> imageUrls = new ArrayList<>();

  @Column(name = "house_rules", length = 2000)
  private String houseRules;

  @Column(name = "shuttle", length = 500)
  private String shuttle;

  @Column(name = "utilities", precision = 12, scale = 2)
  private BigDecimal utilities = BigDecimal.ZERO;

  /* synthetic seed rows say so, provider rows never do */
  @Column(name = "sample", nullable = false)
  private boolean sample = false;

  /* admin-controlled publish, landlord-controlled activity — independent */
  @Column(name = "is_published", nullable = false)
  private boolean published = false;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  @Column(name = "approval_status", nullable = false, length = 20)
  private String approvalStatus = "pending";

  @Column(name = "rejection_reason", length = 500)
  private String rejectionReason;

  @OneToMany(mappedBy = "accommodation", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
  private List<RoomListing> rooms = new ArrayList<>();

  protected Accommodation() {
    /* jpa only */
  }

  public Accommodation(User owner, String title, String description, String location, String campus) {
    this.owner = owner;
    this.title = title;
    this.description = description;
    this.location = location;
    this.campus = campus;
  }

  public User getOwner() {
    return owner;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getCampus() {
    return campus;
  }

  public void setCampus(String campus) {
    this.campus = campus;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public boolean isOnCampus() {
    return onCampus;
  }

  public void setOnCampus(boolean onCampus) {
    this.onCampus = onCampus;
  }

  public boolean isNsfasClaim() {
    return nsfasClaim;
  }

  public void setNsfasClaim(boolean nsfasClaim) {
    this.nsfasClaim = nsfasClaim;
  }

  public String getGender() {
    return gender;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  public List<String> getAmenities() {
    return amenities;
  }

  public void setAmenities(List<String> amenities) {
    this.amenities = amenities;
  }

  public List<String> getImageUrls() {
    return imageUrls;
  }

  public void setImageUrls(List<String> imageUrls) {
    this.imageUrls = imageUrls;
  }

  public String getHouseRules() {
    return houseRules;
  }

  public void setHouseRules(String houseRules) {
    this.houseRules = houseRules;
  }

  public String getShuttle() {
    return shuttle;
  }

  public void setShuttle(String shuttle) {
    this.shuttle = shuttle;
  }

  public BigDecimal getUtilities() {
    return utilities;
  }

  public void setUtilities(BigDecimal utilities) {
    this.utilities = utilities;
  }

  public boolean isSample() {
    return sample;
  }

  public void setSample(boolean sample) {
    this.sample = sample;
  }

  public boolean isPublished() {
    return published;
  }

  public void setPublished(boolean published) {
    this.published = published;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public String getApprovalStatus() {
    return approvalStatus;
  }

  public void setApprovalStatus(String approvalStatus) {
    this.approvalStatus = approvalStatus;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public void setRejectionReason(String rejectionReason) {
    this.rejectionReason = rejectionReason;
  }

  public List<RoomListing> getRooms() {
    return rooms;
  }
}
