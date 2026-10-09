package com.cputhome.housing;

import com.cputhome.common.*;
import com.cputhome.listing.*;
import com.cputhome.security.UserPrincipal;
import com.cputhome.user.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class HousingService {
  private final HousingEntryRepository entries;
  private final AccommodationRepository listings;
  private final UserRepository users;
  private final Path folder;
  public HousingService(HousingEntryRepository entries, AccommodationRepository listings, UserRepository users,
      @Value("${app.media.upload-dir:./uploads}") String folder) {
    this.entries = entries; this.listings = listings; this.users = users;
    this.folder = Path.of(folder).toAbsolutePath().resolve("applications").normalize();
  }
  public record Document(String name, String url) {}
  public record Entry(Long id, Long listingId, String title, String student, String name, String note,
      String status, Integer rating, LocalDate moveIn, Instant createdAt, List<Document> documents) {}
  public record Review(Long id, String name, int rating, String text, Instant createdAt) {}
  Entry view(HousingEntry row) {
    return new Entry(row.getId(), row.listing.getId(), row.listing.getTitle(), row.author.getEmail(),
        row.author.getFullName(), row.note, row.status, row.rating, row.moveIn, row.getCreatedAt(),
        row.documents.stream().map(value -> {
          String[] parts = value.split("\\|", 2);
          return new Document(parts[1], "/applications/" + row.getId() + "/documents/" + parts[0]);
        }).toList());
  }
  public Entry create(String kind, Long id, UserPrincipal principal, String note, Integer rating, LocalDate moveIn) {
    if (principal.role() != UserRole.STUDENT) throw new ForbiddenException("FORBIDDEN", "only students may submit this request");
    Accommodation listing = listings.findById(id).orElseThrow(() -> new NotFoundException("listing not found"));
    if (!listing.isActive() || !listing.isPublished() || !"approved".equals(listing.getApprovalStatus()) || !listing.getOwner().isEnabled())
      throw new NotFoundException("listing is not publicly available");
    if (kind.equals("APPLICATION") && listing.getRooms().stream().noneMatch(RoomListing::isAvailable))
      throw new BadRequestException("UNAVAILABLE", "no rooms are currently available");
    if (note == null || note.trim().length() < 3 || note.length() > 2000)
      throw new BadRequestException("VALIDATION_ERROR", "enter between 3 and 2000 characters");
    if (kind.equals("REVIEW") && (rating == null || rating < 1 || rating > 5))
      throw new BadRequestException("VALIDATION_ERROR", "rating must be between 1 and 5");
    if (kind.equals("APPLICATION") && (moveIn == null || moveIn.isBefore(LocalDate.now())))
      throw new BadRequestException("VALIDATION_ERROR", "choose a move-in date from today onwards");
    if (entries.existsByKindAndListingIdAndAuthorId(kind, id, principal.id()))
      throw new ConflictException("ALREADY_SUBMITTED", "you have already submitted this for the residence");
    User author = users.findById(principal.id()).orElseThrow(() -> new NotFoundException("user not found"));
    HousingEntry row = new HousingEntry(kind, listing, author, note.trim()); row.rating = rating; row.moveIn = moveIn;
    return view(entries.save(row));
  }
  @Transactional(readOnly = true)
  public List<Entry> inbox(String kind, UserPrincipal principal) {
    return entries.findByKindOrderByCreatedAtDesc(kind).stream().filter(row -> visible(row, principal)).map(this::view).toList();
  }
  private boolean visible(HousingEntry row, UserPrincipal principal) {
    return principal.role() == UserRole.ADMIN || row.author.getId().equals(principal.id()) ||
        (row.kind.equals("APPLICATION") && row.listing.getOwner().getId().equals(principal.id()));
  }
  private HousingEntry entry(Long id, String kind) {
    HousingEntry row = entries.findById(id).orElseThrow(() -> new NotFoundException("request not found"));
    if (!kind.equals(row.kind)) throw new NotFoundException("request not found"); return row;
  }
  public Entry status(Long id, String kind, String status, UserPrincipal principal) {
    if (status == null) throw new BadRequestException("VALIDATION_ERROR", "status is required");
    HousingEntry row = entry(id, kind);
    boolean admin = principal.role() == UserRole.ADMIN;
    boolean owner = row.listing.getOwner().getId().equals(principal.id());
    boolean author = row.author.getId().equals(principal.id());
    if (kind.equals("REPORT")) {
      if (!admin) throw new ForbiddenException("FORBIDDEN", "only admins may resolve concerns");
      if (!Set.of("resolved", "dismissed").contains(status)) throw new BadRequestException("VALIDATION_ERROR", "invalid report status");
    } else {
      if (author && "withdrawn".equals(status)) { row.status = status; return view(row); }
      if (!admin && !owner) throw new ForbiddenException("FORBIDDEN", "only the provider or admin may decide an application");
      if ("withdrawn".equals(row.status)) throw new ConflictException("WITHDRAWN", "this application was withdrawn");
      if (!Set.of("accepted", "declined", "waitlisted").contains(status)) throw new BadRequestException("VALIDATION_ERROR", "invalid application status");
    }
    row.status = status; return view(row);
  }
  @Transactional(readOnly = true)
  public List<Review> reviews(Long id) {
    Accommodation listing = listings.findById(id).orElseThrow(() -> new NotFoundException("listing not found"));
    if (!listing.isPublished() || !listing.isActive() || !"approved".equals(listing.getApprovalStatus()) || !listing.getOwner().isEnabled())
      throw new NotFoundException("listing not found");
    return entries.findByKindAndListingIdOrderByCreatedAtDesc("REVIEW", id).stream()
        .map(row -> new Review(row.getId(), row.author.getFullName(), row.rating, row.note, row.getCreatedAt())).toList();
  }
  public Entry upload(Long id, MultipartFile file, UserPrincipal principal) throws IOException {
    HousingEntry row = entry(id, "APPLICATION");
    if (!row.author.getId().equals(principal.id())) throw new ForbiddenException("FORBIDDEN", "only the applicant may upload documents");
    if (Set.of("withdrawn", "declined").contains(row.status)) throw new ConflictException("CLOSED", "this application is closed");
    if (row.documents.size() >= 3) throw new BadRequestException("DOCUMENT_LIMIT", "up to three documents per application");
    if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) throw new BadRequestException("FILE_SIZE", "documents must be between 1 byte and 5 MB");
    byte[] bytes = file.getBytes();
    String ext = bytes.length > 4 && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F' ? ".pdf" :
        bytes.length > 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255 ? ".jpg" :
        bytes.length > 8 && (bytes[0] & 255) == 137 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G' ? ".png" : null;
    if (ext == null) throw new BadRequestException("FILE_TYPE", "upload a PDF, JPEG or PNG document");
    String name = Optional.ofNullable(file.getOriginalFilename()).orElse("document" + ext).replaceAll("[^a-zA-Z0-9._ -]", "_");
    if (name.length() > 180) name = name.substring(name.length() - 180);
    String stored = UUID.randomUUID() + ext;
    Files.createDirectories(folder); Files.write(folder.resolve(stored), bytes, StandardOpenOption.CREATE_NEW);
    row.documents.add(stored + "|" + name); return view(row);
  }
  @Transactional(readOnly = true)
  public byte[] download(Long id, String filename, UserPrincipal principal) throws IOException {
    HousingEntry row = entry(id, "APPLICATION");
    if (!visible(row, principal)) throw new ForbiddenException("FORBIDDEN", "document access denied");
    if (row.documents.stream().noneMatch(value -> value.startsWith(filename + "|"))) throw new NotFoundException("document not found");
    Path path = folder.resolve(filename).normalize();
    if (!path.getParent().equals(folder)) throw new NotFoundException("document not found");
    return Files.readAllBytes(path);
  }
}
