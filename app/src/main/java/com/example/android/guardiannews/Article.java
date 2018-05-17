package com.example.android.guardiannews;

public class Article {

    private String headline;
    private String thumbnail;
    private String author;
    private String webUrl;
    private String category;
    private String webPublicationDate;

    public Article (String headline, String thumbnail, String author, String webUrl,
                    String category, String webPublicationDate) {
        this.headline = headline;
        this.thumbnail = thumbnail;
        this.author = author;
        this.webUrl = webUrl;
        this.category = category;
        this.webPublicationDate = webPublicationDate;
    }

    public String getHeadline() {
        return headline;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public String getAuthor() {
        return author;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public String getCategory() {
        return category;
    }

    public String getWebPublicationDate() {
        return webPublicationDate;
    }

}
