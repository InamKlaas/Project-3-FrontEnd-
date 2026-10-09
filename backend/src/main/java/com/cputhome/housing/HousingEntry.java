package com.cputhome.housing;

import com.cputhome.common.AuditableEntity;
import com.cputhome.listing.Accommodation;
import com.cputhome.user.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "housing_entries", uniqueConstraints = @UniqueConstraint(columnNames = {"kind", "listing_id", "author_id"}))
public class HousingEntry extends AuditableEntity {
  @Column(nullable = false, length = 20) String kind;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "listing_id") Accommodation listing;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "author_id") User author;
  @Column(nullable = false, length = 2000) String note;
  @Column(nullable = false, length = 20) String status;
  Integer rating;
  @Column(name = "move_in") LocalDate moveIn;
  @ElementCollection
  @CollectionTable(name = "application_documents", joinColumns = @JoinColumn(name = "entry_id"))
  @Column(name = "document", length = 400)
  List<String> documents = new ArrayList<>();
  protected HousingEntry() {}
  HousingEntry(String kind, Accommodation listing, User author, String note) {
    this.kind = kind; this.listing = listing; this.author = author; this.note = note;
    this.status = kind.equals("APPLICATION") ? "submitted" : kind.equals("REPORT") ? "open" : "published";
  }
}
