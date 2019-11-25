package com.project.githubsearch.model;

public class ResolvedFile {
    private String url;
    private String pathFile;
    private int line;
    private int column;

    public ResolvedFile(String url, String pathFile, int line, int column) {
        this.url = url;
        this.pathFile = pathFile;
        this.line = line;
        this.column = column;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return the pathFile
     */
    public String getPathFile() {
        return pathFile;
    }

    /**
     * @return the line
     */
    public int getLine() {
        return line;
    }

    /**
     * @return the column
     */
    public int getColumn() {
        return column;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @param pathFile the pathFile to set
     */
    public void setPathFile(String pathFile) {
        this.pathFile = pathFile;
    }

    /**
     * @param line the line to set
     */
    public void setLine(int line) {
        this.line = line;
    }

    /**
     * @param column the column to set
     */
    public void setColumn(int column) {
        this.column = column;
    }
}