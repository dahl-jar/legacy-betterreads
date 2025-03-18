package com.br.betterreads.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OpenLibraryTrendingResponse {


    private List<TrendingBook> works;
    private int limit;
    private int offset;

    public List<TrendingBook> getWorks() {return works;}

    public void setWorks(List<TrendingBook> works) {this.works = works;}

    public int getLimit() {return limit;}

    public void setLimit(int limit) {this.limit = limit;}

    public int getOffset() {return offset;}

    public void setOffset(int offset) {this.offset = offset;}


    public static class TrendingBook {
        private String key;
        private String title;
        @JsonProperty("author_name")
        private List<String> authorNames;
        @JsonProperty("first_published_year")
        private Integer firstPublishedYear;
        @JsonProperty("cover_i")
        private Integer coverId;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public List<String> getAuthorNames() {
            return authorNames;
        }

        public void setAuthorNames(List<String> authorNames) {
            this.authorNames = authorNames;
        }

        public Integer getFirstPublishedYear() {
            return firstPublishedYear;
        }

        public void setFirstPublishedYear(Integer firstPublishedYear) {
            this.firstPublishedYear = firstPublishedYear;
        }

        public Integer getCoverId() {
            return coverId;
        }

        public void setCoverId(Integer coverId) {
            this.coverId = coverId;
        }

        public String getCoverUrl() {
            if(coverId!=null) {
                return "https://covers.openlibrary.org/b/id/" + coverId + "-M.jpg";
            }
            return "https://www.nypl.org/scout/_next/image?url=https%3A%2F%2Fdrupal.nypl.org%2Fsites-drupal%2Fdefault%2Ffiles%2Fstyles%2Fmax_width_960%2Fpublic%2Fblogs%2FJ5LVHEL.jpg%3Fitok%3DDkMp1Irh&w=3840&q=90";
        }
    }

}
