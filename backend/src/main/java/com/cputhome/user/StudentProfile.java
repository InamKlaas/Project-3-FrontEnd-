package com.cputhome.user;

import com.cputhome.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/* student side of an account, one row per student user */
@Entity
@Table(name = "student_profiles")
public class StudentProfile extends AuditableEntity {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(name = "student_number", nullable = false, unique = true, length = 30)
  private String studentNumber;

  @Column(name = "campus", nullable = false, length = 80)
  private String campus;

  @Column(name = "year_of_study", length = 10)
  private String yearOfStudy;

  @Column(name = "funding", length = 30)
  private String funding;

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified = false;

  protected StudentProfile() {
    /* jpa only */
  }

  public StudentProfile(User user, String studentNumber, String campus) {
    this.user = user;
    this.studentNumber = studentNumber;
    this.campus = campus;
  }

  public User getUser() {
    return user;
  }

  public String getStudentNumber() {
    return studentNumber;
  }

  public String getCampus() {
    return campus;
  }

  public void setCampus(String campus) {
    this.campus = campus;
  }

  public String getYearOfStudy() {
    return yearOfStudy;
  }

  public void setYearOfStudy(String yearOfStudy) {
    this.yearOfStudy = yearOfStudy;
  }

  public String getFunding() {
    return funding;
  }

  public void setFunding(String funding) {
    this.funding = funding;
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }

  public void setEmailVerified(boolean emailVerified) {
    this.emailVerified = emailVerified;
  }
}
