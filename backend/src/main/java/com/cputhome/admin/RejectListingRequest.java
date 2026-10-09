package com.cputhome.admin;

/* rejection reason rides along, blank means the admin gave none */
public record RejectListingRequest(@jakarta.validation.constraints.Size(max = 500) String reason) {}
