package com.cputhome.user;

import com.cputhome.common.AuditableEntity;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/* landlord side of an account, verification is human-admin owned */
@Entity
@Table(name = "landlord_profiles")
public class LandlordProfile extends AuditableEntity {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "verification_status", nullable = false, length = 20)
  private VerificationStatus verificationStatus = VerificationStatus.PENDING;

  @Column(name = "registration_number", length = 60)
  private String registrationNumber;

  @Column(name = "proof_url", length = 500)
  private String proofUrl;

  @Column(name = "accreditation", nullable = false)
  private boolean accreditation = false;

  protected LandlordProfile() {
    /* jpa only */
  }

  public LandlordProfile(User user) {
    this.user = user;
  }

  public User getUser() {
    return user;
  }

  public VerificationStatus getVerificationStatus() {
    return verificationStatus;
  }

  public void setVerificationStatus(VerificationStatus verificationStatus) {
    this.verificationStatus = verificationStatus;
  }

  /* verified means an admin said so, nothing else flips this */
  public boolean isVerified() {
    return verificationStatus == VerificationStatus.VERIFIED;
  }

  public String getRegistrationNumber() {
    return registrationNumber;
  }

  public void setRegistrationNumber(String registrationNumber) {
    this.registrationNumber = registrationNumber;
  }

  public String getProofUrl() {
    return proofUrl;
  }

  public void setProofUrl(String proofUrl) {
    this.proofUrl = proofUrl;
  }

  public boolean isAccreditation() {
    return accreditation;
  }

  public void setAccreditation(boolean accreditation) {
    this.accreditation = accreditation;
  }

  public enum VerificationStatus {
    PENDING("pending"),
    VERIFIED("verified"),
    REJECTED("rejected");

    private final String wire;

    VerificationStatus(String wire) {
      this.wire = wire;
    }

    @JsonValue
    public String wire() {
      return wire;
    }
  }
}
