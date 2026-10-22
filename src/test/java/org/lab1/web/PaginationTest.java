package org.lab1.web;

import org.junit.jupiter.api.Test;
import org.lab1.dto.SliceResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaginationTest {

    @Test
    void of_keepsValidValues() {
        Pageable pageable = Pagination.of(2, 10, Sort.by("id"));

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.by("id"));
    }

    @Test
    void of_clampsSizeToMaximumAndMinimum() {
        assertThat(Pagination.of(0, 500, Sort.unsorted()).getPageSize()).isEqualTo(Pagination.MAX_PAGE_SIZE);
        assertThat(Pagination.of(0, 0, Sort.unsorted()).getPageSize()).isEqualTo(1);
        assertThat(Pagination.of(-5, 10, Sort.unsorted()).getPageNumber()).isZero();
    }

    @Test
    void sliceResponse_copiesSliceMetadata() {
        SliceImpl<String> slice = new SliceImpl<>(List.of("a", "b"), PageRequest.of(1, 2), true);

        SliceResponse<String> response = SliceResponse.from(slice);

        assertThat(response.content()).containsExactly("a", "b");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.hasNext()).isTrue();
    }
}
