package com.kludgeworks.mcp.sparql.query;

import org.jspecify.annotations.Nullable;

/** One page of a backend response, so large results fit within an MCP client's context. */
public record PagedResult(Pagination pagination, String result) {

  public static final int DEFAULT_PAGE_SIZE_CHARS = 5000;

  public record Pagination(
      int totalChars,
      int pageSizeChars,
      int totalPages,
      int pageIndex,
      int offset,
      int returnedChars) {}

  /**
   * Cuts the page starting at {@code offset}. A missing or non-positive {@code maxChars} uses the
   * default page size, and a missing or negative {@code offset} starts at the beginning.
   */
  static PagedResult of(String responseBody, @Nullable Integer maxChars, @Nullable Integer offset) {
    int pageSizeChars = (maxChars == null || maxChars <= 0) ? DEFAULT_PAGE_SIZE_CHARS : maxChars;
    int effectiveOffset = (offset == null || offset < 0) ? 0 : offset;
    int totalChars = responseBody.length();
    int totalPages = totalChars == 0 ? 1 : (int) Math.ceil((double) totalChars / pageSizeChars);
    int pageIndex = effectiveOffset / pageSizeChars;
    int start = Math.min(effectiveOffset, totalChars);
    int end = Math.min(start + pageSizeChars, totalChars);
    String resultChunk = responseBody.substring(start, end);

    return new PagedResult(
        new Pagination(
            totalChars,
            pageSizeChars,
            totalPages,
            pageIndex,
            effectiveOffset,
            resultChunk.length()),
        resultChunk);
  }
}
