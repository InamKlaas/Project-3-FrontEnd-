package com.cputhome.message;

import com.cputhome.common.AuditableEntity;
import com.cputhome.listing.Accommodation;
import com.cputhome.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/* one chat line, thread identity is (listing, student) */
@Entity
@Table(name = "messages")
public class Message extends AuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "listing_id", nullable = false)
  private Accommodation listing;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "student_id", nullable = false)
  private User student;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sender_id", nullable = false)
  private User sender;

  @Column(name = "body", nullable = false, length = 2000)
  private String text;

  protected Message() {
    /* jpa only */
  }

  public Message(Accommodation listing, User student, User sender, String text) {
    this.listing = listing;
    this.student = student;
    this.sender = sender;
    this.text = text;
  }

  public Accommodation getListing() {
    return listing;
  }

  public User getStudent() {
    return student;
  }

  public User getSender() {
    return sender;
  }

  public String getText() {
    return text;
  }
}
