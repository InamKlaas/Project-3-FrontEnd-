package com.cputhome.auth;

import com.cputhome.common.BadRequestException;
import com.cputhome.common.ConflictException;
import com.cputhome.common.ForbiddenException;
import com.cputhome.common.UnauthorizedException;
import com.cputhome.security.JwtService;
import com.cputhome.user.LandlordProfile;
import com.cputhome.user.LandlordProfileRepository;
import com.cputhome.user.StudentProfile;
import com.cputhome.user.StudentProfileRepository;
import com.cputhome.user.User;
import com.cputhome.user.UserRepository;
import com.cputhome.user.UserRole;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/* registration + login live here, controllers stay thin */
@Service
public class AuthService {

  public static final String STUDENT_DOMAIN = "@mycput.ac.za";

  private final UserRepository users;
  private final StudentProfileRepository students;
  private final LandlordProfileRepository landlords;
  private final PasswordEncoder passwords;
  private final JwtService jwt;

  public AuthService(
      UserRepository users,
      StudentProfileRepository students,
      LandlordProfileRepository landlords,
      PasswordEncoder passwords,
      JwtService jwt) {
    this.users = users;
    this.students = students;
    this.landlords = landlords;
    this.passwords = passwords;
    this.jwt = jwt;
  }

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    String email = request.email().trim().toLowerCase(Locale.ROOT);
    UserRole role = request.parsedRole();
    if (role == null || role == UserRole.ADMIN) {
      /* unknown roles and public admins both stop here */
      throw new ForbiddenException("FORBIDDEN", "role is not allowed for public registration");
    }
    if (users.existsByEmail(email)) {
      throw new ConflictException("EMAIL_ALREADY_EXISTS", "account already exists");
    }

    User user = new User(request.fullName().trim(), email, passwords.encode(request.password()), role);
    if (role == UserRole.STUDENT) {
      if (!email.endsWith(STUDENT_DOMAIN)) {
        throw new BadRequestException("INVALID_STUDENT_EMAIL", "students must register with a CPUT email (@mycput.ac.za)");
      }
      if (request.studentNumber() == null || request.studentNumber().isBlank()) {
        throw new BadRequestException("VALIDATION_ERROR", "student number is required for students");
      }
      String number = request.studentNumber().trim();
      if (students.existsByStudentNumber(number)) {
        throw new ConflictException("STUDENT_NUMBER_ALREADY_EXISTS", "student number is already registered");
      }
      user.setStatus(User.UserStatus.PENDING_EMAIL);
      users.save(user);
      StudentProfile profile = new StudentProfile(user, number, campusOf(request));
      profile.setYearOfStudy(request.year());
      profile.setFunding(request.funding());
      students.save(profile);
    } else {
      user.setStatus(User.UserStatus.PENDING_VERIFICATION);
      users.save(user);
      landlords.save(new LandlordProfile(user));
    }
    return new AuthResponse(jwt.generate(user), me(user));
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest request) {
    String identifier = request.identifier().trim();
    User user;
    if (identifier.contains("@")) {
      user =
          users
              .findByEmail(identifier.toLowerCase(Locale.ROOT))
              .orElseThrow(
                  () -> new UnauthorizedException("INVALID_CREDENTIALS", "wrong email or password"));
    } else {
      user =
          students
              .findByStudentNumber(identifier)
              .map(StudentProfile::getUser)
              .orElseThrow(
                  () -> new UnauthorizedException("INVALID_CREDENTIALS", "wrong email or password"));
    }
    if (!user.isEnabled()) {
      throw new ForbiddenException("FORBIDDEN", "this account is suspended. contact an administrator");
    }
    if (!passwords.matches(request.password(), user.getPasswordHash())) {
      throw new UnauthorizedException("INVALID_CREDENTIALS", "wrong email or password");
    }
    return new AuthResponse(jwt.generate(user), me(user));
  }

  @Transactional(readOnly = true)
  public MeResponse me(com.cputhome.security.UserPrincipal principal) {
    User user =
        users.findById(principal.id()).orElseThrow(() -> new UnauthorizedException("UNAUTHORIZED", "authentication required"));
    return me(user);
  }

  private MeResponse me(User user) {
    String studentNumber = null;
    String campus = null;
    if (user.getRole() == UserRole.STUDENT) {
      StudentProfile profile = students.findByUserId(user.getId()).orElse(null);
      if (profile != null) {
        studentNumber = profile.getStudentNumber();
        campus = profile.getCampus();
      }
    }
    return new MeResponse(
        user.getId(),
        user.getFullName(),
        user.getEmail(),
        user.getRole(),
        user.getStatus().wire(),
        studentNumber,
        campus);
  }

  private static String campusOf(RegisterRequest request) {
    return request.campus() == null || request.campus().isBlank() ? "Bellville" : request.campus().trim();
  }
}
