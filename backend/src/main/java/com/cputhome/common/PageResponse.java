package com.cputhome.common;

import java.util.List;

/* every paged answer wears the same shape */
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last) {

  /* nobody pages a thousand rows in the POC */
  public static int capSize(int size) {
    return Math.min(Math.max(size, 1), 50);
  }
}
