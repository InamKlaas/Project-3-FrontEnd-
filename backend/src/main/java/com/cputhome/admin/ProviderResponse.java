package com.cputhome.admin;

import com.cputhome.user.LandlordProfile;
import com.cputhome.user.User;
import java.util.UUID;

/* landlord desk row, verification is the only thing that moves here */
public record ProviderResponse(
    Long userId,
    String email,
    String fullName,
    String status,
    String verificationStatus,
    boolean accreditation,
    String registrationNumber) {

  public static ProviderResponse from(User user, LandlordProfile profile) {
    return new ProviderResponse(
        user.getId(),
        user.getEmail(),
        user.getFullName(),
        user.getStatus().wire(),
        profile.getVerificationStatus().wire(),
        profile.isAccreditation(),
        profile.getRegistrationNumber());
  }
}
